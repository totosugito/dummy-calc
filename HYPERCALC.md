# Dokumentasi Implementasi Display & Pecahan (Mode Expression HiPER)

Dokumen ini mencatat temuan teknis dari kode sumber asli HiPER Calculator (`temp/app/src/main/java/android/core/`) serta implementasi tampilan ekspresi matematika 2D (khususnya rendering pecahan dan kotak slot placeholder) ke dalam proyek kalkulator ini.

---

## 1. Arsitektur Komponen Display HiPER

Pada kode sumber asli HiPER Calculator, rendering ekspresi 2D ditangani oleh sistem pohon token (AST) dan kelas-kelas visualizer turunan dari `AbstractC0335wD`:

1. **`android.core.QA.java`**
   - Merepresentasikan token slot kosong (placeholder / *Empty Token*).
   - Menentukan apakah slot harus digambar sebagai kotak kosong atau tersembunyi berdasarkan konteks hirarki token (`B()`, `D()`).

2. **`android.core.C0357yG.java`**
   - Visualizer untuk token slot kosong (`QA`).
   - Bertanggung jawab menghitung dimensi bounding box kotak placeholder dan menggambarnya ke `Canvas`.

3. **`android.core.Qg.java`**
   - Visualizer untuk struktur pecahan (*Fraction* / `EnumC0300sa.mB`).
   - Mengatur pembagian skala font pembilang dan penyebut (skala `0.8x`).
   - Mengatur perataan vertikal (*numerator*, garis pecahan *fraction bar*, dan *denominator*).
   - Menghitung koordinat sentuhan (hit test) untuk seleksi tap pada pembilang atau penyebut.

4. **`android.core.C0281qH.java` & `BE.java`**
   - View kanvas utama untuk menggambar seluruh ekspresi formula.
   - Mengatur kursor teks berkedip (garis vertikal lurus) dan posisi kursor di dalam token/slot pecahan.

---

## 2. Cara Kerja Rendering Pecahan & Kotak Slot (Placeholder Box)

### A. Dimensi Kotak Slot Kosong (Berdasarkan `C0357yG.java`)
Saat tombol pecahan (`a/b`) ditekan tanpa angka awal, HiPER memunculkan kotak slot kosong di atas (pembilang) dan kotak slot kosong di bawah (penyebut). Rumus ukurannya diambil langsung dari kode asli:

- **Lebar Kotak (`pointF.x`)**:
  ```java
  // C0357yG.java baris 72:
  float digitZeroW = paint.measureText("0");
  float emptyBoxW = digitZeroW * 1.2f;
  ```
- **Tinggi Kotak (`pointF.y`)**:
  ```java
  // C0357yG.java baris 42–46 & AbstractC0335wD.java baris 328:
  float fG = -paint.ascent() + (0.9f * density); // Tinggi ascent font
  float fL1 = paint.descent();                    // Descent font
  float emptyBoxH = fG + fL1;
  ```
- **Ketebalan Garis Stroke Kotak (`strokeWidth`)**:
  ```java
  // C0357yG.java baris 210:
  float strokeWidth = digitZeroW * 0.1f;
  paint.setStyle(Paint.Style.STROKE);
  ```
- **Penggambaran Kotak (`drawRect`)**:
  ```java
  // C0357yG.java baris 218:
  canvas.drawRect(startX, startY, startX + emptyBoxW, startY + emptyBoxH, placeholderPaint);
  ```

### B. Garis Pembagi Pecahan (Fraction Bar) (Berdasarkan `Qg.java`)
- **Skala Font**: Pembilang dan penyebut menggunakan ukuran font yang diperkecil:
  ```java
  float subSize = textSize * 0.8f;
  ```
- **Ketebalan Garis Pembagi Pecahan**:
  ```java
  // Qg.java baris 121 & AbstractC0335wD.HiPER(paint, 0.3f):
  float spaceW = paint.measureText(" ");
  float barThickness = Math.max(2.2f * density, spaceW * 0.45f);
  ```
