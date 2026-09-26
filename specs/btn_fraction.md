# SPECIFICATION: Tombol a/b (Fraction / Pecahan)
**Target: 100% Persis Kode Asli HiPER Calc**  
**Referensi File Asli Decompile:**
- Model AST: `android.core.C0067Lb.java`, `android.core.QA.java`, `android.core.EnumC0300sa.java` (`EnumC0300sa.mB`), `android.core.EA.java`, `android.core.ZB.java`
- Visual Renderer: `android.core.Qg.java`, `android.core.C0357yG.java` (Empty Placeholder Box), `android.core.PH.java`
- Touch & Cursor Caret: `android.core.AbstractC0335wD.java`, `android.core.UF.java`, `android.core.BE.java`
- Keypad Action & Navigation: `android.core.C0196hc.java`, `android.core.GA.java`

---

## 1. Variabel & Data Storage (AST Model)
Di HiPER Calc, pecahan bukan kelas ad-hoc, melainkan fungsi/operator bertingkat:
- **Tipe Node:** `C0067Lb` dengan tipe operator `EnumC0300sa.mB` (*FRACTION*).
- **List Anak (`c`):**
  - Index `0`: `numerator` (Pembilang) -> tipe `ZB`. Jika kosong, berisi **`QA`** (*Empty Placeholder Node*).
  - Index `1`: `denominator` (Penyebut) -> tipe `ZB`. Jika kosong, berisi **`QA`** (*Empty Placeholder Node*).
- **Mode Linear (`a`):** Boolean flag `a` pada `Qg.java`: jika layar sempit atau mode linear aktif, dirender format `a/b` dengan karakter `'/'`. Jika vertikal, dirender bertumpuk $\frac{a}{b}$.
- **Nested Indicator (`m$3()`):** `m$3()` bernilai `0.0f` jika bukan nested fraction, dan `AbstractC0335wD.HiPER(paint, 1.0f)` jika ada pecahan di dalam pecahan.

---

## 2. Reaksi Saat Tombol a/b Diklik (Keypad Insertion Action)
Berdasarkan `C0196hc.java` & `EA.m72HiPER`:
- **Kasus 1: Ada angka/token sebelum posisi kursor saat ini (misal ketik `5` lalu tekan `a/b`):**
  - Angka `5` otomatis diangkat menjadi **pembilang (`numerator`)** pada index `0`.
  - Dibuatkan **penyebut (`denominator`)** kosong bertipe `QA`.
  - **Kursor langsung diarahkan ke penyebut (denominator)** pada index 0.
- **Kasus 2: Tidak ada angka sebelum kursor (display kosong atau setelah operator):**
  - Dibuat fraction dengan pembilang `QA` (kotak kosong) dan penyebut `QA` (kotak kosong).
  - **Kursor diletakkan di pembilang (numerator)** pada index 0.
- **Kasus 3: Kursor sedang berada di dalam pecahan (misal di pembilang):**
  - Menekan tombol `a/b` memindahkan kursor ke penyebut (`denominator`).

---

## 3. Empty Placeholder Box Renderer (`C0357yG.java`)
Ketika pembilang atau penyebut berupa node kosong (`QA`), HiPER **TIDAK membiarkannya kosong tak terlihat**, melainkan merendernya dengan `C0357yG`:
- **Dimensi Kotak Kosong (`C0357yG.java` baris 72):**
  $$\text{boxWidth} = \text{paint.measureText}("0") \times 1.2\text{f}$$
  $$\text{boxHeight} = \text{paint.getTextSize}() \times 0.9\text{f}$$
- **Gaya Kotak (`C0357yG.java` baris 204–218):**
  - Tebal garis outline: `paint.measureText("0") * 0.1f` (Stroke style).
  - Margin inset: `f = 0.15f * paint.measureText("0")`.
  - Warna: Mengikuti warna teks ekspresi sekunder (`#80FFFFFF` atau warna tema).
  - Command canvas:
    ```java
    canvas.drawRect(left + f, top, right - f, bottom, paintStroke);
    ```
- **Kursor di Placeholder:**
  Ketika kursor berada di dalam node `QA`, kursor digambar berkedip di dalam kotak tersebut.

---

## 4. Font Size & Skala Visual (Visual Scale)
Berdasarkan `Qg.java` baris 34–63:
- **Skala Font Anak Pecahan (`this.D`):**
  $$\text{childScale} = D \times 0.8\text{f}$$
  (Wajib tepat $80\%$ dari ukuran font dasar ekspresi).
- **Font Face:** Menggunakan font `Arial` (`Typeface.create("Arial", Typeface.NORMAL)`).

