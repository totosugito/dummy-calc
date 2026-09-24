# CalcTastic Sample App - Rekayasa Balik & Rekonstruksi Fitur

Dokumen ini mencatat pencapaian, komponen yang diimplementasikan, serta solusi teknis yang telah diterapkan dalam merekonstruksi fungsionalitas dan antarmuka aplikasi **CalcTastic**.

---

## 1. Fitur-Fitur Utama yang Telah Diimplementasikan

### A. Tampilan Layar Kalkulator (Screen & History List)
- **Struktur ListView Bawah-ke-Atas**: Menggunakan `ListView` dengan `stackFromBottom="true"` dan `transcriptMode="alwaysScroll"`, sesuai tata letak asli CalcTastic.
- **Dua Jenis Baris Data**:
  - **Baris Riwayat (History Line)**: Menampilkan simbol operasi (`=`), ekspresi hitungan sebelumnya (`history_calculation`), dan hasil perhitungan (`history_result`).
  - **Baris Aktif (Active Input Line)**: Menampilkan divider bar `▼ ON SCREEN ▼`, input kustom yang dapat diedit (`VerticalListEditText`), kontrol navigasi baris (chevron ▲/▼), dan live preview hasil perhitungan.
- **Batasan Tampilan Baris & Truncation**:
  - **Input Persamaan (Maksimal 3 Baris)**: Otomatis membungkus teks hingga 3 baris. Jika rumus melebihi 3 baris, tombol chevron navigasi baris (`▲` dan `▼`) otomatis muncul di sisi kanan baris input untuk berpindah baris.
  - **Hasil Perhitungan (Maksimal 2 Baris)**: Ditampilkan maksimal 2 baris (`maxLines="2"`), dan jika hasil sangat panjang akan di-truncate dengan elipsis (`...`) di akhir baris kedua (`ellipsize="end"`).
- **Tipografi & Font Asli**:
  - `font_inter_medium.ttf`: Digunakan untuk simbol, label, divider bar, dan tombol fungsi.
  - `font_roboto_mono_variable.ttf`: Digunakan untuk digit angka persamaan dan hasil hitungan.

### B. Input Interaktif & Manajemen Kursor Kustom
- **Kursor Oranye Berkedip Mandiri**:
  - Menggunakan kursor kustom oranye (`#FF9800`) setebal 2dp.
  - Tidak memicu toolbar selection/floating bawaan Android ("Cut/Copy/Paste/Select All").
- **Navigasi Kursor**:
  - Tombol panah kiri (`◀`) dan kanan (`▶`) pada keypad untuk memindahkan posisi kursor di antara karakter.
  - Penempatan kursor langsung saat pengguna mengetuk (tap) pada posisi teks tertentu, dipetakan secara akurat menggunakan perhitungan lebar karakter (`printedSizes`).
- **Pencegahan Android Soft Keyboard (IME Pop-up)**:
  - Input dapat disentuh dan kursor dapat dipindahkan tanpa memicu munculnya keyboard sistem Android (Gboard).
  - Mengombinasikan `setShowSoftInputOnFocus(false)`, `InputMethodManager.hideSoftInputFromWindow`, serta atribut `android:focusable="false"` dan `android:descendantFocusability="afterDescendants"` pada ListView.

### C. Pemisah Ribuan Dinamis (Dynamic Digit Grouping)
- Mengimplementasikan algoritma pemisah ribuan dinamis berbasis `y0.a.f`:
  - Format angka ribuan diperbarui secara real-time saat pengguna mengetik (misal `144000` menjadi `144,000`).
  - Menjaga konsistensi indeks kursor logika (raw cursor) vs indeks visual berformat (formatted cursor).

