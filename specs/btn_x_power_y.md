# Tombol "xʸ" (General Power)

## A. Status di kode asli

Ada di kode asli, terdaftar sebagai token "POWY" (`EnumC0209ia.java:433`), tipe node
`EnumC0300sa.dd`. Beda dengan `x²`/`x³` (`K`/`qB`, lihat `btn_x_square.md`) yang cuma menyimpan
base sebagai child tunggal dengan exponent konstanta terkunci, node `dd` punya **2 child sungguhan**
(base & exponent, keduanya beneran node yang bisa dibaca lewat index — `EA.java` `m72HiPER`
±baris 2821-2833, `m74HiPER` ±baris 1749) — jadi exponent-nya memang didesain editable, konsisten
dengan implementasi kita.

**Tapi:** logika klik-tombol (bagian mana yang jadi "base" saat diklik, apakah base langsung
dibungkus kurung, dll.) **tidak ada di source `android.core`** — sudah dicari lewat semua referensi
`EnumC0300sa.dd` (29 file) dan token "POWY" (3 file, semuanya cuma string template parser, bukan
kode insersi). Package ini murni mesin CAS/matematika (konstruksi & evaluasi tree), bukan kode
UI/keypad. Jadi bagian "apa yang terjadi saat tombol xʸ diklik" **tidak bisa diverifikasi dari
decompile** — desain di bawah berdasarkan konfirmasi langsung dari user yang membandingkan dengan
app asli, bukan dari source.

## B. Perilaku (dikonfirmasi user dari app asli, 2026-09-26)

User melaporkan bug: tombol xʸ di app kita hasilnya salah — masih pakai logika lama (asal bungkus
`cursorPointer.node` apa adanya, tanpa kurung). Setelah dicek ke app asli:

- **Klik xʸ tanpa ada apa-apa buat diangkat → hasilnya `(□)^□`** (base kosong **dibungkus kurung**,
  exponent kosong) — bukan `□^□` tanpa kurung.
- **Konfirmasi user:** kurung base ini muncul **konsisten sesuai pola a/b Kasus 1/2** — kalau ada
  operand tepat sebelum kursor, operand itu diangkat jadi base (dibungkus kurung, mis. `5` → xʸ →
  `(5)^□`, kursor di exponent). Kalau tidak ada apa-apa (awal ekspresi / tepat setelah operator),
  base kosong DAN exponent kosong sama-sama muncul sebagai kotak placeholder: `(□)^□`, kursor di
  base (supaya user isi base dulu, baru pindah ke exponent).
- Ini beda dari `x²`/`x³`/`x⁻¹` yang basenya **tidak** dibungkus kurung (lihat `btn_x_square.md`) —
  cuma `xʸ` yang selalu pakai kurung di base.

## C. Implementasi

`engine/ExpressionEditor.insertPower()` (ditulis ulang, bukan lagi lewat helper `insertPowerNode`
yang dipakai x²/x³/x⁻¹ — beda base-selection & parens):
- Operand sebelum kursor (`hasOperandBeforeCursor`, sama seperti a/b) → diangkat, dibungkus
  `ParenthesisNode`, jadi base dari `PowerNode(ParenthesisNode(operand), exponent kosong)`, kursor
  di exponent.
- Tidak ada apa-apa → `PowerNode(ParenthesisNode(NumberNode("")), NumberNode(""))` disisipkan di
  kursor, kursor di base (di dalam kurung).

Diuji di emulator:
- Fresh: `xʸ` → `()^{}` (representasi debug LaTeX untuk `(□)^□` — kotak kosong dirender sebagai
  string kosong untuk `NumberNode("")`, bukan simbol `\square` seperti `EmptyNode` di slot pecahan;
  ini konvensi lama yang sudah dipakai `insertSqrt`/`insertParenthesis` sebelum sesi ini, bukan
  perubahan baru).
- Lift: `5` → `xʸ` → `3` → `(5)^{3}`.
- Setelah operator (tidak ada operand): `2 +` → `xʸ` → `3` → `2 + (3)^{}` (angka `3` masuk ke base,
  bukan salah-angkat `+`).
- Nesting: `1/2` → `▶▶` → `xʸ` → `3` → `(\frac{1}{2})^{3}` (pecahan yang sudah selesai ikut
  diangkat jadi base, konsisten dengan keputusan nesting Task 20b di `btn_fraction.md`).

## D. Bug yang ditemukan & diperbaiki di sepanjang jalan: helper `insertPowerNode` (dipakai x²/x³/x⁻¹)