---

## 5. Algoritma Layout & Geometri Garis (Exact 100% Qg.java Math)
Berdasarkan `Qg.java` baris 113–142:
- **Lebar Total (`b.x`):**
  $$\text{width} = (m\$3() \times 2.0\text{f}) + \max(\text{numW}, \text{denW})$$
- **Posisi Y Garis Pecahan (`this.c` / `barY`):**
  $$\text{barY} = \text{numH} + (\text{measureText}(" ") \times 0.2\text{f})$$
- **Tebal Garis Pecahan (`thickness`):**
  $$\text{thickness} = \text{measureText}(" ") \times 0.3\text{f}$$
- **Posisi Denominator (`fHiPER3`):**
  $$\text{denY} = \text{barY} + \text{thickness} + (\text{measureText}(" ") \times 0.2\text{f})$$
- **Tinggi Total (`b.y`):**
  $$\text{height} = \text{denY} + \text{denH}$$
- **Titik Baseline Ekspresi (`this.m`):**
  $$\mathbf{m = ((-paint.ascent()) \times 0.4\text{f}) + barY}$$
  *(Operator $+$ atau $\times$ di samping pecahan sejajar presisi di tengah garis pecahan)*.

---

## 6. Koordinat & Ukuran Kursor (Cursor Caret Geometry)
Berdasarkan `AbstractC0335wD.mo360HiPER()` & `UF.k(Canvas)`:
- **Ukuran Lebar Kursor:**
  $$\text{cursorWidth} = \text{paintChild.measureText}(" ") \times 0.35\text{f}$$
- **Tinggi Kursor di Pembilang / Penyebut:**
  Mengikuti skala font anak $0.8\text{f}$ sehingga kursor di dalam pecahan lebih kecil $20\%$ dibanding kursor di luar pecahan.
- **Kursor di Luar Pecahan (Center / Main Baseline):**
  Tinggi dan lebar kursor mengikuti skala penuh $1.0\text{f}$ dengan titik tengah Y tepat di baseline utama $m$.

---

## 7. Hit-Testing 2D Presisi (Horizontal Bounds & Center Outer Slot)
Berdasarkan `Qg.java` baris 144–186:
- Pembilang menempati span horizontal $[\text{numX}, \text{numX} + \text{numW}]$ pada rentang $Y < \text{barY}$.
- Penyebut menempati span horizontal $[\text{denX}, \text{denX} + \text{denW}]$ pada rentang $Y \ge \text{barY}$.
- **Pengecekan Horizontal:**
  - Jika sentuhan berada **di sebelah kiri pecahan ($X < \text{margin}$)**:
    Kursor mendarat di **Sequence induk sebelum pecahan** (`index = indexInParent`). Kursor berada di posisi **Center (sejajar garis pecahan)**.
  - Jika sentuhan berada **di sebelah kanan pecahan ($X > \text{fractionWidth} - \text{margin}$)**:
    Kursor mendarat di **Sequence induk setelah pecahan** (`index = indexInParent + 1`). Kursor berada di posisi **Center (sejajar garis pecahan)**.
  - Jika sentuhan berada di dalam rentang horizontal:
    - $Y < \text{barY} \rightarrow$ mendarat ke pembilang (`numerator`).
    - $Y \ge \text{barY} \rightarrow$ mendarat ke penyebut (`denominator`).

---

## 8. Navigasi Kursor & Penghapusan (DEL / Left / Right)
Berdasarkan `C0067Lb.java` & `ZB.java`:
- **Panah Kanan (`btn_cursor_right`):**
  - Dari posisi Center sebelum pecahan $\rightarrow$ melompat ke awal pembilang.
  - Dari akhir pembilang $\rightarrow$ melompat ke awal penyebut.
  - Dari akhir penyebut $\rightarrow$ melompat ke **posisi Center tepat setelah pecahan $\frac{a}{b}|$**.
  - Dari posisi Center setelah pecahan $\rightarrow$ melompat ke token berikutnya di Sequence.
- **Panah Kiri (`btn_cursor_left`):**
  - Dari posisi setelah pecahan $\rightarrow$ melompat ke posisi Center tepat setelah pecahan.
  - Dari posisi Center setelah pecahan $\rightarrow$ melompat ke akhir penyebut.
  - Dari awal penyebut $\rightarrow$ melompat ke akhir pembilang.
  - Dari awal pembilang $\rightarrow$ melompat ke **posisi Center tepat sebelum pecahan $|\frac{a}{b}$**.
  - Dari posisi Center sebelum pecahan $\rightarrow$ melompat ke token sebelumnya.
