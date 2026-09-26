# Tombol "1/x" (Reciprocal)

## A. Status di kode asli

**Tidak ada.** Sudah dicek waktu mengerjakan tombol pecahan (`btn_fraction.md`) dan pecahan campuran
(`a b/c`) — pendaftaran tombol di `EnumC0209ia.java` untuk grup pecahan hanya berisi entri untuk
`a/b` (`EnumC0300sa.mB`, baris 485) dan `a b/c` (`EnumC0300sa.sa`, baris 487). Tidak ada entri ketiga
untuk "1/x". HiPER Calc tidak punya tombol reciprocal khusus di keypad ini — kalau user mau
`1/x`, caranya ya ketik `a/b` lalu isi pembilang `1` secara manual.

Jadi tombol `btn_reciprocal` ("1/x") di sample kita ini **murni tambahan kita sendiri**, bukan hasil
port dari sesuatu yang ada di source asli. Karena tidak ada referensi decompile untuk perilakunya,
desain di bawah ini dibuat supaya **konsisten dengan konvensi tombol pecahan yang sudah ada**
(`a/b`, `a b/c`), bukan hasil reverse-engineering.

## B. Ide desain

Tombol ini pada dasarnya cuma "pintasan" untuk `1/(operand)` — jadi perilakunya dibuat meniru
Kasus 1 dari `a/b` (operand tepat sebelum kursor diangkat), tapi hasilnya taruh operand itu di
**penyebut** (bukan pembilang, karena pembilangnya sudah pasti `1`):

1. **Ada operand tepat sebelum kursor** (angka, pecahan, sqrt, power, parenthesis yang sudah selesai
   — dicek pakai helper yang sama dengan `a/b`, `hasOperandBeforeCursor`) → operand itu diangkat jadi
   penyebut pecahan baru `1/operand`, kursor pindah ke Center **setelah** pecahan (siap lanjut ngetik
   operator berikutnya). Contoh: `5` lalu `1/x` → `\frac{1}{5}`, kursor setelah pecahan.
   - Berlaku juga untuk pecahan yang sudah selesai (konsisten dengan keputusan nesting Task 20b di
     `btn_fraction.md`): `1/2` lalu `1/x` → `\frac{1}{\frac{1}{2}}`.
2. **Tidak ada apa-apa untuk diangkat** (awal ekspresi, tepat setelah operator, di kotak kosong) →
   sisipkan `1/[]` kosong, kursor langsung di kotak penyebut supaya user tinggal ngetik. Sama seperti
   perilaku `a/b` Kasus 2.

Kenapa bukan "cycle" seperti `a/b`/`a b/c` (klik lagi buat pindah slot)? Karena `1/x` cuma py 1 slot
yang bisa diisi user (penyebut — pembilangnya selalu `1`, tidak pernah kosong untuk diisi), jadi tidak
ada slot kedua untuk di-cycle ke sana. Ini beda dengan `a/b` (2 slot: pembilang→penyebut) dan `a b/c`
(3 slot: bulat→pembilang→penyebut).

Kenapa bukan meniru pola `x²`/`xʸ` (yang selalu membungkus token, tanpa fallback kotak kosong)?
Karena `x²`/`xʸ` di kode asli memang selalu punya base (default `NumberNode("x")` kalau kosong), sedangkan
pecahan (termasuk `1/x`) secara desain sample ini punya kotak placeholder kosong sebagai state valid
(lihat `btn_fraction.md` Bagian 2) — jadi `1/x` di awal ekspresi kosong seharusnya juga menampilkan
kotak kosong yang bisa diisi, bukan memaksa base default `"x"` yang tidak masuk akal untuk kalkulator.

## C. Implementasi

`engine/ExpressionEditor.insertReciprocal()`:
- Operand sebelum kursor terdeteksi (`hasOperandBeforeCursor`, helper yang sama dipakai `insertFraction`
  dan `insertMixedFraction`) → operand dilepas dari sequence induknya, dibungkus
  `new FractionNode(new NumberNode("1"), target)`, disisipkan balik di posisi yang sama, kursor
  di-set ke `afterNode(frac)` (Center setelah pecahan — helper yang sama dipakai di tempat lain).
- Tidak ada operand → `insertAtCursor` pecahan `1/[]` kosong, kursor di `NumberNode("")` penyebut
  (perilaku lama, tidak berubah).

Diuji di emulator:
- `5` → `1/x` → `\frac{1}{5}` (angka diangkat jadi penyebut)
- `1/x` (state kosong) → `3` → `\frac{1}{3}` (fallback kotak kosong terisi)
- `2 +` → `1/x` → `4` → `2 + \frac{1}{4}` (tidak ada operand sebelum kursor karena baru habis operator
  → fallback kotak kosong, bukan salah mengangkat `+`)
- `1/2` → `1/x` → `\frac{1}{\frac{1}{2}}` (nesting pecahan konsisten dengan Task 20b)
- `5` → `1/x` → `+ 3` → `\frac{1}{5} + 3` (kursor memang mendarat setelah pecahan, siap lanjut ngetik)

Tidak ada perubahan pada `render/FractionVisual.java` atau file render lain — `1/x` cuma
menghasilkan `FractionNode` biasa (`integerPart == null`), jadi tampilannya otomatis sama persis
dengan pecahan `a/b` biasa.
