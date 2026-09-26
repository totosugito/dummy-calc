# TODO lintas-tombol: Kotak Kosong Bisa Digerakkan Kiri/Kanan

Ini bukan spec per-tombol — file terpisah karena masalahnya berlaku untuk **semua jenis kotak
kosong** di aplikasi: slot pecahan (`a/b`, `a b/c`, lihat `btn_fraction.md`/`btn_ab_c.md`), base &
exponent `xʸ`/`x²`/`x³`/`x⁻¹` (lihat `btn_x_power_y.md`, `btn_x_square.md`), isi `√`, dan isi
`(...)`.

## Konteks

Saat mengerjakan `xʸ` (lihat `specs/btn_x_power_y.md` Bagian F–H), ditemukan & sudah diperbaiki bug
nyata untuk kotak kosong (posisi kursor & hit-test **di luar batas kotak**, mis. tap jauh di kanan
seluruh ekspresi salah mendarat di kiri kotak). Tapi setelah itu user menguji lebih jauh dan sadar:
**klik di DALAM kotak kosong (kiri vs kanan kotak itu sendiri) selalu menghasilkan posisi yang
sama.** Sudah diverifikasi ulang di emulator: tap sisi kiri vs sisi kanan kotak pembilang kosong
(`\frac{□}{□}`) menghasilkan screenshot kursor yang identik; gejala yang sama juga terjadi di kotak
`xʸ`.

**Ini bukan bug** — sudah dikonfirmasi lewat riset decompile (dua kali, untuk kasus pecahan dan
untuk `xʸ`): `C0357yG.mo359HiPER` (method posisi-kursor asli untuk kotak placeholder) mengembalikan
titik yang sama persis untuk index berapa pun saat kotak kosong, dan kelas ini juga sama sekali
tidak override method hit-test — hit-test untuk kotak kosong 100% diwariskan dari base class, yang
untuk leaf tanpa anak juga tidak melakukan split kiri/kanan berdasar posisi tap. Jadi kode kita
sudah **persis sama** dengan perilaku asli di titik ini, konsisten di semua jenis kotak (pecahan
maupun `xʸ`) — bukan masalah baru, cuma konsekuensi dari cara kode asli memang tidak membedakan
kiri/kanan untuk slot yang benar-benar kosong.

## Ide user

Tambahkan fitur supaya kotak kosong TETAP bisa punya 2 posisi kursor berbeda (kiri & kanan):
- **Klik di kotak:** klik di sisi kiri kotak → kursor ke posisi kiri; klik di sisi kanan kotak →
  kursor ke posisi kanan.
- **Tombol panah ◀/▶:** satu kali tekan = geser satu posisi di dalam kotak (kiri↔kanan), baru
  tekan berikutnya keluar dari kotak ke elemen tetangga/slot induk.

Ini akan jadi **penyimpangan sengaja dari kode asli** (HiPER sendiri tidak punya pembedaan ini untuk
kotak kosong) — bukan perbaikan port, murni demi UX yang lebih presisi/dapat diprediksi.

## Perkiraan tingkat kesulitan: sedang

Butuh perubahan di 3 tempat sekaligus supaya konsisten:

1. **Navigasi (`ExpressionEditor.moveCursorLeft/Right`):** `EmptyNode` (dipakai slot pecahan) saat
   ini dipaksa selalu posisi 0 lewat `afterNode()` (`if (node instanceof EmptyNode) return new
   CursorPointer(node, 0);`) — perlu diperlakukan mirip `NumberNode` yang bisa bergerak
   posisi-demi-posisi (di sini cuma 2 posisi: 0=kiri, 1=kanan), sebelum baru lanjut keluar ke
   sequence/slot induk.
2. **Hit-test (`PlaceholderVisual.hitTest` & `NumberVisual.hitTest` untuk teks kosong):** saat ini
   keduanya `return new CursorPointer(node, 0)` tanpa syarat — perlu bandingkan `point.x` terhadap
   titik tengah kotak (`b.x / 2`) untuk memilih posisi 0 (kiri) atau 1 (kanan).
3. **DEL (`ExpressionEditor.deleteChar`):** logika DEL untuk pecahan (Task 19/20b di
   `btn_fraction.md`, cukup rumit & sudah pas hasil beberapa iterasi) berasumsi `EmptyNode` cuma
   punya 1 posisi — perlu ditinjau ulang supaya DEL saat kursor di posisi 1 (kanan kotak kosong)
   tidak merusak perilaku unwrap/nesting yang sudah teruji.

## Konsistensi lintas tipe box

Slot pecahan pakai `EmptyNode` (di dalam `SequenceNode`), sedangkan isi `xʸ`/`√`/`(...)` yang kosong
pakai `NumberNode("")` polos (bukan `EmptyNode`) — implementasinya perlu berlaku sama untuk kedua
representasi ini supaya tidak muncul lagi inkonsistensi seperti beberapa bug sebelumnya di
`btn_x_power_y.md` (Bagian F–H).

## Status

**Belum dikerjakan.** Menunggu keputusan eksplisit user apakah penyimpangan dari perilaku asli ini
disetujui sebelum implementasi dimulai.