- **Hapus (`btn_delete` / DEL):**
  - Di penyebut: jika berisi angka, hapus angka. Jika kosong (`QA`), pindah ke akhir pembilang.
  - Di pembilang: jika kosong dan penyebut kosong, hapus pecahan seluruhnya. Jika pembilang kosong tapi penyebut ada, biarkan kotak pembilang tetap tampak.
  - Jika di posisi Center setelah pecahan lalu DEL: hapus penyebut atau unwrapping pecahan.

---

## 9. Penemuan Decompile & Titik Perbedaan dengan Kode Kita Sebelumnya

### A. Tanda Kotak Kosong (Empty Placeholder Box)
- **Status Sebelumnya:** Kita menggunakan `NumberNode("")` yang merender string kosong `""` sehingga tidak tampak kotak sama sekali saat numerator/denominator kosong.
- **Kode Asli HiPER (`android.core.QA.java` & `C0357yG.java`):**
  - Menggunakan node `QA` (`EmptyNode`).
  - Dirender oleh `C0357yG`:
    - `boxWidth = paint.measureText("0") * 1.2f`
    - Inset margin: `f = 0.15f * paint.measureText("0")`
    - Stroke tebal: `fMeasureText * 0.1f` dengan `Paint.Style.STROKE`
    - Canvas drawRect: `canvas.drawRect(left + f, top + fB, right - f, bottom + fB, paint)`
  - Saat input angka pada `EmptyNode`, node `EmptyNode` digantikan/diisi dengan `NumberNode(digit)`.

### B. Posisi Kursor di Sebelah Kiri / Kanan Pecahan (Center Slot Navigation)
- **Status Sebelumnya:** Tap atau navigasi di dekat pecahan selalu memaksa kursor masuk ke pembilang atau penyebut (`FractionNode.numerator` atau `FractionNode.denominator`), dan kursor tidak bisa berhenti di level garis pecahan (posisi Center pada Sequence parent).
- **Kode Asli HiPER (`Qg.java` baris 144–186, `ZB.java` mo265F & mo267HiPER, `AbstractC0335wD.java` mo359HiPER):**
  - **Hit Testing 2D:**
    - Jika `point.x < activeLeft` (di sebelah kiri span aktif numerator/denominator): hit test mengembalikan kursor di **parent Sequence sebelum Fraction** (`indexInParent`, baseline Center sejajar garis pecahan).
    - Jika `point.x > activeRight` (di sebelah kanan span aktif numerator/denominator): hit test mengembalikan kursor di **parent Sequence setelah Fraction** (`indexInParent + 1`, baseline Center sejajar garis pecahan).
  - **Cursor Position & Visual Size:**
    - Kursor di parent Sequence memiliki ukuran tinggi penuh $1.0\times$ dan $Y = \text{Sequence.m}$ (tepat sejajar garis pecahan/baseline operator $+$ atau $\times$).
    - Kursor di dalam pembilang/penyebut berukuran $0.8\times$ (skala anak).

---

## 10. Task List Implementasi & Status

- [x] **Task 1: Empty Placeholder Node (`EmptyNode.java` / `QA`)**
  - Buat `EmptyNode` turunan `ExpressionNode` merepresentasikan `QA.java`.
  - Implementasikan rendering kotak outline `PlaceholderVisual.java` (`C0357yG.java`) dengan `paint.measureText("0") * 1.2f` dan style STROKE.
- [x] **Task 2: Integrasi Placeholder ke `FractionNode` & `VisualTreeBuilder`**
  - FractionNode default menggunakan `EmptyNode` jika slot kosong.
  - `VisualTreeBuilder` memetakan `EmptyNode -> PlaceholderVisual`.
- [x] **Task 3: Hit-Testing Horizontal Presisi (`FractionVisual.hitTest`)**
  - Cek span horizontal: jika tap di luar $[ \text{numX}, \text{numX} + \text{numW} ]$ dan $[ \text{denX}, \text{denX} + \text{denW} ]$, kembalikan kursor ke posisi Center di parent `SequenceNode`.
- [x] **Task 4: Keypad & Navigasi Kursor (`HyperCalActivity.java`)** — lihat Task 6–8 untuk perbaikan lanjutan
  - `insertFraction()`: buat `FractionNode(new EmptyNode(), new EmptyNode())` dan arahkan kursor ke `numerator` (`EmptyNode`).
  - `appendDigit(char c)`: jika kursor berada pada `EmptyNode`, ganti `EmptyNode` dengan `NumberNode(c)`.
  - `deleteChar()`: jika di penyebut berisi angka sampai habis, kembalikan ke `EmptyNode`. Jika DEL lagi di `EmptyNode` penyebut, pindahkan kursor ke akhir pembilang. Jika pembilang dan penyebut keduanya `EmptyNode`, hapus pecahan dari Sequence.
  - `moveCursorLeft()` & `moveCursorRight()`: dukung transisi kursor ke posisi Center di Sequence parent sebelum/setelah pecahan.