Saat pertama kali merefactor `insertPower`/`insertSquare` jadi satu helper `insertPowerNode()`
(sebelum tahu soal kurung xʸ), urutan operasinya salah: `PowerNode` dibuat (yang meng-assign ulang
`parent` si base ke `PowerNode` itu sendiri, lewat constructor) **sebelum** mengecek
`targetBase.getParent() instanceof SequenceNode`. Akibatnya pengecekan itu melihat parent yang sudah
berubah (jadi `PowerNode`, bukan `SequenceNode` lagi), masuk ke cabang yang salah, dan node lama
tidak pernah dihapus dari sequence induknya — jadi `5` → `x²` menghasilkan `55^{2}` (angka "5" lama
tertinggal di tempat + "5" yang sama juga jadi base pangkat).

Diperbaiki dengan menentukan parent/index dan `removeChild` **sebelum** membuat `PowerNode` baru
(urutan yang sama seperti kode asli sebelum refactor). Sudah diuji ulang: `5` → `x²` → `5^{2}` (benar).
Bug ini murni kesalahan urutan kode kita sendiri saat refactor, bukan temuan dari kode asli.

## E. Bug lanjutan (2026-09-26): kotak exponent kosong kegedean & navigasi base↔exponent tidak jalan

Dua laporan user setelah kotak placeholder untuk `NumberNode` kosong ditambahkan (lihat
`btn_1_per_x.md`... tidak, lihat catatan di `render/NumberVisual.java`):

**E.1 Kotak exponent kosong kegedean.** Root cause dicek langsung ke `C0357yG.java` (placeholder asli)
lewat subagent: rumus tinggi kotaknya emang punya `0.9f * density` — nilai **piksel absolut, tidak
ikut mengecil** kalau elemen di-scale kecil (beda dari ascent/descent yang otomatis ikut scale text
size). Ini rumus asli yang sama persis kita pakai (faithful), TAPI dikonfirmasi juga: `C0294rh.java`
(NumberVisual asli) **tidak pernah** menggambar kotak untuk angka kosong sama sekali — kotak kosong di
kode asli cuma ada di `C0357yG`/`QA`, dan `C0311tf.java` (layout exponent) tidak py logika kotak-kosong
apa pun. Jadi kombinasi "kotak kosong di dalam exponent yang di-scale kecil" ini **tidak py padanan
di kode asli** untuk dicontek — HiPER kemungkinan besar tidak pernah menampilkan kotak kosong dalam
konteks superscript sama sekali. Karena kita sengaja menambah fitur ini (supaya exponent kosong `x^y`
juga kelihatan sebagai kotak, konsisten dengan a/b), perbaikannya: kalikan `0.9f * density` dengan
skala elemen (`D`) di `NumberVisual.calculateLayout`, supaya kotak ikut mengecil proporsional saat
di-nest dalam konteks kecil (exponent, dst.) — ini **penyimpangan sengaja** dari rumus asli, bukan
salah port, karena skenario yang dituju memang tidak ada di kode asli.

**E.2 Navigasi kiri/kanan/atas/bawah tidak bisa pindah base↔exponent.** `ExpressionEditor.moveCursorLeft/
Right` sebelumnya cuma tahu cara masuk/keluar slot pecahan (`FractionNode`) — untuk `PowerNode` (base/
exponent) dan `ParenthesisNode` (content), begitu `node.getParent()` bukan `SequenceNode`, fungsinya
langsung `return` tanpa berbuat apa-apa. Ditambahkan penanganan simetris seperti pecahan: helper baru
`startOfNode`/`endOfNode` (versi generik `startOf`/`endOf` yang menerima node tunggal, bukan cuma
`SequenceNode`, karena base/exponent power & content parenthesis di implementasi kita bukan
`SequenceNode`-wrapped seperti slot pecahan), lalu cabang baru di kedua arah untuk:
- Masuk/keluar `PowerNode` di posisi Center (`position 0` = sebelum, `1` = sesudah), meniru pola
  `FractionNode` yang sudah ada.
- Masuk/keluar `ParenthesisNode` di posisi Center juga.
- Lompat dari base ke exponent (dan sebaliknya) saat keluar dari salah satu slot itu.

Karena base `xʸ` dibungkus `ParenthesisNode` (lihat Bagian C), keluar dari base perlu **2 kali tekan**
◀/▶ (satu untuk keluar isi angka, satu lagi untuk keluar kurungnya) sebelum masuk ke exponent — ini
konsekuensi struktur bersarang yang wajar (persis seperti pecahan bersarang butuh beberapa kali tekan
buat keluar semua level), bukan bug.

