// Hidden Leaf: an interpreter for the EE (R5900) code the engine was not recompiled from (docs/plano.md, "Mods sem pacote
// de código"). It is a RecompiledFunction like the generated ones and keeps their contract: it runs from ctx->pc, a call
// goes through dispatchGuestBranch, a jump or a return leaves with ctx->pc on the target, a backward branch is an EE safe
// point (eeCheckpointDue), and it can be entered again at any instruction. Each instruction does what the PS2Recomp
// translators emit for it, with the same macros (ps2_runtime_macros.h), so it gives what the generated code gives.
//
// HL_INTERP=all: every generated function of the program (not the stubs: HL_INTERP_KEEP=<un5_config.toml>, its
// name@0xaddress entries) whose instructions are all covered goes on the interpreter; the rest stays native. The functions'
// bounds come from HL_INTERP_MAP=<un5_map.csv> (name,start,end: the generated table only has their entries and resume
// points, not every address). A test of the
// interpreter against the generated code: same route, same state (tools/hlbench.py diff).
// HL_INTERP=0xLO-0xHI[,...]: only the functions that start in those ranges.
#include "ps2_runtime_macros.h"
#include "ps2_runtime.h"

#include <algorithm>
#include <atomic>
#include <climits>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <map>
#include <regex>
#include <stdexcept>
#include <string>
#include <unordered_set>
#include <utility>
#include <vector>

void hlInterpRun(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime);

// (the runtime builds its sources in unity batches: everything here in its own namespace)
namespace hl_interp_detail
{
    using std::isnan; // (the FPU compare macros call it unqualified)

    enum : uint32_t
    {
        OP_SPECIAL = 0x00, OP_REGIMM = 0x01, OP_J = 0x02, OP_JAL = 0x03, OP_BEQ = 0x04, OP_BNE = 0x05, OP_BLEZ = 0x06,
        OP_BGTZ = 0x07, OP_ADDI = 0x08, OP_ADDIU = 0x09, OP_SLTI = 0x0A, OP_SLTIU = 0x0B, OP_ANDI = 0x0C, OP_ORI = 0x0D,
        OP_XORI = 0x0E, OP_LUI = 0x0F, OP_COP0 = 0x10, OP_COP1 = 0x11, OP_COP2 = 0x12, OP_BEQL = 0x14, OP_BNEL = 0x15,
        OP_BLEZL = 0x16, OP_BGTZL = 0x17, OP_DADDI = 0x18, OP_DADDIU = 0x19, OP_LDL = 0x1A, OP_LDR = 0x1B, OP_MMI = 0x1C,
        OP_LQ = 0x1E, OP_SQ = 0x1F, OP_LB = 0x20, OP_LH = 0x21, OP_LWL = 0x22, OP_LW = 0x23, OP_LBU = 0x24, OP_LHU = 0x25,
        OP_LWR = 0x26, OP_LWU = 0x27, OP_SB = 0x28, OP_SH = 0x29, OP_SWL = 0x2A, OP_SW = 0x2B, OP_SDL = 0x2C,
        OP_SDR = 0x2D, OP_SWR = 0x2E, OP_CACHE = 0x2F, OP_LL = 0x30, OP_LWC1 = 0x31, OP_PREF = 0x33, OP_LQC2 = 0x36,
        OP_LD = 0x37, OP_SC = 0x38, OP_SWC1 = 0x39, OP_SQC2 = 0x3E, OP_SD = 0x3F,
    };

    struct Fields
    {
        uint32_t w, op, rs, rt, rd, sa, fn, imm;
        int32_t simm;
        explicit Fields(uint32_t word)
            : w(word), op(word >> 26), rs((word >> 21) & 31u), rt((word >> 16) & 31u), rd((word >> 11) & 31u),
              sa((word >> 6) & 31u), fn(word & 63u), imm(word & 0xFFFFu), simm(static_cast<int16_t>(word & 0xFFFFu))
        {
        }
    };

    [[noreturn]] void unhandled(const char *what, uint32_t pc, uint32_t w)
    {
        // what the generated code does for an instruction its translator does not handle
        char text[160];
        std::snprintf(text, sizeof(text), "%s at 0x%X raw=0x%08X", what, pc, w);
        throw std::runtime_error(text);
    }

// X(...): the instruction's work, run only when it is not a dry pass (the dry pass answers "is it covered?")
#define X(...)                    \
    do                            \
    {                             \
        if constexpr (!Dry)       \
        {                         \
            __VA_ARGS__;          \
        }                         \
        return true;              \
    } while (0)
// T(text): an instruction the translator emits as a throw
#define T(text) X(unhandled(text, pc, f.w))

