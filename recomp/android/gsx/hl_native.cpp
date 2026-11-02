// The native fight renderer of Hidden Leaf. HL_NATIVE=1 turns it on.
//
// In a fight the game's VU1 sends about 60 thousand vertices a frame, and the GS thread of PCSX2 spends more on parsing them,
// on its texture cache and on working out what each draw means than the Vulkan draws cost. But a fight uses very little of
// the GS (HL_GSCAP + tools/gscap.py): one 512x448 frame buffer, three blend modes, no DATE, no dither, no copies, and nearly
// every texture is uploaded right before the draw that uses it. So here, on the replay thread (a small core is enough), the
// stream is read before the GS sees it:
//
//   - a draw this file understands becomes vertices, indices and a state key in a frame, which the GS thread then draws
//     directly into the same targets PCSX2's GS uses (its texture cache hands them over), in stream order;
//   - anything else (classify() says what) goes on to PCSX2's GS, with the registers and the uploads it missed before it.
//
// What the screen shows still goes through PCSX2's output (Merge, the display), and so does every scene where this is off.
#include "hl_native.h"

#include "GS/GS.h"
#include "GS/GSState.h"
#include "GS/Renderers/Common/GSRenderer.h"
#include "GS/Renderers/HW/GSRendererHW.h"
#include "GS/Renderers/HW/GSTextureCache.h"
#include "GS/Renderers/Vulkan/GSDeviceVK.h"
#include "GS/Renderers/Vulkan/GSTextureVK.h"
#include "GS/Renderers/Vulkan/VKBuilders.h"
#include "GS/Renderers/Vulkan/VKShaderCache.h"
#include "GS/Renderers/Vulkan/VKStreamBuffer.h"
#include "Config.h"
#include "MTGS.h"

#include <cmath>

#include <sched.h>

#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <mutex>
#include <string>
#include <thread>
#include <unordered_map>
#include <vector>

namespace hl_native
{
namespace
{
	// ---- GS registers
	enum : uint32_t
	{
		R_PRIM = 0x00, R_RGBAQ = 0x01, R_ST = 0x02, R_UV = 0x03, R_XYZF2 = 0x04, R_XYZ2 = 0x05, R_TEX0 = 0x06, R_CLAMP = 0x08,
		R_FOG = 0x0A, R_XYZF3 = 0x0C, R_XYZ3 = 0x0D, R_TEX1 = 0x14, R_TEX2 = 0x16, R_XYOFFSET = 0x18, R_PRMODECONT = 0x1A,
		R_PRMODE = 0x1B, R_TEXCLUT = 0x1C, R_SCANMSK = 0x22, R_MIPTBP1 = 0x34, R_MIPTBP2 = 0x36, R_TEXA = 0x3B, R_FOGCOL = 0x3D,
		R_TEXFLUSH = 0x3F, R_SCISSOR = 0x40, R_ALPHA = 0x42, R_DIMX = 0x44, R_DTHE = 0x45, R_COLCLAMP = 0x46, R_TEST = 0x47,
		R_PABE = 0x49, R_FBA = 0x4A, R_FRAME = 0x4C, R_ZBUF = 0x4E, R_BITBLTBUF = 0x50, R_TRXPOS = 0x51, R_TRXREG = 0x52,
		R_TRXDIR = 0x53,
	};
	// (packed registers of a GIF tag)
	enum : uint32_t { P_PRIM = 0, P_RGBA = 1, P_STQ = 2, P_UV = 3, P_XYZF2 = 4, P_XYZ2 = 5, P_FOG = 0xA, P_AD = 0xE, P_NOP = 0xF };
	enum : uint32_t { PSM_CT32 = 0x00, PSM_T8 = 0x13, PSM_T4 = 0x14, PSM_T8H = 0x1B, PSM_T4HL = 0x24, PSM_T4HH = 0x2C };

	// A fight's targets. The scene (page 0, Z at page 0xF0) is PCSX2's own target: it goes on from there, and the copy of the
	// finished frame to page 0x70 that the display reads stays with it. The rest of what a frame does (shadows, bloom) happens
	// in smaller targets that only these same passes read, and those are private images here:
	//
	//   - shadows, page 0x1C0, 256x256 for the whole 512x448: shadow volumes add or take 1 from red where they pass the Z test,
	//     against a copy of the scene's Z the game shrinks to page 0x1E0. Here the image has the scene's size and the test is
	//     against the scene's Z itself: same pixels, finer, and no copy. It is read as CT24 with TEXA's AEM: no colour, no shade;
	//   - bloom, page 0x1C0 again, then pages 0x1E0 and 0x1E8 (128 and 64): shrink, blur, and added back to the scene.
	constexpr uint32_t kSceneFbp = 0, kSceneFbw = 8, kSceneZbp = 0xF0, kPostFbp = 0x1C0, kHalfFbp = 0x1E0, kQuarterFbp = 0x1E8, kDisplayFbp = 0x70;
	constexpr int kSceneW = 512, kSceneH = 448, kDisplayW = 512, kDisplayH = 512;
	enum Target : uint8_t { kTScene, kTShadow, kTBloom, kTHalf, kTQuarter, kTDisplay, kTargets };
	constexpr int32_t texOfTarget(int target) { return -2 - target; }
	constexpr int targetOfTex(int32_t tex) { return -2 - tex; }

	// ---- what a frame is made of
	struct Vertex
	{
		float x, y, z;
		float s, t, q;
		uint32_t rgba;
		uint32_t fog;
	};
	static_assert(sizeof(Vertex) == 32);

	enum Blend : uint8_t { kBlendAlpha, kBlendSub, kBlendAdd, kBlendNone, kBlendZOnly, kBlends };
	enum ZMode : uint8_t { kZAlways, kZGequal, kZGreater, kZModes };
	enum AMode : uint8_t { kANone, kAKeep, kAFbOnly }; // (alpha test: none; failing is dropped; failing writes colour, not Z)

	struct DrawKey
	{
		int32_t tex;       // -1: no texture; below that, a target (texOfTarget)
		uint32_t fogcol;
		uint16_t sc[4];    // scissor: x0, y0, x1, y1 (inclusive, as the GS)
		uint8_t blend, zmode, zwrite, amode;
		uint8_t aref, sampler, textured, target; // (textured: 0 no, 1 yes, 2 with alpha from the vertex, 3 the shadow's CT24)
		bool operator==(const DrawKey& o) const { return std::memcmp(this, &o, sizeof(DrawKey)) == 0; }
	};
	static_assert(sizeof(DrawKey) == 24);

	struct Draw
	{
		DrawKey key;
		uint32_t first, count;
	};

	struct NewTexture
	{
		int32_t id;
		int w, h;
		std::vector<uint8_t> rgba;
		// (or read from the GS's memory when the GS thread gets to it: see readFromMemory)
		bool fromMemory = false;
		uint32_t tbp = 0, tbw = 0, psm = 0, cbp = 0;
	};

	struct Frame
	{
		std::vector<Vertex> vertices;
		std::vector<uint32_t> indices;
		std::vector<Draw> draws;
		std::vector<NewTexture> textures;
		void clear()
		{
			vertices.clear();
			indices.clear();
			draws.clear();
			textures.clear();
		}
	};

	// What was uploaded to the GS's memory: by base block and format, with its bytes (in the upload's own order)
	struct Upload
	{
		uint64_t bitbltbuf = 0, trxpos = 0, trxreg = 0;
		std::vector<uint8_t> bytes;
		uint64_t hash = 0;
		uint32_t w = 0, h = 0, psm = 0;
		bool hashed = false, complete = false, pendingForGs = false;
	};

	// ---- the replay thread's side
	struct Op
	{
		Frame* frame = nullptr;       // draw this frame on the GS thread, or
		uint8_t path = 0;             // send these bytes to the GS
		std::vector<uint8_t> bytes;
	};

	bool s_on = false, s_sceneOn = false, s_wholeTags = true;
	struct DrawMatch
	{
		uint32_t bFirst, aFirst, count;
	};
	std::vector<DrawMatch> s_matches;
	std::atomic<int> s_rate{50};
	std::atomic<bool> s_presentsHere{false};
	std::atomic<int> s_presentRate{0};
	std::atomic<bool> s_sceneOnShared{false};
	// widescreen: the picture's shape while drawing here (0 stretched to the screen), and the HUD's own squeeze: the 3D is wider
	// because the game's camera sees more, but the HUD is flat and would be stretched with the rest.
	float s_shape = 4.0f / 3.0f, s_hudScale = 1.0f;
	bool s_hudDraw = false;
	// HL_NATIVE_CAP=<file>:<vsync>:<count>: what goes to the GS from then on, as HL_GSCAP (and a record 0xFE for each frame drawn here)
	FILE* s_cap = nullptr;
	uint64_t s_capFrom = 0, s_capUntil = 0;
	uint64_t s_from = 0, s_vsync = 0;
	uint64_t s_regs[256] = {};
	bool s_seen[256] = {};
	bool s_gsInSync = true;
	uint32_t s_prim = 0;

	// the current vertex
	float s_s = 0, s_t = 0, s_q = 1;
	uint32_t s_rgba = 0x80808080u, s_f = 0;
	uint32_t s_u = 0, s_v = 0;
	uint32_t s_kicks = 0;           // since the primitive started
	uint32_t s_i0 = 0, s_i1 = 0;    // indices of the last two
	Vertex s_spriteFirst{};

	enum TextureUse : uint8_t { kTexNone, kTexModulate, kTexNoAlpha, kTexShadow };
	enum Verdict : uint8_t { kNative, kToGs, kDrop };
	bool s_dirty = true;
	Verdict s_verdict = kToGs;
	DrawKey s_key{};
	const char* s_why = "";
	float s_ofx = 0, s_ofy = 0;
	uint32_t s_tw = 1, s_th = 1;
	bool s_fge = false, s_fst = false, s_tme = false;
	uint32_t s_type = 0;

	Frame* s_frame = nullptr;
	std::vector<Frame*> s_framePool;
	std::mutex s_poolLock;
	std::atomic<int> s_inFlight{0};
	std::deque<Op> s_ops;
	std::vector<uint8_t> s_outBytes;

	// uploads
	constexpr int kPsmClasses = 6;
	std::array<std::unordered_map<uint32_t, Upload*>, kPsmClasses> s_uploads;
	Upload* s_current = nullptr;    // the IMAGE being received
	uint32_t s_currentNeed = 0;
	std::vector<Upload*> s_pending; // uploads the GS has not seen, in order

	// textures
	struct TexKey
	{
		uint64_t texHash, clutHash;
		uint32_t psm, size;
		bool operator==(const TexKey& o) const { return texHash == o.texHash && clutHash == o.clutHash && psm == o.psm && size == o.size; }
	};
	struct TexKeyHash
	{
		size_t operator()(const TexKey& k) const { return static_cast<size_t>(k.texHash ^ (k.clutHash * 31u) ^ (static_cast<uint64_t>(k.psm) << 40) ^ k.size); }
	};
	std::unordered_map<TexKey, int32_t, TexKeyHash> s_textureIds;
	uint32_t s_memoryGeneration = 0; // (a copy inside the GS's memory may have changed what the textures read from it hold)
	int32_t s_nextTexture = 0;

	// counters for the log
	struct Stats
	{
		uint64_t frames = 0, draws = 0, verts = 0, tris = 0, routedTags = 0, dropped = 0, miss = 0, uploads = 0;
		std::unordered_map<const char*, uint32_t> why;
	} s_stats;

	int psmClass(uint32_t psm)
	{
		switch (psm)
		{
			case PSM_CT32: return 0;
			case PSM_T8: return 1;
			case PSM_T4: return 2;
			case PSM_T8H: return 3;
			case PSM_T4HH: return 4;
			case PSM_T4HL: return 5;
			default: return -1;
		}
	}

	uint64_t hashBytes(const uint8_t* data, size_t size)
	{
		uint64_t h = 0x9E3779B97F4A7C15ull ^ size;
		size_t i = 0;
		for (; i + 8 <= size; i += 8)
		{
			uint64_t w;
			std::memcpy(&w, data + i, 8);
			h = (h ^ w) * 0xFF51AFD7ED558CCDull;
			h = (h << 31) | (h >> 33);
		}
		for (; i < size; ++i)
			h = (h ^ data[i]) * 0x100000001B3ull;
		return h ^ (h >> 29);
	}

	uint64_t hashOf(Upload& u)
	{
		if (!u.hashed)
		{
			u.hash = hashBytes(u.bytes.data(), u.bytes.size());
			u.hashed = true;
		}
		return u.hash;
	}

	void pushBytes(uint8_t path, const void* data, size_t size)
	{
		if (s_ops.empty() || s_ops.back().frame || s_ops.back().path != path)
		{
			s_ops.emplace_back();
			s_ops.back().path = path;
		}
		std::vector<uint8_t>& out = s_ops.back().bytes;
		const size_t at = out.size();
		out.resize(at + size);
		std::memcpy(out.data() + at, data, size);
	}

	Frame* takeFrame()
	{
		// (the GS thread gives them back; no more than a few waiting, or the game runs ahead of what is on screen)
		while (s_inFlight.load(std::memory_order_acquire) >= 4)
			std::this_thread::sleep_for(std::chrono::microseconds(200));
		Frame* f;
		{
			std::lock_guard<std::mutex> lock(s_poolLock);
			if (s_framePool.empty())
				f = new Frame();
			else
			{
				f = s_framePool.back();
				s_framePool.pop_back();
			}
		}
		f->clear();
		f->vertices.reserve(96u * 1024u);
		f->indices.reserve(256u * 1024u);
		f->draws.reserve(2048u);
		return f;
	}

	// ---- the game's button prompts ("Mash", "Unleash Ultimate Jutsu", the sequences of a jutsu): what the player sees is what
	// the player must press, so they are read from what is drawn: which icon of the game's own sheets, left to right.
	// Codes: 1 circle 2 triangle 3 square 4 cross 5 up 6 right 7 down 8 left. In nibbles from bit 4, the count in the lowest.
	constexpr uint64_t kPromptSheet = 0xb4632ed2e6242b9full, kPromptClut = 0x4c8bd97dcf4bac6cull;
	constexpr uint64_t kSequenceSheet = 0xc69da492afd2ac87ull, kSequenceClut = 0xdf797daaa20abe4cull;
	int32_t s_promptTex = -1, s_sequenceTex = -1;
	std::atomic<uint32_t> s_prompt{0};