- [x] **Task 5: Verifikasi di Emulator** — tap kiri/kanan terverifikasi 2026-09-26 (`5 + [tap kiri]8 123/7 [tap kanan]9 + 3`)
  - Tekan tombol `a/b` saat display kosong $\rightarrow$ harus muncul 2 kotak outline (pembilang & penyebut) dengan kursor di kotak pembilang.
  - Ketik `7` $\rightarrow$ kotak pembilang menjadi angka 7.
  - Tekan `a/b` atau panah kanan $\rightarrow$ kursor pindah ke kotak penyebut.
  - Ketuk di sebelah kanan / kiri pecahan $\rightarrow$ kursor pindah ke posisi Center sejajar garis pecahan.

---

## 11. Gap Analysis vs Kode Asli (2026-09-26) & Task List Lanjutan

Hasil perbandingan ulang `Qg.java`, `C0357yG.java`, `QA.java`, `AbstractC0335wD.java` dengan implementasi kita.

### A. Struktur & Logika Input (dikerjakan)
- [x] **Task 6: Slot pembilang/penyebut = `SequenceNode` (GA di dalam `C0067Lb`)**
  - `FractionNode.numerator/denominator` kini `SequenceNode`; slot kosong berisi satu `EmptyNode`.
  - Operator/angka bisa diketik di dalam slot (`1+2` di pembilang). Sebelumnya `appendOperator` jatuh ke `rootSequence`.
  - Helper baru di `HyperCalActivity`: `insertAtCursor`, `fractionOfSlot`, `startOf`/`endOf`/`afterNode`, `removeFromSequence`, `deleteBefore`.
  - Navigasi kiri/kanan kini berbasis batas slot (awal/akhir sequence), bukan identitas node tunggal.
- [x] **Task 7: `insertFraction()` setelah operator = Kasus 2**
  - Hanya operand tepat sebelum kursor (Number/Power/Sqrt/Paren/Fraction dengan posisi > 0) yang diangkat ke pembilang.
  - Setelah operator / di placeholder / display kosong → dua kotak kosong, kursor di pembilang.
- [x] **Task 8: DEL di pembilang kosong saat penyebut terisi**
  - Kotak pembilang tetap tampil, kursor pindah ke Center sebelum pecahan (sebelumnya token terakhir `rootSequence` ikut terhapus).
  - Angka yang habis dihapus di slot langsung kembali ke `EmptyNode`.
  - DEL di Center sebelum pecahan menghapus satu karakter/token sebelumnya (bukan seluruh angka).
- [x] **Task 9: Verifikasi emulator** — `a/b 1 + 2 → 3` → `\frac{1 + 2}{3}`; `5 + a/b 7` → `5 + \frac{7}{□}`; DEL di pembilang kosong → `7 + \frac{□}{3}` tetap; `a/b DEL` → display kosong.

### B. Belum Dikerjakan
- [ ] **Task 10: Mode linear `a/b`** (`Qg.a`, `Qg.java` baris 96–126): render sebaris dengan `'/'`, anak tidak diskala 0.8, baseline `max(numM, -ascent, denM)`.
  - **STATUS: DINONAKTIFKAN / DITUNDA** atas permintaan user (2026-09-26). Belum ada rencana pengerjaan; jangan diimplementasikan sampai ada instruksi eksplisit. Alasan pemicu di HiPER (`Qg.a`, layar sempit atau mode linear aktif dari pengaturan) belum ditentukan skenarionya untuk sample app ini.
- [x] **Task 11: Padding nested fraction `m$3()`**
  - `FractionVisual.calculateLayout`: `m3 = spaceWidth × 1.0f` bila pembilang/penyebut adalah `FractionVisual` lagi, else 0. `numX`/`denX` digeser `+m3`, `b.x = maxW + 2×m3`.
  - Garis pecahan tetap digambar `0..b.x` — karena pembilang/penyebut sudah digeser `+m3`, versi asli `fMin - m$3()` otomatis kembali ke 0 dan `F()` (perkiraan `b.x`) mencakup padding baru, jadi rumusnya sama saja secara efektif.
  - Uji emulator: `a/b 1 ▶ a/b 2 ▶` → `\frac{1}{\frac{2}{□}}`, garis luar tampak melebar mengakomodasi pecahan bertingkat.
