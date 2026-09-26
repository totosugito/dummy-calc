# Tombol "x²" (Square)

## A. Status: sudah ada di kedua sisi (app kita & kode asli)

Beda dengan `a b/c` dan `1/x`, tombol ini **sudah ada di app kita** (`btn_square` di
`activity_hypercal.xml`, `ExpressionEditor.insertSquare()`) **dan ada juga di kode asli HiPER
Calc**. Permintaan user: cek dulu perilaku persisnya di kode asli sebelum menambah/mengubah fitur
di sisi kita.

## B. Temuan decompile (via subagent riset, string XOR-decode `EF.HiPER()`/`DC.HiPER()`)

- **Registrasi tombol:** `EnumC0209ia.java:431` — token khusus "POW2", tipe node `EnumC0300sa.K`.
  Tetangga di tabel yang sama: `qB`=POW3 (baris 432), `dd`=POWY / x^y (baris 433). Jadi x² **bukan**
  gula sintaksis untuk node power umum dengan child exponent literal "2" — dia enum node
  tersendiri, terpisah dari x³ dan dari x^y umum.
- **Struktur node:** node `K` **cuma punya 1 child (base)**. Nilai exponent "2" **bukan child yang
  disimpan** — `EA.java` (`m74HiPER`, ±baris 1885) mengembalikan konstanta statis siap-pakai ("2")
  setiap kali exponent dibutuhkan, alih-alih membaca child index tertentu seperti node power umum
  (`c0067Lb.L(HiPER(enumC0300sa))`). Analog persis untuk POW3 dengan konstanta "3".
- **Akibatnya, exponent "2" di kode asli TERKUNCI** — tidak ada child node untuk exponent yang bisa
  diklik/diedit user. Mau ubah ke pangkat 3 caranya pencet tombol POW3 terpisah, bukan edit exponent
  x² yang sudah ada.
- **Konversi internal** (`C0196hc.java:255-267`): ada jalur konversi antara bentuk ringkas ini (K/qB)
  dan bentuk power umum dengan exponent literal beneran, tapi ini cuma dipakai untuk kebutuhan mode
  tampilan tertentu (mis. serialisasi ke bentuk "flat" — lihat juga `EB.java:929-934`), bukan hasil
  dari klik tombol x² itu sendiri.
- **Belum terverifikasi** (obfuscation terlalu dalam untuk ditelusuri dalam budget riset ini):
  logika target-base-selection persis (apa saja yang "diangkat" jadi base saat x² diklik — cuma
  token tepat sebelum kursor, atau bisa juga expression yang lebih besar), dan penanganan DEL/backspace
  spesifik untuk node K (apakah ada unwrap seperti pecahan). Tidak ditemukan bukti unwrap khusus di
  `C0067Lb.java:1004/1118`, `C0070Mc.java:266`, `EB.java:933` dalam pass ini.

## C. Keputusan

Ditanyakan ke user: ikuti kode asli (kunci exponent, node khusus `SquareNode(base)` tanpa child
exponent) atau tetap editable seperti sekarang (real `PowerNode(base, NumberNode("2"))`)?

**Keputusan user (2026-09-26): tetap editable** (opsi yang direkomendasikan). Alasan: lebih fleksibel
untuk kalkulator sample ini — user bisa ubah "2" jadi angka lain tanpa hapus-ulang seluruh ekspresi,
dan ini konsisten dengan tombol `xʸ` (`insertPower()`) yang memang sejak awal exponent-nya editable
(disimpan sebagai `NumberNode` beneran, bukan konstanta).

**Jadi: tidak ada perubahan kode.** `ExpressionEditor.insertSquare()` tetap seperti sekarang — bikin
`PowerNode(targetBase, NumberNode("2"), "x²")` yang exponent-nya adalah child sungguhan (bisa
dikursori dan diedit), bukan dikunci. Ini didokumentasikan sebagai **penyimpangan sengaja** dari
kode asli (lebih permisif daripada HiPER), bukan kesalahan port.

## D. Sisa yang belum terverifikasi (kalau nanti mau presisi lebih jauh)

- Target-base-selection persis saat x² diklik (dibandingkan `insertSquare()`/`insertPower()` kita
  yang mengambil `cursorPointer.node` apa adanya, termasuk kalau itu `EmptyNode`/placeholder).
- Perilaku DEL tepat setelah x² di kode asli (unwrap ke base saja, atau cuma hapus whole node).

Keduanya diblokir kedalaman obfuscation yang sama dengan Task 18/Display Task 12 di `btn_fraction.md`
(butuh baca bytecode smali langsung, tool `apktool`/`baksmali` tidak tersedia di lingkungan ini).

## E. Update (2026-09-26): `insertCube()` (x³) & `insertNegativeOnePower()` (x⁻¹) ditambahkan

Mengikuti ide user untuk menjadikan mekanisme "wrap base jadi power" ini reusable, dua tombol baru
ditambahkan lewat helper yang sama (`ExpressionEditor.insertPowerNode`, private): `insertCube()`
("x³", exponent tetap "3") dan `insertNegativeOnePower()` ("x⁻¹", exponent tetap "-1"). Sama seperti
x²: exponent-nya *editable* (child `NumberNode` beneran, bukan konstanta terkunci seperti kode asli
`EnumC0300sa.qB`), base tidak dibungkus kurung. ~~Belum ada tombol UI untuk keduanya~~
**Update (2026-09-26):** tombol UI `btn_cube` ("x³") dan `btn_neg_one` ("x⁻¹") sekarang ada di
baris pertama `activity_hypercal.xml` — baris tombol memory (M+/M−/MC/MS/MR) dihapus atas
permintaan user dan slotnya dipakai untuk x³, x⁻¹, plus % / ▲ / ▼ yang naik dari baris
navigasi lama. Sudah di-wiring di `HyperCalActivity.onClick` → `editor.insertCube()` /
`editor.insertNegativeOnePower()`.

**Bug yang sempat masuk lalu diperbaiki:** saat helper `insertPowerNode` ini pertama kali dibuat,
urutan operasinya salah dan menyebabkan `5` → `x²` menghasilkan `55^{2}` (node lama tertinggal di
sequence). Detail root cause & fix ada di `specs/btn_x_power_y.md` Bagian D (ditemukan sambil
mengerjakan tombol `xʸ`, tapi bug-nya ada di helper yang dipakai bersama x²/x³/x⁻¹ ini).