	void updatePrompt(const Frame& f)
	{
		if (s_promptTex < 0 && s_sequenceTex < 0)
			return;
		struct Icon
		{
			float x;
			uint32_t code;
		};
		Icon icons[7];
		uint32_t count = 0u;
		for (const Draw& d : f.draws)
		{
			if (d.key.tex < 0 || (d.key.tex != s_promptTex && d.key.tex != s_sequenceTex))
				continue;
			for (uint32_t q = 0; q + 6u <= d.count && count < 7u; q += 6u)
			{
				float s0 = 1e9f, t0 = 1e9f, x0 = 1e9f, x1 = -1e9f;
				for (uint32_t k = d.first + q; k < d.first + q + 6u; ++k)
				{
					const Vertex& v = f.vertices[f.indices[k]];
					s0 = std::min(s0, v.s);
					t0 = std::min(t0, v.t);
					x0 = std::min(x0, v.x);
					x1 = std::max(x1, v.x);
				}
				uint32_t code = 0u;
				if (d.key.tex == s_promptTex)
				{
					// (rows of 24 of 128: cross, square, triangle, circle; the row above is an ornament)
					const int row = static_cast<int>((t0 + 0.01f) / 0.1875f);
					code = row == 4 ? 1u : row == 3 ? 2u : row == 2 ? 3u : row == 1 ? 4u : 0u;
				}
				else
				{
					if ((x0 + x1) * 0.5f >= kSceneW * 0.5f)
						continue; // (that side's is the other player's: the game shows the first player's on the left)
					if (t0 >= 0.49f)
					{
						const int col = static_cast<int>((s0 + 0.01f) / 0.25f);
						code = col == 0 ? 7u : col == 1 ? 5u : col == 2 ? 6u : 8u;
					}
					else
					{
						const int col = static_cast<int>((s0 + 0.01f) / 0.2f);
						code = col < 4 ? static_cast<uint32_t>(col) + 1u : 0u;
					}
				}
				if (code == 0u)
					continue;
				icons[count++] = Icon{x0, code};
			}
		}
		std::sort(icons, icons + count, [](const Icon& a, const Icon& b) { return a.x < b.x; });
		uint32_t packed = count;
		for (uint32_t i = 0; i < count; ++i)
			packed |= icons[i].code << (4u + i * 4u);
		static const bool s_log = std::getenv("HL_HUD_LOG") != nullptr;
		if (s_log && packed != s_prompt.load(std::memory_order_relaxed))
			std::fprintf(stderr, "[prompt] v%llu %x (first x %.0f)\n", static_cast<unsigned long long>(s_vsync), packed, count ? static_cast<double>(icons[0].x) : 0.0);
		s_prompt.store(packed, std::memory_order_relaxed);
	}

	// HL_HUD_LOG=1: flat textured draws on top of everything that were not there in the last 100 pictures (how the game's button
	// prompts were found). HL_TEX_DUMP=1: the textures as they arrive, hl_frames/tex_<id>_<w>x<h>.rgba.
	void hudLog(const Frame& f)
	{
		static const bool s_hudLog = std::getenv("HL_HUD_LOG") != nullptr;
		if (!s_hudLog)
			return;
		static std::unordered_map<uint64_t, uint64_t> s_lastSeen;
		static uint64_t s_pictures = 0;
		++s_pictures;
		for (const Draw& d : f.draws)
		{
			if (d.key.target != kTScene || d.key.zmode != kZAlways || d.key.zwrite || d.key.textured != kTexModulate || d.count == 0u)
				continue;
			float x0 = 1e9f, y0 = 1e9f, x1 = -1e9f, y1 = -1e9f, s0 = 1e9f, t0 = 1e9f, s1 = -1e9f, t1 = -1e9f;
			for (uint32_t k = d.first; k < d.first + d.count; ++k)
			{
				const Vertex& v = f.vertices[f.indices[k]];
				x0 = std::min(x0, v.x); x1 = std::max(x1, v.x); y0 = std::min(y0, v.y); y1 = std::max(y1, v.y);
				s0 = std::min(s0, v.s); s1 = std::max(s1, v.s); t0 = std::min(t0, v.t); t1 = std::max(t1, v.t);
			}
			const uint64_t id = (static_cast<uint64_t>(static_cast<uint32_t>(d.key.tex)) << 32) ^ (static_cast<uint64_t>(s0 * 512.0f) << 24) ^ (static_cast<uint64_t>(t0 * 512.0f) << 12) ^ static_cast<uint64_t>(d.count);
			uint64_t& last = s_lastSeen[id];
			if (last + 100u < s_pictures || last == 0u)
				std::fprintf(stderr, "[hud] v%llu tex %d idx %u xy %.0f,%.0f-%.0f,%.0f uv %.3f,%.3f-%.3f,%.3f rgba %08x\n", static_cast<unsigned long long>(s_vsync), d.key.tex, d.count,
					static_cast<double>(x0), static_cast<double>(y0), static_cast<double>(x1), static_cast<double>(y1), static_cast<double>(s0), static_cast<double>(t0), static_cast<double>(s1), static_cast<double>(t1),
					f.vertices[f.indices[d.first]].rgba);
			last = s_pictures;
		}
	}

	void flushFrame()
	{
		if (!s_frame || (s_frame->draws.empty() && s_frame->textures.empty()))
			return;
		hudLog(*s_frame);
		updatePrompt(*s_frame);
		s_stats.frames++;
		s_stats.draws += s_frame->draws.size();
		s_stats.verts += s_frame->vertices.size();
		s_stats.tris += s_frame->indices.size() / 3u;
		s_ops.emplace_back();
		s_ops.back().frame = s_frame;
		s_inFlight.fetch_add(1, std::memory_order_acq_rel);
		s_frame = nullptr;
		s_kicks = 0; // (a strip does not continue across this: its indices stay in the frame that went)
	}

	// ---- what PCSX2's GS has to be told before it can draw: the uploads it missed and the registers as they are now
	void emitPending(uint8_t path)
	{
		for (Upload* u : s_pending)
		{
			u->pendingForGs = false;
			if (!u->complete)
				continue;
			const uint64_t head[2] = {4u | (1ull << 60), 0xEull};
			const uint64_t regs[8] = {u->bitbltbuf, R_BITBLTBUF, u->trxpos, R_TRXPOS, u->trxreg, R_TRXREG, 0, R_TRXDIR};
			pushBytes(path, head, sizeof(head));
			pushBytes(path, regs, sizeof(regs));
			const uint32_t qwords = static_cast<uint32_t>(u->bytes.size() / 16u);
			for (uint32_t done = 0; done < qwords;)
			{
				const uint32_t now = std::min(qwords - done, 0x7FFFu);
				const uint64_t tag[2] = {now | (2ull << 58), 0ull};
				pushBytes(path, tag, sizeof(tag));
				pushBytes(path, u->bytes.data() + done * 16u, now * 16u);
				done += now;
			}
		}
		s_pending.clear();
	}

	void emitRegisters(uint8_t path)
	{
		static const uint8_t list[] = {R_PRMODECONT, R_PRMODE, R_TEXCLUT, R_SCANMSK, R_TEXA, R_FOGCOL, R_DTHE, R_COLCLAMP, R_PABE, R_DIMX,
			R_XYOFFSET, R_XYOFFSET + 1, R_SCISSOR, R_SCISSOR + 1, R_ALPHA, R_ALPHA + 1, R_TEST, R_TEST + 1, R_FBA, R_FBA + 1, R_FRAME, R_FRAME + 1,
			R_ZBUF, R_ZBUF + 1, R_CLAMP, R_CLAMP + 1, R_TEX1, R_TEX1 + 1, R_TEX2, R_TEX2 + 1, R_MIPTBP1, R_MIPTBP1 + 1, R_MIPTBP2, R_MIPTBP2 + 1,
			R_TEX0, R_TEX0 + 1, R_BITBLTBUF, R_TRXPOS, R_TRXREG};
		uint32_t count = 5u; // (the current vertex's four and the PRIM below)
		for (const uint8_t r : list)
			count += s_seen[r] ? 1u : 0u;
		const uint64_t head[2] = {count | (1ull << 60), 0xEull};
		pushBytes(path, head, sizeof(head));
		for (const uint8_t r : list)
		{
			if (!s_seen[r])
				continue;
			const uint64_t pair[2] = {s_regs[r], r};
			pushBytes(path, pair, sizeof(pair));
		}
		// (what a vertex has now, not the last register write: most of it comes in packed form, which leaves no register here)
		uint64_t st, rgbaq;
		std::memcpy(&st, &s_s, 4);
		std::memcpy(reinterpret_cast<uint8_t*>(&st) + 4, &s_t, 4);
		rgbaq = s_rgba;
		std::memcpy(reinterpret_cast<uint8_t*>(&rgbaq) + 4, &s_q, 4);
		const uint64_t vertexState[8] = {rgbaq, R_RGBAQ, st, R_ST, static_cast<uint64_t>(s_u) | (static_cast<uint64_t>(s_v) << 16), R_UV, static_cast<uint64_t>(s_f) << 56, R_FOG};
		pushBytes(path, vertexState, sizeof(vertexState));
		const uint64_t prim[2] = {s_prim, R_PRIM};
		pushBytes(path, prim, sizeof(prim));
		s_gsInSync = true;
	}

	// The one upload of a format at a place, if the GS has not seen it yet.
	void emitOne(uint8_t path, uint32_t bp, uint32_t psm)
	{
		const int cls = psmClass(psm);
		if (cls < 0)
			return;
		const auto found = s_uploads[cls].find(bp);
		if (found == s_uploads[cls].end() || !found->second->pendingForGs)
			return;
		Upload* u = found->second;
		std::vector<Upload*> rest;
		rest.swap(s_pending);
		s_pending.push_back(u);
		emitPending(path);
		rest.erase(std::remove(rest.begin(), rest.end(), u), rest.end());
		s_pending.swap(rest);
	}

	// The GS has to draw what comes next: what it needs of everything before goes first. Of the uploads, only what this draw
	// reads: what the GS draws in a fight reads the frame buffers, and those uploads are nearly 2 MB a frame, some of them
	// over a target it uses. (All of them go when this module stops, or before a read back: flushAll.)
	void beforeGs(uint8_t path)
	{
		flushFrame();
		if ((s_prim >> 4) & 1u)
		{
			const uint64_t tex0 = s_regs[R_TEX0 + ((s_prim >> 9) & 1u)];
			emitOne(path, tex0 & 0x3FFFu, (tex0 >> 20) & 63u);
			emitOne(path, (tex0 >> 37) & 0x3FFFu, (tex0 >> 51) & 15u);
		}
		if (!s_gsInSync)
			emitRegisters(path);
	}

	// The replay thread is not one of the heavy ones and does not need a big core: the three that do come first there.
	cpu_set_t s_replayCores;
	bool s_replayCoresSaved = false;
	void toSmallCores()
	{
		const char* off = std::getenv("HL_NATIVE_LITTLE");
		if (off && off[0] == '0')
			return;
		if (!s_replayCoresSaved)
			s_replayCoresSaved = sched_getaffinity(0, sizeof(s_replayCores), &s_replayCores) == 0;
		int capacity[32], lowest = 1 << 30, highest = 0, count = 0;
		for (; count < 32; ++count)
		{
			char path[96];
			std::snprintf(path, sizeof(path), "/sys/devices/system/cpu/cpu%d/cpu_capacity", count);
			FILE* f = std::fopen(path, "r");
			if (!f)
				break;
			capacity[count] = 0;
			const int got = std::fscanf(f, "%d", &capacity[count]);
			std::fclose(f);
			if (got != 1)
				break;
			lowest = std::min(lowest, capacity[count]);
			highest = std::max(highest, capacity[count]);
		}
		if (count == 0 || lowest == highest)
			return;
		cpu_set_t set;
		CPU_ZERO(&set);
		for (int i = 0; i < count; ++i)
			if (capacity[i] == lowest)
				CPU_SET(i, &set);
		const int result = sched_setaffinity(0, sizeof(set), &set);
		std::fprintf(stderr, "hl_native: replay thread on the cores of capacity %d: %d\n", lowest, result);
	}
	// (out of the fight the replay thread runs PCSX2's GS, which is real work)
	void toAllCores()
	{
		if (s_replayCoresSaved)
			sched_setaffinity(0, sizeof(s_replayCores), &s_replayCores);
	}

	// ---- textures
	void decode(const Upload& tex, const Upload& clut, uint32_t psm, int w, int h, uint8_t* out)
	{
		const uint32_t* palette = reinterpret_cast<const uint32_t*>(clut.bytes.data());
		uint32_t* to = reinterpret_cast<uint32_t*>(out);
		const uint32_t stride = tex.w;
		const uint8_t* from = tex.bytes.data();
		if (psm == PSM_T8 || psm == PSM_T8H)
		{
			uint32_t map[256];
			for (uint32_t i = 0; i < 256u; ++i)
				map[i] = palette[(i & 0xE7u) | ((i & 8u) << 1) | ((i & 16u) >> 1)]; // (the CLUT's own order of blocks)
			for (int y = 0; y < h; ++y)
			{
				const uint8_t* row = from + y * stride;
				for (int x = 0; x < w; ++x)
					*to++ = map[row[x]];
			}
		}
		else
		{
			for (int y = 0; y < h; ++y)
			{
				const uint32_t base = y * stride;
				for (int x = 0; x < w; ++x)
				{
					const uint32_t n = base + x;
					const uint8_t b = from[n >> 1];
					*to++ = palette[(n & 1u) ? (b >> 4) : (b & 15u)];
				}
			}
		}
	}