- [x] **Task 12: Navigasi atas/bawah** (`Qg.HiPER(PointF, Df)` / `Qg.E(PointF, Df)`): pindah pembilang ↔ penyebut, x di-clamp dengan margin `ZD.ab (0.2) × density`. Perlu tombol ▲/▼.
  - Selesai: baris tombol baru `btn_cursor_up` / `btn_cursor_down` (di atas ◀ / ▶) di `activity_hypercal.xml`; `HyperCalDisplayView.findVerticalCursorTarget()` mencari pecahan terdekat lalu hit-test slot seberang dengan x kursor. Uji: `a/b 123 ▼ 7 ▲ 5` → `\frac{1253}{7}` (5 masuk di posisi x yang sama).
- [x] **Task 13: Geometri placeholder persis `C0357yG`**
  - `PlaceholderVisual`: `m = -ascent + 0.9 × density`, `b.y = descent + m`. Kotak digambar dari `y=0` sampai `y=b.y` (versi kita tidak mengimplementasikan padding vertikal `L` milik `AbstractC0335wD`, jadi `fB` disederhanakan jadi 0 — ini simplifikasi yang disengaja, bukan berdasarkan kode).
  - Rumus lama (`textSize × 0.9`, kotak berpusat di `m ± ascent×0.9`) sudah tidak dipakai.
- [ ] **Task 14: Mode placeholder tersembunyi `C0357yG.E()` / `QA.B()` / `QA.D()`** — **TIDAK DIKERJAKAN, bergantung subsistem yang tidak ada di sample ini**.
  - `C0357yG.E()` (dibaca ulang 2026-09-26): hanya mengecek `qa.D()`/`qa.B()` ketika `AbstractC0033Df.k.mo40HiPER() == EnumC0051Ha.HiPER` — semacam mode aplikasi global (kemungkinan "mode isi argumen fungsi/template", bukan mode kalkulasi biasa). Di luar mode itu, `E()` selalu `true` (kotak tidak pernah disembunyikan).
  - `QA.B()`/`QA.D()` sendiri isinya pengecekan grammar yang rumit: apakah slot ini argumen terakhir dari fungsi variadic (`GA`/`C0067Lb` dengan `enumC0300sa.xa`), apakah aman dihapus tanpa mengubah makna, dll — semua bergantung pada struktur AST fungsi (`GA`, kode error `EnumC0300sa`) yang tidak ada padanannya sama sekali di `engine/model` sample ini (`FractionNode`, `SqrtNode`, dst. jauh lebih sederhana, tidak ada konsep "fungsi dengan argumen variadic/opsional").
  - Mengimplementasikan ini secara jujur berarti membangun ulang subsistem mode + grammar function-argument yang tidak dibutuhkan sample kalkulator ini. Bukan prioritas sampai ada fitur fungsi/argumen yang butuh perilaku ini.
  - Elipsis `"…"` (`qa.b()` / `HiPER()` method) juga bagian dari subsistem yang sama (dipakai saat argumen fungsi disembunyikan sebagai ringkasan) — ikut ditunda.
- [x] **Task 15: Warna garis & kotak memakai warna tema, bukan hardcode**
  - **Temuan:** `AbstractC0335wD.HiPER(Paint, String)` (dipakai `C0357yG` untuk kotak placeholder) mengembalikan paint **apa adanya** kalau key highlight yang dicari tidak ada di map tema — yaitu kondisi normal (tidak sedang di-highlight). Jadi kotak placeholder dan garis pecahan **memakai warna teks biasa**, bukan warna sekunder/transparan terpisah seperti asumsi lama (`#80FFFFFF`).
  - `PlaceholderVisual`: kotak sekarang mewarisi warna dari `basePaint` (sama seperti teks), tidak ada `setColor` terpisah.
  - `FractionVisual`: garis pecahan sudah mewarisi warna sejak awal (tidak berubah).
  - `HyperCalDisplayView`: warna teks & kursor dipindah ke `res/values/colors.xml` (`hypercal_text`, `hypercal_cursor`) supaya ikut tema aplikasi, bukan literal hex di kode. Kursor diselaraskan ke warna aksen aplikasi (`#FF9800`, sama dengan kursor `CalctasticCalculatorActivity`) menggantikan biru yang tidak berhubungan.
  - Uji emulator: kursor tampil oranye (menyatu dengan tombol `=`), kotak placeholder putih solid seperti teks.
