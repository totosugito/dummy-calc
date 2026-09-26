# Bug: Navigasi Kursor "Macet" Saat Melewati Operator (+, −, ×, ÷)

Bukan spec per-tombol — ini bug navigasi kursor global, berlaku untuk semua `OperatorNode`
(`+`, `−`, `×`, `÷`, dan operator lain apa pun yang direpresentasikan lewat class yang sama).

## Laporan user

Pada ekspresi `4 + 3`, menggerakkan kursor ke kiri (◀) butuh **2 kali klik** untuk melewati tanda
`+` — padahal seharusnya cukup 1 kali (operator cuma simbol tunggal, tidak ada isi di dalamnya untuk
dijelajahi). Dikonfirmasi user juga muncul di operator lain (`−`, `×`, `÷`), bukan cuma `+`.

## Root cause (diverifikasi via screenshot per-langkah di emulator)

`ExpressionEditor.moveCursorLeft/Right` dulu memberi `OperatorNode` posisi kursor sendiri (0 dan 1,
lewat `afterNode()` default `CursorPointer(node, node.getLength())`), padahal:
1. Kedua posisi itu **dirender di titik yang persis sama secara visual** (karena `OperatorNode` tidak
   override `getCursorPosition`, jadi pakai formula default `MathVisual` yang untuk objek sekecil
   simbol operator praktis tidak membedakan "kiri" vs "kanan" secara terlihat).
2. Navigasi kiri/kanan **tidak pernah benar-benar memakai posisi operator itu sendiri** — begitu
   kursor "di atas" operator (posisi berapa pun), tekan panah berikutnya selalu lompat ke sibling
   sebelum/sesudahnya. Jadi posisi kedua itu murni beban mati (dead state) yang kebetulan digambar di lokasi yang sama dengan batas token tetangganya — user merasa 1 klik "tidak
   ngapa-ngapain" (padahal state model-nya berubah, cuma tidak terlihat).

Percobaan pertama (cuma mengubah `afterNode()` supaya `OperatorNode` selalu posisi 0, meniru
`EmptyNode`) **belum cukup** — cuma memindahkan masalah, bukan menghilangkan: klik "macet" pindah ke
pasangan state lain (`CursorPointer(operator, 0)` vs `CursorPointer(angkaSebelumnya, akhir)`), yang
render-nya JUGA di titik yang sama (batas antara angka dan operator). Baru ketahuan setelah
screenshot per-langkah dibandingkan satu-satu — pentingnya verifikasi visual, bukan cuma baca kode.

## Perbaikan final

Operator sekarang **tidak pernah jadi tempat singgah kursor sendiri sama sekali** — navigasi
kiri/kanan pada level sequence (`ExpressionEditor.moveCursorLeft/Right`) mendeteksi kalau sibling
yang mau dituju adalah `OperatorNode`, dan langsung lompat SATU LANGKAH LEBIH JAUH lagi (ke sibling
di seberang operator itu), bukan berhenti di operatornya:
- `moveCursorLeft`: kalau sibling sebelumnya adalah operator, lompat ke `afterNode(sibling
  sebelum-sebelumnya)` (atau ke awal sequence kalau operator itu token pertama).
- `moveCursorRight`: kalau sibling berikutnya adalah operator, lompat ke `enterFromLeft(sibling
  sesudah-sesudahnya)` (helper baru, generalisasi cek "cara masuk dari kiri" yang tadinya inline) —
  atau ke akhir sequence kalau operator itu token terakhir (mis. `4 +` yang belum lengkap).

`afterNode()` juga tetap diberi fallback posisi tunggal untuk `OperatorNode` (seperti `EmptyNode`)
untuk kasus pemanggilan lain (mis. lewat `endOf`/unwrap fraction) yang mungkin kebetulan mengenai
operator — jaga-jaga, bukan jalur utama perbaikan.

## Verifikasi

Diuji di emulator dengan screenshot per-langkah (bukan cuma baca teks LaTeX, karena itu tidak
menunjukkan posisi kursor): `4 + 3`, tekan ◀ tiga kali dari akhir "3" → tiap klik sekarang
menghasilkan posisi kursor yang **jelas berbeda secara visual** (akhir³→awal³→akhir4→awal4), tidak
ada lagi klik yang terlihat diam di tempat. Diuji juga arah ▶ (mirror), operator lain (`−`, `×`,
`÷`), kasus tepi (operator sebagai token pertama/terakhir dalam ekspresi belum lengkap), dan regresi
penuh (pecahan, mixed number, 1/x, x², xʸ, kombinasi bersarang) — semua masih benar, tidak ada
crash.