	// The texture a TEX0 means, from what was uploaded. -1: not one this file can read.
	int32_t resolveTexture(uint64_t tex0)
	{
		const uint32_t tbp = tex0 & 0x3FFFu, psm = (tex0 >> 20) & 63u, tw = (tex0 >> 26) & 15u, th = (tex0 >> 30) & 15u;
		const uint32_t cbp = (tex0 >> 37) & 0x3FFFu;
		const int cls = psmClass(psm);
		static uint32_t s_shownMisses = 0;
		const auto missed = [&](const char* why) {
			static std::unordered_map<uint64_t, bool> s_shown;
			if (s_shownMisses < 40u && !s_shown[tbp | (static_cast<uint64_t>(psm) << 32)])
			{
				s_shown[tbp | (static_cast<uint64_t>(psm) << 32)] = true;
				++s_shownMisses;
				std::fprintf(stderr, "[native] texture %x psm %x %ux%u cbp %x: %s\n", tbp, psm, 1u << tw, 1u << th, cbp, why);
			}
			return -1;
		};
		if (cls <= 0 || tw > 10u || th > 10u)
			return missed("format");
		const auto ti = s_uploads[cls].find(tbp);
		const auto ci = s_uploads[0].find(cbp);
		if (ti == s_uploads[cls].end() || ci == s_uploads[0].end())
		{
			// Not sent as such (the stage's effect textures live in the high bits of the display buffer and of Z, put there by copies
			// inside the GS's memory when the stage loads): it is in the GS's memory, and only the GS thread may read that. Whatever
			// of it is still waiting for the GS goes first, and the texture is read when the GS thread reaches this point.
			const uint32_t tbw = (tex0 >> 14) & 63u;
			if (tbw == 0u)
				return missed("no width");
			const TexKey key{(static_cast<uint64_t>(tbp) | (static_cast<uint64_t>(tbw) << 16) | (static_cast<uint64_t>(s_memoryGeneration) << 32)) ^ 0xA5A5000000000000ull, cbp, psm, (tw << 8) | th};
			const auto found = s_textureIds.find(key);
			if (found != s_textureIds.end())
				return found->second;
			flushFrame();
			emitOne(3u, tbp, psm);
			emitOne(3u, cbp, PSM_CT32);
			const int32_t id = s_nextTexture++;
			s_textureIds.emplace(key, id);
			s_stats.miss++;
			if (!s_frame)
				s_frame = takeFrame();
			s_frame->textures.emplace_back();
			NewTexture& n = s_frame->textures.back();
			n.id = id;
			n.w = 1 << tw;
			n.h = 1 << th;
			n.fromMemory = true;
			n.tbp = tbp;
			n.tbw = tbw;
			n.psm = psm;
			n.cbp = cbp;
			return id;
		}
		Upload& tex = *ti->second;
		Upload& clut = *ci->second;
		const int w = 1 << tw, h = 1 << th;
		const bool eight = psm == PSM_T8 || psm == PSM_T8H;
		if (!tex.complete || !clut.complete || tex.trxpos != 0u || clut.trxpos != 0u || tex.w < static_cast<uint32_t>(w) || tex.h < static_cast<uint32_t>(h)
			|| clut.bytes.size() < (eight ? 1024u : 64u))
		{
			char why[160];
			std::snprintf(why, sizeof(why), "upload %ux%u complete %d pos %llx, clut %zu bytes complete %d pos %llx", tex.w, tex.h, tex.complete ? 1 : 0, static_cast<unsigned long long>(tex.trxpos),
				clut.bytes.size(), clut.complete ? 1 : 0, static_cast<unsigned long long>(clut.trxpos));
			return missed(why);
		}
		const TexKey key{hashOf(tex), hashOf(clut), psm, (tw << 8) | th};
		const auto found = s_textureIds.find(key);
		if (found != s_textureIds.end())
			return found->second;
		const int32_t id = s_nextTexture++;
		s_textureIds.emplace(key, id);
		if (key.texHash == kPromptSheet && key.clutHash == kPromptClut)
			s_promptTex = id;
		else if (key.texHash == kSequenceSheet && key.clutHash == kSequenceClut)
			s_sequenceTex = id;
		static const bool s_texLog = std::getenv("HL_HUD_LOG") != nullptr;
		if (s_texLog)
			std::fprintf(stderr, "[tex] id %d %016llx %016llx psm %x %dx%d\n", id, static_cast<unsigned long long>(key.texHash), static_cast<unsigned long long>(key.clutHash), psm, w, h);
		s_stats.miss++;
		if (!s_frame)
			s_frame = takeFrame();
		s_frame->textures.emplace_back();
		NewTexture& n = s_frame->textures.back();
		n.id = id;
		n.w = w;
		n.h = h;
		n.rgba.resize(static_cast<size_t>(w) * h * 4u);
		decode(tex, clut, psm, w, h, n.rgba.data());
		return id;
	}

	// ---- what a draw means, in the state as it is
	Verdict classify()
	{
		s_dirty = false;
		const uint32_t prim = s_prim;
		const uint32_t c = (prim >> 9) & 1u;
		s_type = prim & 7u;
		s_tme = (prim >> 4) & 1u;
		s_fge = (prim >> 5) & 1u;
		s_fst = (prim >> 8) & 1u;
		const uint64_t frame = s_regs[R_FRAME + c], zbuf = s_regs[R_ZBUF + c], test = s_regs[R_TEST + c], alpha = s_regs[R_ALPHA + c];

#define TO_GS(reason) do { s_why = reason; return kToGs; } while (0)
		if (s_type != 3u && s_type != 4u && s_type != 5u && s_type != 6u)
			TO_GS("prim type");
		if ((prim >> 7) & 1u)
			TO_GS("AA1");
		if (s_seen[R_PRMODECONT] && !(s_regs[R_PRMODECONT] & 1u))
			TO_GS("PRMODE");
		const uint32_t fbp = frame & 0x1FFu, fbw = (frame >> 16) & 63u, fpsm = (frame >> 24) & 63u;
		const uint32_t fbmsk = static_cast<uint32_t>(frame >> 32);
		// (the copy of the scene's Z for the shadows: they test against the scene's own. And alpha alone: nothing reads the frame's alpha.)
		static const char* const s_debugKeep = std::getenv("HL_NATIVE_OFF");
		if (fbp == kHalfFbp && fbw == 4u && fpsm == 0x31u && s_tme && (s_regs[R_TEX0 + c] & 0x3FFFu) == kSceneZbp * 32u && !(s_debugKeep && std::strchr(s_debugKeep, 'p')))
			return kDrop;
		if (fbmsk == 0x00FFFFFFu)
			return kDrop;
		uint32_t target;
		if (fpsm != PSM_CT32)
			TO_GS("frame buffer");
		static const char* const s_debugOff = std::getenv("HL_NATIVE_OFF"); // (letters: p the targets of the passes, z writing Z alone)
		if (s_debugOff && std::strchr(s_debugOff, 'p') && fbp != kSceneFbp)
			TO_GS("frame buffer");
		if (fbp == kSceneFbp && fbw == kSceneFbw)
			target = kTScene;
		else if (fbp == kPostFbp && fbw == 4u)
			target = s_tme ? kTBloom : kTShadow;
		else if (fbp == kHalfFbp && fbw == 2u)
			target = kTHalf;
		else if (fbp == kQuarterFbp && fbw == 1u)
			target = kTQuarter;
		else if (fbp == kDisplayFbp && fbw == kSceneFbw)
			target = kTDisplay;
		else
			TO_GS("frame buffer");
		if (fbmsk != 0u && fbmsk != 0xFF000000u)
			TO_GS("FBMSK");
		if ((s_regs[R_PABE] & 1u) || (s_regs[R_FBA + c] & 1u) || (s_regs[R_DTHE] & 1u) || (s_seen[R_COLCLAMP] && !(s_regs[R_COLCLAMP] & 1u)))
			TO_GS("PABE/FBA/DTHE/COLCLAMP");
		// (SCANMSK: the Ultimate Jutsu's scenes draw every other line. Their picture is the whole frame here, nothing to mask:
		// with the mask, the lines left out keep the white flash from before the scene, for the whole scene.)

		DrawKey k{};
		k.target = static_cast<uint8_t>(target);
		const uint64_t sc = s_regs[R_SCISSOR + c];
		k.sc[0] = sc & 0x7FFu;
		k.sc[2] = (sc >> 16) & 0x7FFu;
		k.sc[1] = (sc >> 32) & 0x7FFu;
		k.sc[3] = (sc >> 48) & 0x7FFu;

		// depth
		const bool ate = test & 1u;
		const uint32_t atst = (test >> 1) & 7u, aref = (test >> 4) & 255u, afail = (test >> 12) & 3u;
		const bool zte = (test >> 16) & 1u;
		const uint32_t ztst = (test >> 17) & 3u;
		const bool zmsk = (zbuf >> 32) & 1u;
		if ((test >> 14) & 1u)
			TO_GS("DATE");
		const bool sceneZ = (zbuf & 0x1FFu) == kSceneZbp;
		if (((zbuf >> 24) & 15u) != 1u && sceneZ)
			TO_GS("Z format");
		if ((!zte || ztst == 0u) && target != kTDisplay)
			TO_GS("Z test");
		k.zmode = ztst == 1u ? kZAlways : (ztst == 2u ? kZGequal : kZGreater);
		k.zwrite = zmsk ? 0u : 1u;
		if (target == kTScene)
		{
			if (!sceneZ && (!zmsk || ztst != 1u))
				TO_GS("Z buffer");
		}
		else if (target == kTDisplay)
		{
			k.zmode = kZAlways;
			k.zwrite = 0u;
		}
		else
		{
			// (none, or the shadows' test, which here goes against the scene's Z)
			const bool shadowTest = target == kTShadow && ztst == 3u && (zbuf & 0x1FFu) == kHalfFbp;
			if (!zmsk || (ztst != 1u && !shadowTest))
				TO_GS("Z buffer");
		}

		// alpha test
		k.amode = kANone;
		if (ate)
		{
			if (atst == 0u && afail == 0u)
				return kDrop; // (writes nothing: the game draws this way only to load a CLUT, and here a CLUT is just its upload)
			if (atst == 1u)
				k.amode = kANone;
			else if (atst == 0u && afail == 1u)
				k.zwrite = 0u; // (never passes, but colour is written: colour and no Z)
			else if (atst == 0u && afail == 2u && target == kTScene && !s_tme && !(s_debugOff && std::strchr(s_debugOff, 'z')))
			{
				// (never passes, but Z is written: how the game clears it)
				k.blend = kBlendZOnly;
				k.zwrite = zmsk ? 0u : 1u;
			}
			else if ((atst == 5u || (atst == 6u && aref < 255u)) && (afail == 0u || afail == 1u))
			{
				k.amode = afail == 0u ? kAKeep : kAFbOnly;
				k.aref = static_cast<uint8_t>(atst == 5u ? aref : aref + 1u);
			}
			else
				TO_GS("alpha test");
		}

		// blend
		if (k.blend == kBlendZOnly)
			;
		else if (!((prim >> 6) & 1u))
			k.blend = kBlendNone;
		else
		{
			const uint32_t a = alpha & 3u, b = (alpha >> 2) & 3u, cc = (alpha >> 4) & 3u, d = (alpha >> 6) & 3u;
			if (cc != 0u || d != 1u)
				TO_GS("blend");
			if (a == 0u && b == 1u)
				k.blend = kBlendAlpha;
			else if (a == 2u && b == 0u)
				k.blend = kBlendSub;
			else if (a == 0u && b == 2u)
				k.blend = kBlendAdd;
			else
				TO_GS("blend");
		}

		k.tex = -1;
		if (s_fge)
			k.fogcol = static_cast<uint32_t>(s_regs[R_FOGCOL] & 0xFFFFFFu);
		if (s_tme)
		{
			const uint64_t tex0 = s_regs[R_TEX0 + c];
			const uint32_t tcc = (tex0 >> 34) & 1u, tfx = (tex0 >> 35) & 3u, cpsm = (tex0 >> 51) & 15u, csm = (tex0 >> 55) & 1u, csa = (tex0 >> 56) & 31u;
			const uint32_t tbp = tex0 & 0x3FFFu, tbw = (tex0 >> 14) & 63u, tpsm = (tex0 >> 20) & 63u;
			// one of the targets, read back?
			int source = -1;
			if (tbp == kSceneFbp * 32u && tbw == kSceneFbw && tpsm == PSM_CT32)
				source = kTScene;
			else if (tbp == kPostFbp * 32u && tbw == 4u && tpsm == 1u)
				source = kTShadow;
			else if (tbp == kPostFbp * 32u && tbw == 4u && tpsm == PSM_CT32)
				source = kTBloom;
			else if (tbp == kHalfFbp * 32u && tbw == 2u && tpsm == PSM_CT32)
				source = kTHalf;
			else if (tbp == kQuarterFbp * 32u && tbw == 1u && tpsm == PSM_CT32)
				source = kTQuarter;
			if (source >= 0 && s_debugOff && std::strchr(s_debugOff, 'p'))
				TO_GS("target read");
			if (tfx != 0u || (tcc != 1u && source < 0))
				TO_GS("TFX");
			if (source < 0 && (cpsm != 0u || csm != 0u || csa != 0u))
				TO_GS("CLUT");
			const uint64_t clamp = s_regs[R_CLAMP + c];
			const uint32_t wms = clamp & 3u, wmt = (clamp >> 2) & 3u;
			if (wms > 1u || wmt > 1u)
				TO_GS("clamp");
			const bool linear = (s_regs[R_TEX1 + c] >> 5) & 1u;
			k.sampler = static_cast<uint8_t>(wms | (wmt << 1) | (linear ? 4u : 0u));
			s_tw = 1u << ((tex0 >> 26) & 15u);
			s_th = 1u << ((tex0 >> 30) & 15u);
			if (source >= 0)
			{
				// (their alpha is not kept here; CT24's comes from the colour and TEXA)
				if (!s_fst || static_cast<uint32_t>(source) == target)
					TO_GS("target read");
				if (source == kTShadow)
				{
					if (tcc != 1u || (s_regs[R_TEXA] & 0xFFFFu) != 0x8020u)
						TO_GS("target read");
					k.textured = kTexShadow;
				}
				else
				{
					if (tcc != 0u)
						TO_GS("target read");
					k.textured = kTexNoAlpha;
				}
				k.tex = texOfTarget(source);
				if (source == kTScene)
				{
					s_tw = kSceneW;
					s_th = kSceneH;
				}
			}
			else
			{
				k.tex = resolveTexture(tex0);
				if (k.tex < 0)
					TO_GS("texture not known");
				k.textured = kTexModulate;
			}
		}
#undef TO_GS
		const uint64_t xy = s_regs[R_XYOFFSET + c];
		s_ofx = static_cast<float>(xy & 0xFFFFu);
		s_ofy = static_cast<float>((xy >> 32) & 0xFFFFu);
		s_key = k;
		// (flat and on top of everything: drawn by UV, with no Z test and no Z written)
		s_hudDraw = k.target == kTScene && k.zmode == kZAlways && !k.zwrite && ((s_tme && s_fst && k.textured == kTexModulate) || (!s_tme && s_type == 6u));
		return kNative;
	}