- [x] **Task 16: Hapus clamp `Math.max(1.5f, …)`** pada gap, tebal garis (`FractionVisual`), dan stroke placeholder (`PlaceholderVisual`). Clamp lebar kursor `Math.max(3.0f/2.0f, …)` di `MathVisual.getCursorWidth` dan `HyperCalDisplayView` juga dihapus (menyatu dengan Display Task 9).
- [x] **Task 17: Geometri kursor `mo359/mo360`**
  - `MathVisual.getCursorRect` (default) sekarang persis rumus dasar `AbstractC0335wD`: tinggi penuh `0..b.y`, lebar berpusat di `getCursorPosition(index).x`.
  - `MathVisual.getCursorPosition` default: index 0 → `-0.5w`, selain itu → `b.x + 0.5w`. Dipakai oleh `FractionVisual` (Center sebelum/sesudah) dan `PlaceholderVisual` (kotak kosong).
  - **Temuan:** `C0357yG` (placeholder) tidak override `mo359HiPER`, jadi kursor di kotak kosong sebenarnya nongkrong di kiri kotak (`-w..0`), bukan di tengah seperti sebelumnya. Sudah dicek visual di emulator (`a/b` pada display kosong) — kursor persis di tepi kiri kotak pembilang.
  - `NumberVisual` posisi kursor antar-digit tetap dipertahankan seperti semula (bukan hasil decompile pasti — lihat catatan di bawah).
- [ ] **Task 18: Verifikasi hit-test `Qg.HiPER(PointF,bool,bool)` via smali** — **TIDAK BISA DIKERJAKAN, diblokir tooling**.
  - JADX gagal decompile method ini ke Java; fragmen yang tersisa menunjukkan perbandingan ke titik tengah, bukan batas min/max anak seperti asumsi implementasi kita.
  - Untuk memastikan perlu baca bytecode smali langsung, tapi tidak ada file `.smali` maupun tool `apktool`/`baksmali` di lingkungan ini (hanya source hasil JADX di `temp/`). Perlu di-generate ulang dari APK asli (`raw/` atau `.apk`) di luar sesi ini kalau mau dituntaskan.
- [x] **Task 20: a/b tepat setelah pecahan — DIBALIK ke nesting (2026-09-26), lihat Task 20b**
  - Riwayat: pertama kali "sibling" (`hasOperandBeforeCursor` mengecualikan `FractionNode`) untuk memperbaiki komplain awal ("kursor keluar pecahan, a/b malah mengisi penyebut otomatis").
  - Efek samping sibling: dua pecahan bersebelahan berdempetan tanpa spasi (kode asli `C0329vH.mo63HiPER()` baris ~176: advance horizontal = `x += lebar elemen` saja, tidak ada mekanisme gap antar elemen sequence — `mo358HiPER()` yang tampak seperti margin ternyata dipakai untuk keputusan word-wrap, bukan spacing visual, dan `Qg`/Fraction tidak override itu).
  - Setelah didiskusikan ulang (lihat Task 20b): teks spec Bagian 2 Kasus 1 ("angka/**token** sebelum kursor diangkat jadi pembilang") tidak mengecualikan pecahan → nesting lebih konsisten dengan aturan umum itu, dan otomatis menghilangkan masalah spasi (tidak ada lagi dua elemen bersebelahan tanpa operator).
- [x] **Task 20b: a/b setelah pecahan → nesting (pecahan lama jadi pembilang pecahan baru)**
  - Keputusan user (2026-09-26): `hasOperandBeforeCursor()` TIDAK mengecualikan `FractionNode` lagi — pecahan yang sudah selesai diperlakukan sama seperti token lain (Number/Sqrt/Power/Parenthesis).
  - Hasil: `a/b 1 ▶ 2 ▶ a/b 7` → `\frac{\frac{1}{2}}{7}` (bukan lagi `\frac{1}{2}\frac{7}{□}`). Kursor setelah a/b langsung di penyebut pecahan baru.
  - Diuji ulang: unwrap DEL (Task 19), navigasi ▲/▼ (Task 12), dan kasus dasar `1/2` semua masih benar setelah perubahan ini.
  - Catatan: ini kebalikan dari komplain awal pengguna ("a/b otomatis mengisi penyebut" dianggap bug) — setelah dipertimbangkan ulang, itu ternyata perilaku yang benar secara teori, bukan bug. Referensi `C0196hc.java` baris 347–363 tetap belum konklusif (kode transformasi evaluasi, bukan aksi tombol), jadi ini keputusan desain berdasarkan konsistensi aturan spec, bukan bukti decompile langsung.
