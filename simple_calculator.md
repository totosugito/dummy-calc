# Arsitektur & Spesifikasi Detail: Simple Calculator (CalcTastic Basic Layout)

Dokumen ini mendokumentasikan secara rinci rancangan antarmuka, arsitektur teknis, perilaku input, navigasi kursor, pemformatan dinamis, serta evaluasi aljabar pada implementasi kalkulator dasar (**Simple Calculator**) yang direkonstruksi dari kode sumber asli CalcTastic.

---

## 1. Ikhtisar Antarmuka (UI Architecture)

Layout utama diimplementasikan pada [`activity_simple_calculator.xml`](file:///home/toto/Downloads/decompile/calctastic/sample_app/app/src/main/res/layout/activity_simple_calculator.xml) dengan pembagian dua area utama:
1. **Area Layar Kalkulator (Screen List Area - Atas)**:
   - Menggunakan `ListView` terbalik (`stackFromBottom="true"`, `transcriptMode="alwaysScroll"`).
   - Memastikan baris perhitungan baru selalu muncul di bagian bawah layar tepat di atas keypad, sedangkan riwayat lama terdorong ke atas.
2. **Divider Bar**:
   - Garis horizontal pemisah setebal `1dp` berwarna `#2E2E2E`.
3. **Area Keypad (7-Row Keypad Area - Bawah)**:
   - Menggunakan `TableLayout` berbobot `layout_weight="2.0"` dengan konfigurasi **7 Baris × 5 Kolom** yang mengikuti spesifikasi `keyboard_portrait_simple.xml`.

---

## 2. Struktur Layar & Dua Jenis Baris (History vs Active Line)

Setiap baris di dalam `ListView` menggunakan layout [`history_dialog_list_item.xml`](file:///home/toto/Downloads/decompile/calctastic/sample_app/app/src/main/res/layout/history_dialog_list_item.xml) dengan adaptasi dua mode tampilan:

```
┌────────────────────────────────────────────────────────┐
│ =   128,456 × 4                                        │ <── Baris Riwayat 1
│                             513,824                    │
├────────────────────────────────────────────────────────┤
│ =   √(144,000) + 2,500.75                              │ <── Baris Riwayat 2
│                           2,879.41                     │
├────────────────────────────────────────────────────────┤
│                     ▼ ON SCREEN ▼                      │ <── Divider Baris Aktif
│ 12,345,678|                                            │ <── Baris Input Aktif (Kursor Oranye)
│                         = 12,345,678                   │ <── Live Preview Hasil
└────────────────────────────────────────────────────────┘
```

### Komponen Tampilan Baris:
- **`history_symbol`**: Menampilkan simbol `=` untuk menandai baris riwayat yang telah selesai dihitung.
- **`history_divider`**: Label teks `▼ ON SCREEN ▼` menggunakan font `font_inter_medium.ttf` dengan warna `#B0BEC5` untuk memisahkan riwayat lampau dengan kalkulasi yang sedang diketik.
- **`history_calculation`**: `TextView` untuk ekspresi riwayat yang telah selesai.
- **`current_calculation`**: Komponen kustom [`VerticalListEditText.java`](file:///home/toto/Downloads/decompile/calctastic/sample_app/app/src/main/java/com/calctastic/sample/VerticalListEditText.java) yang mendukung interaksi sentuh mandiri, navigasi kursor, dan pewarnaan markup. Dibatasi **maksimal 3 baris** (`maxLines="3"`).
- **`chevron_container` (`btn_chevron_up` & `btn_chevron_down`)**: Kontrol navigasi vertikal antar-baris (▲ / ▼) yang terletak di sisi kanan input. Hanya muncul otomatis ketika teks melebihi 3 baris. Memungkinkan pengguna berpindah baris secara instan dan memperbarui posisi kursor.
- **`history_result`**: `TextView` hasil kalkulasi. Dibatasi **maksimal 2 baris** (`maxLines="2"`) dengan pemotongan otomatis tanda elipsis (`ellipsize="end"`) jika digit hasil melebihi 2 baris.

---

## 3. Manajemen Kursor & Pencegahan Keyboard Sistem (Anti-IME)

### A. Kursor Oranye Mandiri
- Kursor dirancang kustom setebal `2dp` dengan warna oranye `#FF9800` (`@drawable/cursor_drawable.xml`).
- Berkedip secara periodik (`500ms`) tanpa memicu context menu Android (seperti tombol floating *Cut / Copy / Paste / Select All* dinonaktifkan).

### B. Isolasi Soft Keyboard (Gboard / IME Lock)
Masalah umum pada Android ketika `EditText` disentuh adalah munculnya keyboard virtual sistem. Pada kalkulator ini diterapkan isolasi 3 lapis:
1. **Atribut XML ListView**:
   ```xml
   android:focusable="false"
   android:descendantFocusability="afterDescendants"
   ```
2. **Penonaktifan Input Method**:
   ```java
   currentCalc.setShowSoftInputOnFocus(false);
   ```
3. **Peredaman Event Sentuh (Touch Interception)**:
   Pada `onTouchListener`, fokus diatur secara internal dan `InputMethodManager.hideSoftInputFromWindow` dipanggil secara instan untuk menjamin keyboard Android tidak pernah muncul ke layar.

---

## 4. Pemformatan Angka Dinamis & Pemetaan Kursor (Number Formatting & Cursor Mapping)

Dikelola secara terpusat oleh [`NumberFormatHelper.java`](file:///home/toto/Downloads/decompile/calctastic/sample_app/app/src/main/java/com/calctastic/sample/NumberFormatHelper.java):

### A. Algoritma Digit Grouping (`y0.a.f`)
- Pemisah ribuan (koma `,`) disisipkan setiap kelipatan 3 digit dari kanan sebelum tanda desimal atau eksponen.
- Pengguna hanya mengetik digit murni (`12345678`), tampilan otomatis memformat menjadi `12,345,678`.

### B. Algoritma `printedSizes` Presisi Asli (`q0.g`)
Agar kursor tidak dapat diletakkan di antara tanda koma dan angka serta tombol `DEL` menghapus digit yang benar:
```java
int searchOffset = 0;
for (int pos = 0; pos < numToken.length(); pos++) {
    char rawDigit = numToken.charAt(pos);
    String rawCharStr = (rawDigit == '.' && !decimalSeparator.equals(".")) 
            ? decimalSeparator : String.valueOf(rawDigit);

    boolean isLast = (pos == numToken.length() - 1);
    int iIndexOf = formattedToken.indexOf(rawCharStr, searchOffset);
    int length = iIndexOf < 0 ? 0 : (isLast ? formattedToken.length() : iIndexOf + 1) - searchOffset;

    printedSizes.add(length);
    searchOffset += length;
}
```
- Setiap koma ribuan otomatis menjadi satu kesatuan (berukuran 2) dengan digit setelahnya (misal `,3` dan `,6`).
- Kursor melompati tanda koma secara mulus saat digeser dengan tombol panah maupun saat disentuh.

---

## 5. Format Penulisan Pecahan Asli (Fraction `a/b`)

Mengikuti format tampilan pecahan khas CalcTastic (`com/calctastic/calculator/numbers/Fraction.java`):

1. **Pecahan Biasa ($^7/_5$)**:
   - Tag Markup: `<sup>7</sup>/<sf>5</sf>`
   - `<sup>`: Mengangkat teks pembilang ke atas dan memperkecil ukuran (`0.75x`).
   - `/`: Garis miring pecahan asli.
   - `<sf>`: Memperkecil ukuran penyebut (`0.75x`).
2. **Pecahan Campuran ($1\ ^2/_3$)**:
   - Tag Markup: `1<hw> </hw><sup>2</sup>/<sf>3</sf>`
   - `<hw> </hw>`: Spasi setengah lebar (*Half-width space*, `ScaleXSpan(0.5f)`) agar angka bulat di depan berjarak proporsional dengan pecahan.
3. **Placeholder Samar**:
   - `<dim>...</dim>`: Mewarnai pembilang/penyebut yang belum selesai diisi dengan warna abu-abu samar (`#777777`).

Markup ini diterjemahkan ke Android Spans secara instan oleh [`CalcSpannableFormatter.java`](file:///home/toto/Downloads/decompile/calctastic/sample_app/app/src/main/java/com/calctastic/sample/CalcSpannableFormatter.java).

---

## 6. Format Pangkat & Eksponen Seragam (`yˣ` & `x²`)

Untuk menjaga estetika dan kenyamanan visual layaknya kalkulator ilmiah modern, penulisan pangkat ditampilkan secara seragam dalam bentuk **superscript melayang**:

1. **Pangkat Bebas (`yˣ`)**:
   - Di memori input: Tersimpan sebagai operator eksponen standar (`^`).
   - Ketika tombol `yˣ` ditekan tanpa angka: Menampilkan kotak placeholder samar `□` melayang (`<sup><dim>□</dim></sup>`).
   - Ketika angka pangkat dimasukkan (misal `3`): Ditampilkan sebagai $2^3$ menggunakan tag `<sup>3</sup>`.
2. **Kuadrat (`x²`)**:
   - Ditampilkan seragam dengan tag `<sup>2</sup>` ($5^2$).
3. **Penyimpanan di Riwayat (History)**:
   - Di-render dalam bentuk superscript cantik ($2^3 = 8$) pada daftar riwayat.
   - Evaluasi aljabar mengevaluasi pola $a^b$ secara instan sebelum operasi biner lainnya.

---

## 6. Tata Letak Keypad 7-Baris (Portrait Simple Keypad)

Berdasarkan `keyboard_portrait_simple.xml` asli:

| Baris | Tombol 1 | Tombol 2 | Tombol 3 | Tombol 4 | Tombol 5 | Kategori & Fungsi |
| :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Row 1** | `M+` | `M−` | `MC` | `MS` | `MR` | **Memori Kalkulator**: Plus, Minus, Clear, Save, Recall |
| **Row 2** | `a/b` | `√x` | `%` | `(` | `)` | **Pecahan & Aljabar Dasar**: Pecahan, Akar Kuadrat, Persen, Kurung |
| **Row 3** | `1/x` | `x²` | `yˣ` | `◀` | `▶` | **Pangkat & Kursor**: Kebalikan, Kuadrat, Pangkat $y^x$, Geser Kiri, Geser Kanan |
| **Row 4** | `7` | `8` | `9` | `DEL` | `CLR` | **Digit & Hapus**: Digit 7-9, Backspace (DEL), Clear Line (CLR) |
| **Row 5** | `4` | `5` | `6` | `×` | `÷` | **Digit & Perkalian/Pembagian**: Digit 4-6, Kali, Bagi |
| **Row 6** | `1` | `2` | `3` | `+` | `−` | **Digit & Penjumlahan/Pengurangan**: Digit 1-3, Tambah, Kurang |
| **Row 7** | `0` | `.` | `±` | `=` *(Lebar 2x)* | - | **Digit, Desimal, Negasi & Eksekusi**: Nol, Titik Desimal, Plus/Minus, Sama Dengan |

---

## 7. Pewarnaan & Palet Tema (Rustic / Dark Theme)

Sesuai dengan palet warna visual CalcTastic:
- **Latar Belakang Layar & Keypad**: Gelap pekat (`#121212`).
- **Tombol Angka (0–9, Titik)**: Abu-abu gelap (`#333333`), teks putih (`#FFFFFF`).
- **Tombol Operator Dasar (`+`, `−`, `×`, `÷`)**: Abu-abu sedang (`#424242`), teks oranye (`#FF9800`).
- **Tombol Fungsi & Memori (`M+`, `a/b`, `√x`, dll)**: Abu-abu redup (`#262626`), teks abu-abu kebiruan (`#B0BEC5`).
- **Tombol Hapus (`DEL`, `CLR`)**: Merah bata gelap (`#5C2424`), teks merah terang (`#FF8A80`).
- **Tombol Sama Dengan (`=`)**: Oranye penuh (`#FF9800`), teks putih (`#FFFFFF`).
- **Hasil Hitungan (`history_result`)**: Hijau terang (`#4CAF50`).
