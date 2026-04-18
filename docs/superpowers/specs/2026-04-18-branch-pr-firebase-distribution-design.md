# Branch, PR, and Firebase Distribution Design

Tanggal: 2026-04-18
Status: Approved — ready for implementation planning

## Ringkasan
Repo Android menggunakan tiga level branch operasional:
- `feature/*` untuk pekerjaan per perubahan
- `develop` untuk integrasi, review, QA, dan bugfix
- `master` untuk state release-ready dan distribusi Firebase

Distribusi Firebase App Distribution dijalankan dari `master`, bukan dari `develop`.
PR ke `master` hanya berasal dari `develop`.

## Tujuan
- Menjaga `master` sebagai branch distribusi yang terkontrol
- Memusatkan review dan QA di `develop`
- Mencegah feature branch masuk langsung ke `master`
- Menghubungkan Firebase distribution dari awal tanpa menjadikan channel distribusi sebagai tempat eksperimen harian

## Non-goals
- Tidak mendefinisikan strategi Play Store release
- Tidak mendefinisikan branching model lintas repo
- Tidak mengubah source-of-truth Android/backend

## Peran branch

### `feature/*`
Dipakai untuk setiap perubahan lokal baru.
Seluruh pekerjaan implementasi dilakukan di branch ini sebelum diajukan ke `develop`.

Aturan:
- dibuat dari `develop`
- setiap perubahan lokal baru dibuatkan branch tersendiri
- tidak pernah merge langsung ke `master`
- auto-delete branch feature dinonaktifkan
- branch feature hanya boleh dihapus manual jika seluruh kondisi berikut terpenuhi:
  - sudah merged ke `develop`
  - scope pekerjaan pada branch tersebut benar-benar selesai total
  - tidak ada follow-up aktif yang masih harus dilanjutkan di branch yang sama

### `develop`
Branch integrasi aktif untuk:
- PR review
- QA
- bugfix integration
- konsolidasi perubahan dari banyak feature branch

Aturan:
- menerima PR dari `feature/*`
- menjadi satu-satunya sumber promosi ke `master`
- tidak dipakai sebagai branch distribusi Firebase

### `master`
Branch distribusi Firebase dan release-ready candidate.

Aturan:
- tidak menerima PR langsung dari `feature/*`
- hanya menerima PR dari `develop`
- menjadi satu-satunya branch yang memicu Firebase App Distribution
- dijaga tetap stabil dan terkontrol

## Flow kerja
1. Developer membuat branch `feature/*` dari `develop`
2. Perubahan dikerjakan dan diverifikasi di branch feature
3. PR dibuka dari `feature/*` ke `develop`
4. Review, perbaikan, dan QA dilakukan melalui siklus `feature/*` ↔ `develop`
5. Setelah `develop` dianggap stabil dan layak distribusi, dibuka PR `develop` ke `master`
6. Merge ke `master` memicu distribusi Firebase

## PR governance

### PR ke `develop`
Minimal rule yang diinginkan:
- wajib melalui PR
- minimal 1 approval
- direct push ditolak
- force push ditolak
- idealnya CI dasar lulus sebelum merge:
  - build
  - unit test
  - lint bila feasible

### PR ke `master`
Minimal rule yang diinginkan:
- wajib melalui PR
- minimal 1 approval
- direct push ditolak
- force push ditolak
- merge dilakukan hanya untuk promotion release-ready dari `develop`
- reviewer assignment mengikuti governance repo yang berlaku; dokumen ini hanya menetapkan branch-flow policy dan tidak menurunkan standar reviewer yang sudah ada
- Firebase distribution hanya berjalan setelah perubahan masuk ke `master`
- tidak ada exception path langsung ke `master`; urgent fix tetap harus masuk ke `develop` lalu dipromosikan ke `master`

## Enforcement model
GitHub branch protection standar mungkin tidak cukup untuk memaksa sumber PR ke `master` hanya dari `develop`.
Karena itu enforcement dibagi menjadi dua lapis:

1. **Repository rule / automation**
   - PR ke `master` hanya valid jika:
     - base branch = `master`
     - head ref = `develop`
     - head branch berasal dari repository yang sama, bukan fork atau branch lain yang kebetulan bernama sama
   - direct push ke `master` dan `develop` ditolak
   - jika repository governance mengizinkan admin bypass, bypass tersebut tidak dipakai untuk melanggar flow ini
   - jika memungkinkan, gunakan workflow/check custom untuk menolak PR ke `master` bila kondisi validasi di atas tidak terpenuhi
2. **Team rule**
   - reviewer dan author memperlakukan PR selain `develop -> master` sebagai invalid
   - PR ke `master` yang tidak merepresentasikan promotion release-ready dari `develop` harus ditolak saat review

## Firebase distribution policy
- Firebase App Distribution dihubungkan sejak awal
- Trigger distribusi hanya berasal dari event perubahan yang sudah masuk ke `master`
- distribusi tidak dijalankan dari PR event
- distribusi tidak dijalankan dari `develop`
- distribusi hanya berjalan setelah workflow build/release yang diperlukan pada `master` selesai sukses
- artifact yang didistribusikan ditetapkan eksplisit saat implementasi workflow dan tidak boleh diambil dari event lain di luar `master`
- group tester / channel distribusi ditetapkan eksplisit saat implementasi workflow
- `develop` dipakai untuk kesiapan integrasi dan QA, bukan kanal distribusi
- tester menerima build yang sudah melewati gate review + integrasi + QA dasar

## Alasan memilih model ini
Model ini dipilih karena:
- lebih disiplin daripada distribusi dari `develop`
- menjaga channel Firebase tetap representatif terhadap state yang siap dibagikan
- tetap memberi ruang review dan QA aktif di `develop`
- memisahkan branch integrasi dari branch distribusi

## Risiko dan trade-off

### Keuntungan
- `master` lebih bersih dan stabil
- distribusi Firebase tidak berisi build eksperimen harian
- release promotion lebih eksplisit

### Trade-off
- feedback tester lebih lambat dibanding distribusi langsung dari `develop`
- butuh disiplin promotion dari `develop` ke `master`
- butuh dokumentasi dan enforcement agar rule “hanya `develop -> master`” tidak dilanggar

## Verification plan
Setelah aturan diimplementasikan, verifikasi minimum:
1. PR `feature/* -> develop` membutuhkan approval
2. push langsung ke `develop` dan `master` ditolak
3. PR `feature/* -> master` ditolak secara rule atau check
4. merge `develop -> master` memicu workflow Firebase
5. merge ke `develop` tidak memicu workflow Firebase
6. branch feature yang belum selesai tidak dihapus otomatis

## File / area implementasi yang kemungkinan terdampak
- GitHub branch protection / ruleset
- GitHub Actions workflow untuk release/distribution
- secrets / credential Firebase distribution
- dokumentasi workflow branch dan release

## Docs / ADR note
`DOCS/ADR UPDATE REQUIRED`

Area dokumentasi yang perlu diperbarui:
- branch strategy
- PR governance
- release promotion flow
- Firebase distribution trigger source

## Keputusan final yang sudah disetujui
- `feature/*` selalu dibuat saat ada perubahan lokal baru
- `feature/*` tidak dihapus sebelum pekerjaan benar-benar selesai total
- `feature/*` merge ke `develop`
- `master` hanya menerima PR dari `develop`
- Firebase App Distribution hanya berjalan dari `master`