- [x] **Task 19: Unwrapping pecahan** saat DEL di Center setelah pecahan.
  - Penyebut kosong → pecahan diganti isi pembilang, kursor di akhir isi tsb (pembilang juga kosong → pecahan dihapus). Penyebut terisi → kursor masuk ke akhir penyebut (seperti sebelumnya).
  - Uji: `5 a/b ▶ DEL 3` → `53`; `2 + a/b 1 ▶ ▶ DEL` → `2 + 1`.

### C. Urutan Prioritas Pengerjaan
1. ~~**Perilaku (langsung terasa pengguna):** Task 20 → Task 19 → Task 12 → verifikasi tap kiri/kanan pecahan (sisa Task 5).~~ ✅ selesai 2026-09-26
2. ~~**Skala display:** `display_scaling_typography.md` Task 7.~~ ✅ selesai 2026-09-26
3. ~~**Akurasi render:** Task 13 → Task 17 → Task 11 → Task 16 (+ display Task 9).~~ ✅ selesai 2026-09-26
4. ~~**Warna tema:** Task 15 (+ display Task 8).~~ ✅ selesai 2026-09-26
5. **Fitur tambahan:** Task 10 (dinonaktifkan/ditunda atas permintaan user), Task 14 (ditunda — bergantung subsistem yang tidak ada), Task 18 (diblokir tooling). Display Task 10 (scroll+clip) ✅ selesai 2026-09-26, Display Task 12 (diblokir tooling).

### D. Sisa Pekerjaan
Semua task yang bisa dikerjakan di lingkungan ini sudah selesai. Yang tersisa:
- **Task 10 (linear mode):** dinonaktifkan/ditunda atas permintaan user — jangan kerjakan tanpa instruksi eksplisit.
- **Task 14 (placeholder tersembunyi):** ditunda — butuh subsistem mode + grammar fungsi/argumen yang tidak ada di sample ini.
- **Task 18 (verifikasi hit-test via smali) & Display Task 12 (string `HcZgWQ.LiVE`):** diblokir kurangnya tooling smali (`apktool`/`baksmali`) di lingkungan ini, bukan soal effort.

### E. Pemindahan Lokasi Kode (2026-09-26): `HyperCalActivity.java` dipecah
`HyperCalActivity.java` sebelumnya 599 baris, mencampur wiring keypad Android dengan logika edit ekspresi murni. Semua logika insert/delete/navigasi kiri-kanan pada bagian ini (Task 6–9, 19, 20/20b) **dipindah** ke class baru:

- **`engine/ExpressionEditor.java`** (baru) — isinya: `appendDigit`, `toggleNegate`, `appendOperator`, `insertSqrt`, `insertFraction` (+ `hasOperandBeforeCursor`), `insertPower`, `insertSquare`, `insertReciprocal`, `insertParenthesis`, `deleteChar` (+ `deleteBefore`, `removeFromSequence`, `unwrapFraction`), `moveCursorLeft`, `moveCursorRight`, dan helper slot pecahan (`fractionOfSlot`, `afterNode`, `startOf`, `endOf`, `insertAtCursor`). Class ini tidak bergantung pada Android (tidak ada `View`/`Paint`), jadi kalau task selanjutnya butuh pengujian unit murni atas logika pecahan, ini tempatnya.
- **`HyperCalActivity.java`** (tersisa 163 baris) — sekarang cuma wiring keypad (`setupKeypad`, `onClick` mendelegasikan ke `editor.xxx()`) dan `moveCursorVertical` (Task 12), yang **tetap di Activity** karena butuh `HyperCalDisplayView.findVerticalCursorTarget` (perlu pohon visual + `Paint`, tidak bisa pindah ke `ExpressionEditor` yang bebas-Android).

**Kalau mencari kode terkait task di atas sekarang:** logika insert/delete/navigasi kiri-kanan pecahan ada di `engine/ExpressionEditor.java`, bukan `HyperCalActivity.java` lagi. Navigasi atas/bawah (Task 12) tetap di `HyperCalActivity.moveCursorVertical` + `HyperCalDisplayView.findVerticalCursorTarget`.

### F. Tombol "a b/c" (Mixed Number, 2026-09-26)

Permintaan user: tambahkan tombol pecahan campuran (bilangan bulat + pecahan, mis. `1 2/3`), yang belum ada di implementasi kita sebelumnya.

