# Tombol "a b/c" (Mixed Number)

Dipisah jadi file sendiri (2026-09-26) dari `btn_fraction.md` Bagian F, supaya spec di folder ini
per-tombol (satu file = satu tombol keypad). Untuk pecahan biasa "a/b" dan dasar-dasar model AST
pecahan (`FractionNode`, `SequenceNode` slot, dll.), lihat `btn_fraction.md` — file ini cuma bagian
yang spesifik untuk mixed number.

## A. Latar belakang

Permintaan user: tambahkan tombol pecahan campuran (bilangan bulat + pecahan, mis. `1 2/3`), yang belum ada di implementasi kita sebelumnya.

## B. Temuan decompile (via subagent riset, `android.core`)

- Bukan class terpisah. HiPER memakai node generik `C0067Lb` yang sama dengan pecahan biasa, hanya dengan **3 anak** (bukan 2): `L(0)`=bagian bulat, `L(1)`=pembilang, `L(2)`=penyebut (`Rc.java:384-392`). Jenis node dibedakan lewat enum `EnumC0300sa.sa` (mixed) vs `EnumC0300sa.mB` (pecahan biasa).
- Tombol didaftarkan di `EnumC0209ia.java:485-487`, persis setelah tombol a/b — string label/drawable tidak bisa dipulihkan (obfuscated), tidak ada resource `res/*.xml` yang menyebut "mixed"/"ab_c" (satu-satunya hit publik, `rfMixedRB`, adalah radio button *format hasil* mixed vs improper, bukan tombol input).
- Serialisasi non-CAS: bagian bulat + pecahan digabung tanpa operator, dipisah karakter spasi tipis (`Rc.java:384-392`) — mengonfirmasi ini bukan `int + a/b` melainkan satu token gabungan.
- Rendering/navigasi kursor: `GA.java:93`, `C0067Lb.java:1004/1011/1118`, `AbstractC0033Df.java:721` memperlakukan `sa` dan `mB` sama di hampir semua predikat "ini pecahan?" — jadi mixed number pakai ulang renderer pecahan biasa, dengan bagian bulat digambar tambahan. `C0197hd.java:734` (`iL==3 && sa`) adalah cabang edit/DEL khusus untuk node 3-anak ini.

## C. Implementasi kita

- `engine/model/FractionNode.java`: tambah field `integerPart` (nullable `SequenceNode`), factory `FractionNode.createMixed(integerPart, num, den)`, `isMixed()`. Pecahan biasa tidak berubah (`integerPart == null`).
- `engine/ExpressionEditor.insertMixedFraction()` (tombol baru "a b/c"): siklus mengikuti pola a/b yang sudah ada —
  - Kursor di bagian bulat → lompat ke pembilang.
  - Kursor di pembilang → lompat ke penyebut (sama seperti a/b biasa).
  - Ada operand sebelum kursor → operand itu diangkat jadi **bagian bulat** (bukan pembilang seperti a/b biasa), pembilang/penyebut kosong, kursor ke pembilang.
  - Tidak ada apa-apa → bikin mixed number kosong semua, kursor di bagian bulat.
  - Konsisten dengan keputusan Task 20b (di `btn_fraction.md`): pecahan/mixed number yang sudah selesai sebagai "operand sebelum kursor" ikut ter-nesting (bukan dikecualikan), jadi `1/2` lalu `a b/c` menghasilkan `\frac{1}{2\ \frac{□}{□}}` — sudah diuji di emulator.
  - DEL (`deleteChar`) dan unwrap (`unwrapFraction`) diperluas untuk 3 slot: DEL di kotak pembilang kosong (mixed) lompat ke akhir bagian bulat; DEL di kotak bagian bulat kosong berperilaku seperti DEL sebelum pecahan; unwrap menyertakan token bagian bulat + pembilang saat penyebut kosong.
  - Navigasi kiri/kanan (`moveCursorLeft/Right`) diperluas: batas kiri pembilang ↔ akhir bagian bulat, batas kanan bagian bulat ↔ awal pembilang.
- `render/FractionVisual.java`: field `integerVisual`, digambar di kiri stack pembilang/penyebut pada ukuran penuh (tidak diskalakan 0.8× seperti pembilang/penyebut), dengan gap kecil (`spaceWidth * 0.5f`) dan diposisikan center vertikal terhadap tinggi total. Garis pembagi (`barLeft`) digeser ke kanan supaya tidak menembus bagian bulat. Hit-test menambah region bagian bulat di sisi kiri.
- `render/VisualTreeBuilder.java`: membangun `integerVisual` dari `frac.integerPart` bila ada.
- `render/VisualTree.java`: `find()` juga menelusuri `frac.integerVisual` (bukan cuma numerator/denominator) — ini sempat jadi bug (lihat Bagian E di bawah).
- UI: tombol baru `btn_mixed_fraction` ("a b/c") di `activity_hypercal.xml`, di sebelah `a/b`; tombol `%` dipindah ke baris navigasi ▲/▼ (kolom 2) untuk memberi ruang. *(Update 2026-09-26: baris memory atas dihapus; % / ▲ / ▼ sekarang di baris 1 bareng x³ / x⁻¹, lihat `btn_x_square.md` Bagian E.)*
- **Catatan jujur:** posisi vertikal/horizontal persis bagian bulat (skala, gap, baseline) adalah rekonstruksi wajar dari deskripsi "pakai ulang renderer pecahan + bagian bulat digambar tambahan di kiri" — koordinat piksel pastinya (`Qg.java` versi mixed) tidak ada di laporan riset (rendering method untuk cabang `sa` tidak eksplisit ditelusuri baris-per-baris seperti pecahan biasa). Kalau nanti ada bukti lebih pasti (mis. smali), sesuaikan ulang.
- Diuji di emulator: lift operand ke bagian bulat, isi 3 slot via siklus tombol, render visual (screenshot), DEL berantai sampai unwrap penuh (denominator → numerator → integer part → hilang), nesting a/b → a b/c, dan regresi alur pecahan biasa (tidak berubah).

## D. Bug yang sempat ditemukan & diperbaiki (2026-09-26)

Setelah implementasi awal, kursor pada slot bagian bulat (`integerPart`) yang baru dibuat malah
tampil di posisi "Center setelah pecahan" (ujung kanan, tinggi penuh), bukan di kotak bagian bulat
paling kiri. Root cause: `render/VisualTree.find()` (helper pemetaan node model → visual untuk
kursor) belum tahu soal field `integerVisual` yang baru, cuma menelusuri
`numeratorVisual`/`denominatorVisual`. Saat kursor di-set ke `integerPart`, pencarian gagal
(return null), lalu `HyperCalDisplayView` jatuh ke fallback rendering di posisi lain. Diperbaiki
dengan menambahkan `find(frac.integerVisual, target)` di awal cabang `FractionVisual` pada
`VisualTree.find()`. Sudah diuji ulang di emulator — kursor sekarang benar mendarat di kotak bagian
bulat.