	inline void setPrim(uint32_t value)
	{
		if (value != s_prim)
			s_dirty = true;
		s_prim = value;
		s_kicks = 0u;
	}

	inline void addIndices(uint32_t a, uint32_t b, uint32_t c)
	{
		Frame& f = *s_frame;
		if (f.draws.empty() || !(f.draws.back().key == s_key) || f.draws.back().first + f.draws.back().count != f.indices.size())
			f.draws.push_back(Draw{s_key, static_cast<uint32_t>(f.indices.size()), 0u});
		f.indices.push_back(a);
		f.indices.push_back(b);
		f.indices.push_back(c);
		f.draws.back().count += 3u;
	}

	inline void kick(uint32_t x, uint32_t y, uint32_t z, uint32_t fog, bool adc)
	{
		if (!s_frame)
			s_frame = takeFrame();
		Frame& f = *s_frame;
		constexpr float kZScale = 1.0f / 4294967296.0f;
		if (z > 0xFFFFFFu)
			z = 0xFFFFFFu;
		Vertex v;
		v.x = (static_cast<float>(x) - s_ofx - 0.05f) * (1.0f / 16.0f);
		v.y = (static_cast<float>(y) - s_ofy - 0.05f) * (1.0f / 16.0f);
		v.z = static_cast<float>(z) * kZScale;
		if (s_fst)
		{
			v.s = static_cast<float>(s_u) / (16.0f * static_cast<float>(s_tw));
			v.t = static_cast<float>(s_v) / (16.0f * static_cast<float>(s_th));
			v.q = 1.0f;
		}
		else
		{
			v.s = s_s;
			v.t = s_t;
			v.q = s_q;
		}
		v.rgba = s_rgba;
		v.fog = s_fge ? fog : 255u;
		const bool squeeze = s_hudDraw && s_hudScale != 1.0f;

		if (s_type == 6u)
		{
			if ((s_kicks++ & 1u) == 0u)
			{
				s_spriteFirst = v;
				return;
			}
			if (adc)
				return;
			if (squeeze && std::fabs(v.x - s_spriteFirst.x) < 500.0f) // (a sprite over the whole screen is not HUD)
			{
				v.x = kSceneW * 0.5f + (v.x - kSceneW * 0.5f) * s_hudScale;
				s_spriteFirst.x = kSceneW * 0.5f + (s_spriteFirst.x - kSceneW * 0.5f) * s_hudScale;
			}
			const Vertex& a = s_spriteFirst;
			const uint32_t n = static_cast<uint32_t>(f.vertices.size());
			// (one Q for the sprite, the last one: S and T of the first as they would be with it)
			const float sa = s_fst ? a.s : a.s / a.q * v.q, ta = s_fst ? a.t : a.t / a.q * v.q;
			f.vertices.push_back(Vertex{a.x, a.y, v.z, sa, ta, v.q, v.rgba, v.fog});
			f.vertices.push_back(Vertex{v.x, a.y, v.z, v.s, ta, v.q, v.rgba, v.fog});
			f.vertices.push_back(Vertex{a.x, v.y, v.z, sa, v.t, v.q, v.rgba, v.fog});
			f.vertices.push_back(v);
			addIndices(n, n + 1u, n + 2u);
			addIndices(n + 1u, n + 3u, n + 2u);
			return;
		}

		const uint32_t n = static_cast<uint32_t>(f.vertices.size());
		if (squeeze)
			v.x = kSceneW * 0.5f + (v.x - kSceneW * 0.5f) * s_hudScale;
		f.vertices.push_back(v);
		const uint32_t count = s_kicks++;
		switch (s_type)
		{
			case 3u:
				if ((count % 3u) == 2u && !adc)
					addIndices(n - 2u, n - 1u, n);
				break;
			case 4u:
				if (count >= 2u && !adc)
					addIndices(s_i0, s_i1, n);
				s_i0 = s_i1;
				s_i1 = n;
				break;
			default: // (fan)
				if (count == 0u)
					s_i0 = n;
				else
				{
					if (count >= 2u && !adc)
						addIndices(s_i0, s_i1, n);
					s_i1 = n;
				}
				break;
		}
	}

	// ---- registers and uploads, seen with this module on or off
	inline bool drawsDependOn(uint32_t r)
	{
		return r != R_RGBAQ && r != R_ST && r != R_UV && r != R_FOG && r != R_TEXFLUSH;
	}

	void beginTransfer()
	{
		const uint64_t bb = s_regs[R_BITBLTBUF];
		const uint32_t dbp = (bb >> 32) & 0x3FFFu, dpsm = (bb >> 56) & 63u;
		s_current = nullptr;
		s_currentNeed = 0u;
		const int cls = psmClass(dpsm);
		{
			// (what uploads exist, once each: to know what the persistent textures are made of)
			static std::unordered_map<uint64_t, bool> s_shown;
			static uint32_t s_count = 0;
			const uint64_t id = dbp | (static_cast<uint64_t>(dpsm) << 16);
			if (std::getenv("HL_NATIVE_UPLOADS") && s_count < 300u && !s_shown[id] && dpsm != PSM_T4 && dpsm != PSM_T8 && (dpsm != PSM_CT32 || (s_regs[R_TRXREG] & 0xFFFu) > 16u))
			{
				s_shown[id] = true;
				++s_count;
				std::fprintf(stderr, "[native] upload to %x psm %x dbw %llu %llux%llu at %llx\n", dbp, dpsm, static_cast<unsigned long long>((bb >> 48) & 63u),
					static_cast<unsigned long long>(s_regs[R_TRXREG] & 0xFFFu), static_cast<unsigned long long>((s_regs[R_TRXREG] >> 32) & 0xFFFu), static_cast<unsigned long long>(s_regs[R_TRXPOS]));
			}
		}
		if (cls < 0)
			return;
		Upload*& slot = s_uploads[cls][dbp];
		if (!slot)
			slot = new Upload();
		Upload& u = *slot;
		u.bitbltbuf = bb;
		u.trxpos = s_regs[R_TRXPOS];
		u.trxreg = s_regs[R_TRXREG];
		u.w = s_regs[R_TRXREG] & 0xFFFu;
		u.h = (s_regs[R_TRXREG] >> 32) & 0xFFFu;
		u.psm = dpsm;
		u.bytes.clear();
		u.hashed = false;
		u.complete = false;
		const uint32_t bits = dpsm == PSM_CT32 ? 32u : ((dpsm == PSM_T8 || dpsm == PSM_T8H) ? 8u : 4u);
		s_currentNeed = (u.w * u.h * bits + 7u) / 8u;
		s_current = &u;
		s_stats.uploads++;
	}

	void receiveImage(const uint8_t* data, uint32_t size)
	{
		if (!s_current)
			return;
		Upload& u = *s_current;
		const size_t at = u.bytes.size();
		u.bytes.resize(at + size);
		std::memcpy(u.bytes.data() + at, data, size);
		if (u.bytes.size() >= s_currentNeed)
		{
			u.complete = true;
			s_current = nullptr;
			s_dirty = true;
			if (s_sceneOn && !u.pendingForGs)
			{
				u.pendingForGs = true;
				s_pending.push_back(&u);
			}
		}
	}

	// What says a fight is on the screen: every frame of one copies the Z to page 0x1E0 (width 4, Z24), for its shadows.
	constexpr uint64_t kFightFrameMask = 0x3F3F01FFull, kFightFrame = 0x310401E0ull;
	constexpr uint32_t kFightFramesToStart = 8u, kFightVsyncsToStop = 30u;
	bool s_fightSeen = false;
	uint32_t s_fightFrames = 0u, s_vsyncsSinceFight = 1u << 30;

	inline void writeRegister(uint32_t r, uint64_t value)
	{
		if (r == R_PRIM)
		{
			setPrim(static_cast<uint32_t>(value & 0x7FFu));
			s_gsInSync = false;
			return;
		}
		if (r == R_RGBAQ)
		{
			s_rgba = static_cast<uint32_t>(value);
			std::memcpy(&s_q, reinterpret_cast<const uint8_t*>(&value) + 4, 4);
		}
		else if (r == R_ST)
		{
			std::memcpy(&s_s, &value, 4);
			std::memcpy(&s_t, reinterpret_cast<const uint8_t*>(&value) + 4, 4);
		}
		else if (r == R_UV)
		{
			s_u = value & 0x3FFFu;
			s_v = (value >> 16) & 0x3FFFu;
		}
		else if (r == R_FOG)
			s_f = (value >> 56) & 255u;
		if (drawsDependOn(r) && s_regs[r & 255u] != value)
			s_dirty = true;
		s_regs[r & 255u] = value;
		s_seen[r & 255u] = true;
		if ((r == R_FRAME || r == R_FRAME + 1u) && (value & kFightFrameMask) == kFightFrame)
			s_fightSeen = true;
		if (r == R_TRXDIR && (value & 3u) == 0u)
			beginTransfer();
		s_gsInSync = false;
	}

	uint32_t tagQwords(uint64_t lo)
	{
		const uint32_t nloop = lo & 0x7FFFu, flg = (lo >> 58) & 3u;
		uint32_t nreg = (lo >> 60) & 15u;
		if (nreg == 0u)
			nreg = 16u;
		return flg == 0u ? nloop * nreg : (flg == 1u ? (nloop * nreg + 1u) / 2u : nloop);
	}

	// With this module off: the stream goes on as it is, and only what it would need later is noted.
	void observe(const uint8_t* data, uint32_t size)
	{
		uint32_t at = 0u;
		while (at + 16u <= size)
		{
			uint64_t lo, hi;
			std::memcpy(&lo, data + at, 8);
			std::memcpy(&hi, data + at + 8, 8);
			const uint32_t qwords = tagQwords(lo), flg = (lo >> 58) & 3u, nreg = ((lo >> 60) & 15u) ? ((lo >> 60) & 15u) : 16u;
			const uint8_t* body = data + at + 16u;
			if ((lo >> 46) & 1u)
				s_prim = (lo >> 47) & 0x7FFu;
			if (flg == 0u)
			{
				const uint32_t nloop = lo & 0x7FFFu;
				for (uint32_t i = 0; i < nreg; ++i)
				{
					const uint32_t p = (hi >> (4u * i)) & 15u;
					if (p != P_AD)
						continue;
					for (uint32_t l = 0; l < nloop; ++l)
					{
						uint64_t a, c;
						std::memcpy(&a, body + (l * nreg + i) * 16u, 8);
						std::memcpy(&c, body + (l * nreg + i) * 16u + 8u, 8);
						if ((c & 0xFFu) != R_XYZ2 && (c & 0xFFu) != R_XYZF2 && (c & 0xFFu) != R_XYZ3 && (c & 0xFFu) != R_XYZF3)
							writeRegister(c & 0xFFu, a);
					}
				}
			}
			else if (flg >= 2u)
				receiveImage(body, qwords * 16u);
			at += 16u + qwords * 16u;
		}
		s_gsInSync = true; // (the GS saw it all)
	}

	void noteRoute()
	{
		s_stats.routedTags++;
		uint32_t& count = s_stats.why[s_why];
		static std::unordered_map<const char*, uint32_t> s_shown;
		if (count++ == 0u && s_shown[s_why]++ < 4u)
		{
			const uint32_t c = (s_prim >> 9) & 1u;
			std::fprintf(stderr, "[native] to the GS (%s): prim %x frame %llx zbuf %llx test %llx alpha %llx tex0 %llx clamp %llx\n", s_why, s_prim,
				static_cast<unsigned long long>(s_regs[R_FRAME + c]), static_cast<unsigned long long>(s_regs[R_ZBUF + c]), static_cast<unsigned long long>(s_regs[R_TEST + c]),
				static_cast<unsigned long long>(s_regs[R_ALPHA + c]), static_cast<unsigned long long>(s_regs[R_TEX0 + c]), static_cast<unsigned long long>(s_regs[R_CLAMP + c]));
		}
	}

	// One tag to PCSX2's GS as it came.
	void toGs(uint8_t path, const uint8_t* tag, uint32_t bytes)
	{
		beforeGs(path);
		// (without its EOP: on path 1 the GS stops reading a packet at one, and what goes on is joined in one packet)
		uint64_t lo;
		std::memcpy(&lo, tag, 8);
		lo &= ~(1ull << 15);
		pushBytes(path, &lo, 8);
		pushBytes(path, tag + 8, bytes - 8u);
		noteRoute();
	}

	// A vertex that came among registers, to wherever the state says.
	void vertex(uint8_t path, uint32_t x, uint32_t y, uint32_t z, uint32_t fog, bool withFog, bool adc)
	{
		if (s_dirty)
			s_verdict = classify();
		if (s_verdict == kNative)
		{
			kick(x, y, z, fog, adc);
			return;
		}
		if (s_verdict == kDrop)
		{
			s_stats.dropped++;
			return;
		}
		// (as registers: what is current of the vertex, then its position)
		uint64_t st, rgbaq;
		std::memcpy(&st, &s_s, 4);
		std::memcpy(reinterpret_cast<uint8_t*>(&st) + 4, &s_t, 4);
		rgbaq = s_rgba;
		std::memcpy(reinterpret_cast<uint8_t*>(&rgbaq) + 4, &s_q, 4);
		const uint64_t position = withFog ? (static_cast<uint64_t>(x) | (static_cast<uint64_t>(y) << 16) | (static_cast<uint64_t>(z & 0xFFFFFFu) << 32) | (static_cast<uint64_t>(fog & 255u) << 56))
										  : (static_cast<uint64_t>(x) | (static_cast<uint64_t>(y) << 16) | (static_cast<uint64_t>(z) << 32));
		const uint64_t words[12] = {5u | (1ull << 60), 0xEull, rgbaq, R_RGBAQ, st, R_ST, static_cast<uint64_t>(s_u) | (static_cast<uint64_t>(s_v) << 16), R_UV, static_cast<uint64_t>(s_f) << 56, R_FOG,
			position, static_cast<uint64_t>(withFog ? (adc ? R_XYZF3 : R_XYZF2) : (adc ? R_XYZ3 : R_XYZ2))};
		beforeGs(path);
		pushBytes(path, words, sizeof(words));
		noteRoute();
	}

