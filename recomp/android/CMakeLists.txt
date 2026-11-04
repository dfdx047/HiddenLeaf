# libun5.so: Naruto Shippuden: Ultimate Ninja 5 recompiled, for Android arm64. Configured by recomp/android/build.sh with
# the NDK's toolchain file. The runtime has no window here: it sends its GS stream through a ring in memory to
# libbt4gsx.so (PCSX2's GS with Vulkan, ARMSX2's Android build, the integration of Dragon Rage's port/bt4).
#
#   PS2R     build/PS2Recomp with recomp/patches, and the generated code in ps2xRuntime/src/runner
#   SDL3     SDL3 for Android (include/, lib/libSDL3.so): the same as the launcher's org.libsdl.app classes
#   FFMPEG   FFmpeg for Android (include/, lib/*.a): the movies
cmake_minimum_required(VERSION 3.21)
project(un5 C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(RT "${PS2R}/ps2xRuntime")
set(HOST "${CMAKE_CURRENT_SOURCE_DIR}/host")

# Both SoCs are at least ARMv8.2-A (Cortex-A55/A76/A77). No FMA contraction: ARM fuses by default, and the PS2 does not.
set(UN5_ARCH -march=armv8.2-a)
set(UN5_NUMERIC -fno-strict-aliasing -fwrapv -ffp-contract=off)
add_compile_options(${UN5_ARCH} ${UN5_NUMERIC})
add_compile_definitions(PS2X_HEADLESS_HOST=1 USE_SSE2NEON)

include(FetchContent)
set(FETCHCONTENT_QUIET TRUE)
# SSE intrinsics of the runtime and of the generated code, on NEON
FetchContent_Declare(sse2neon GIT_REPOSITORY https://github.com/DLTcollab/sse2neon.git GIT_TAG v1.9.1 GIT_SHALLOW TRUE)
FetchContent_MakeAvailable(sse2neon)
include_directories(${sse2neon_SOURCE_DIR})
# only raylib's header: its few functions the runtime uses are answered by host/raylib_host.cpp
FetchContent_Declare(raylib GIT_REPOSITORY https://github.com/raysan5/raylib.git GIT_TAG 5.5 GIT_SHALLOW TRUE)
FetchContent_GetProperties(raylib)
if(NOT raylib_POPULATED)
    FetchContent_Populate(raylib)
endif()
include_directories("${raylib_SOURCE_DIR}/src")

add_library(ffmpeg INTERFACE)
target_include_directories(ffmpeg INTERFACE "${FFMPEG}/include")
target_link_libraries(ffmpeg INTERFACE "${FFMPEG}/lib/libavformat.a" "${FFMPEG}/lib/libavcodec.a" "${FFMPEG}/lib/libswresample.a"
    "${FFMPEG}/lib/libswscale.a" "${FFMPEG}/lib/libavutil.a")

add_subdirectory("${PS2R}/ps2xIOP" ps2xIOP EXCLUDE_FROM_ALL)
# (its read32 and the like are called once per IOP instruction, across files)
target_compile_options(ps2_iop PRIVATE -flto=thin)

# The SPU2 of ARMSX2 (see spu2/hl_spu2.cpp), its sources as they are in Dragon Rage's tree
set(SPU2_SRC "${ARMSX2}/pcsx2/SPU2")
add_library(hl_spu2 STATIC spu2/hl_spu2.cpp ${SPU2_SRC}/ADSR.cpp ${SPU2_SRC}/Debug.cpp ${SPU2_SRC}/Dma.cpp ${SPU2_SRC}/Mixer.cpp
    ${SPU2_SRC}/ReadInput.cpp ${SPU2_SRC}/RegTable.cpp ${SPU2_SRC}/Reverb.cpp ${SPU2_SRC}/ReverbResample.cpp ${SPU2_SRC}/spu2_neon.cpp
    ${SPU2_SRC}/spu2sys.cpp)
target_include_directories(hl_spu2 PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/spu2/compat" "${ARMSX2}/pcsx2" "${ARMSX2}"
    "${ARMSX2}/3rdparty/fmt/include" "${PS2R}/ps2xIOP/src" "${SDL3}/include")
target_compile_definitions(hl_spu2 PRIVATE PCSX2_CORE)
target_compile_options(hl_spu2 PRIVATE -O3)

set(L "${RT}/src/lib")
file(GLOB_RECURSE KERNEL_SRC CONFIGURE_DEPENDS "${L}/Kernel/*.cpp")
add_library(ps2_runtime STATIC
    ${L}/game_overrides.cpp ${L}/gs/ps2_gif_arbiter.cpp ${L}/ps2_audio.cpp ${L}/ps2_audio_vag.cpp ${L}/gs/ps2_gs_memory.cpp
    ${L}/gs/gs_frontend.cpp ${L}/gs/gs_cpu_backend.cpp ${L}/gs/hl_gs_live.cpp ${L}/ps2_iop_host.cpp ${L}/ps2_memory.cpp ${L}/ps2_pad.cpp
    ${L}/ps2_rom_device.cpp ${L}/ps2_vfs.cpp ${L}/hl_perf.cpp ${L}/hl_vif_dump.cpp ${L}/hl_vu_worker.cpp ${L}/ps2_runtime.cpp ${L}/ps2_vif1_interpreter.cpp ${L}/vu/ps2_vu1_core.cpp ${L}/vu/ps2_vu_fast.cpp
    ${L}/vu/ps2_vu1_upper.cpp ${L}/vu/ps2_vu1_lower.cpp ${L}/games_database.cpp ${KERNEL_SRC})
target_include_directories(ps2_runtime PUBLIC "${RT}/include" "${L}/Kernel" "${L}/gs" "${HOST}")
target_compile_definitions(ps2_runtime PUBLIC PS2X_IOP_RPC_TRACE=0 PS2X_ENABLE_IOP_RPC_TRACE=0)
target_compile_definitions(ps2_runtime PRIVATE PS2X_HAS_FFMPEG=1)
target_compile_options(ps2_runtime PRIVATE -O3)
# The threaded VU: without these LLVM folds every "goto *op->label" into one indirect branch, and the predictor has
# one entry for the whole program
set_source_files_properties(${L}/vu/ps2_vu_fast.cpp PROPERTIES COMPILE_FLAGS "-mllvm -tail-dup-pred-size=4096 -mllvm -tail-dup-succ-size=4096")
target_link_libraries(ps2_runtime PUBLIC ffmpeg ps2_iop)

# The generated game code: one translation unit per 32 functions, or the build takes hours.
file(GLOB RUNNER_SRC CONFIGURE_DEPENDS "${RT}/src/runner/*.cpp")
list(REMOVE_ITEM RUNNER_SRC "${RT}/src/runner/main.cpp")
add_library(un5 SHARED ${RUNNER_SRC} "${RT}/src/main.cpp" "${HOST}/un5_main.cpp" "${HOST}/raylib_host.cpp" "${HOST}/hl_game.cpp")
set_target_properties(un5 PROPERTIES UNITY_BUILD ON UNITY_BUILD_BATCH_SIZE 32)
# HL_PACK_ID=<mod>: a mod's code pack (recomp/android/build.sh --mod <mod>), libun5_<mod>.so, which runs the mod's program
if(HL_PACK_ID)
    set_source_files_properties("${HOST}/un5_main.cpp" PROPERTIES COMPILE_DEFINITIONS "HL_PACK_ID=\"${HL_PACK_ID}\"")
endif()
set_source_files_properties("${RT}/src/main.cpp" "${HOST}/un5_main.cpp" "${HOST}/raylib_host.cpp" "${HOST}/hl_game.cpp" PROPERTIES SKIP_UNITY_BUILD_INCLUSION ON)
set_source_files_properties("${RT}/src/main.cpp" PROPERTIES COMPILE_DEFINITIONS "main=ps2x_runtime_main")
# the function tables are long and plain: optimizing them only costs time
file(GLOB UN5_TABLES CONFIGURE_DEPENDS "${RT}/src/runner/register_functions.cpp" "${RT}/src/runner/*overlays*.cpp" "${RT}/src/runner/*_register.cpp")
set_source_files_properties(${UN5_TABLES} PROPERTIES COMPILE_OPTIONS "-O0" SKIP_UNITY_BUILD_INCLUSION ON)
set(UN5_RUNNER_OPT "-O2" CACHE STRING "Optimisation of the generated game code")
target_compile_options(un5 PRIVATE ${UN5_RUNNER_OPT})
target_include_directories(un5 PRIVATE "${RT}/include" "${L}" "${L}/Kernel" "${SDL3}/include")
target_link_libraries(un5 PRIVATE ps2_runtime hl_spu2 "${SDL3}/lib/libSDL3.so" android log z m dl)
target_link_options(un5 PRIVATE -Wl,--gc-sections -Wl,-z,max-page-size=16384 -Wl,-Bsymbolic-functions -flto=thin -static-libstdc++)