**Navigasi atas/bawah (▲/▼):** ditambahkan juga di
`view/HyperCalDisplayView.findVerticalCursorTarget`, cabang baru untuk `PowerVisual` meniru cabang
`FractionVisual` yang sudah ada — bedanya exponent digambar di ATAS base (bukan di bawah seperti
denominator), jadi ▲ = base→exponent dan ▼ = exponent→base (kebalikan urutan fraction).

Diuji ulang di emulator: kotak exponent sekarang proporsional (screenshot dibandingkan sebelum/sesudah),
◀/▶ dan ▲/▼ berhasil pindah base↔exponent untuk `x²` (base/exponent polos, 1 tekan per boundary) dan
`xʸ` (base dibungkus kurung, 2 tekan untuk keluar base), plus regresi penuh pecahan/mixed/1x/sqrt/
parenthesis/kombinasi bersarang — semua masih benar.

## F. Bug lanjutan (2026-09-26): kursor nempel DI DALAM kotak, bukan di sebelah kotak

User membandingkan lagi ke app asli: menggerakkan ◀/▶ seharusnya menempatkan kursor **di sebelah
kiri/kanan kotak** (di luar kotak), bukan di dalam/menempel garis kotaknya. Kotak pecahan (`a/b`,
lewat `EmptyNode`/`PlaceholderVisual`) sudah benar sejak awal — sudah pakai konvensi default
`MathVisual.getCursorPosition` (index 0 → `-0.5×lebarKursor` yaitu SEBELAH KIRI kotak, else →
`b.x + 0.5×lebarKursor` yaitu SEBELAH KANAN kotak). Tapi kotak-kotak baru yang lewat `NumberVisual`
(exponent/base `xʸ`/`x²`/`x³`, isi `√`, isi `(...)`) **tidak** ikut konvensi ini — `NumberVisual.
getCursorPosition` selalu override dengan rumus karakter-per-karakter, dan untuk teks kosong itu
jatuh ke `return new PointF(0, m)` yaitu **x=0**, persis di tepi kiri DALAM kotak (karena kotak
digambar mulai dari `insetX` yang kecil, x=0 nyaris menempel/overlap garis kotak), bukan di luar
kotak seperti `PlaceholderVisual`.

**Perbaikan:** `NumberVisual.getCursorPosition` sekarang delegasi ke `super.getCursorPosition()`
(default `MathVisual`, yang sama dipakai `PlaceholderVisual`) kalau teksnya kosong, dan baru pakai
rumus pengukuran karakter kalau ada isinya. Sudah diuji ulang lewat screenshot (`√` dan `xʸ`): kursor
sekarang jelas berada di luar sebelah kiri kotak, sama seperti kotak pecahan.

## G. Bug lanjutan (2026-09-26): ◀/▶ dari kotak exponent "macet", dan tap di kotak kosong

User laporkan dua hal setelah Bagian F: (1) kalau kursor di sebelah kiri kotak exponent kosong,
tekan ▶ tidak bisa pindah ke sebelah kanan kotak; (2) tap di dalam kotak kosong tidak bisa memilih
posisi kiri/kanan sesuai lokasi tap. Dicek lagi ke kode asli via subagent:

**G.1 (tap di kotak kosong) — TERNYATA SUDAH SESUAI KODE ASLI, BUKAN BUG.** `C0357yG.java:165-166`
(`mo359HiPER`, method posisi-kursor-berdasar-index): kalau kotak kosong, method ini **selalu
return `(0,0)` apa pun index-nya** — tidak ada percabangan berdasar index sama sekali. Dan
`C0357yG.java` juga **tidak** override method hit-test (`AbstractC0335wD.HiPER(PointF,bool,bool)`)
sama sekali — hit-test untuk QA 100% diwariskan dari base class, yang untuk leaf tanpa anak juga
**tidak** melakukan split kiri/kanan berdasar tap-x (`AbstractC0335wD.java` ±baris 75-207: untuk
leaf dengan 0 anak, langsung return satu hasil yang sama, tidak ada pengecekan `pointF.x` terhadap
titik tengah). Kesimpulan: **di kode asli, tap di mana pun di dalam kotak kosong memang selalu
menghasilkan kursor di posisi yang sama** — tidak ada perbedaan kiri/kanan untuk kotak kosong. Kode
kita (`PlaceholderVisual.hitTest` & `NumberVisual.hitTest` untuk teks kosong, keduanya
`return CursorPointer(node, 0)` tanpa syarat) sudah **persis sama** dengan perilaku asli ini. Tidak
ada perubahan kode untuk poin ini.

