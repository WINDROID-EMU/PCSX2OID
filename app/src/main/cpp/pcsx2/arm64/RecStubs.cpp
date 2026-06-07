// SPDX-FileCopyrightText: 2021-2024 Connor McLaughlin <stenzek@gmail.com>, PCSX2 Team
// SPDX-License-Identifier: GPL-3.0
//
// NOTE: This file is NOT compiled into the build (commented out in CMakeLists.txt).
// Both vtlb_DynBackpatchLoadStore() and vuJITFreeze() are now fully implemented
// in their respective source files:
//   - vtlb_DynBackpatchLoadStore() -> x86/ix86-32/recVTLB.cpp (ARM64 ported)
//   - SaveStateBase::vuJITFreeze() -> x86/microVU.cpp (uses microVU0/microVU1.prog.lpState)
//
// This file is kept as a historical reference and can be deleted when the ARM64
// port is considered stable.

#include "common/Console.h"
#include "MTVU.h"
#include "SaveState.h"
#include "vtlb.h"

#include "common/Assertions.h"

#if 0 // Dead code - real implementations exist in recVTLB.cpp and microVU.cpp

void vtlb_DynBackpatchLoadStore(uptr code_address, u32 code_size, u32 guest_pc, u32 guest_addr, u32 gpr_bitmask, u32 fpr_bitmask, u8 address_register, u8 data_register, u8 size_in_bits, bool is_signed, bool is_load, bool is_fpr)
{
  pxFailRel("Not implemented - see recVTLB.cpp for the real ARM64 implementation.");
}

bool SaveStateBase::vuJITFreeze()
{
	if(IsSaving())
		vu1Thread.WaitVU();

	Console.Error("ARM64: vuJITFreeze() stub called - this should never happen. "
	              "The real implementation in microVU.cpp should be used instead.");

	// Fallback: write empty microRegInfo structures (96 bytes each)
	// to maintain save state format compatibility.
	std::array<u8,96> empty_data{};
	Freeze(empty_data);
	Freeze(empty_data);
	return true;
}

#endif