	void parse(uint8_t path, const uint8_t* data, uint32_t size)
	{
		uint32_t at = 0u;
		while (at + 16u <= size)
		{
			uint64_t lo, hi;
			std::memcpy(&lo, data + at, 8);
			std::memcpy(&hi, data + at + 8, 8);
			const uint32_t qwords = tagQwords(lo), flg = (lo >> 58) & 3u;
			const uint32_t nreg = ((lo >> 60) & 15u) ? ((lo >> 60) & 15u) : 16u;
			const uint32_t nloop = lo & 0x7FFFu;
			const uint8_t* body = data + at + 16u;
			const uint32_t tagBytes = 16u + qwords * 16u;
			if (flg >= 2u)
			{
				receiveImage(body, qwords * 16u);
				at += tagBytes;
				continue;
			}
			if ((lo >> 46) & 1u)
			{
				setPrim(static_cast<uint32_t>((lo >> 47) & 0x7FFu));
				s_gsInSync = false;
			}
			if (nloop == 0u)
			{
				at += tagBytes;
				continue;
			}
			if (flg == 1u)
			{
				s_why = "REGLIST";
				toGs(path, data + at, tagBytes);
				at += tagBytes;
				continue;
			}
			uint32_t hasVertex = 0u, hasState = 0u;
			uint8_t regs[16];
			for (uint32_t i = 0; i < nreg; ++i)
			{
				const uint32_t p = (hi >> (4u * i)) & 15u;
				regs[i] = static_cast<uint8_t>(p);
				if (p == P_XYZF2 || p == P_XYZ2)
					hasVertex = 1u;
				else if (p != P_RGBA && p != P_STQ && p != P_UV && p != P_FOG && p != P_NOP)
					hasState = 1u;
			}
			const uint8_t* q = body;
			if (hasState)
			{
				// registers among the rest, one by one. (If the first vertex is for the GS, the whole tag goes as it is, and
				// the rest of it only updates the registers here.)
				bool sent = false, firstVertex = true;
				const auto sendWhole = [&]() {
					if (!s_wholeTags)
						return false;
					if (s_dirty)
						s_verdict = classify();
					bool whole = firstVertex && s_verdict == kToGs;
					firstVertex = false;
					// (not a tag that changes the target or the texture along the way: it holds other draws, and those may be for here)
					const uint8_t* scan = body;
					for (uint32_t l = 0; whole && l < nloop; ++l)
					{
						for (uint32_t i = 0; whole && i < nreg; ++i, scan += 16)
						{
							const uint32_t p = regs[i];
							if (p == P_PRIM || (p >= R_TEX0 && p <= R_CLAMP + 1u))
								whole = false;
							else if (p == P_AD)
							{
								const uint32_t r = scan[8];
								whole = r == R_XYZ2 || r == R_XYZ3 || r == R_XYZF2 || r == R_XYZF3 || r == R_RGBAQ || r == R_ST || r == R_UV || r == R_FOG || r == 0x42u || r == 0x43u;
							}
						}
					}
					if (whole)
					{
						toGs(path, data + at, tagBytes);
						sent = true;
					}
					return whole;
				};
				for (uint32_t l = 0; l < nloop; ++l)
				{
					for (uint32_t i = 0; i < nreg; ++i, q += 16)
					{
						uint64_t a, c;
						std::memcpy(&a, q, 8);
						std::memcpy(&c, q + 8, 8);
						const uint32_t p = regs[i];
						switch (p)
						{
							case P_NOP:
								break;
							case P_RGBA:
								s_rgba = (a & 255u) | (((a >> 32) & 255u) << 8) | ((c & 255u) << 16) | (static_cast<uint32_t>((c >> 32) & 255u) << 24);
								break;
							case P_STQ:
								std::memcpy(&s_s, &a, 4);
								std::memcpy(&s_t, reinterpret_cast<const uint8_t*>(&a) + 4, 4);
								std::memcpy(&s_q, &c, 4);
								break;
							case P_UV:
								s_u = a & 0x3FFFu;
								s_v = (a >> 32) & 0x3FFFu;
								break;
							case P_FOG:
								s_f = (c >> 36) & 255u;
								break;
							case P_XYZ2:
								if (sent || sendWhole())
									break;
								vertex(path, a & 0xFFFFu, (a >> 32) & 0xFFFFu, static_cast<uint32_t>(c), s_f, false, (c >> 47) & 1u);
								break;
							case P_XYZF2:
								s_f = (c >> 36) & 255u;
								if (sent || sendWhole())
									break;
								vertex(path, a & 0xFFFFu, (a >> 32) & 0xFFFFu, (c >> 4) & 0xFFFFFFu, s_f, true, (c >> 47) & 1u);
								break;
							case P_PRIM:
								setPrim(static_cast<uint32_t>(a & 0x7FFu));
								s_gsInSync = false;
								break;
							case P_AD:
							{
								const uint32_t r = c & 0xFFu;
								const uint32_t x = a & 0xFFFFu, y = (a >> 16) & 0xFFFFu;
								if (r == R_XYZ2 || r == R_XYZ3)
								{
									if (!sent && !sendWhole())
										vertex(path, x, y, static_cast<uint32_t>(a >> 32), s_f, false, r == R_XYZ3);
								}
								else if (r == R_XYZF2 || r == R_XYZF3)
								{
									s_f = static_cast<uint32_t>(a >> 56);
									if (!sent && !sendWhole())
										vertex(path, x, y, (a >> 32) & 0xFFFFFFu, s_f, true, r == R_XYZF3);
								}
								else
								{
									writeRegister(r, a);
									if (!sent && r == R_TRXDIR && (a & 3u) != 0u)
									{
										// a copy inside the GS's memory or a read back: only the GS can do those
										const uint64_t head[2] = {1u | (1ull << 60), 0xEull};
										s_why = "local copy";
										++s_memoryGeneration;
										beforeGs(path);
										pushBytes(path, head, sizeof(head));
										pushBytes(path, q, 16);
										noteRoute();
									}
								}
								break;
							}
							default:
								// TEX0 and CLAMP of a context (packed in the same numbers as the registers), or nothing
								if (p >= R_TEX0 && p <= R_CLAMP + 1u)
									writeRegister(p, a);
								break;
						}
					}
				}
				s_gsInSync = sent; // (if it went, it had them all, the ones after its first vertex too)
				at += tagBytes;
				continue;
			}
			if (hasVertex)
			{
				if (s_dirty)
					s_verdict = classify();
				if (s_verdict == kDrop)
				{
					s_stats.dropped++;
					at += tagBytes;
					continue;
				}
				if (s_verdict == kToGs)
				{
					toGs(path, data + at, tagBytes);
					at += tagBytes;
					continue;
				}
			}
			for (uint32_t l = 0; l < nloop; ++l)
			{
				for (uint32_t i = 0; i < nreg; ++i, q += 16)
				{
					uint32_t w[4];
					std::memcpy(w, q, 16);
					switch (regs[i])
					{
						case P_RGBA:
							s_rgba = (w[0] & 255u) | ((w[1] & 255u) << 8) | ((w[2] & 255u) << 16) | (w[3] << 24);
							break;
						case P_STQ:
							std::memcpy(&s_s, &w[0], 4);
							std::memcpy(&s_t, &w[1], 4);
							std::memcpy(&s_q, &w[2], 4);
							break;
						case P_UV:
							s_u = w[0] & 0x3FFFu;
							s_v = w[1] & 0x3FFFu;
							break;
						case P_FOG:
							s_f = (w[3] >> 4) & 255u;
							break;
						case P_XYZ2:
							kick(w[0] & 0xFFFFu, w[1] & 0xFFFFu, w[2], s_f, (w[3] >> 15) & 1u);
							break;
						case P_XYZF2:
							s_f = (w[3] >> 4) & 255u;
							kick(w[0] & 0xFFFFu, w[1] & 0xFFFFu, (w[2] >> 4) & 0xFFFFFFu, s_f, (w[3] >> 15) & 1u);
							break;
						default:
							break;
					}
				}
			}
			s_gsInSync = false;
			at += tagBytes;
		}
	}

	// ---- the GS thread's side
	struct PushConstants
	{
		float scale[4];
		float fog[4];
		float test[4]; // reference, 1 to keep what fails instead, the colour's scale
		float uv[4];   // the part of an image the texture coordinates mean
	};

	struct GpuTexture
	{
		GSTextureVK* texture = nullptr;
		VkDescriptorSet sets[8] = {};
	};

	std::vector<GpuTexture> s_gpuTextures;
	bool s_gpuReady = false, s_gpuFailed = false;
	VkDescriptorSetLayout s_setLayout = VK_NULL_HANDLE;
	VkPipelineLayout s_layout = VK_NULL_HANDLE;
	VkDescriptorPool s_pool = VK_NULL_HANDLE;
	VkSampler s_samplers[8] = {};
	VkShaderModule s_vs = VK_NULL_HANDLE;
	enum FsVariant { kFsPlain, kFsTest, kFsNoAlpha, kFsNoAlphaTest, kFsShadow, kFsVariants };
	VkShaderModule s_fs[kFsVariants] = {};
	// the targets: the scene's is PCSX2's; the others live here. (logical size * sx, sy = pixels)
	struct NativeTarget
	{
		GpuTexture tex;
		float sx = 1.0f, sy = 1.0f;
	};
	NativeTarget s_targets[kTargets];
	VKStreamBuffer s_vb, s_ib;
	GSTextureVK* s_white = nullptr;
	GpuTexture s_whiteTex;
	VkRenderPass s_renderPass = VK_NULL_HANDLE, s_renderPassColor = VK_NULL_HANDLE;
	std::unordered_map<uint32_t, VkPipeline> s_pipelines;

