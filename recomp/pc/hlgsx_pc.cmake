# libbt4gsx.so for the PC (Linux x86_64): the same sources as recomp/android/gsx/hlgsx.cmake (ARMSX2's gsrunner reading
# the live GS stream, hl_gsx.cpp, and the native fight renderer, hl_native.cpp), on ARMSX2's Linux core. Included at the
# end of build/armsx2-pc's CMakeLists.txt (-DHL_EXTRA_CMAKE=<this file>, recomp/pc/build_gsx_pc.sh).
#   BT4GSX_DIR       recomp/android/gsx
#   BT4GSX_RING_DIR  the folder with ps2_live_ring.h (build/PS2Recomp/ps2xRuntime/src/lib/gs)
set(GSR "${CMAKE_SOURCE_DIR}/pcsx2-gsrunner")
add_library(bt4gsx SHARED
	"${GSR}/Main.cpp"
	"${GSR}/GSLadder.cpp"
	"${GSR}/GSReplayPayload.cpp"
	"${GSR}/RenderDocCapture.cpp"
	"${BT4GSX_DIR}/hl_gsx.cpp"
	"${BT4GSX_DIR}/hl_native.cpp")
target_compile_definitions(bt4gsx PRIVATE BT4GSX=1)
set_source_files_properties("${GSR}/Main.cpp" PROPERTIES COMPILE_DEFINITIONS "main=bt4gsx_runner_main")
target_include_directories(bt4gsx PRIVATE
	"${CMAKE_BINARY_DIR}/common/include"
	"${CMAKE_SOURCE_DIR}"
	"${CMAKE_SOURCE_DIR}/pcsx2"
	"${CMAKE_SOURCE_DIR}/3rdparty/include"
	"${BT4GSX_RING_DIR}")
target_link_libraries(bt4gsx PRIVATE PCSX2_FLAGS PCSX2 ${CMAKE_DL_LIBS})
# only Bt4Gsx_* leave the library
target_link_options(bt4gsx PRIVATE -Wl,--exclude-libs,ALL)
