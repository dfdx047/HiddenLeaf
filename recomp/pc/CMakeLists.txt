# un5pc: Ultimate Ninja 5 recompiled for the PC with the Android's drawing (recomp/pc/hl_pc_main.cpp): the runtime headless as
# in recomp/android/CMakeLists.txt (its GS stream into a ring), the same host code of the game (hl_game.cpp, Nominum's VU1),
# and libbt4gsx.so for Linux (recomp/pc/build_gsx_pc.sh) loaded at run time. Configured by recomp/pc/build.sh. clang, as
# the NDK: the same compiler as the devices. No SPU2 yet (recomp/android/spu2 is ARM's): the IOP runs without it, as the
# PC runner does.
#
#   PS2R     build/PS2Recomp with recomp/patches, and the generated code in ps2xRuntime/src/runner
#   RAYLIB   raylib's source (only its header: host/raylib_host.cpp answers its few functions)
cmake_minimum_required(VERSION 3.21)
project(un5pc C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(RT "${PS2R}/ps2xRuntime")
set(HOST "${CMAKE_CURRENT_SOURCE_DIR}/../android/host")

# the PC runner's -march=native, without AVX-512 (as the GS); the devices' numeric flags
add_compile_options(-march=native -mno-avx512f -fno-strict-aliasing -fwrapv -ffp-contract=off)
add_compile_definitions(PS2X_HEADLESS_HOST=1)
include_directories("${RAYLIB}/src")

find_package(PkgConfig REQUIRED)
pkg_check_modules(FFMPEG REQUIRED IMPORTED_TARGET libavformat libavcodec libswresample libswscale libavutil)
find_package(SDL3 REQUIRED)

add_subdirectory("${PS2R}/ps2xIOP" ps2xIOP EXCLUDE_FROM_ALL)

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
set_source_files_properties(${L}/vu/ps2_vu_fast.cpp PROPERTIES COMPILE_FLAGS "-mllvm -tail-dup-pred-size=4096 -mllvm -tail-dup-succ-size=4096")
target_link_libraries(ps2_runtime PUBLIC PkgConfig::FFMPEG ps2_iop)

# Nominum Render's VIF1 and VU1 (NOMINUM_VU1=1), as on Android
set(HL_NOMINUM_DIR "" CACHE PATH "Nominum Render's source (an export of one commit)")
if(NOT HL_NOMINUM_DIR OR NOT EXISTS "${HL_NOMINUM_DIR}/core/vif1.cpp")
    message(FATAL_ERROR "HL_NOMINUM_DIR: no Nominum Render source at '${HL_NOMINUM_DIR}'")
endif()
add_library(nominum_vu1 STATIC "${HL_NOMINUM_DIR}/core/vif1.cpp" "${HL_NOMINUM_DIR}/core/vu1/vu1_core.cpp"
    "${HL_NOMINUM_DIR}/core/vu1/vu1_upper.cpp" "${HL_NOMINUM_DIR}/core/vu1/vu1_lower.cpp" "${HL_NOMINUM_DIR}/core/vu1/vu_fast.cpp")
file(GLOB HL_NOMINUM_REC "${HL_NOMINUM_DIR}/build/vu1rec/*.cpp")
if(HL_NOMINUM_REC)
    target_sources(nominum_vu1 PRIVATE ${HL_NOMINUM_REC})
else()
    target_sources(nominum_vu1 PRIVATE "${HL_NOMINUM_DIR}/core/vu1/rec_none.cpp")
endif()
target_include_directories(nominum_vu1 PUBLIC "${HL_NOMINUM_DIR}")
target_compile_options(nominum_vu1 PRIVATE -O2 -Wno-unused-parameter -Wno-unused-variable -Wno-unused-label
    -Wno-unused-but-set-variable -Wno-deprecated-enum-enum-conversion)
set_source_files_properties("${HL_NOMINUM_DIR}/core/vu1/vu_fast.cpp" PROPERTIES COMPILE_FLAGS "-mllvm -tail-dup-pred-size=4096 -mllvm -tail-dup-succ-size=4096")

file(GLOB RUNNER_SRC CONFIGURE_DEPENDS "${RT}/src/runner/*.cpp")
list(REMOVE_ITEM RUNNER_SRC "${RT}/src/runner/main.cpp")
set(PC_HOST "${CMAKE_CURRENT_SOURCE_DIR}/hl_pc_main.cpp" "${HOST}/raylib_host.cpp" "${HOST}/hl_game.cpp" "${HOST}/hl_nominum_vu1.cpp"
    "${HOST}/hl_watch.cpp")
add_executable(un5pc ${RUNNER_SRC} "${RT}/src/main.cpp" ${PC_HOST})
set_target_properties(un5pc PROPERTIES UNITY_BUILD ON UNITY_BUILD_BATCH_SIZE 32)
set_source_files_properties("${RT}/src/main.cpp" ${PC_HOST} PROPERTIES SKIP_UNITY_BUILD_INCLUSION ON)
set_source_files_properties("${RT}/src/main.cpp" PROPERTIES COMPILE_DEFINITIONS "main=ps2x_runtime_main")
file(GLOB UN5_TABLES CONFIGURE_DEPENDS "${RT}/src/runner/register_functions.cpp" "${RT}/src/runner/*overlays*.cpp" "${RT}/src/runner/*_register.cpp")
set_source_files_properties(${UN5_TABLES} PROPERTIES COMPILE_OPTIONS "-O0" SKIP_UNITY_BUILD_INCLUSION ON)
target_compile_options(un5pc PRIVATE -O2)
target_include_directories(un5pc PRIVATE "${RT}/include" "${L}" "${L}/Kernel")
target_link_libraries(un5pc PRIVATE ps2_runtime nominum_vu1 SDL3::SDL3 z m dl pthread)
target_link_options(un5pc PRIVATE -rdynamic)