	const char kVS[] = R"(#version 450
layout(push_constant) uniform PC { vec4 scale; vec4 fog; vec4 test; vec4 uv; } pc;
layout(location = 0) in vec3 a_pos;
layout(location = 1) in vec3 a_stq;
layout(location = 2) in vec4 a_col;
layout(location = 3) in float a_fog;
layout(location = 0) noperspective out vec3 v_stq;
layout(location = 1) noperspective out vec4 v_col;
layout(location = 2) noperspective out float v_fog;
void main()
{
	gl_Position = vec4(a_pos.xy * pc.scale.xy + pc.scale.zw, a_pos.z, 1.0);
	v_stq = vec3(a_stq.xy * pc.uv.xy, a_stq.z);
	v_col = a_col;
	v_fog = a_fog;
}
)";
	const char kFS[] = R"(
layout(push_constant) uniform PC { vec4 scale; vec4 fog; vec4 test; vec4 uv; } pc;
layout(set = 0, binding = 0) uniform sampler2D u_tex;
layout(location = 0) noperspective in vec3 v_stq;
layout(location = 1) noperspective in vec4 v_col;
layout(location = 2) noperspective in float v_fog;
layout(location = 0) out vec4 o_col;
void main()
{
	vec2 st = v_stq.xy / v_stq.z;
	vec4 t = texture(u_tex, st);
	// the GS's modulate: colour * texture / 128, and alpha the same; 8-bit alpha of 128 is 1 in a blend
#if defined(SHADOW)
	// (CT24 with AEM: texels with no colour have alpha 0, the others TA0, and the GS filters those)
	vec4 g = textureGather(u_tex, st, 0);
	vec4 on = step(vec4(0.5 / 255.0), g);
	vec2 f = fract(st * vec2(textureSize(u_tex, 0)) - 0.5);
	float a = mix(mix(on.w, on.z, f.x), mix(on.x, on.y, f.x), f.y) * 32.0 * v_col.a * 255.0 / 128.0;
#elif defined(NO_ALPHA)
	float a = v_col.a * 255.0;
#else
	float a = t.a * v_col.a * 255.0 * pc.test.z;
#endif
#ifdef ALPHA_TEST
	if ((a + 0.5 >= pc.test.x) == (pc.test.y > 0.5))
		discard;
#endif
	vec3 c = v_col.rgb * t.rgb * pc.test.z;
	o_col = vec4(mix(pc.fog.rgb, c, v_fog), a / 128.0);
}
)";

	VkPipeline getPipeline(GSDeviceVK* dev, const DrawKey& k, bool test, bool zwrite)
	{
		const bool withDepth = k.target == kTScene || k.target == kTShadow;
		const uint32_t variant = k.textured == kTexShadow ? kFsShadow : (k.textured == kTexNoAlpha ? (test ? kFsNoAlphaTest : kFsNoAlpha) : (test ? kFsTest : kFsPlain));
		const uint32_t id = k.blend | (k.zmode << 3) | ((zwrite ? 1u : 0u) << 5) | (variant << 6) | ((withDepth ? 1u : 0u) << 9);
		const auto found = s_pipelines.find(id);
		if (found != s_pipelines.end())
			return found->second;
		Vulkan::GraphicsPipelineBuilder gb;
		gb.SetPipelineLayout(s_layout);
		gb.SetRenderPass(withDepth ? s_renderPass : s_renderPassColor, 0u);
		gb.AddVertexBuffer(0u, sizeof(Vertex));
		gb.AddVertexAttribute(0u, 0u, VK_FORMAT_R32G32B32_SFLOAT, 0u);
		gb.AddVertexAttribute(1u, 0u, VK_FORMAT_R32G32B32_SFLOAT, 12u);
		gb.AddVertexAttribute(2u, 0u, VK_FORMAT_R8G8B8A8_UNORM, 24u);
		gb.AddVertexAttribute(3u, 0u, VK_FORMAT_R8_UNORM, 28u);
		gb.SetPrimitiveTopology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
		gb.SetNoCullRasterizationState();
		gb.SetDynamicViewportAndScissorState();
		gb.SetVertexShader(s_vs);
		gb.SetFragmentShader(s_fs[variant]);
		gb.SetDepthState(k.zmode != kZAlways || zwrite, zwrite, k.zmode == kZAlways ? VK_COMPARE_OP_ALWAYS : (k.zmode == kZGequal ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_GREATER));
		gb.SetNoStencilState();
		const VkColorComponentFlags rgb = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT;
		switch (k.blend)
		{
			case kBlendAlpha:
				gb.SetBlendAttachment(0u, true, VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA, VK_BLEND_OP_ADD, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD, rgb);
				break;
			case kBlendSub:
				gb.SetBlendAttachment(0u, true, VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE, VK_BLEND_OP_REVERSE_SUBTRACT, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD, rgb);
				break;
			case kBlendAdd:
				gb.SetBlendAttachment(0u, true, VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE, VK_BLEND_OP_ADD, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD, rgb);
				break;
			case kBlendZOnly:
				gb.SetBlendAttachment(0u, false, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD, 0u);
				break;
			default:
				gb.SetBlendAttachment(0u, false, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD, rgb);
				break;
		}
		VkPipeline p = gb.Create(dev->GetDevice(), g_vulkan_shader_cache->GetPipelineCache(true), false);
		s_pipelines.emplace(id, p);
		return p;
	}

	VkDescriptorSet setFor(GSDeviceVK* dev, GpuTexture& t, uint32_t sampler)
	{
		if (t.sets[sampler] != VK_NULL_HANDLE)
			return t.sets[sampler];
		VkDescriptorSetAllocateInfo ai{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO};
		ai.descriptorPool = s_pool;
		ai.descriptorSetCount = 1u;
		ai.pSetLayouts = &s_setLayout;
		VkDescriptorSet set = VK_NULL_HANDLE;
		if (vkAllocateDescriptorSets(dev->GetDevice(), &ai, &set) != VK_SUCCESS)
		{
			std::fprintf(stderr, "hl_native: out of descriptor sets (%zu textures)\n", s_gpuTextures.size());
			return VK_NULL_HANDLE;
		}
		VkDescriptorImageInfo ii{s_samplers[sampler], t.texture->GetView(), VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL};
		VkWriteDescriptorSet w{VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET};
		w.dstSet = set;
		w.descriptorCount = 1u;
		w.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
		w.pImageInfo = &ii;
		vkUpdateDescriptorSets(dev->GetDevice(), 1u, &w, 0u, nullptr);
		t.sets[sampler] = set;
		return set;
	}

	bool initGpu(GSDeviceVK* dev, VkFormat color, VkFormat depth)
	{
		const VkDevice d = dev->GetDevice();
		VkDescriptorSetLayoutBinding b{};
		b.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
		b.descriptorCount = 1u;
		b.stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;
		VkDescriptorSetLayoutCreateInfo sli{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO};
		sli.bindingCount = 1u;
		sli.pBindings = &b;
		if (vkCreateDescriptorSetLayout(d, &sli, nullptr, &s_setLayout) != VK_SUCCESS)
			return false;
		VkPushConstantRange pc{VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0u, sizeof(PushConstants)};
		VkPipelineLayoutCreateInfo pli{VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO};
		pli.setLayoutCount = 1u;
		pli.pSetLayouts = &s_setLayout;
		pli.pushConstantRangeCount = 1u;
		pli.pPushConstantRanges = &pc;
		if (vkCreatePipelineLayout(d, &pli, nullptr, &s_layout) != VK_SUCCESS)
			return false;
		VkDescriptorPoolSize ps{VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 16384u};
		VkDescriptorPoolCreateInfo pi{VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO};
		pi.maxSets = 16384u;
		pi.poolSizeCount = 1u;
		pi.pPoolSizes = &ps;
		if (vkCreateDescriptorPool(d, &pi, nullptr, &s_pool) != VK_SUCCESS)
			return false;
		for (uint32_t i = 0; i < 8u; ++i)
		{
			VkSamplerCreateInfo si{VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO};
			si.magFilter = si.minFilter = (i & 4u) ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;
			si.addressModeU = (i & 1u) ? VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE : VK_SAMPLER_ADDRESS_MODE_REPEAT;
			si.addressModeV = (i & 2u) ? VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE : VK_SAMPLER_ADDRESS_MODE_REPEAT;
			si.addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
			if (vkCreateSampler(d, &si, nullptr, &s_samplers[i]) != VK_SUCCESS)
				return false;
		}
		s_vs = g_vulkan_shader_cache->GetVertexShader(kVS);
		static const char* const kDefines[kFsVariants] = {"", "#define ALPHA_TEST 1\n", "#define NO_ALPHA 1\n", "#define NO_ALPHA 1\n#define ALPHA_TEST 1\n", "#define SHADOW 1\n"};
		bool compiled = s_vs != VK_NULL_HANDLE;
		for (int i = 0; i < kFsVariants; ++i)
		{
			s_fs[i] = g_vulkan_shader_cache->GetFragmentShader(std::string("#version 450\n") + kDefines[i] + kFS);
			compiled = compiled && s_fs[i] != VK_NULL_HANDLE;
		}
		if (!compiled)
		{
			std::fprintf(stderr, "hl_native: shaders did not compile\n");
			return false;
		}
		if (!s_vb.Create(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 16u << 20, "hl_native vertices") || !s_ib.Create(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, 8u << 20, "hl_native indices"))
			return false;
		s_renderPass = dev->GetRenderPass(color, depth);
		s_renderPassColor = dev->GetRenderPass(color, VK_FORMAT_UNDEFINED);
		if (!s_renderPass || !s_renderPassColor)
			return false;
		s_white = static_cast<GSTextureVK*>(g_gs_device->CreateTexture(1, 1, 1, GSTexture::Format::Color));
		if (!s_white)
			return false;
		const uint32_t white = 0xFFFFFFFFu;
		s_white->Update(GSVector4i(0, 0, 1, 1), &white, 4u);
		s_whiteTex.texture = s_white;
		return true;
	}

	// A texture from the GS's memory, as the stream has left it up to here.
	void readFromMemory(const NewTexture& n, std::vector<uint8_t>& out)
	{
		const GSLocalMemory& mem = g_gs_renderer->m_mem;
		const bool eight = n.psm == PSM_T8 || n.psm == PSM_T8H;
		uint32_t palette[256];
		for (uint32_t i = 0; i < (eight ? 256u : 16u); ++i)
		{
			const uint32_t at = eight ? ((i & 0xE7u) | ((i & 8u) << 1) | ((i & 16u) >> 1)) : i; // (the CLUT's own order of blocks)
			palette[i] = eight ? mem.ReadPixel32(static_cast<int>(at & 15u), static_cast<int>(at >> 4), n.cbp, 1u) : mem.ReadPixel32(static_cast<int>(at & 7u), static_cast<int>(at >> 3), n.cbp, 1u);
		}
		out.resize(static_cast<size_t>(n.w) * n.h * 4u);
		uint32_t* to = reinterpret_cast<uint32_t*>(out.data());
		for (int y = 0; y < n.h; ++y)
		{
			for (int x = 0; x < n.w; ++x)
			{
				uint32_t index;
				switch (n.psm)
				{
					case PSM_T8: index = mem.ReadPixel8(x, y, n.tbp, n.tbw); break;
					case PSM_T8H: index = mem.ReadPixel8H(x, y, n.tbp, n.tbw); break;
					case PSM_T4: index = mem.ReadPixel4(x, y, n.tbp, n.tbw); break;
					case PSM_T4HL: index = mem.ReadPixel4HL(x, y, n.tbp, n.tbw); break;
					default: index = mem.ReadPixel4HH(x, y, n.tbp, n.tbw); break;
				}
				*to++ = palette[index];
			}
		}
	}

	// The private targets, sized from the scene's; and the scene's own image, which PCSX2 may have replaced.
	void prepareTargets(GSTextureVK* color, float scale)
	{
		NativeTarget& scene = s_targets[kTScene];
		if (scene.tex.texture != color)
			scene.tex = GpuTexture{color};
		scene.sx = scene.sy = scale;
		const auto ensure = [&](NativeTarget& t, int w, int h) {
			if (t.tex.texture && t.tex.texture->GetWidth() == w && t.tex.texture->GetHeight() == h)
				return;
			t.tex = GpuTexture{static_cast<GSTextureVK*>(g_gs_device->CreateRenderTarget(w, h, GSTexture::Format::Color, false, false))};
			if (!t.tex.texture)
				std::fprintf(stderr, "hl_native: no target of %dx%d\n", w, h);
		};
		ensure(s_targets[kTShadow], color->GetWidth(), color->GetHeight());
		s_targets[kTShadow].sx = 2.0f * scale;
		s_targets[kTShadow].sy = (static_cast<float>(kSceneH) / 256.0f) * scale;
		ensure(s_targets[kTBloom], 256, 256);
		ensure(s_targets[kTHalf], 128, 128);
		ensure(s_targets[kTQuarter], 64, 64);
	}

	// GSDeviceVK::SetInitialState is private. (An explicit instantiation may name a private member.)
	struct InitialState
	{
		using Type = void (GSDeviceVK::*)(VkCommandBuffer);
		friend Type privateMember(InitialState);
	};
	template <typename Tag, typename Tag::Type Member>
	struct ExposePrivate
	{
		friend typename Tag::Type privateMember(Tag) { return Member; }
	};
	template struct ExposePrivate<InitialState, &GSDeviceVK::SetInitialState>;

	// One go through a frame's draws, with its own vertices or others in their place (fresh: the first time, its textures come in).
	void drawFrame(Frame* f, const Vertex* vertices, bool fresh)
	{
		GSDeviceVK* dev = GSDeviceVK::GetInstance();
		if (dev && g_gs_renderer && g_texture_cache && !s_gpuFailed)
		{
			g_gs_renderer->Flush(GSState::VSYNC);
			dev->EndRenderPass();

			// the textures this frame brought (the replay thread counts on them from now on, whatever happens to this frame)
			if (fresh)
			for (const NewTexture& n : f->textures)
			{
				if (s_gpuTextures.size() <= static_cast<size_t>(n.id))
					s_gpuTextures.resize(n.id + 1u);
				GSTextureVK* t = static_cast<GSTextureVK*>(g_gs_device->CreateTexture(n.w, n.h, 1, GSTexture::Format::Color));
				static std::vector<uint8_t> s_read;
				if (t && n.fromMemory)
				{
					readFromMemory(n, s_read);
					t->Update(GSVector4i(0, 0, n.w, n.h), s_read.data(), static_cast<u32>(n.w) * 4u);
				}
				else if (t)
					t->Update(GSVector4i(0, 0, n.w, n.h), n.rgba.data(), static_cast<u32>(n.w) * 4u);
				static const bool s_texDump = std::getenv("HL_TEX_DUMP") != nullptr;
				if (s_texDump)
				{
					char name[96];
					std::snprintf(name, sizeof(name), "hl_frames/tex_%d_%dx%d.rgba", n.id, n.w, n.h);
					if (FILE* out = std::fopen(name, "wb"))
					{
						std::fwrite(n.fromMemory ? s_read.data() : n.rgba.data(), 4u, static_cast<size_t>(n.w) * static_cast<size_t>(n.h), out);
						std::fclose(out);
					}
				}
				s_gpuTextures[n.id].texture = t;
			}

			GIFRegTEX0 frameTex{}, zTex{};
			frameTex.TBP0 = kSceneFbp * 32u;
			frameTex.TBW = kSceneFbw;
			frameTex.PSM = PSMCT32;
			zTex.TBP0 = kSceneZbp * 32u;
			zTex.TBW = kSceneFbw;
			zTex.PSM = PSMZ24;
			const GSVector2i size(kSceneW, kSceneH);
			const float scale = GSConfig.UpscaleMultiplier;
			const GSVector4i rc(0, 0, kSceneW, kSceneH);
			GSTextureCache::Target* rt = g_texture_cache->LookupDrawTarget(frameTex, size, scale, GSTextureCache::RenderTarget, true, 0u, false, true, true, rc);
			GSTextureCache::Target* ds = g_texture_cache->LookupDrawTarget(zTex, size, scale, GSTextureCache::DepthStencil, true, 0u, false, true, true, rc);
			{
				static GSTextureCache::Target* s_lastRt = nullptr;
				static uint32_t s_rtShown = 0;
				if (rt != s_lastRt && s_rtShown < 12u)
				{
					++s_rtShown;
					s_lastRt = rt;
					std::fprintf(stderr, "hl_native: scene target now %p tex %p (TBP %x TBW %u PSM %x, %dx%d, drawn %d %d %d %d)\n", static_cast<void*>(rt), rt ? static_cast<void*>(rt->m_texture) : nullptr,
						rt ? rt->m_TEX0.TBP0 : 0u, rt ? rt->m_TEX0.TBW : 0u, rt ? rt->m_TEX0.PSM : 0u, rt ? rt->m_texture->GetWidth() : 0, rt ? rt->m_texture->GetHeight() : 0,
						rt ? rt->m_drawn_since_read.x : 0, rt ? rt->m_drawn_since_read.y : 0, rt ? rt->m_drawn_since_read.z : 0, rt ? rt->m_drawn_since_read.w : 0);
				}
			}
			// (the GS has not drawn with one of them yet)
			if (!rt)
				rt = g_texture_cache->CreateTarget(frameTex, size, size, scale, GSTextureCache::RenderTarget, true, 0u, false, false, true, rc);
			if (!ds)
				ds = g_texture_cache->CreateTarget(zTex, size, size, scale, GSTextureCache::DepthStencil, true, 0u, false, false, true, rc);
			// (a GS draw would do this first: with something dirty in them, the GS's own reads of these targets go to its memory instead)
			static uint32_t s_dirtyShown = 0;
			if (rt && ds)
			{
				if (s_dirtyShown < 6u && (!rt->m_dirty.empty() || !ds->m_dirty.empty()))
				{
					++s_dirtyShown;
					std::fprintf(stderr, "hl_native: dirty parts in the targets: %zu %zu\n", rt->m_dirty.size(), ds->m_dirty.size());
				}
				rt->Update();
				ds->Update();
			}
			// the page the display reads, if this frame copies to it
			GSTextureCache::Target* displayRt = nullptr;
			const GSVector4i displayRc(0, 0, kDisplayW, kDisplayH);
			for (const Draw& d : f->draws)
			{
				if (d.key.target != kTDisplay)
					continue;
				GIFRegTEX0 displayTex{};
				displayTex.TBP0 = kDisplayFbp * 32u;
				displayTex.TBW = kSceneFbw;
				displayTex.PSM = PSMCT32;
				const GSVector2i displaySize(kDisplayW, kDisplayH);
				displayRt = g_texture_cache->LookupDrawTarget(displayTex, displaySize, scale, GSTextureCache::RenderTarget, true, 0u, false, true, true, displayRc);
				if (!displayRt)
					displayRt = g_texture_cache->CreateTarget(displayTex, displaySize, displaySize, scale, GSTextureCache::RenderTarget, true, 0u, false, false, true, displayRc);
				break;
			}
			GSTextureVK* color = rt ? static_cast<GSTextureVK*>(rt->m_texture) : nullptr;
			GSTextureVK* depth = ds ? static_cast<GSTextureVK*>(ds->m_texture) : nullptr;
			if (!color || !depth)
				std::fprintf(stderr, "hl_native: no targets (%p %p)\n", static_cast<void*>(rt), static_cast<void*>(ds));
			else
			{
				if (!s_gpuReady)
				{
					s_gpuReady = initGpu(dev, color->GetVkFormat(), depth->GetVkFormat());
					s_gpuFailed = !s_gpuReady;
					std::fprintf(stderr, "hl_native: %s (color format %d, depth format %d, target %dx%d, depth %dx%d, scale %.2f)\n", s_gpuReady ? "ready" : "FAILED",
						static_cast<int>(color->GetVkFormat()), static_cast<int>(depth->GetVkFormat()), color->GetWidth(), color->GetHeight(), depth->GetWidth(), depth->GetHeight(),
						static_cast<double>(scale));
				}
				if (s_gpuReady)
				{
					if (!f->draws.empty())
					{
						const uint32_t vbytes = static_cast<uint32_t>(f->vertices.size() * sizeof(Vertex));
						const uint32_t ibytes = static_cast<uint32_t>(f->indices.size() * sizeof(uint32_t));
						if (vbytes <= s_vb.GetCurrentSize() && ibytes <= s_ib.GetCurrentSize() && s_vb.ReserveMemory(vbytes, sizeof(Vertex)) && s_ib.ReserveMemory(ibytes, 4u))
						{
							std::memcpy(s_vb.GetCurrentHostPointer(), vertices ? vertices : f->vertices.data(), vbytes);
							std::memcpy(s_ib.GetCurrentHostPointer(), f->indices.data(), ibytes);
							const VkDeviceSize voffset = s_vb.GetCurrentOffset();
							const uint32_t ifirst = s_ib.GetCurrentOffset() / 4u;
							s_vb.CommitMemory(vbytes);
							s_ib.CommitMemory(ibytes);

							color->CommitClear();
							depth->CommitClear();
							if (s_white->GetLayout() != GSTextureVK::Layout::ShaderReadOnly)
								s_white->TransitionToLayout(GSTextureVK::Layout::ShaderReadOnly);
							const u64 fence = dev->GetCurrentFenceCounter();
							for (const Draw& d : f->draws)
							{
								if (d.key.tex < 0)
									continue;
								GSTextureVK* t = s_gpuTextures[d.key.tex].texture;
								if (t && t->GetLayout() != GSTextureVK::Layout::ShaderReadOnly)
									t->TransitionToLayout(GSTextureVK::Layout::ShaderReadOnly);
							}
							prepareTargets(color, scale);
							{
								GSTextureVK* displayImage = displayRt ? static_cast<GSTextureVK*>(displayRt->m_texture) : nullptr;
								NativeTarget& display = s_targets[kTDisplay];
								if (displayImage && display.tex.texture != displayImage)
									display.tex = GpuTexture{displayImage};
								display.sx = display.sy = scale;
							}
							VkCommandBuffer cmd = dev->GetCurrentCommandBuffer();
							const VkBuffer vb = s_vb.GetBuffer();
							// (what the scene's image holds of the logical size, in its own width and height: it has a little more)
							const float sceneU = static_cast<float>(kSceneW) * scale / static_cast<float>(color->GetWidth());
							const float sceneV = static_cast<float>(kSceneH) * scale / static_cast<float>(color->GetHeight());
							bool sceneDrawn = false;

							// in runs of the same target, with a render pass each
							for (size_t run = 0; run < f->draws.size();)
							{
								const uint32_t targetId = f->draws[run].key.target;
								size_t runEnd = run + 1u;
								while (runEnd < f->draws.size() && f->draws[runEnd].key.target == targetId)
									++runEnd;
								NativeTarget& target = s_targets[targetId];
								GSTextureVK* out = target.tex.texture;
								if (!out)
								{
									run = runEnd;
									continue;
								}
								const bool withDepth = targetId == kTScene || targetId == kTShadow;
								for (size_t i = run; i < runEnd; ++i)
								{
									if (f->draws[i].key.tex >= -1)
										continue;
									GSTextureVK* source = s_targets[targetOfTex(f->draws[i].key.tex)].tex.texture;
									if (source && source->GetLayout() != GSTextureVK::Layout::ShaderReadOnly)
										source->TransitionToLayout(GSTextureVK::Layout::ShaderReadOnly);
								}
								out->TransitionToLayout(GSTextureVK::Layout::ColorAttachment);
								if (withDepth)
									depth->TransitionToLayout(GSTextureVK::Layout::DepthStencilAttachment);
								const int w = withDepth ? std::min(out->GetWidth(), depth->GetWidth()) : out->GetWidth();
								const int h = withDepth ? std::min(out->GetHeight(), depth->GetHeight()) : out->GetHeight();
								VkRenderPassBeginInfo rpi{VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO};
								rpi.renderPass = withDepth ? s_renderPass : s_renderPassColor;
								rpi.framebuffer = out->GetLinkedFramebuffer(withDepth ? depth : nullptr, false, false);
								rpi.renderArea.extent = {static_cast<uint32_t>(w), static_cast<uint32_t>(h)};
								vkCmdBeginRenderPass(cmd, &rpi, VK_SUBPASS_CONTENTS_INLINE);
								vkCmdBindVertexBuffers(cmd, 0u, 1u, &vb, &voffset);
								vkCmdBindIndexBuffer(cmd, s_ib.GetBuffer(), 0u, VK_INDEX_TYPE_UINT32);
								const float cw = static_cast<float>(out->GetWidth()), ch = static_cast<float>(out->GetHeight());
								const VkViewport vp{0.0f, 0.0f, cw, ch, 0.0f, 1.0f};
								vkCmdSetViewport(cmd, 0u, 1u, &vp);

								PushConstants pc{};
								pc.scale[0] = 2.0f * target.sx / cw;
								pc.scale[1] = 2.0f * target.sy / ch;
								pc.scale[2] = 1.0f / cw - 1.0f;
								pc.scale[3] = 1.0f / ch - 1.0f;
								VkPipeline lastPipeline = VK_NULL_HANDLE;
								VkDescriptorSet lastSet = VK_NULL_HANDLE;
								uint16_t lastSc[4] = {0xFFFFu, 0xFFFFu, 0xFFFFu, 0xFFFFu};
								const auto drawPass = [&](const Draw& d, bool test, bool zwrite, float invert) {
									const VkPipeline p = getPipeline(dev, d.key, test, zwrite);
									if (p != lastPipeline)
									{
										vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, p);
										lastPipeline = p;
									}
									pc.test[0] = static_cast<float>(d.key.aref);
									pc.test[1] = invert;
									vkCmdPushConstants(cmd, s_layout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0u, sizeof(pc), &pc);
									vkCmdDrawIndexed(cmd, d.count, 1u, ifirst + d.first, 0, 0u);
								};
								for (size_t i = run; i < runEnd; ++i)
								{
									const Draw& d = f->draws[i];
									GpuTexture* gt = d.key.tex >= 0 ? &s_gpuTextures[d.key.tex] : (d.key.tex == -1 ? &s_whiteTex : &s_targets[targetOfTex(d.key.tex)].tex);
									if (!gt->texture)
										continue;
									gt->texture->SetUseFenceCounter(fence);
									const VkDescriptorSet set = setFor(dev, *gt, d.key.tex != -1 ? d.key.sampler : 0u);
									if (set == VK_NULL_HANDLE)
										continue;
									if (set != lastSet)
									{
										vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, s_layout, 0u, 1u, &set, 0u, nullptr);
										lastSet = set;
									}
									if (std::memcmp(lastSc, d.key.sc, sizeof(lastSc)) != 0)
									{
										std::memcpy(lastSc, d.key.sc, sizeof(lastSc));
										const int x0 = std::min(static_cast<int>(d.key.sc[0] * target.sx), w), y0 = std::min(static_cast<int>(d.key.sc[1] * target.sy), h);
										const int x1 = std::min(static_cast<int>((d.key.sc[2] + 1) * target.sx), w), y1 = std::min(static_cast<int>((d.key.sc[3] + 1) * target.sy), h);
										const VkRect2D sc{{x0, y0}, {static_cast<uint32_t>(std::max(x1 - x0, 0)), static_cast<uint32_t>(std::max(y1 - y0, 0))}};
										vkCmdSetScissor(cmd, 0u, 1u, &sc);
									}
									pc.fog[0] = static_cast<float>(d.key.fogcol & 255u) / 255.0f;
									pc.fog[1] = static_cast<float>((d.key.fogcol >> 8) & 255u) / 255.0f;
									pc.fog[2] = static_cast<float>((d.key.fogcol >> 16) & 255u) / 255.0f;
									pc.test[2] = d.key.textured ? 255.0f / 128.0f : 1.0f;
									const bool sceneSized = d.key.tex == texOfTarget(kTScene) || d.key.tex == texOfTarget(kTShadow);
									pc.uv[0] = sceneSized ? sceneU : 1.0f;
									pc.uv[1] = sceneSized ? sceneV : 1.0f;
									if (d.key.amode == kANone)
										drawPass(d, false, d.key.zwrite != 0u, 0.0f);
									else
									{
										drawPass(d, true, d.key.zwrite != 0u, 0.0f);
										if (d.key.amode == kAFbOnly)
											drawPass(d, true, false, 1.0f); // (what failed the test: colour, no Z)
									}
								}
								vkCmdEndRenderPass(cmd);
								out->SetUseFenceCounter(fence);
								out->SetState(GSTexture::State::Dirty);
								sceneDrawn = sceneDrawn || targetId == kTScene;
								run = runEnd;
							}
							(void)sceneDrawn;
							// (the device binds its vertex buffer once per command buffer, and no dirty flag brings it back: without
							// this, its next draws in this command buffer, the display's merge among them, read our vertices)
							(dev->*privateMember(InitialState{}))(cmd);
							color->SetUseFenceCounter(fence);
							depth->SetUseFenceCounter(fence);
							color->SetState(GSTexture::State::Dirty);
							depth->SetState(GSTexture::State::Dirty);
							dev->InvalidateCachedState();
							rt->UpdateValidity(rc);
							rt->UpdateDrawn(rc);
							// (PCSX2's GS may read this target as a texture, the copy to the display does: it only does so if these say
							// the target holds the colours and alpha, not GS memory)
							rt->m_valid_rgb = true;
							rt->m_valid_alpha_low = true;
							rt->m_valid_alpha_high = true;
							rt->m_alpha_max = 255;
							rt->m_alpha_min = 0;
							rt->m_alpha_range = true;
							ds->UpdateValidity(rc);
							ds->UpdateDrawn(rc);
							if (displayRt)
							{
								displayRt->UpdateValidity(displayRc);
								displayRt->UpdateDrawn(displayRc);
								displayRt->m_valid_rgb = true;
								static_cast<GSTextureVK*>(displayRt->m_texture)->SetState(GSTexture::State::Dirty);
								g_texture_cache->InvalidateVideoMem(g_gs_renderer->m_mem.GetOffset(kDisplayFbp * 32u, kSceneFbw, PSMCT32), displayRc, false);
							}
							// (what the GS made from these targets before is old now: its copy to the display must read them again)
							g_texture_cache->InvalidateVideoMem(g_gs_renderer->m_mem.GetOffset(kSceneFbp * 32u, kSceneFbw, PSMCT32), rc, false);
							g_texture_cache->InvalidateVideoMem(g_gs_renderer->m_mem.GetOffset(kSceneZbp * 32u, kSceneFbw, PSMZ24), rc, false);
						}
						else
							std::fprintf(stderr, "hl_native: frame too large (%u, %u bytes)\n", vbytes, ibytes);
					}
				}
			}
		}
	}

	void toPool(Frame* f)
	{
		std::lock_guard<std::mutex> lock(s_poolLock);
		s_framePool.push_back(f);
	}

	// ---- more pictures than the game makes (60 and 120). The game's logic works in fixed steps per frame, so running it faster
	// changes its speed: these are pictures drawn in between. A frame B that arrives is held back and shown as it comes out of
	// the frame before it, A: draws of the same kind and size, in the same order, are the same things, and their vertices move
	// from where they were in A to where they are in B. That is one picture late, 40 ms. What has no match shows as in B.
	struct VRange
	{
		uint32_t first, count;
	};
	Frame* s_prev = nullptr;
	std::vector<Vertex> s_lerp;
	std::vector<VRange> s_rangesA, s_rangesB;
	std::chrono::steady_clock::time_point s_intervalEnd{}, s_nextPresent{};
	uint32_t s_presents = 0;
	std::chrono::steady_clock::time_point s_presentsFrom{};
	uint32_t s_field = 0;

	void vertexRanges(const Frame& f, std::vector<VRange>& out)
	{
		out.resize(f.draws.size());
		for (size_t i = 0; i < f.draws.size(); ++i)
		{
			const Draw& d = f.draws[i];
			uint32_t lo = UINT32_MAX, hi = 0u;
			for (uint32_t k = d.first; k < d.first + d.count; ++k)
			{
				const uint32_t v = f.indices[k];
				lo = std::min(lo, v);
				hi = std::max(hi, v);
			}
			out[i] = d.count ? VRange{lo, hi - lo + 1u} : VRange{0u, 0u};
		}
	}

	// What of B comes from what of A. false: too little in common, a cut of the camera or a new screen.
	bool matchFrames(const Frame& a, const Frame& b)
	{
		vertexRanges(a, s_rangesA);
		vertexRanges(b, s_rangesB);
		s_matches.clear();
		size_t j = 0;
		uint64_t matched = 0, total = 0;
		constexpr size_t kWindow = 24;
		constexpr float kMaxMove = 96.0f; // (pixels of 512x448 in 40 ms: further than this is a jump, not a movement)
		for (size_t i = 0; i < b.draws.size(); ++i)
		{
			const Draw& db = b.draws[i];
			const VRange rb = s_rangesB[i];
			if (db.key.target == kTScene || db.key.target == kTShadow)
				total += rb.count;
			for (size_t k = j; k < a.draws.size() && k < j + kWindow; ++k)
			{
				const Draw& da = a.draws[k];
				if (da.count != db.count || s_rangesA[k].count != rb.count || !(da.key == db.key))
					continue;
				j = k + 1u;
				const Vertex* va = a.vertices.data() + s_rangesA[k].first;
				const Vertex* vb = b.vertices.data() + rb.first;
				bool same = true, near = true;
				for (uint32_t v = 0; v < rb.count; ++v)
				{
					const float dx = vb[v].x - va[v].x, dy = vb[v].y - va[v].y;
					if (dx != 0.0f || dy != 0.0f || vb[v].z != va[v].z)
						same = false;
					if (std::fabs(dx) > kMaxMove || std::fabs(dy) > kMaxMove)
					{
						near = false;
						break;
					}
				}
				if (near)
				{
					if (db.key.target == kTScene || db.key.target == kTShadow)
						matched += rb.count;
					if (!same)
						s_matches.push_back(DrawMatch{rb.first, s_rangesA[k].first, rb.count});
				}
				break;
			}
		}
		return total > 0u && matched * 10u >= total * 6u;
	}

	void buildBetween(const Frame& a, const Frame& b, float alpha)
	{
		s_lerp.resize(b.vertices.size());
		std::memcpy(s_lerp.data(), b.vertices.data(), b.vertices.size() * sizeof(Vertex));
		const float inv = 1.0f - alpha;
		for (const DrawMatch& m : s_matches)
		{
			const Vertex* va = a.vertices.data() + m.aFirst;
			const Vertex* vb = b.vertices.data() + m.bFirst;
			Vertex* out = s_lerp.data() + m.bFirst;
			for (uint32_t v = 0; v < m.count; ++v)
			{
				out[v].x = va[v].x * inv + vb[v].x * alpha;
				out[v].y = va[v].y * inv + vb[v].y * alpha;
				out[v].z = va[v].z * inv + vb[v].z * alpha;
				out[v].s = va[v].s * inv + vb[v].s * alpha;
				out[v].t = va[v].t * inv + vb[v].t * alpha;
				out[v].q = va[v].q * inv + vb[v].q * alpha;
			}
		}
	}

	void presentNow()
	{
		// (the stream's own vsyncs are not presented meanwhile: hl_gsx.cpp asks presentsHere)
		s_field ^= 1u;
		GSvsync(s_field, false);
		++s_presents;
		const auto now = std::chrono::steady_clock::now();
		const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(now - s_presentsFrom).count();
		if (ms >= 1000)
		{
			s_presentRate.store(static_cast<int>(s_presents * 1000 / ms), std::memory_order_relaxed);
			s_presents = 0u;
			s_presentsFrom = now;
		}
	}

	void execute(Frame* f)
	{
		const int rate = s_rate.load(std::memory_order_relaxed);
		bool picture = false;
		for (const Draw& d : f->draws)
			picture = picture || d.key.target == kTDisplay;
		if (rate <= 50 || !s_sceneOnShared.load(std::memory_order_relaxed))
		{
			s_presentsHere.store(false, std::memory_order_relaxed);
			s_presentRate.store(0, std::memory_order_relaxed);
			if (s_prev)
			{
				toPool(s_prev);
				s_prev = nullptr;
			}
			drawFrame(f, nullptr, true);
			toPool(f);
			s_inFlight.fetch_sub(1, std::memory_order_acq_rel);
			return;
		}
		s_presentsHere.store(true, std::memory_order_relaxed);
		if (!picture)
		{
			// (part of a picture: it goes as it is, and the picture comes with the rest)
			drawFrame(f, nullptr, true);
			toPool(f);
			s_inFlight.fetch_sub(1, std::memory_order_acq_rel);
			return;
		}
		using Clock = std::chrono::steady_clock;
		// the time of a picture of the game, a little less if pictures are piling up behind this one
		const auto period = std::chrono::microseconds(s_inFlight.load(std::memory_order_relaxed) >= 3 ? 30000 : 40000);
		const auto step = std::chrono::microseconds(1000000 / rate);
		const auto now = Clock::now();
		const Clock::time_point start = (now >= s_intervalEnd && now - s_intervalEnd < std::chrono::milliseconds(12)) ? s_intervalEnd : now;
		const Clock::time_point end = start + period;
		s_intervalEnd = end;
		if (s_nextPresent < now || s_nextPresent > now + step * 2)
			s_nextPresent = now;
		const bool between = s_prev && matchFrames(*s_prev, *f);
		bool fresh = true;
		while (s_nextPresent < end)
		{
			const auto wait = s_nextPresent - Clock::now();
			if (wait > std::chrono::microseconds(200))
				std::this_thread::sleep_for(wait);
			if (between)
			{
				const float alpha = std::chrono::duration<float>(s_nextPresent - start).count() / std::chrono::duration<float>(period).count();
				buildBetween(*s_prev, *f, std::min(alpha, 1.0f));
				drawFrame(f, s_lerp.data(), fresh);
			}
			else
				drawFrame(f, nullptr, fresh);
			fresh = false;
			presentNow();
			s_nextPresent += step;
			// (behind the clock: the pictures that cannot be made are not made)
			const auto after = Clock::now();
			if (s_nextPresent < after)
				s_nextPresent = after;
		}
		if (fresh)
			drawFrame(f, nullptr, true); // (no present fell in this time: its textures still come in)
		if (s_prev)
			toPool(s_prev);
		s_prev = f;
		s_inFlight.fetch_sub(1, std::memory_order_acq_rel);
	}

	void logStats()
	{
		if (s_stats.frames == 0u && s_stats.routedTags == 0u)
			return;
		std::string why;
		for (const auto& [name, count] : s_stats.why)
			why += std::string(" ") + (name[0] ? name : "?") + "=" + std::to_string(count);
		const uint64_t frames = std::max<uint64_t>(s_stats.frames, 1u);
		if (s_rate.load(std::memory_order_relaxed) > 50)
			std::fprintf(stderr, "[interp] v%llu: asked %d, presents %d a second, %zu draws moved\n", static_cast<unsigned long long>(s_vsync), s_rate.load(std::memory_order_relaxed),
				s_presentRate.load(std::memory_order_relaxed), s_matches.size());
		std::fprintf(stderr, "[native] v%llu: %llu flushes, %.0f draws/f, %.0f verts/f, %.0f tris/f, %llu tags dropped, %llu to the GS%s; %llu uploads, %llu new textures (%d)\n",
			static_cast<unsigned long long>(s_vsync), static_cast<unsigned long long>(s_stats.frames), static_cast<double>(s_stats.draws) / frames,
			static_cast<double>(s_stats.verts) / frames, static_cast<double>(s_stats.tris) / frames, static_cast<unsigned long long>(s_stats.dropped),
			static_cast<unsigned long long>(s_stats.routedTags), why.c_str(), static_cast<unsigned long long>(s_stats.uploads), static_cast<unsigned long long>(s_stats.miss), s_nextTexture);
		s_stats = Stats{};
	}
} // namespace