    template <bool Dry>
    bool execSpecial(const Fields &f, uint32_t pc, uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t rs = f.rs, rt = f.rt, rd = f.rd, sa = f.sa;
        switch (f.fn)
        {
        case 0x00: // SLL (sll $0,... is a nop)
            if (rd == 0)
                X((void)0);
            X(SET_GPR_S32(ctx, rd, (int32_t)SLL32(GPR_U32(ctx, rt), sa)));
        case 0x02: X(SET_GPR_S32(ctx, rd, (int32_t)SRL32(GPR_U32(ctx, rt), sa)));
        case 0x03: X(SET_GPR_S32(ctx, rd, SRA32(GPR_S32(ctx, rt), sa)));
        case 0x04: X(SET_GPR_S32(ctx, rd, (int32_t)SLL32(GPR_U32(ctx, rt), GPR_U32(ctx, rs) & 0x1F)));
        case 0x06: X(SET_GPR_S32(ctx, rd, (int32_t)SRL32(GPR_U32(ctx, rt), GPR_U32(ctx, rs) & 0x1F)));
        case 0x07: X(SET_GPR_S32(ctx, rd, SRA32(GPR_S32(ctx, rt), GPR_U32(ctx, rs) & 0x1F)));
        case 0x0A: X(if (GPR_U64(ctx, rt) == 0) SET_GPR_VEC(ctx, rd, GPR_VEC(ctx, rs)));
        case 0x0B: X(if (GPR_U64(ctx, rt) != 0) SET_GPR_VEC(ctx, rd, GPR_VEC(ctx, rs)));
        case 0x0C: // SYSCALL
            X(ctx->pc = pc + 4u; runtime->handleSyscall(rdram, ctx, (f.w >> 6) & 0xFFFFFu));
        case 0x0D: X(runtime->handleBreak(rdram, ctx));
        case 0x0F: X((void)0); // SYNC
        case 0x10: X(SET_GPR_U64(ctx, rd, ctx->hi));
        case 0x11: X(ctx->hi = GPR_U64(ctx, rs));
        case 0x12: X(SET_GPR_U64(ctx, rd, ctx->lo));
        case 0x13: X(ctx->lo = GPR_U64(ctx, rs));
        case 0x14: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rt) << (GPR_U32(ctx, rs) & 0x3F)));
        case 0x16: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rt) >> (GPR_U32(ctx, rs) & 0x3F)));
        case 0x17: X(SET_GPR_S64(ctx, rd, GPR_S64(ctx, rt) >> (GPR_U32(ctx, rs) & 0x3F)));
        case 0x18: // MULT (with the R5900's rd)
            X({
                int64_t result = (int64_t)GPR_S32(ctx, rs) * (int64_t)GPR_S32(ctx, rt);
                ctx->lo = (uint64_t)(int64_t)(int32_t)result;
                ctx->hi = (uint64_t)(int64_t)(int32_t)(result >> 32);
                if (rd != 0)
                    SET_GPR_S32(ctx, rd, (int32_t)result);
            });
        case 0x19: // MULTU
            X({
                uint64_t result = (uint64_t)GPR_U32(ctx, rs) * (uint64_t)GPR_U32(ctx, rt);
                ctx->lo = (uint64_t)(int64_t)(int32_t)result;
                ctx->hi = (uint64_t)(int64_t)(int32_t)(result >> 32);
                if (rd != 0)
                    SET_GPR_S32(ctx, rd, (int32_t)result);
            });
        case 0x1A: // DIV
            X({
                int32_t divisor = GPR_S32(ctx, rt);
                int32_t dividend = GPR_S32(ctx, rs);
                if (divisor != 0)
                {
                    if (divisor == -1 && dividend == INT32_MIN)
                    {
                        ctx->lo = (uint64_t)(int64_t)INT32_MIN;
                        ctx->hi = 0;
                    }
                    else
                    {
                        ctx->lo = (uint64_t)(int64_t)(dividend / divisor);
                        ctx->hi = (uint64_t)(int64_t)(dividend % divisor);
                    }
                }
                else
                {
                    ctx->lo = (dividend < 0) ? 1ull : 0xFFFFFFFFFFFFFFFFull;
                    ctx->hi = (uint64_t)(int64_t)dividend;
                }
            });
        case 0x1B: // DIVU
            X({
                uint32_t divisor = GPR_U32(ctx, rt);
                if (divisor != 0)
                {
                    ctx->lo = (uint64_t)(int64_t)(int32_t)(GPR_U32(ctx, rs) / divisor);
                    ctx->hi = (uint64_t)(int64_t)(int32_t)(GPR_U32(ctx, rs) % divisor);
                }
                else
                {
                    ctx->lo = 0xFFFFFFFFFFFFFFFFull;
                    ctx->hi = (uint64_t)(int64_t)(int32_t)GPR_U32(ctx, rs);
                }
            });
        case 0x20: // ADD
            X({
                int32_t rsVal = GPR_S32(ctx, rs);
                int32_t rtVal = GPR_S32(ctx, rt);
                int64_t result = (int64_t)rsVal + (int64_t)rtVal;
                if (result > INT32_MAX || result < INT32_MIN)
                    runtime->SignalException(ctx, EXCEPTION_INTEGER_OVERFLOW);
                else
                    SET_GPR_S32(ctx, rd, (int32_t)result);
            });
        case 0x21: X(SET_GPR_S32(ctx, rd, (int32_t)ADD32(GPR_U32(ctx, rs), GPR_U32(ctx, rt))));
        case 0x22: // SUB
            X({
                uint32_t tmp;
                bool ov;
                SUB32_OV(GPR_U32(ctx, rs), GPR_U32(ctx, rt), tmp, ov);
                if (ov)
                    runtime->SignalException(ctx, EXCEPTION_INTEGER_OVERFLOW);
                else
                    SET_GPR_S32(ctx, rd, (int32_t)tmp);
            });
        case 0x23: X(SET_GPR_S32(ctx, rd, (int32_t)SUB32(GPR_U32(ctx, rs), GPR_U32(ctx, rt))));
        case 0x24: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rs) & GPR_U64(ctx, rt)));
        case 0x25: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rs) | GPR_U64(ctx, rt)));
        case 0x26: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rs) ^ GPR_U64(ctx, rt)));
        case 0x27: X(SET_GPR_U64(ctx, rd, ~(GPR_U64(ctx, rs) | GPR_U64(ctx, rt))));
        case 0x28: X(SET_GPR_U32(ctx, rd, ctx->sa));
        case 0x29: X(ctx->sa = GPR_U32(ctx, rs) & 0x7F);
        case 0x2A: X(SET_GPR_U64(ctx, rd, ((int64_t)GPR_S64(ctx, rs) < (int64_t)GPR_S64(ctx, rt)) ? 1 : 0));
        case 0x2B: X(SET_GPR_U64(ctx, rd, ((uint64_t)GPR_U64(ctx, rs) < (uint64_t)GPR_U64(ctx, rt)) ? 1 : 0));
        case 0x2C: // DADD
            X({
                int64_t a = (int64_t)GPR_S64(ctx, rs);
                int64_t b = (int64_t)GPR_S64(ctx, rt);
                int64_t r = (int64_t)((uint64_t)a + (uint64_t)b);
                if (((a ^ b) >= 0) && ((a ^ r) < 0))
                    runtime->SignalException(ctx, EXCEPTION_INTEGER_OVERFLOW);
                else
                    SET_GPR_S64(ctx, rd, r);
            });
        case 0x2D: X(SET_GPR_U64(ctx, rd, (uint64_t)GPR_U64(ctx, rs) + (uint64_t)GPR_U64(ctx, rt)));
        case 0x2E: // DSUB
            X({
                int64_t a = (int64_t)GPR_S64(ctx, rs);
                int64_t b = (int64_t)GPR_S64(ctx, rt);
                int64_t r = (int64_t)((uint64_t)a - (uint64_t)b);
                if (((a ^ b) < 0) && ((a ^ r) < 0))
                    runtime->SignalException(ctx, EXCEPTION_INTEGER_OVERFLOW);
                else
                    SET_GPR_S64(ctx, rd, r);
            });
        case 0x2F: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rs) - GPR_U64(ctx, rt)));
        case 0x30: X(if (GPR_S64(ctx, rs) >= GPR_S64(ctx, rt)) runtime->handleTrap(rdram, ctx));
        case 0x31: X(if (GPR_U64(ctx, rs) >= GPR_U64(ctx, rt)) runtime->handleTrap(rdram, ctx));
        case 0x32: X(if (GPR_S64(ctx, rs) < GPR_S64(ctx, rt)) runtime->handleTrap(rdram, ctx));
        case 0x33: X(if (GPR_U64(ctx, rs) < GPR_U64(ctx, rt)) runtime->handleTrap(rdram, ctx));
        case 0x34: X(if (GPR_U64(ctx, rs) == GPR_U64(ctx, rt)) runtime->handleTrap(rdram, ctx));
        case 0x36: X(if (GPR_U64(ctx, rs) != GPR_U64(ctx, rt)) runtime->handleTrap(rdram, ctx));
        case 0x38: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rt) << sa));
        case 0x3A: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rt) >> sa));
        case 0x3B: X(SET_GPR_S64(ctx, rd, GPR_S64(ctx, rt) >> sa));
        case 0x3C: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rt) << (32 + sa)));
        case 0x3E: X(SET_GPR_U64(ctx, rd, GPR_U64(ctx, rt) >> (32 + sa)));
        case 0x3F: X(SET_GPR_S64(ctx, rd, GPR_S64(ctx, rt) >> (32 + sa)));
        default:
            T("Unhandled SPECIAL instruction");
        }
    }

    template <bool Dry>
    bool execRegimm(const Fields &f, uint32_t pc, uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t rs = f.rs;
        const int32_t simm = f.simm;
        switch (f.rt)
        {
        case 0x08: X(if (GPR_S64(ctx, rs) >= (int64_t)(int32_t)simm) runtime->handleTrap(rdram, ctx));
        case 0x09: X(if (GPR_U64(ctx, rs) >= (uint64_t)(int64_t)(int32_t)simm) runtime->handleTrap(rdram, ctx));
        case 0x0A: X(if (GPR_S64(ctx, rs) < (int64_t)(int32_t)simm) runtime->handleTrap(rdram, ctx));
        case 0x0B: X(if (GPR_U64(ctx, rs) < (uint64_t)(int64_t)(int32_t)simm) runtime->handleTrap(rdram, ctx));
        case 0x0C: X(if (GPR_S64(ctx, rs) == (int64_t)(int32_t)simm) runtime->handleTrap(rdram, ctx));
        case 0x0E: X(if (GPR_S64(ctx, rs) != (int64_t)(int32_t)simm) runtime->handleTrap(rdram, ctx));
        case 0x18: X(ctx->sa = ((GPR_U32(ctx, rs) ^ (uint32_t)simm) & 0xF) << 3);
        case 0x19: X(ctx->sa = ((GPR_U32(ctx, rs) ^ (uint32_t)simm) & 0x7) << 4);
        default:
            T("Unhandled REGIMM instruction");
        }
    }

    template <bool Dry>
    bool execCop0(const Fields &f, uint32_t pc, uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t rt = f.rt;
        switch (f.rs)
        {
        case 0x00: // MFC0
            switch (f.rd)
            {
            case 0: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_index));
            case 1: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_random));
            case 2: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_entrylo0));
            case 3: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_entrylo1));
            case 4: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_context));
            case 5: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_pagemask));
            case 6: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_wired));
            case 8: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_badvaddr));
            case 9: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_count));
            case 10: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_entryhi));
            case 11: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_compare));
            case 12: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_status));
            case 13: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_cause));
            case 14: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_epc));
            case 15: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_prid));
            case 16: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_config));
            case 23: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_badpaddr));
            case 24: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_debug));
            case 25: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_perf));
            case 28: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_taglo));
            case 29: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_taghi));
            case 30: X(SET_GPR_S32(ctx, rt, (int32_t)ctx->cop0_errorepc));
            default: X(SET_GPR_S32(ctx, rt, 0));
            }
        case 0x04: // MTC0
            switch (f.rd)
            {
            case 0: X(ctx->cop0_index = GPR_U32(ctx, rt) & 0x3F);
            case 2: X(ctx->cop0_entrylo0 = GPR_U32(ctx, rt) & 0x3FFFFFFF);
            case 3: X(ctx->cop0_entrylo1 = GPR_U32(ctx, rt) & 0x3FFFFFFF);
            case 4: X(ctx->cop0_context = (ctx->cop0_context & 0xFF800000) | (GPR_U32(ctx, rt) & 0x7FFFFF));
            case 5: X(ctx->cop0_pagemask = GPR_U32(ctx, rt) & 0x01FFE000);
            case 6: X(ctx->cop0_wired = GPR_U32(ctx, rt) & 0x3F; ctx->cop0_random = 47);
            case 9: X(ctx->cop0_count = GPR_U32(ctx, rt));
            case 10: X(ctx->cop0_entryhi = GPR_U32(ctx, rt) & 0xC00000FF);
            case 11: X(ctx->cop0_compare = GPR_U32(ctx, rt); ctx->cop0_cause &= ~0x8000);
            case 12: X(ctx->cop0_status = GPR_U32(ctx, rt) & 0xFF57FFFF);
            case 13: X(ctx->cop0_cause = (ctx->cop0_cause & ~0x00000300) | (GPR_U32(ctx, rt) & 0x00000300));
            case 14: X(ctx->cop0_epc = GPR_U32(ctx, rt));
            case 16: X(ctx->cop0_config = (ctx->cop0_config & ~0x7) | (GPR_U32(ctx, rt) & 0x7));
            case 24: X(ctx->cop0_debug = GPR_U32(ctx, rt));
            case 25: X(ctx->cop0_perf = GPR_U32(ctx, rt));
            case 28: X(ctx->cop0_taglo = GPR_U32(ctx, rt));
            case 29: X(ctx->cop0_taghi = GPR_U32(ctx, rt));
            case 30: X(ctx->cop0_errorepc = GPR_U32(ctx, rt));
            default: X((void)0); // (read-only or not kept: ignored)
            }
        case 0x10: // CO
            switch (f.fn)
            {
            case 0x01: X(runtime->handleTLBR(rdram, ctx));
            case 0x02: X(runtime->handleTLBWI(rdram, ctx));
            case 0x06: X(runtime->handleTLBWR(rdram, ctx));
            case 0x08: X(runtime->handleTLBP(rdram, ctx));
            case 0x38: X(ctx->cop0_status |= 0x10000);
            case 0x39: X(ctx->cop0_status &= ~0x10000);
            default: T("Unhandled COP0 CO-OP"); // (ERET is a jump: run())
            }
        default:
            T("Unhandled COP0 instruction format");
        }
    }

    template <bool Dry>
    bool execCop1(const Fields &f, uint32_t pc, uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        (void)rdram;
        (void)runtime;
        const uint32_t ft = f.rt, fs = f.rd, fd = f.sa;
#define FCMP(cond) X(ctx->fcr31 = (cond(ctx->f[fs], ctx->f[ft])) ? (ctx->fcr31 | 0x800000) : (ctx->fcr31 & ~0x800000))
        switch (f.rs)
        {
        case 0x00: X({ uint32_t bits; std::memcpy(&bits, &ctx->f[fs], sizeof(bits)); SET_GPR_U32(ctx, ft, bits); });
        case 0x04: X({ uint32_t bits = GPR_U32(ctx, ft); std::memcpy(&ctx->f[fs], &bits, sizeof(bits)); });
        case 0x02: // CFC1
            if (fs == 31)
                X(SET_GPR_U32(ctx, ft, ctx->fcr31));
            X(SET_GPR_U32(ctx, ft, 0));
        case 0x06: // CTC1
            if (fs == 31)
                X(ctx->fcr31 = GPR_U32(ctx, ft) & 0x0183FFFF);
            X((void)0);
        case 0x10: // S
            switch (f.fn)
            {
            case 0x00: X(ctx->f[fd] = FPU_ADD_S(ctx->f[fs], ctx->f[ft]));
            case 0x01: X(ctx->f[fd] = FPU_SUB_S(ctx->f[fs], ctx->f[ft]));
            case 0x02: X(ctx->f[fd] = FPU_MUL_S(ctx->f[fs], ctx->f[ft]));
            case 0x03: X(if (ctx->f[ft] == 0.0f) ctx->fcr31 |= 0x100000; ctx->f[fd] = FPU_DIV_S(ctx->f[fs], ctx->f[ft]));
            case 0x04: X(ctx->f[fd] = FPU_SQRT_S(ctx->f[ft]));
            case 0x05: X(ctx->f[fd] = FPU_ABS_S(ctx->f[fs]));
            case 0x06: X(ctx->f[fd] = FPU_MOV_S(ctx->f[fs]));
            case 0x07: X(ctx->f[fd] = FPU_NEG_S(ctx->f[fs]));
            case 0x0C: X({ int32_t tmp = FPU_ROUND_W_S(ctx->f[fs]); std::memcpy(&ctx->f[fd], &tmp, sizeof(tmp)); });
            case 0x0D: X({ int32_t tmp = FPU_TRUNC_W_S(ctx->f[fs]); std::memcpy(&ctx->f[fd], &tmp, sizeof(tmp)); });
            case 0x0E: X({ int32_t tmp = FPU_CEIL_W_S(ctx->f[fs]); std::memcpy(&ctx->f[fd], &tmp, sizeof(tmp)); });
            case 0x0F: X({ int32_t tmp = FPU_FLOOR_W_S(ctx->f[fs]); std::memcpy(&ctx->f[fd], &tmp, sizeof(tmp)); });
            case 0x24: X({ int32_t tmp = FPU_CVT_W_S(ctx->f[fs]); std::memcpy(&ctx->f[fd], &tmp, sizeof(tmp)); });
            case 0x16: X(ctx->f[fd] = FPU_RSQRT_S(ctx->f[fs], ctx->f[ft]));
            case 0x18: X(FPU_SET_ACC(ctx, FPU_ADD_S(ctx->f[fs], ctx->f[ft])));
            case 0x19: X(FPU_SET_ACC(ctx, FPU_SUB_S(ctx->f[fs], ctx->f[ft])));
            case 0x1A: X(FPU_SET_ACC(ctx, FPU_MUL_S(ctx->f[fs], ctx->f[ft])));
            case 0x1C: X(ctx->f[fd] = FPU_ADD_S(ctx->f_acc, FPU_MUL_S(ctx->f[fs], ctx->f[ft])));
            case 0x1D: X(ctx->f[fd] = FPU_SUB_S(ctx->f_acc, FPU_MUL_S(ctx->f[fs], ctx->f[ft])));
            case 0x1E: X(FPU_SET_ACC(ctx, FPU_ADD_S(ctx->f_acc, FPU_MUL_S(ctx->f[fs], ctx->f[ft]))));
            case 0x1F: X(FPU_SET_ACC(ctx, FPU_SUB_S(ctx->f_acc, FPU_MUL_S(ctx->f[fs], ctx->f[ft]))));
            case 0x28: X(ctx->f[fd] = std::max(ctx->f[fs], ctx->f[ft]));
            case 0x29: X(ctx->f[fd] = std::min(ctx->f[fs], ctx->f[ft]));
            case 0x30: X(ctx->fcr31 &= ~0x800000);
            case 0x31: FCMP(FPU_C_UN_S);
            case 0x32: FCMP(FPU_C_EQ_S);
            case 0x33: FCMP(FPU_C_UEQ_S);
            case 0x34: FCMP(FPU_C_OLT_S);
            case 0x35: FCMP(FPU_C_ULT_S);
            case 0x36: FCMP(FPU_C_OLE_S);
            case 0x37: FCMP(FPU_C_ULE_S);
            case 0x38: X(ctx->fcr31 &= ~0x800000);
            case 0x39: FCMP(FPU_C_NGLE_S);
            case 0x3A: FCMP(FPU_C_SEQ_S);
            case 0x3B: FCMP(FPU_C_NGL_S);
            case 0x3C: FCMP(FPU_C_LT_S);
            case 0x3D: FCMP(FPU_C_NGE_S);
            case 0x3E: FCMP(FPU_C_LE_S);
            case 0x3F: FCMP(FPU_C_NGT_S);
            default: T("Unhandled FPU.S instruction");
            }
        case 0x14: // W
            if (f.fn == 0x20)
                X({ int32_t tmp; std::memcpy(&tmp, &ctx->f[fs], sizeof(tmp)); ctx->f[fd] = FPU_CVT_S_W(tmp); });
            T("Unhandled FPU.W instruction");
        default:
            T("Unhandled FPU instruction");
        }
#undef FCMP
    }

    template <bool Dry>
    bool execMmi(const Fields &f, uint32_t pc, uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        (void)rdram;
        (void)runtime;
        (void)pc;
        const uint32_t rs = f.rs, rt = f.rt, rd = f.rd, sa = f.sa;
// the multiply-accumulates: acc = hi:lo (or hi1:lo1), then the sum or difference back, sign-extended halves, and rd
#define MACC(HI, LO, PT, GET, OPER)                                                \
    X({                                                                            \
        uint64_t acc = Ps2HiLoToU64(ctx->HI, ctx->LO);                             \
        PT prod = (PT)GET(ctx, rs) * (PT)GET(ctx, rt);                             \
        PT result = (PT)(acc OPER (uint64_t)prod);                                 \
        ctx->LO = Ps2SignExt32ToU64((uint32_t)result);                             \
        ctx->HI = Ps2SignExt32ToU64((uint32_t)((uint64_t)result >> 32));           \
        if (rd != 0)                                                               \
            SET_GPR_S32(ctx, rd, (int32_t)result);                                 \
    })
        switch (f.fn)
        {
        case 0x10: X(SET_GPR_U64(ctx, rd, ctx->hi1));
        case 0x11: X(ctx->hi1 = GPR_U64(ctx, rs));
        case 0x12: X(SET_GPR_U64(ctx, rd, ctx->lo1));
        case 0x13: X(ctx->lo1 = GPR_U64(ctx, rs));
        case 0x18: // MULT1
            X({
                int64_t result = (int64_t)GPR_S32(ctx, rs) * (int64_t)GPR_S32(ctx, rt);
                ctx->lo1 = (uint64_t)(int64_t)(int32_t)result;
                ctx->hi1 = (uint64_t)(int64_t)(int32_t)(result >> 32);
                if (rd != 0)
                    SET_GPR_S32(ctx, rd, (int32_t)result);
            });
        case 0x19: // MULTU1
            X({
                uint64_t result = (uint64_t)GPR_U32(ctx, rs) * (uint64_t)GPR_U32(ctx, rt);
                ctx->lo1 = (uint64_t)(int64_t)(int32_t)result;
                ctx->hi1 = (uint64_t)(int64_t)(int32_t)(result >> 32);
                if (rd != 0)
                    SET_GPR_S32(ctx, rd, (int32_t)result);
            });
        case 0x1A: // DIV1
            X({
                int32_t divisor = GPR_S32(ctx, rt);
                int32_t dividend = GPR_S32(ctx, rs);
                if (divisor != 0)
                {
                    if (divisor == -1 && dividend == INT32_MIN)
                    {
                        ctx->lo1 = (uint64_t)(int64_t)INT32_MIN;
                        ctx->hi1 = 0;
                    }
                    else
                    {
                        ctx->lo1 = (uint64_t)(int64_t)(dividend / divisor);
                        ctx->hi1 = (uint64_t)(int64_t)(dividend % divisor);
                    }
                }
                else
                {
                    ctx->lo1 = (dividend < 0) ? 1ull : 0xFFFFFFFFFFFFFFFFull;
                    ctx->hi1 = (uint64_t)(int64_t)dividend;
                }
            });
        case 0x1B: // DIVU1
            X({
                uint32_t divisor = GPR_U32(ctx, rt);
                if (divisor != 0)
                {
                    ctx->lo1 = (uint64_t)(int64_t)(int32_t)(GPR_U32(ctx, rs) / divisor);
                    ctx->hi1 = (uint64_t)(int64_t)(int32_t)(GPR_U32(ctx, rs) % divisor);
                }
                else
                {
                    ctx->lo1 = 0xFFFFFFFFFFFFFFFFull;
                    ctx->hi1 = (uint64_t)(int64_t)(int32_t)GPR_U32(ctx, rs);
                }
            });
        case 0x00: MACC(hi, lo, int64_t, GPR_S32, +);
        case 0x01: MACC(hi, lo, uint64_t, GPR_U32, +);
        case 0x02: MACC(hi, lo, int64_t, GPR_S32, -);
        case 0x03: MACC(hi, lo, uint64_t, GPR_U32, -);
        case 0x20: MACC(hi1, lo1, int64_t, GPR_S32, +);
        case 0x21: MACC(hi1, lo1, uint64_t, GPR_U32, +);
        case 0x04: // PLZCW
            X({
                uint64_t v = GPR_U64(ctx, rs);
                uint32_t lo = (uint32_t)(v & 0xFFFFFFFFu);
                uint32_t hi = (uint32_t)(v >> 32);
                SET_GPR_U64(ctx, rd, ((uint64_t)ps2_plzcw32(hi) << 32) | (uint64_t)ps2_plzcw32(lo));
            });
        case 0x34: X(SET_GPR_VEC(ctx, rd, _mm_slli_epi16(GPR_VEC(ctx, rt), sa)));
        case 0x36: X(SET_GPR_VEC(ctx, rd, _mm_srli_epi16(GPR_VEC(ctx, rt), sa)));
        case 0x37: X(SET_GPR_VEC(ctx, rd, _mm_srai_epi16(GPR_VEC(ctx, rt), sa)));
        case 0x3C: X(SET_GPR_VEC(ctx, rd, _mm_slli_epi32(GPR_VEC(ctx, rt), sa)));
        case 0x3E: X(SET_GPR_VEC(ctx, rd, _mm_srli_epi32(GPR_VEC(ctx, rt), sa)));
        case 0x3F: X(SET_GPR_VEC(ctx, rd, _mm_srai_epi32(GPR_VEC(ctx, rt), sa)));
        case 0x28: // MMI1
            if (sa == 0x18) // PADDUB
                X(SET_GPR_VEC(ctx, rd, _mm_adds_epu8(GPR_VEC(ctx, rs), GPR_VEC(ctx, rt))));
            return false;
        case 0x09: // MMI2
            switch (sa)
            {
            case 0x0E: X(SET_GPR_VEC(ctx, rd, PS2_PCPYLD(GPR_VEC(ctx, rs), GPR_VEC(ctx, rt))));
            case 0x12: X(SET_GPR_VEC(ctx, rd, PS2_PAND(GPR_VEC(ctx, rs), GPR_VEC(ctx, rt))));
            case 0x13: X(SET_GPR_VEC(ctx, rd, PS2_PXOR(GPR_VEC(ctx, rs), GPR_VEC(ctx, rt))));
            default: return false;
            }
        case 0x29: // MMI3
            switch (sa)
            {
            case 0x0E: X(SET_GPR_VEC(ctx, rd, _mm_unpackhi_epi64(GPR_VEC(ctx, rs), GPR_VEC(ctx, rt))));
            case 0x12: X(SET_GPR_VEC(ctx, rd, PS2_POR(GPR_VEC(ctx, rs), GPR_VEC(ctx, rt))));
            case 0x13: X(SET_GPR_VEC(ctx, rd, PS2_PNOR(GPR_VEC(ctx, rs), GPR_VEC(ctx, rt))));
            default: return false;
            }
        default:
            return false; // (MMI0, PMFHL, PMTHL, the rest of MMI1-3: not yet)
        }
#undef MACC
    }

    // A non-branch instruction. False: not covered yet (the dry pass keeps such a function native).
    template <bool Dry>
    bool exec(uint32_t w, uint32_t pc, uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const Fields f(w);
        const uint32_t rs = f.rs, rt = f.rt;
        const int32_t simm = f.simm;
        switch (f.op)
        {
        case OP_SPECIAL: return execSpecial<Dry>(f, pc, rdram, ctx, runtime);
        case OP_REGIMM: return execRegimm<Dry>(f, pc, rdram, ctx, runtime);
        case OP_COP0: return execCop0<Dry>(f, pc, rdram, ctx, runtime);
        case OP_COP1: return execCop1<Dry>(f, pc, rdram, ctx, runtime);
        case OP_MMI: return execMmi<Dry>(f, pc, rdram, ctx, runtime);
        case OP_ADDI:
            if (rt == 0)
                X((void)0);
            X({
                uint32_t tmp;
                bool ov;
                ADD32_OV(GPR_U32(ctx, rs), (int32_t)simm, tmp, ov);
                if (ov)
                    runtime->SignalException(ctx, EXCEPTION_INTEGER_OVERFLOW);
                else
                    SET_GPR_S32(ctx, rt, (int32_t)tmp);
            });
        case OP_ADDIU:
            if (rt == 0)
                X((void)0);
            X(SET_GPR_S32(ctx, rt, (int32_t)ADD32(GPR_U32(ctx, rs), simm)));
        case OP_SLTI: X(SET_GPR_U64(ctx, rt, ((int64_t)GPR_S64(ctx, rs) < (int64_t)(int32_t)simm) ? 1 : 0));
        case OP_SLTIU: X(SET_GPR_U64(ctx, rt, ((uint64_t)GPR_U64(ctx, rs) < (uint64_t)(int64_t)(int32_t)simm) ? 1 : 0));
        case OP_ANDI: X(SET_GPR_U64(ctx, rt, GPR_U64(ctx, rs) & (uint64_t)(uint16_t)f.imm));
        case OP_ORI: X(SET_GPR_U64(ctx, rt, GPR_U64(ctx, rs) | (uint64_t)(uint16_t)f.imm));
        case OP_XORI: X(SET_GPR_U64(ctx, rt, GPR_U64(ctx, rs) ^ (uint64_t)(uint16_t)f.imm));
        case OP_LUI: X(SET_GPR_S32(ctx, rt, (int32_t)((uint32_t)f.imm << 16)));
        case OP_LB: X(SET_GPR_S32(ctx, rt, (int8_t)READ8(ADD32(GPR_U32(ctx, rs), simm))));
        case OP_LH: X(SET_GPR_S32(ctx, rt, (int16_t)READ16(ADD32(GPR_U32(ctx, rs), simm))));
        case OP_LW: X(SET_GPR_S32(ctx, rt, (int32_t)READ32(ADD32(GPR_U32(ctx, rs), simm))));
        case OP_LBU: X(SET_GPR_ZE32(ctx, rt, (uint8_t)READ8(ADD32(GPR_U32(ctx, rs), simm))));
        case OP_LHU: X(SET_GPR_ZE32(ctx, rt, (uint16_t)READ16(ADD32(GPR_U32(ctx, rs), simm))));
        case OP_LWU: X(SET_GPR_ZE32(ctx, rt, READ32(ADD32(GPR_U32(ctx, rs), simm))));
        case OP_SB: X(WRITE8(ADD32(GPR_U32(ctx, rs), simm), (uint8_t)GPR_U32(ctx, rt)));
        case OP_SH: X(WRITE16(ADD32(GPR_U32(ctx, rs), simm), (uint16_t)GPR_U32(ctx, rt)));
        case OP_SW: X(WRITE32(ADD32(GPR_U32(ctx, rs), simm), GPR_U32(ctx, rt)));
        case OP_LQ: X(SET_GPR_VEC(ctx, rt, READ128(ADD32(GPR_U32(ctx, rs), simm))));
        case OP_SQ: X(WRITE128(ADD32(GPR_U32(ctx, rs), simm), GPR_VEC(ctx, rt)));
        case OP_LD: X(SET_GPR_U64(ctx, rt, READ64(ADD32(GPR_U32(ctx, rs), simm))));
        case OP_SD: X(WRITE64(ADD32(GPR_U32(ctx, rs), simm), GPR_U64(ctx, rt)));
        case OP_LWC1:
            X({
                uint32_t bits = READ32(ADD32(GPR_U32(ctx, rs), simm));
                float fv;
                std::memcpy(&fv, &bits, sizeof(fv));
                ctx->f[rt] = fv;
            });
        case OP_SWC1:
            X({
                float fv = ctx->f[rt];
                uint32_t bits;
                std::memcpy(&bits, &fv, sizeof(bits));
                WRITE32(ADD32(GPR_U32(ctx, rs), simm), bits);
            });
        case OP_LQC2: X(ctx->vu0_vf[rt] = _mm_castsi128_ps(READ128(ADD32(GPR_U32(ctx, rs), simm))));
        case OP_SQC2: X(WRITE128(ADD32(GPR_U32(ctx, rs), simm), _mm_castps_si128(ctx->vu0_vf[rt])));
        case OP_DADDI:
            X({
                int64_t src = (int64_t)GPR_S64(ctx, rs);
                int64_t imm = (int64_t)(int32_t)simm;
                int64_t res = (int64_t)((uint64_t)src + (uint64_t)imm);
                if (((src ^ imm) >= 0) && ((src ^ res) < 0))
                    runtime->SignalException(ctx, EXCEPTION_INTEGER_OVERFLOW);
                else
                    SET_GPR_S64(ctx, rt, res);
            });
        case OP_DADDIU: X(SET_GPR_S64(ctx, rt, (int64_t)((uint64_t)GPR_S64(ctx, rs) + (uint64_t)(int64_t)(int32_t)simm)));
        case OP_LDL:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                uint32_t aligned = addr & ~7u, offset = addr & 7u;
                uint64_t mem = READ64(aligned);
                uint32_t shift = (7u - offset) << 3;
                uint64_t keepMask = (shift == 0) ? 0ull : ((1ull << shift) - 1ull);
                SET_GPR_U64(ctx, rt, (GPR_U64(ctx, rt) & keepMask) | (mem << shift));
            });
        case OP_LDR:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                uint32_t aligned = addr & ~7u, offset = addr & 7u;
                uint64_t mem = READ64(aligned);
                uint32_t shift = offset << 3;
                uint64_t keepMask = (offset == 0) ? 0ull : (0xFFFFFFFFFFFFFFFFull << ((8u - offset) << 3));
                SET_GPR_U64(ctx, rt, (GPR_U64(ctx, rt) & keepMask) | (mem >> shift));
            });
        case OP_LWL:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                uint32_t aligned = addr & ~3u, offset = addr & 3u;
                uint32_t mem = READ32(aligned);
                uint32_t shift = (3u - offset) << 3;
                uint32_t keepMask = (shift == 0) ? 0u : ((1u << shift) - 1u);
                uint32_t merged = (GPR_U32(ctx, rt) & keepMask) | (mem << shift);
                SET_GPR_S32(ctx, rt, (int32_t)merged);
            });
        case OP_LWR:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                uint32_t aligned = addr & ~3u, offset = addr & 3u;
                uint32_t mem = READ32(aligned);
                uint32_t shift = offset << 3;
                uint32_t keepMask = (offset == 0) ? 0u : (0xFFFFFFFFu << ((4u - offset) << 3));
                uint32_t merged32 = (GPR_U32(ctx, rt) & keepMask) | (mem >> shift);
                uint64_t merged64 = (GPR_U64(ctx, rt) & 0xFFFFFFFF00000000ull) | (uint64_t)merged32;
                if (offset == 0)
                    merged64 = (uint64_t)(int64_t)(int32_t)merged32;
                SET_GPR_U64(ctx, rt, merged64);
            });
        case OP_SWL:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                uint32_t aligned = addr & ~3u, offset = addr & 3u;
                uint32_t shift = (3u - offset) << 3;
                uint32_t mask = 0xFFFFFFFFu >> shift;
                uint32_t oldData = READ32(aligned);
                uint32_t val = GPR_U32(ctx, rt);
                uint32_t newData = (oldData & ~mask) | ((val >> shift) & mask);
                WRITE32(aligned, newData);
            });
        case OP_SWR:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                uint32_t aligned = addr & ~3u, offset = addr & 3u;
                uint32_t shift = offset << 3;
                uint32_t mask = 0xFFFFFFFFu << shift;
                uint32_t oldData = READ32(aligned);
                uint32_t val = GPR_U32(ctx, rt);
                uint32_t newData = (oldData & ~mask) | ((val << shift) & mask);
                WRITE32(aligned, newData);
            });
        case OP_SDL:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                uint32_t aligned = addr & ~7u, offset = addr & 7u;
                uint32_t shift = (7u - offset) << 3;
                uint64_t mask = 0xFFFFFFFFFFFFFFFFull >> shift;
                uint64_t oldData = READ64(aligned);
                uint64_t val = GPR_U64(ctx, rt);
                uint64_t newData = (oldData & ~mask) | ((val >> shift) & mask);
                WRITE64(aligned, newData);
            });
        case OP_SDR:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                uint32_t aligned = addr & ~7u, offset = addr & 7u;
                uint32_t shift = offset << 3;
                uint64_t mask = 0xFFFFFFFFFFFFFFFFull << shift;
                uint64_t oldData = READ64(aligned);
                uint64_t val = GPR_U64(ctx, rt);
                uint64_t newData = (oldData & ~mask) | ((val << shift) & mask);
                WRITE64(aligned, newData);
            });
        case OP_CACHE: X((void)0);
        case OP_PREF: X((void)0);
        case OP_LL:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                SET_GPR_S32(ctx, rt, (int32_t)READ32(addr));
                ctx->llbit = 1;
                ctx->lladdr = addr;
            });
        case OP_SC:
            X({
                uint32_t addr = ADD32(GPR_U32(ctx, rs), simm);
                if (ctx->llbit && ctx->lladdr == addr)
                {
                    WRITE32(addr, GPR_U32(ctx, rt));
                    SET_GPR_S32(ctx, rt, 1);
                }
                else
                {
                    SET_GPR_S32(ctx, rt, 0);
                }
                ctx->llbit = 0;
                ctx->lladdr = 0;
            });
        case OP_COP2:
            return false; // the VU0 macro instructions: not yet
        default:
            T("Unhandled opcode");
        }
    }
