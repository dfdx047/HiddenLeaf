// The native fight renderer of Hidden Leaf (hl_native.cpp), between the game's GS stream and PCSX2's GS.
#pragma once

#include <cstdint>

namespace hl_native
{
	// All on the replay thread (the CPU Thread of the core), called by the stream's reader (hl_gsx.cpp).

	// A GS packet of the stream. true: this module took it, and what the GS must still see comes out of next().
	// false: it goes on to the GS as it is.
	bool transfer(uint8_t path, const uint8_t* data, uint32_t size);
	// A vsync of the stream: what was collected of the frame goes to the GS thread.
	void vsync();
	// Something else needs the GS to be up to date (a read back): everything collected goes out now.
	void flushAll();
	// The native renderer is drawing (a fight, the campaign).
	bool sceneOn();
	// The picture while it draws (width/height, 0 stretched to the screen) and what undoes the stretch on the HUD.
	void setShape(float aspect, float hudScale);
	// A packet to send to the GS ahead of whatever is read next. Runs what is queued for the GS thread on the way.
	bool next(uint8_t* path, const uint8_t** data, uint32_t* size);
} // namespace hl_native
