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
- [ ] **Task 4: Keypad & Navigasi Kursor (`HyperCalActivity.java`)**
  - `insertFraction()`: buat `FractionNode(new EmptyNode(), new EmptyNode())` dan arahkan kursor ke `numerator` (`EmptyNode`).
  - `appendDigit(char c)`: jika kursor berada pada `EmptyNode`, ganti `EmptyNode` dengan `NumberNode(c)`.
  - `deleteChar()`: jika di penyebut berisi angka sampai habis, kembalikan ke `EmptyNode`. Jika DEL lagi di `EmptyNode` penyebut, pindahkan kursor ke akhir pembilang. Jika pembilang dan penyebut keduanya `EmptyNode`, hapus pecahan dari Sequence.
  - `moveCursorLeft()` & `moveCursorRight()`: dukung transisi kursor ke posisi Center di Sequence parent sebelum/setelah pecahan.
- [ ] **Task 5: Verifikasi di Emulator**
  - Tekan tombol `a/b` saat display kosong $\rightarrow$ harus muncul 2 kotak outline (pembilang & penyebut) dengan kursor di kotak pembilang.
  - Ketik `7` $\rightarrow$ kotak pembilang menjadi angka 7.
  - Tekan `a/b` atau panah kanan $\rightarrow$ kursor pindah ke kotak penyebut.
  - Ketuk di sebelah kanan / kiri pecahan $\rightarrow$ kursor pindah ke posisi Center sejajar garis pecahan.