void setRate(int rate)
{
	s_rate.store(rate, std::memory_order_relaxed);
}

bool presentsHere()
{
	return s_presentsHere.load(std::memory_order_relaxed);
}

int presentRate()
{
	return s_presentRate.load(std::memory_order_relaxed);
}

uint32_t prompt()
{
	return s_prompt.load(std::memory_order_relaxed);
}

bool sceneOn()
{
	return s_sceneOnShared.load(std::memory_order_relaxed);
}

// (on the GS thread, in the stream's order)
static void applyAspect()
{
	const float shape = s_sceneOn ? s_shape : 4.0f / 3.0f;
	const AspectRatioType type = shape <= 1.34f ? AspectRatioType::R4_3
		: (shape > 1.77f && shape < 1.79f) ? AspectRatioType::R16_9
		: (shape > 2.21f && shape < 2.23f) ? AspectRatioType::R20_9
		: (shape > 2.32f && shape < 2.35f) ? AspectRatioType::R21_9
		: AspectRatioType::Stretch;
	MTGS::RunOnGSThread([type]() { EmuConfig.CurrentAspectRatio = type; });
}

void setShape(float aspect, float hudScale)
{
	s_shape = aspect;
	s_hudScale = hudScale;
	s_dirty = true;
	applyAspect();
}