#undef X
#undef T

    // ---- branches: the condition is taken before the delay slot, as in the generated code ----
    enum class Kind
    {
        None,
        Cond,
        J,
        Jal,
        Jr,
        Jalr,
        Eret,
    };

    struct Branch
    {
        Kind kind = Kind::None;
        bool likely = false;
        int link = 0; // 1: $ra always (BLTZAL/BGEZAL), 2: $ra if taken (BLTZALL/BGEZALL)
        uint32_t target = 0;
    };

    Branch decodeBranch(uint32_t w, uint32_t pc)
    {
        Branch b;
        const Fields f(w);
        const uint32_t relTarget = static_cast<uint32_t>(static_cast<int64_t>(pc + 4u) + (static_cast<int64_t>(f.simm) << 2));
        switch (f.op)
        {
        case OP_J:
        case OP_JAL:
            b.kind = f.op == OP_J ? Kind::J : Kind::Jal;
            b.target = ((pc + 4u) & 0xF0000000u) | ((w & 0x03FFFFFFu) << 2);
            return b;
        case OP_SPECIAL:
            if (f.fn == 0x08)
                b.kind = Kind::Jr;
            else if (f.fn == 0x09)
                b.kind = Kind::Jalr;
            return b;
        case OP_BEQ: case OP_BNE: case OP_BLEZ: case OP_BGTZ:
            b.kind = Kind::Cond;
            b.target = relTarget;
            return b;
        case OP_BEQL: case OP_BNEL: case OP_BLEZL: case OP_BGTZL:
            b.kind = Kind::Cond;
            b.likely = true;
            b.target = relTarget;
            return b;
        case OP_REGIMM:
            switch (f.rt)
            {
            case 0x00: case 0x01: b.kind = Kind::Cond; break;
            case 0x02: case 0x03: b.kind = Kind::Cond; b.likely = true; break;
            case 0x10: case 0x11: b.kind = Kind::Cond; b.link = 1; break;
            case 0x12: case 0x13: b.kind = Kind::Cond; b.likely = true; b.link = 2; break;
            default: return b;
            }
            b.target = relTarget;
            return b;
        case OP_COP0:
            if (f.rs == 0x08) // BC0x: the generated code never takes it (no condition), the delay slot always runs
            {
                b.kind = Kind::Cond;
                b.target = relTarget;
            }
            else if (f.rs == 0x10 && f.fn == 0x18)
            {
                b.kind = Kind::Eret;
            }
            return b;
        case OP_COP1:
        case OP_COP2:
            if (f.rs == 0x08)
            {
                b.kind = Kind::Cond;
                b.likely = f.rt == 0x02 || f.rt == 0x03;
                b.target = relTarget;
            }
            return b;
        default:
            return b;
        }
    }

    bool condition(uint32_t w, R5900Context *ctx)
    {
        const Fields f(w);
        const uint32_t rs = f.rs, rt = f.rt;
        switch (f.op)
        {
        case OP_BEQ: case OP_BEQL: return GPR_U64(ctx, rs) == GPR_U64(ctx, rt);
        case OP_BNE: case OP_BNEL: return GPR_U64(ctx, rs) != GPR_U64(ctx, rt);
        case OP_BLEZ: case OP_BLEZL: return GPR_S64(ctx, rs) <= 0;
        case OP_BGTZ: case OP_BGTZL: return GPR_S64(ctx, rs) > 0;
        case OP_REGIMM: return (rt & 1u) ? GPR_S64(ctx, rs) >= 0 : GPR_S64(ctx, rs) < 0;
        case OP_COP1: return (rt == 0x00 || rt == 0x02) ? !(ctx->fcr31 & 0x800000) : (ctx->fcr31 & 0x800000) != 0;
        case OP_COP2: return (rt == 0x00 || rt == 0x02) ? !(ctx->vu0_status & 0x1) : (ctx->vu0_status & 0x1) != 0;
        default: return false; // (BC0x)
        }
    }

    bool covered(uint32_t w, uint32_t pc)
    {
        const Branch b = decodeBranch(w, pc);
        if (b.kind != Kind::None)
            return true;
        try
        {
            return exec<true>(w, pc, nullptr, nullptr, nullptr);
        }
        catch (...)
        {
            return true; // (a throw in the dry pass would be a bug; the instruction is one the translator throws on)
        }
    }

    [[noreturn]] void notCovered(uint32_t pc, uint32_t w)
    {
        std::fprintf(stderr, "[interp] instruction not covered yet at 0x%x: 0x%08x (op 0x%x fn 0x%x)\n", pc, w, w >> 26, w & 63u);
        std::fflush(stderr);
        std::abort();
    }

    // ---- what runs on the interpreter ----
    std::atomic<bool> g_on{false};
    uint64_t g_runs = 0;

    bool handledHere(uint32_t address)
    {
        if ((address & 3u) != 0u || address < g_ps2RecompiledFunctionTableBase || address >= g_ps2RecompiledFunctionTableEnd)
            return false;
        return g_ps2RecompiledFunctionTable[(address - g_ps2RecompiledFunctionTableBase) >> 2] == &hlInterpRun;
    }

    std::vector<std::pair<uint32_t, uint32_t>> parseRanges(const char *text)
    {
        std::vector<std::pair<uint32_t, uint32_t>> out;
        std::string s(text);
        size_t at = 0;
        while (at < s.size())
        {
            size_t comma = s.find(',', at);
            std::string part = s.substr(at, comma == std::string::npos ? std::string::npos : comma - at);
            const size_t dash = part.find('-');
            if (dash != std::string::npos)
                out.emplace_back(static_cast<uint32_t>(std::strtoul(part.substr(0, dash).c_str(), nullptr, 16)),
                                 static_cast<uint32_t>(std::strtoul(part.substr(dash + 1).c_str(), nullptr, 16)));
            if (comma == std::string::npos)
                break;
            at = comma + 1;
        }
        return out;
    }

    std::unordered_set<uint32_t> readKeep(const char *path)
    {
        std::unordered_set<uint32_t> keep;
        if (path == nullptr)
            return keep;
        std::ifstream in(path);
        std::string line;
        const std::regex at("@0x([0-9A-Fa-f]+)");
        while (std::getline(in, line))
        {
            for (std::sregex_iterator it(line.begin(), line.end(), at), end; it != end; ++it)
                keep.insert(static_cast<uint32_t>(std::strtoul((*it)[1].str().c_str(), nullptr, 16)));
        }
        return keep;
    }

    void afterElfLoad(PS2Runtime *runtime, const std::string &elfPath)
    {
        const char *mode = std::getenv("HL_INTERP");
        if (mode == nullptr || mode[0] == '\0' || mode[0] == '0')
            return;
        const bool all = std::strcmp(mode, "all") == 0;
        const auto ranges = all ? std::vector<std::pair<uint32_t, uint32_t>>{} : parseRanges(mode);
        const auto keep = readKeep(std::getenv("HL_INTERP_KEEP"));
        if (keep.empty())
            std::fprintf(stderr, "[interp] no HL_INTERP_KEEP: the stubs would run their MIPS code; nothing put on the interpreter\n");
        const uint8_t *rdram = runtime->memory().getRDRAM();
        const uint32_t base = g_ps2RecompiledFunctionTableBase;
        const uint32_t tableEnd = g_ps2RecompiledFunctionTableEnd;
        size_t functions = 0, slots = 0, kept = 0, uncovered = 0;
        std::map<uint32_t, size_t> missing; // (op << 8 | fn): count, for the log
        std::ifstream map(std::getenv("HL_INTERP_MAP") ? std::getenv("HL_INTERP_MAP") : "");
        if (!map)
            std::fprintf(stderr, "[interp] no HL_INTERP_MAP (un5_map.csv): nothing put on the interpreter\n");
        std::string line;
        while (!keep.empty() && std::getline(map, line))
        {
            const size_t c1 = line.find(','), c2 = line.find(',', c1 + 1);
            if (c1 == std::string::npos || c2 == std::string::npos || line.compare(c1 + 1, 2, "0x") != 0)
                continue; // (the header)
            const uint32_t start = static_cast<uint32_t>(std::strtoul(line.c_str() + c1 + 1, nullptr, 16));
            const uint32_t end = static_cast<uint32_t>(std::strtoul(line.c_str() + c2 + 1, nullptr, 16));
            if (start < base || end > tableEnd || end <= start || (start & 3u) != 0u)
                continue;
            if (keep.count(start) != 0)
            {
                ++kept;
                continue;
            }
            bool want = all;
            for (const auto &r : ranges)
                want = want || (start >= r.first && start < r.second);
            if (!want)
                continue;
            bool ok = true;
            for (uint32_t a = start; a < end && ok; a += 4u)
            {
                const uint32_t w = Ps2FastRead32(rdram, a);
                if (!covered(w, a))
                {
                    ok = false;
                    ++missing[((w >> 26) << 8) | ((w >> 26) == OP_COP2 || (w >> 26) == OP_MMI ? (w & 63u) : 0u)];
                }
            }
            if (!ok)
            {
                ++uncovered;
                continue;
            }
            for (uint32_t a = start; a < end; a += 4u)
                g_ps2RecompiledFunctionTable[(a - base) >> 2] = &hlInterpRun;
            ++functions;
            slots += (end - start) >> 2;
        }
        g_on.store(functions != 0, std::memory_order_release);
        std::fprintf(stderr, "[interp] %s: %zu functions (%zu slots) on the interpreter; %zu stubs native; %zu not covered yet\n",
                     elfPath.c_str(), functions, slots, kept, uncovered);
        std::vector<std::pair<size_t, uint32_t>> top;
        for (const auto &m : missing)
            top.emplace_back(m.second, m.first);
        std::sort(top.rbegin(), top.rend());
        for (size_t k = 0; k < top.size() && k < 12u; ++k)
            std::fprintf(stderr, "[interp]   not covered: op 0x%x fn 0x%x in %zu functions\n", top[k].second >> 8, top[k].second & 0xFFu, top[k].first);
    }

    struct InterpInstall
    {
        InterpInstall() { g_hlAfterElfLoad = &afterElfLoad; }
    } interpInstall;

    // the delay slot, as the generated code runs it (a nop is skipped)
    inline void delaySlot(uint32_t branchPc, uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
    {
        const uint32_t w = Ps2FastRead32(rdram, branchPc + 4u);
        if (w == 0u)
            return;
        ctx->pc = branchPc + 4u;
        ctx->in_delay_slot = true;
        ctx->branch_pc = branchPc;
        if (decodeBranch(w, branchPc + 4u).kind != Kind::None || !exec<false>(w, branchPc + 4u, rdram, ctx, runtime))
            notCovered(branchPc + 4u, w);
        ctx->in_delay_slot = false;
    }
}

