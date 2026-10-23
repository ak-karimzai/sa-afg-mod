# postpatch: bytecode-level mission patching

`missions_realism/` holds all 24 missions of `main.scm` as editable text (disassembled with the SA opcode
database `cleo/.config/sa.json` from the CLEO install), plus `orig/` (untouched originals).
`asm.py` reassembles the text byte-exactly (unedited text reproduces the original bytes).

    export AFG_SA_JSON=path/to/sa.json AFG_ORIG_SCM=path/to/built/main.scm
    cd missions_realism            # scripts read/write ./work and ./orig
    export PYTHONPATH=../tools/postpatch
    python3 ../tools/postpatch/extract.py                    # (optional) re-dump missions of a fresh build to work/ and orig/
    python3 ../tools/postpatch/check.py m00 m01 ...   # assemble + lint
    python3 ../tools/postpatch/build.py out_main.scm  # splice reassembled missions into main.scm

Then copy `out_main.scm` to `modloader/afghanistan/main.scm` and append
`missions_realism/afghan_text_additions.fxt` to `cleo/CLEO_TEXT/afghan.fxt`.

NOTE: the patch is applied to the *built* main.scm produced by tools/build.py (v1.3.0, 24 missions).
Regenerating main.scm from the generator overwrites it, so re-run build.py above afterwards.
Not play-tested in game; see missions_realism/work/mNN.notes.md for per-mission notes and risks.
