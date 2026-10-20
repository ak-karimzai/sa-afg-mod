"""afghan_limits.asi: raises the game's "extra objects" directory from 550 to 16000 entries.

Why: the mod's gta.dat drops the city IDEs (SA's 14,000 simple-model limit), so ~11,400 DFFs in gta3.img have
no model info. CStreaming::LoadCdDirectory puts such files into CStreaming::ms_pExtraObjectsDir, created in
CStreaming::Init with `push 550; call CDirectory::CDirectory` (1.0 US, 0x5B8DDF). It overflows ("Too many objects
without modelinfo structures") long before player.dff, so RequestSpecialModel(0, "player") cannot find CJ and the
game (or ModLoader's hook) crashes when the player is created.

The DLL is written byte by byte (no compiler or linker needed). DllMain checks that the patch site really holds
`push 550` and only then makes the 4 bytes writable with VirtualProtect and writes the new size. ModLoader loads
.asi files from mod folders before the game initialises streaming.
"""
import struct

SITE = 0x5B8DDF          # push 550 in CStreaming::Init (1.0 US)
NEW_SIZE = 16000

IMAGE_BASE = 0x10000000
TEXT_RVA = 0x1000
IMPORT_RVA = TEXT_RVA + 0x100
ILT_RVA = TEXT_RVA + 0x140
IAT_RVA = TEXT_RVA + 0x150
HINT_RVA = TEXT_RVA + 0x160
DLLNAME_RVA = TEXT_RVA + 0x180
RELOC_RVA = TEXT_RVA + 0x1A0
SECTION_SIZE = 0x200


def _code():
    imm = SITE + 1
    c = bytearray()
    jumps = {}

    def jcc(op, label):            # short conditional jump, patched below
        c.extend((op, 0))
        jumps.setdefault(label, []).append(len(c) - 1)

    c += bytes.fromhex("837C240801")                      # cmp dword [esp+8], 1   (DLL_PROCESS_ATTACH)
    jcc(0x75, "done")                                     # jne done
    c += b"\x80\x3D" + struct.pack("<I", SITE) + b"\x68"  # cmp byte [SITE], 68h  (push imm32)
    jcc(0x75, "done")
    c += b"\x81\x3D" + struct.pack("<II", imm, 550)       # cmp dword [SITE+1], 550
    jcc(0x75, "done")
    c += b"\x53"                                          # push ebx
    c += b"\xE8\x00\x00\x00\x00"                          # call $+5
    here = TEXT_RVA + len(c)
    c += b"\x5B"                                          # pop ebx  (ebx = address of this pop)
    c += b"\x50\x54"                                      # push eax (oldProtect slot); push esp (&slot)
    c += b"\x6A\x40\x6A\x04"                              # push PAGE_EXECUTE_READWRITE; push 4
    c += b"\x68" + struct.pack("<I", imm)                 # push SITE+1
    c += b"\xFF\x93" + struct.pack("<i", IAT_RVA - here)  # call [ebx + (IAT - here)]  -> VirtualProtect
    c += b"\x85\xC0"                                      # test eax, eax
    jcc(0x74, "fail")                                     # jz fail
    c += b"\xC7\x05" + struct.pack("<II", imm, NEW_SIZE)  # mov dword [SITE+1], NEW_SIZE
    labels = {"fail": len(c)}
    c += b"\x58\x5B"                                      # pop eax (slot); pop ebx
    labels["done"] = len(c)
    c += b"\xB8\x01\x00\x00\x00\xC2\x0C\x00"              # mov eax, 1; ret 0Ch
    for label, sites in jumps.items():
        for at in sites:
            c[at] = labels[label] - (at + 1)
    return bytes(c)


def build(path):
    sec = bytearray(SECTION_SIZE)
    code = _code()
    sec[0:len(code)] = code
    o = IMPORT_RVA - TEXT_RVA
    sec[o:o + 20] = struct.pack("<5I", ILT_RVA, 0, 0, DLLNAME_RVA, IAT_RVA)  # + zeroed terminator
    for rva in (ILT_RVA, IAT_RVA):
        o = rva - TEXT_RVA
        sec[o:o + 4] = struct.pack("<I", HINT_RVA)
    o = HINT_RVA - TEXT_RVA
    sec[o:o + 17] = b"\x00\x00VirtualProtect\x00"
    o = DLLNAME_RVA - TEXT_RVA
    sec[o:o + 13] = b"KERNEL32.dll\x00"
    o = RELOC_RVA - TEXT_RVA
    sec[o:o + 12] = struct.pack("<IIHH", TEXT_RVA, 12, 0, 0)  # one block of padding entries: code is PIC

    dos = bytearray(0x40)
    dos[0:2] = b"MZ"
    dos[0x3C:0x40] = struct.pack("<I", 0x40)
    coff = struct.pack("<HHIIIHH", 0x14C, 1, 0, 0, 0, 0xE0, 0x2102)  # i386, 1 section, EXE|32BIT|DLL
    dirs = [(0, 0)] * 16
    dirs[1] = (IMPORT_RVA, 40)
    dirs[5] = (RELOC_RVA, 12)
    dirs[12] = (IAT_RVA, 8)
    opt = struct.pack("<HBBIIIIIIIIIHHHHHHIIIIHHIIIIII",
                      0x10B, 14, 0, SECTION_SIZE, 0, 0, TEXT_RVA, TEXT_RVA, TEXT_RVA,
                      IMAGE_BASE, 0x1000, 0x200, 5, 1, 0, 0, 5, 1, 0,
                      TEXT_RVA + 0x1000, 0x200, 0, 2, 0x0140,  # GUI; DYNAMIC_BASE | NX_COMPAT
                      0x100000, 0x1000, 0x100000, 0x1000, 0, 16)
    opt += b"".join(struct.pack("<II", *d) for d in dirs)
    shdr = struct.pack("<8sIIIIIIHHI", b".text", SECTION_SIZE, TEXT_RVA, SECTION_SIZE, 0x200, 0, 0, 0, 0,
                       0xE0000060)  # code | initialised data | exec | read | write
    hdr = bytes(dos) + b"PE\0\0" + coff + opt + shdr
    with open(path, "wb") as f:
        f.write(hdr + bytes(0x200 - len(hdr)) + bytes(sec))