### D. Tata Letak Keypad Portret 7 Baris (Portrait Simple Layout)
Keypad disusun persis mengikuti konfigurasi layout `keyboard_portrait_simple.xml` asli CalcTastic:
- **Baris 1 (Memori)**: `M+`, `M−`, `MC`, `MS`, `MR`
- **Baris 2 (Fungsi Pecahan & Kurung)**: `a/b`, `√x`, `%`, `(`, `)`
- **Baris 3 (Aljabar & Navigasi)**: `1/x`, `x²`, `yˣ`, `◀`, `▶`
- **Baris 4 (Angka & Hapus)**: `7`, `8`, `9`, `DEL`, `CLR`
- **Baris 5 (Angka & Operator Dasar)**: `4`, `5`, `6`, `×`, `÷`
- **Baris 6 (Angka & Operator Dasar)**: `1`, `2`, `3`, `+`, `−`
- **Baris 7 (Desimal & Eksekusi)**: `0`, `.`, `±`, `=`

### E. Penulisan dan Tampilan Pecahan Asli (Fraction `a/b`)
- **Rendering Tag Asli CalcTastic**:
  - Pembilang di atas menggunakan `<sup>...</sup>` (`SuperscriptSpan` + `RelativeSizeSpan(0.75f)`).
  - Tanda garis pecahan `/`.
  - Penyebut di bawah menggunakan `<sf>...</sf>` (`RelativeSizeSpan(0.75f)`).
  - Jarak spasi rapat untuk pecahan campuran menggunakan `<hw> </hw>` (`ScaleXSpan(0.5f)`).
  - Tampilan teks samar/placeholder menggunakan `<dim>...</dim>`.
- **Dukungan Format**:
  - **Pecahan Biasa**: Misal $7/5$ ditampilkan dalam bentuk superscript pembilang dan subscript penyebut.
  - **Pecahan Campuran**: Misal $1\ 2/3$ ditampilkan rapi dengan angka bulat di depan, spasi rapat, dan pecahan di belakangnya.
- **Evaluasi Matematis**: Parser perhitungan dapat mengevaluasi nilai pecahan dan pecahan campuran secara tepat ke dalam operasi perhitungan.

---

## 2. Struktur File Inti

| File | Deskripsi |
| :--- | :--- |
| `SimpleCalculatorActivity.java` | Controller UI: lifecycle, wiring tombol simple/scientific, insert/backspace, adapter riwayat. |
| `ExpressionEvaluator.java` | Mesin evaluasi ekspresi (fungsi trig/ln/log, sudut DEG/RAD/GRD, HYP, `!` `%` `mod` `nPr`…). |
| `ExpressionDecorator.java` | Superskrip sudut `d`/`r`/`g`/`h` pada nama fungsi di tampilan ekspresi (setara `CalculatorCommand.C`). |
| `CalculatorMemory.java` | Reg memori 0: save / recall / M+ / M− / MC (setara `CalcMemory` + `Calculator` case 44–49). |
| `CalcTokens.java` | Tabel token: auto-paren fungsi (ordinal 68–86), operator multi-char, postfix. |
| `NumberFormatHelper.java` | Engine pemformat digit ribuan, mapping indeks kursor layar, serta penyusunan tag pecahan (`sup`, `sf`, `hw`, `dim`). |
| `CalcSpannableFormatter.java` | Parser markup XML mini ke Android `SpannableStringBuilder` untuk pewarnaan operator, kurung, dan styling pecahan. |
| `CalcTypefaceHelper.java` | Pengelola font kustom (Inter Medium, Roboto Mono). |
| `activity_simple_calculator.xml` | Layout utama berisi area layar kalkulator (ListView) dan 7 baris tombol keypad. |
| `history_dialog_list_item.xml` | Layout baris ListView yang menampung riwayat perhitungan maupun baris kalkulasi aktif. |

---

## 3. Kompilasi & Pengujian

- **Gradle Version**: Menggunakan Gradle 8.14 (kompatibel dengan AGP yang digunakan, hindari Gradle 9).
- **Target Emulator**: Diuji secara langsung pada emulator Android (`emulator-5554`) memastikan respons tombol, tampilan font, dan ketiadaan regresi keyboard sistem.