void hlInterpRun(uint8_t *rdram, R5900Context *ctx, PS2Runtime *runtime)
{
    using namespace hl_interp_detail;
    ++g_runs;
    uint32_t pc = ctx->pc;
    for (;;)
    {
        const uint32_t w = Ps2FastRead32(rdram, pc);
        const Branch b = decodeBranch(w, pc);
        ctx->pc = pc;
        if (b.kind == Kind::None)
        {
            if (!exec<false>(w, pc, rdram, ctx, runtime))
                notCovered(pc, w);
            pc += 4u;
            continue;
        }
        const uint32_t fall = pc + 8u;
        uint32_t target = b.target;
        switch (b.kind)
        {
        case Kind::Eret:
            if (ctx->cop0_status & 0x4)
            {
                ctx->pc = ctx->cop0_errorepc;
                ctx->cop0_status &= ~0x4;
            }
            else
            {
                ctx->pc = ctx->cop0_epc;
                ctx->cop0_status &= ~0x2;
            }
            runtime->clearLLBit(ctx);
            return;
        case Kind::Jal:
            SET_GPR_U32(ctx, 31, fall);
            delaySlot(pc, rdram, ctx, runtime);
            ctx->pc = target;
            if (!runtime->dispatchGuestBranch(rdram, ctx, target, pc, fall, PS2Runtime::GuestBranchKind::DirectCall, "JAL"))
                return;
            pc = fall;
            continue;
        case Kind::Jalr:
        {
            const Fields f(w);
            target = GPR_U32(ctx, f.rs);
            if (f.rd != 0u)
                SET_GPR_U32(ctx, f.rd, fall);
            delaySlot(pc, rdram, ctx, runtime);
            ctx->pc = target;
            if (!runtime->dispatchGuestBranch(rdram, ctx, target, pc, fall, PS2Runtime::GuestBranchKind::IndirectCall, "JALR"))
                return;
            pc = fall;
            continue;
        }
        case Kind::Jr:
        {
            const Fields f(w);
            target = GPR_U32(ctx, f.rs);
            delaySlot(pc, rdram, ctx, runtime);
            ctx->pc = target;
            if (f.rs == 31u || !handledHere(target))
                return; // a return, or a jump out: the dispatcher goes on from ctx->pc
            pc = target; // (a jump table inside code on the interpreter)
            continue;
        }
        case Kind::J:
            delaySlot(pc, rdram, ctx, runtime);
            ctx->pc = target;
            if (!handledHere(target) || (target <= pc && runtime->eeCheckpointDue()))
                return;
            pc = target;
            continue;
        case Kind::Cond:
        {
            const bool taken = condition(w, ctx);
            if (b.link == 1)
                SET_GPR_U32(ctx, 31, fall);
            if (b.likely)
            {
                if (!taken)
                {
                    pc = fall;
                    continue;
                }
                if (b.link == 2)
                    SET_GPR_U32(ctx, 31, fall);
                delaySlot(pc, rdram, ctx, runtime);
            }
            else
            {
                delaySlot(pc, rdram, ctx, runtime);
                if (!taken)
                {
                    pc = fall;
                    continue;
                }
            }
            ctx->pc = target;
            if (target <= pc && runtime->eeCheckpointDue())
                return;
            if (!handledHere(target))
                return;
            pc = target;
            continue;
        }
        default:
            notCovered(pc, w);
        }
    }
}
