# PROJECT_STATUS — Android (skrisi/android)

> File DINAMIS. Baca di awal task, update di akhir. Bukan tempat aturan permanen (CLAUDE.md).

## Snapshot
- Branch aktif: develop
- Baseline: UNBLOCK IN PROGRESS (2026-06-15). .gitignore fixed (untracked 23.129 -> ~17). 2 commit aman: abd2483 (gitignore di develop), 8b2de7d (rescue snapshot di wip/android-baseline-2026-06-15).
- BELUM selesai (host): develop working tree masih kotor (churn 401/424); finish via host: del lock -> git checkout -f develop -> git reset --hard abd2483 -> del "tash apply stash@{2}".
- google-services.json: TRACKED (keputusan sadar user; jangan untrack).
- .mcp.json: Maestro (runtime UI test) sudah ada.

## Sedang berjalan
- INF-147 silent refresh: SUDAH merged (PR #10) — ini VERIFICATION/closure card (runtime evidence), bukan implement ulang.

## Risiko / blocker aktif
- Develop belum bersih sampai reset di host selesai.
- Runtime gate wajib (emulator/Maestro) untuk auth/attendance — compile-only tidak cukup.

## Next safe action
- Selesaikan reset develop di host -> verifikasi bersih -> turun ke siap-worktree.

## Catatan
- Konsumen kontrak backend. Base URL hardcoded di NetworkModule = titik verifikasi awal.