bool transfer(uint8_t path, const uint8_t* data, uint32_t size)
{
	static const bool once = [] {
		const char* on = std::getenv("HL_NATIVE");
		s_on = !(on && on[0] == '0');
		const char* from = std::getenv("HL_NATIVE_FROM");
		s_from = from ? std::strtoull(from, nullptr, 10) : 0u;
		if (s_on)
		{
			const char* whole = std::getenv("HL_NATIVE_WHOLE");
			s_wholeTags = !(whole && whole[0] == '0');
			if (const char* cap = std::getenv("HL_NATIVE_CAP"))
			{
				std::string text = cap;
				const size_t a = text.find(':'), b = text.find(':', a + 1);
				if (a != std::string::npos && b != std::string::npos)
				{
					s_capFrom = std::strtoull(text.c_str() + a + 1, nullptr, 10);
					s_capUntil = s_capFrom + std::strtoull(text.c_str() + b + 1, nullptr, 10);
					s_cap = std::fopen(text.substr(0, a).c_str(), "wb");
				}
			}
			std::fprintf(stderr, "hl_native: waiting for a fight (not before vsync %llu)\n", static_cast<unsigned long long>(s_from));
		}
		return true;
	}();
	(void)once;
	if (!s_on)
		return false;
	if (!s_sceneOn)
	{
		observe(data, size);
		return false;
	}
	parse(path, data, size);
	return true;
}

void vsync()
{
	if (!s_on)
		return;
	flushFrame();
	if (s_cap && s_vsync >= s_capFrom && s_vsync < s_capUntil)
	{
		const uint32_t head[2] = {0xFFu, static_cast<uint32_t>(s_vsync)};
		std::fwrite(head, sizeof(head), 1, s_cap);
		std::fflush(s_cap);
	}
	++s_vsync;
	if (s_fightSeen)
	{
		s_fightSeen = false;
		s_fightFrames = s_vsyncsSinceFight <= 4u ? s_fightFrames + 1u : 1u;
		s_vsyncsSinceFight = 0u;
	}
	else if (s_vsyncsSinceFight < (1u << 30))
		++s_vsyncsSinceFight;
	if (!s_sceneOn && s_fightFrames >= kFightFramesToStart && s_vsyncsSinceFight <= 4u && s_vsync >= s_from)
	{
		s_sceneOn = true;
		s_sceneOnShared.store(true, std::memory_order_relaxed);
		applyAspect();
		s_dirty = true;
		s_gsInSync = true;
		toSmallCores();
		std::fprintf(stderr, "hl_native: scene on at vsync %llu\n", static_cast<unsigned long long>(s_vsync));
	}
	else if (s_sceneOn && s_vsyncsSinceFight > kFightVsyncsToStop)
	{
		// (the GS takes over from what the targets hold: it needs the registers and whatever was held back of the uploads)
		flushAll();
		s_sceneOn = false;
		s_sceneOnShared.store(false, std::memory_order_relaxed);
		s_prompt.store(0u, std::memory_order_relaxed);
		applyAspect();
		s_fightFrames = 0u;
		toAllCores();
		std::fprintf(stderr, "hl_native: scene off at vsync %llu\n", static_cast<unsigned long long>(s_vsync));
	}
	if ((s_vsync % 500u) == 0u)
		logStats();
}

void flushAll()
{
	if (s_on && s_sceneOn)
	{
		beforeGs(3u);
		emitPending(3u);
	}
}

bool next(uint8_t* path, const uint8_t** data, uint32_t* size)
{
	while (!s_ops.empty())
	{
		Op& op = s_ops.front();
		if (op.frame)
		{
			Frame* f = op.frame;
			s_ops.pop_front();
			if (s_cap && s_vsync >= s_capFrom && s_vsync < s_capUntil)
			{
				const uint32_t head[2] = {0xFEu, static_cast<uint32_t>(f->draws.size())};
				std::fwrite(head, sizeof(head), 1, s_cap);
			}
			MTGS::RunOnGSThread([f]() { execute(f); });
			continue;
		}
		s_outBytes.swap(op.bytes);
		if (s_cap && s_vsync >= s_capFrom && s_vsync < s_capUntil)
		{
			const uint32_t head[2] = {op.path, static_cast<uint32_t>(s_outBytes.size())};
			std::fwrite(head, sizeof(head), 1, s_cap);
			std::fwrite(s_outBytes.data(), 1, s_outBytes.size(), s_cap);
		}
		*path = op.path;
		*data = s_outBytes.data();
		*size = static_cast<uint32_t>(s_outBytes.size());
		s_ops.pop_front();
		return true;
	}
	return false;
}
} // namespace hl_native