**G.2 (◀/▶ macet di exponent) — INI BUG BENERAN, sudah diperbaiki.** Root cause: `PowerVisual.
getCursorPosition(index)` kita yang lama **mendelegasikan** index==1 ("Center setelah power") ke
`exponentVisual.getCursorPosition(0)` — yaitu SELALU ke posisi awal exponent, bukan ke tepi kanan
seluruh power. Dicek ke `C0311tf.java` (asli): file ini **tidak override `mo359HiPER(int)` sama
sekali** — jadi index 0/1 pada node power sendiri (bukan pada exponent-nya) murni pakai rumus
default `AbstractC0335wD.mo359HiPER` (±baris 744-765): index 0 → tepi kiri (`-0.5×lebarKursor`),
index 1 → tepi kanan **pakai lebar node power itu sendiri** (`0.5×lebarKursor + this.b.x`) — TIDAK
pernah delegasi ke child manapun. Jadi arsitektur asli: "Center setelah power" seharusnya muncul di
ujung KANAN seluruh ekspresi `x^y`, bukan di awal kotak exponent — persis seperti pola `Fraction`/
`PlaceholderVisual` yang sudah dipakai di tempat lain (tidak override, andalkan default `MathVisual`).

**Perbaikan:** override `getCursorPosition` di `PowerVisual` dihapus total, kembali pakai default
`MathVisual` (paralel dengan cara `PlaceholderVisual` sudah tidak override method ini sejak awal
sesi). Efek samping baik: ini juga memperbaiki isu serupa yang belum sempat dilaporkan untuk `x²`/
`x³` — sebelumnya kursor "setelah `x²`" (buat lanjut ngetik) muncul salah di awal superscript "2",
sekarang muncul benar di ujung kanan seluruh `x²`. Diuji ulang: screenshot sebelum/sesudah ▶ dari
kotak exponent kosong (sekarang jelas pindah ke ujung kanan seluruh ekspresi), plus lanjut ngetik
setelah `x²` (`5` → x² → `+` → `9` → `5^{2} + 9`, bukan salah sisip di tengah), dan regresi penuh.

## H. Bug lanjutan (2026-09-26): tap di kanvas jauh di kanan kotak tetap mendarat di kiri kotak

Setelah Bagian G, user masih lapor: klik di display (bukan tombol panah) untuk pindah ke kanan kotak
tetap tidak jalan. Dites langsung di emulator (bukan cuma baca kode): tap di kotak exponent kosong
sendiri sudah benar (selalu ke posisi yang sama — cocok Bagian G.1, faithful). Tapi tap **jauh di
kanan seluruh ekspresi `(5)^{}`** (di kanvas kosong, bukan di kotaknya) **juga** mendarat di kiri
kotak exponent, tidak masuk akal — seharusnya minimal mendarat di "Center setelah power" (ujung
kanan seluruh ekspresi), sama seperti perilaku `FractionVisual` untuk tap di luar batas kontennya.

**Root cause:** `PowerVisual.hitTest` tidak punya pengecekan batas (`activeLeft`/`activeRight`)
seperti `FractionVisual` — method itu cuma cek `point.x >= exponentVisual.HiPER.x` TANPA batas atas,
jadi tap berapa pun jauhnya ke kanan tetap dianggap "masuk exponent", lalu exponent yang kosong
selalu balikin posisi yang sama (kiri kotak) — makanya kelihatan seperti tidak bisa pindah ke kanan.

**Perbaikan:** tambahkan pengecekan `activeLeft`/`activeRight` di awal `PowerVisual.hitTest`, persis
pola `FractionVisual` (Bagian 7 `btn_fraction.md`) — tap di luar batas gabungan base+exponent balik
`CursorPointer(pow, 0)` (sebelum) atau `CursorPointer(pow, 1)` (sesudah), baru kalau di dalam batas
diteruskan ke base/exponent seperti sebelumnya. Diuji ulang: tap di kanvas kosong jauh di kanan
`(5)^{}` sekarang benar mendarat di ujung kanan seluruh ekspresi (bukan lagi nyangkut di kiri kotak
exponent); tap di dalam kotak exponent sendiri tetap konsisten (posisi tunggal, sesuai Bagian G.1);
regresi penuh masih benar.

## I. Perubahan (2026-09-26): "()" dihapus sebagai node beneran, jadi dekorasi render saja