**Temuan decompile** (via subagent riset, `android.core`):
- Bukan class terpisah. HiPER memakai node generik `C0067Lb` yang sama dengan pecahan biasa, hanya dengan **3 anak** (bukan 2): `L(0)`=bagian bulat, `L(1)`=pembilang, `L(2)`=penyebut (`Rc.java:384-392`). Jenis node dibedakan lewat enum `EnumC0300sa.sa` (mixed) vs `EnumC0300sa.mB` (pecahan biasa).
- Tombol didaftarkan di `EnumC0209ia.java:485-487`, persis setelah tombol a/b — string label/drawable tidak bisa dipulihkan (obfuscated), tidak ada resource `res/*.xml` yang menyebut "mixed"/"ab_c" (satu-satunya hit publik, `rfMixedRB`, adalah radio button *format hasil* mixed vs improper, bukan tombol input).
- Serialisasi non-CAS: bagian bulat + pecahan digabung tanpa operator, dipisah karakter spasi tipis (`Rc.java:384-392`) — mengonfirmasi ini bukan `int + a/b` melainkan satu token gabungan.
- Rendering/navigasi kursor: `GA.java:93`, `C0067Lb.java:1004/1011/1118`, `AbstractC0033Df.java:721` memperlakukan `sa` dan `mB` sama di hampir semua predikat "ini pecahan?" — jadi mixed number pakai ulang renderer pecahan biasa, dengan bagian bulat digambar tambahan. `C0197hd.java:734` (`iL==3 && sa`) adalah cabang edit/DEL khusus untuk node 3-anak ini.

**Implementasi kita:**
- `engine/model/FractionNode.java`: tambah field `integerPart` (nullable `SequenceNode`), factory `FractionNode.createMixed(integerPart, num, den)`, `isMixed()`. Pecahan biasa tidak berubah (`integerPart == null`).
- `engine/ExpressionEditor.insertMixedFraction()` (tombol baru "a b/c"): siklus mengikuti pola a/b yang sudah ada —
  - Kursor di bagian bulat → lompat ke pembilang.
  - Kursor di pembilang → lompat ke penyebut (sama seperti a/b biasa).
  - Ada operand sebelum kursor → operand itu diangkat jadi **bagian bulat** (bukan pembilang seperti a/b biasa), pembilang/penyebut kosong, kursor ke pembilang.
  - Tidak ada apa-apa → bikin mixed number kosong semua, kursor di bagian bulat.
  - Konsisten dengan keputusan Task 20b: pecahan/mixed number yang sudah selesai sebagai "operand sebelum kursor" ikut ter-nesting (bukan dikecualikan), jadi `1/2` lalu `a b/c` menghasilkan `\frac{1}{2\ \frac{□}{□}}` — sudah diuji di emulator.
  - DEL (`deleteChar`) dan unwrap (`unwrapFraction`) diperluas untuk 3 slot: DEL di kotak pembilang kosong (mixed) lompat ke akhir bagian bulat; DEL di kotak bagian bulat kosong berperilaku seperti DEL sebelum pecahan; unwrap menyertakan token bagian bulat + pembilang saat penyebut kosong.
  - Navigasi kiri/kanan (`moveCursorLeft/Right`) diperluas: batas kiri pembilang ↔ akhir bagian bulat, batas kanan bagian bulat ↔ awal pembilang.
- `render/FractionVisual.java`: field `integerVisual`, digambar di kiri stack pembilang/penyebut pada ukuran penuh (tidak diskalakan 0.8× seperti pembilang/penyebut), dengan gap kecil (`spaceWidth * 0.5f`) dan diposisikan center vertikal terhadap tinggi total. Garis pembagi (`barLeft`) digeser ke kanan supaya tidak menembus bagian bulat. Hit-test menambah region bagian bulat di sisi kiri.
- `render/VisualTreeBuilder.java`: membangun `integerVisual` dari `frac.integerPart` bila ada.
- UI: tombol baru `btn_mixed_fraction` ("a b/c") di `activity_hypercal.xml`, di sebelah `a/b`; tombol `%` dipindah ke baris navigasi ▲/▼ (kolom 2) untuk memberi ruang.
- **Catatan jujur:** posisi vertikal/horizontal persis bagian bulat (skala, gap, baseline) adalah rekonstruksi wajar dari deskripsi "pakai ulang renderer pecahan + bagian bulat digambar tambahan di kiri" — koordinat piksel pastinya (`Qg.java` versi mixed) tidak ada di laporan riset (rendering method untuk cabang `sa` tidak eksplisit ditelusuri baris-per-baris seperti pecahan biasa). Kalau nanti ada bukti lebih pasti (mis. smali), sesuaikan ulang.
- Diuji di emulator: lift operand ke bagian bulat, isi 3 slot via siklus tombol, render visual (screenshot), DEL berantai sampai unwrap penuh (denominator → numerator → integer part → hilang), nesting a/b → a b/c, dan regresi alur pecahan biasa (tidak berubah).