- **Jarak Vertikal Kotak dari Garis Pecahan**:
  ```java
  float gapY = emptyBoxH * 0.75f;
  float numY = lineY - gapY; // Posisi vertikal pembilang (atas)
  float denY = lineY + gapY; // Posisi vertikal penyebut (bawah)
  ```

---

## 3. Sistem Kursor & Interaksi

1. **Bentuk Kursor**:
   - Berupa garis vertikal lurus (`canvas.drawLine`) yang berkedip setiap interval 500ms.
   - Tidak menggunakan bulatan/pin seleksi di bawahnya.
2. **Fokus & Pengetikan di Dalam Kotak**:
   - Jika slot pembilang kosong, kotak atas aktif dan kursor berada di tengah kotak pembilang.
   - Angka yang diketikkan pengguna langsung masuk ke pembilang, menggantikan kotak placeholder dengan teks angka riil.
   - Tombol kursor kanan (`▶`) atau tap pada kotak bawah memindahkan fokus ke penyebut.
   - Angka berikutnya masuk ke penyebut dan menggantikan kotak placeholder bawah.
3. **Pewarnaan State Kotak**:
   - Kotak aktif (sedang difokuskan untuk diisi): `#93B4FF` (biru terang HiPER).
   - Kotak inaktif (belum diisi, tapi tidak aktif): `#446088` (biru abu-abu gelap HiPER).

---

## 4. File Implementasi di Proyek Ini (Setelah Refactoring Modular)

Struktur kode kini telah direfaktor menjadi **multi-file modular** mengikuti pembagian peran kode asli HiPER:

1. **Kanvas & Gesture Viewport**:
   - [`HiPerExpressionCanvasView.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/view/HiPerExpressionCanvasView.java): Hanya fokus pada viewport, kedip kursor vertikal lurus, scroll offset, dan mendelegasikan rendering ke registry.
   - [`HiPerDisplayContainerView.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/view/HiPerDisplayContainerView.java): Kontainer display dengan status bar atas (DEG/RAD).

2. **Sistem Visualizer / Token Renderers (`display/renderer/`)**:
   - [`MathTokenRenderer.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/renderer/MathTokenRenderer.java): Interface dasar untuk setiap renderer komponen (setara `AbstractC0335wD.java`).
   - [`FractionRenderer.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/renderer/FractionRenderer.java): Visualizer khusus struktur pecahan pembilang, fraction bar, penyebut (setara `Qg.java`).
   - [`PlaceholderBoxRenderer.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/renderer/PlaceholderBoxRenderer.java): Visualizer khusus kotak slot kosong (setara `C0357yG.java`).
   - [`SqrtRenderer.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/renderer/SqrtRenderer.java): Visualizer khusus tanda akar (setara `C0119aE.java`).
   - [`PowerRenderer.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/renderer/PowerRenderer.java): Visualizer khusus pangkat/eksponen (setara `C0349xH.java`).
   - [`TextTokenRenderer.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/renderer/TextTokenRenderer.java): Visualizer teks angka, operator, dan kurung (setara `C0329vH.java`).
   - [`TokenRendererRegistry.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/renderer/TokenRendererRegistry.java): Registri penghubung tipe token ke visualizernya (setara `PH.java`).
   - [`RenderContext.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/renderer/RenderContext.java): Data context yang dibagikan antar visualizer saat merender pohon token (setara `C0150dI.java`).

3. **Tema & Integrasi**:
   - [`HiPerThemeColors.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/display/view/HiPerThemeColors.java): Definisi palet warna tema HiPER.
   - [`HyperCalActivity.java`](file:///home/toto/Documents/dummy-calc/app/src/main/java/com/calctastic/sample/hypercal/HyperCalActivity.java): Activity penghubung keypad Calctastic dan display HiPER.
