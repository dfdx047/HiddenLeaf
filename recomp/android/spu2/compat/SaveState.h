// compat: no savestates here
#pragma once
#include "common/Pcsx2Types.h"
enum class FreezeAction { Load, Save, Size };
struct freezeData { int size; u8 *data; };