Setelah Bagian C–H, user cek ulang ke app asli dan sadar: `()` di sekitar base `xʸ` itu **dummy** —
bukan tanda kurung beneran yang bisa dinavigasi terpisah (seperti tombol `(` `)` asli). Disepakati
untuk dihapus dari model sebagai `ParenthesisNode`, diganti murni dekorasi render, memakai mekanisme
yang sudah ada di kode: `PowerNode.needsParenthesesForBase()` (awalnya cuma dipakai `toLatexString`,
sekarang juga dipakai render kanvas) — ditambah kondisi baru: `"xʸ".equals(operationName)` selalu
`true` (base `xʸ` selalu berkurung apa pun isinya, beda dari kondisi lain yang cuma untuk kasus
angka negatif/ekspresi majemuk). `x²`/`x³`/`x⁻¹` tidak terpengaruh (operationName-nya bukan "xʸ").

**Perubahan kode:**
- `ExpressionEditor.insertPower()` sekarang delegasi penuh ke `insertPowerNode("", "xʸ", true)` —
  tidak ada lagi cabang lift/`ParenthesisNode` terpisah. Base jadi operand polos, sama seperti
  x²/x³/x⁻¹.
- `render/PowerVisual.java`: `calculateLayout` menghitung `parenW` (lebar dekorasi kurung, formula
  sama seperti `ParenthesisVisual`) saat `needsParenthesesForBase()` true, menggeser posisi base ke
  kanan sejauh `parenW` dan menambah lebar total. `draw` menggambar arc kurung kiri/kanan di sekitar
  base (formula arc sama persis `ParenthesisVisual`) sebelum menggambar base itu sendiri.
- Efek samping baik: navigasi ◀/▶ antara base dan exponent `xʸ` sekarang **1 kali tekan** (bukan 2×
  seperti sebelumnya saat base beneran dibungkus `ParenthesisNode`) — lebih sederhana & konsisten
  dengan `x²`/`x³`.

**Bug serius yang ketemu di jalan (dan diperbaiki bersamaan):** `insertPowerNode` (helper dipakai
x²/x³/x⁻¹, dan sekarang xʸ juga) ternyata **crash `StackOverflowError`** kalau dipanggil pada
ekspresi yang benar-benar kosong (`cursorPointer.node` adalah `rootSequence` itu sendiri, bukan
token). Constructor `PowerNode` meng-assign ulang parent dari `targetBase` (di sini `rootSequence`)
ke `PowerNode` yang baru dibuat, lalu `PowerNode` itu ditambahkan sebagai child dari `rootSequence`
yang SAMA — jadi `rootSequence` jadi anak dari anaknya sendiri (siklus), bikin `toLatexString`
rekursi tak terhingga. Bug ini sudah ada dari awal `insertPowerNode` dibuat, tapi baru ketahuan
sekarang karena sebelumnya x²/x³/xʸ selalu dites setelah ngetik angka dulu ("5" lalu x²), tidak
pernah dites di keadaan benar-benar kosong. Diperbaiki: `insertPowerNode` sekarang cek eksplisit
`cursorPointer.node instanceof SequenceNode` (dan node terakhir di root kalau itu juga
`SequenceNode`) → treat sebagai "tidak ada yang bisa diangkat", pakai `insertAtCursor` dengan base
kosong baru (bukan `rootSequence` itu sendiri), kursor ke base. Diuji ulang: `x²`, `x³`, `xʸ` semua
langsung dari keadaan kosong (tanpa ngetik apa pun dulu) — tidak crash lagi, kursor mendarat wajar
di kotak base kosong.

## J. Perubahan (2026-09-26): "()" dihapus total dari tampilan `xʸ`

Setelah Bagian I (kurung jadi dekorasi render, bukan node), user minta lebih jauh: jangan tampilkan
`()` sama sekali untuk `xʸ`. Dibatalkan kondisi `"xʸ".equals(operationName)` yang barusan ditambahkan
di `PowerNode.needsParenthesesForBase()` — method ini balik ke logika aslinya (kurung otomatis hanya
untuk kasus yang memang butuh secara matematis: basis negatif, ekspresi majemuk, operator), yang juga
dipakai bersama oleh x²/x³/x⁻¹/xʸ tanpa perbedaan lagi. Mekanisme render dekorasi di `PowerVisual`
(Bagian I) tidak diubah — otomatis berhenti menggambar kurung karena `needsParenthesesForBase()`
sekarang mengembalikan `false` untuk base kosong/angka biasa seperti sebelumnya. Diuji ulang: `xʸ`
kosong sekarang `^{}` (bukan `()^{}`), `5` → `xʸ` → `3` sekarang `5^{3}` (bukan `(5)^{3}`); x²/x³/
pecahan/1x/kombinasi masih benar semua.
