# Desain Kalkulator Scientific (CalcTastic Advanced Layout)

Dokumen ini menyajikan spesifikasi arsitektur antarmuka, daftar tombol lengkap (primer & sekunder/shift), serta fungsionalitas matematis mode **Scientific Calculator** yang diekstrak langsung dari kode sumber dekompilasi aplikasi asli CalcTastic (`keyboard_portrait_full.xml`, `p007d0/g.java`, dan `CalculatorCommand.java`).

---

## 1. Arsitektur Layout

- **Struktur Grid**: **7 Baris × 6 Kolom** (khusus baris ke-7 tombol sama dengan `=` menggunakan `layout_weight="2.02"` sehingga baris ke-7 memiliki 5 tombol fisik).
- **Mekanisme Shift / Fungsi Ganda**:
  - Tombol fisik menampilkan label primer (*Main function*) di bagian tengah dan label sekunder (*Shifted function*) kecil di bagian atas.
  - Menekan tombol **`SHIFT`** mengubah mode input untuk mengakses fungsi sekunder matematika tingkat lanjut.
- **Tipografi Tombol**:
  - Font primer: `font_inter_medium.ttf` atau `font_hepta_slab_medium.ttf`.
  - Font eksponen & simbol aljabar: format markup (`<sup>`, `<sf>`, `<bf>`, `<xw>`).

---

## 2. Rincian Tombol Per Baris

### Baris 1: Kontrol Mode, Satuan Sudut, Notasi & Memori
| Posisi | ID Tag | Tombol Utama | Label Utama | Tombol Shift (2nd) | Label Shift | Efek & Penjelasan |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `MAIN_BUTTON_000` | `KEYBOARD_SHIFT` | **SHIFT** | - | - | Mengaktifkan/menonaktifkan mode fungsi sekunder (*2nd function*). Mengubah warna indikator shift di layar. |
| **2** | `MAIN_BUTTON_300` | `COMPLEX_RECT` | **i** | `COMPLEX_POLAR` | **∠** | **Utama**: Memasukkan bilangan imajiner $i$ (format rektangular $a + bi$).<br>**Shift**: Memasukkan sudut fase polar ($\angle$) untuk format polar $r\angle\theta$. |
| **3** | `MAIN_BUTTON_301` | `ANGLE_UNIT` | **DRG** | `HYPERBOLIC` | **HYP** | **Utama**: Mengganti satuan sudut siklis: **DEG** (Derajat) &rarr; **RAD** (Radian) &rarr; **GRAD** (Gradian).<br>**Shift**: Mengaktifkan fungsi trigonometri hiperbolik (`sinh`, `cosh`, `tanh`). |
| **4** | `MAIN_BUTTON_302` | `NOTATION` | **FSE** | `PRECISION` | *(dialog)* | **Utama**: Mengganti notasi angka: **FIX** &rarr; **SCI** (Scientific) &rarr; **ENG** (Engineering).<br>**Shift**: Membuka dialog pemilihan presisi/desimal di belakang koma. |
| **5** | `MAIN_BUTTON_303` | `MEMORY_SAVE` | **MS** | - | - | Menyimpan (*Save*) nilai/hasil saat ini ke dalam memori independen kalkulator. |
| **6** | `MAIN_BUTTON_304` | `MEMORY_RECALL` | **MR** | - | - | Memanggil kembali (*Recall*) nilai tersimpan dari memori ke ekspresi aktif. |

---

### Baris 2: Pecahan, Trigonometri & Pengelompokan
| Posisi | ID Tag | Tombol Utama | Label Utama | Tombol Shift (2nd) | Label Shift | Efek & Penjelasan |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `MAIN_BUTTON_305` | `FRACTION` | **a/b** | `DMS` | **D°M'S"** | **Utama**: Memasukkan pecahan ($^a/_b$ atau campuran $a\ ^b/_c$).<br>**Shift**: Konversi format sudut Derajat, Menit, Detik. |
| **2** | `MAIN_BUTTON_306` | `SINE` | **Sin** | `ARCSINE` | **asin** | **Utama**: Menghitung sinus ($\sin(x)$).<br>**Shift**: Menghitung invers sinus ($\arcsin(x)$ / $\sin^{-1}(x)$). |
| **3** | `MAIN_BUTTON_307` | `COSINE` | **Cos** | `ARCCOSINE` | **acos** | **Utama**: Menghitung kosinus ($\cos(x)$).<br>**Shift**: Menghitung invers kosinus ($\arccos(x)$ / $\cos^{-1}(x)$). |
| **4** | `MAIN_BUTTON_308` | `TANGENT` | **Tan** | `ARCTANGENT` | **atan** | **Utama**: Menghitung tangen ($\tan(x)$).<br>**Shift**: Menghitung invers tangen ($\arctan(x)$ / $\tan^{-1}(x)$). |
| **5** | `MAIN_BUTTON_309` | `PARENTH_OPEN` | **(** | - | - | Membuka tanda kurung aljabar. |
| **6** | `MAIN_BUTTON_310` | `PARENTH_CLOSE` | **)** | - | - | Menutup tanda kurung aljabar. |

---

### Baris 3: Kebalikan, Konstanta, Logaritma & Navigasi Kursor
| Posisi | ID Tag | Tombol Utama | Label Utama | Tombol Shift (2nd) | Label Shift | Efek & Penjelasan |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `MAIN_BUTTON_311` | `RECIPROCAL` | **1/x** | - | - | Menghitung kebalikan nilai saat ini ($x^{-1} = \frac{1}{x}$). |
| **2** | `MAIN_BUTTON_312` | `CONST_PI` | **π** | `CONST_E` | **e** | **Utama**: Memasukkan konstanta $\pi \approx 3.14159265...$<br>**Shift**: Memasukkan bilangan Euler $e \approx 2.71828182...$ |
| **3** | `MAIN_BUTTON_313` | `LOG_E` | **Ln** | `EXP_E` | **eˣ** | **Utama**: Logaritma natural basis $e$ ($\ln(x)$).<br>**Shift**: Eksponensial natural basis $e$ ($e^x$). |
| **4** | `MAIN_BUTTON_314` | `LOG_10` | **Log** | `EXP_10` | **10ˣ** | **Utama**: Logaritma umum basis 10 ($\log_{10}(x)$).<br>**Shift**: Pangkat sepuluh ($10^x$). |
| **5** | `MAIN_BUTTON_315` | `CURSOR_LEFT` | **◀** | `CURSOR_HOME` | **\|◀** | **Utama**: Menggeser kursor satu karakter ke kiri.<br>**Shift**: Melompat ke awal ekspresi hitungan (*Home*). |
| **6** | `MAIN_BUTTON_316` | `CURSOR_RIGHT` | **▶** | `CURSOR_END` | **▶\|** | **Utama**: Menggeser kursor satu karakter ke kanan.<br>**Shift**: Melompat ke akhir ekspresi hitungan (*End*). |

---

### Baris 4: Kuadrat & Angka (7, 8, 9) + Hapus
| Posisi | ID Tag | Tombol Utama | Label Utama | Tombol Shift (2nd) | Label Shift | Efek & Penjelasan |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `MAIN_BUTTON_317` | `SQUARE` | **x²** | `SQRT` | **√x** | **Utama**: Kuadrat bilangan ($x^2$).<br>**Shift**: Akar kuadrat ($\sqrt{x}$). |
| **2** | `MAIN_BUTTON_001` | `NUM_7` | **7** | `CEILING` | **Ceil** | **Utama**: Input digit 7.<br>**Shift**: Pembulatan ke atas ke integer terdekat ($\lceil x \rceil$). |
| **3** | `MAIN_BUTTON_002` | `NUM_8` | **8** | `COMPLEX_REAL` | **Re** | **Utama**: Input digit 8.<br>**Shift**: Mengambil bagian riil bilangan kompleks ($\text{Re}(z)$). |
| **4** | `MAIN_BUTTON_003` | `NUM_9` | **9** | `COMPLEX_IMAG` | **Im** | **Utama**: Input digit 9.<br>**Shift**: Mengambil bagian imajiner bilangan kompleks ($\text{Im}(z)$). |
| **5** | `MAIN_BUTTON_004` | `BACKSPACE` | **DEL** | `MEMORY_CLEAR` | **MC** | **Utama**: Menghapus satu karakter sebelum kursor.<br>**Shift**: Mengosongkan memori kalkulator (*Memory Clear*). |
| **6** | `MAIN_BUTTON_005` | `CLEAR` | **CLR** | `CLEAR_SCREEN` | **CLS** | **Utama**: Menghapus baris kalkulasi aktif yang sedang diketik.<br>**Shift**: Menghapus seluruh riwayat layar (*Clear Screen / All*). |

---

### Baris 5: Pangkat & Angka (4, 5, 6) + Perkalian & Pembagian
| Posisi | ID Tag | Tombol Utama | Label Utama | Tombol Shift (2nd) | Label Shift | Efek & Penjelasan |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `MAIN_BUTTON_318` | `POWER` | **yˣ** | `NTH_ROOT` | **ˣ√y** | **Utama**: Perpangkatan sebarang ($y^x$).<br>**Shift**: Akar pangkat ke-$x$ ($\sqrt[x]{y}$). |
| **2** | `MAIN_BUTTON_006` | `NUM_4` | **4** | `FLOOR` | **Floor** | **Utama**: Input digit 4.<br>**Shift**: Pembulatan ke bawah ke integer terdekat ($\lfloor x \rfloor$). |
| **3** | `MAIN_BUTTON_007` | `NUM_5` | **5** | `ABSOLUTE` | **Abs** | **Utama**: Input digit 5.<br>**Shift**: Nilai mutlak / modulus ($|x|$). |
| **4** | `MAIN_BUTTON_008` | `NUM_6` | **6** | `COMPLEX_ARG` | **Arg** | **Utama**: Input digit 6.<br>**Shift**: Argumen sudut fase bilangan kompleks ($\text{arg}(z)$). |
| **5** | `MAIN_BUTTON_009` | `MULTIPLY` | **×** | `COMPLEX_CONJ` | **Conj** | **Utama**: Perkalian aljabar ($a \times b$).<br>**Shift**: Konjugat bilangan kompleks ($\bar{z}$). |
| **6** | `MAIN_BUTTON_010` | `DIVIDE` | **÷** | `MODULO` | **Mod** | **Utama**: Pembagian aljabar ($a \div b$).<br>**Shift**: Sisa pembagian integer ($a \pmod b$). |

---

### Baris 6: Persen & Angka (1, 2, 3) + Penjumlahan & Pengurangan
| Posisi | ID Tag | Tombol Utama | Label Utama | Tombol Shift (2nd) | Label Shift | Efek & Penjelasan |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `MAIN_BUTTON_319` | `PERCENT` | **%** | `DELTA_PERCENT`| **Δ%** | **Utama**: Persentase aljabar ($x\%$).<br>**Shift**: Perubahan persentase relatif / selisih persen. |
| **2** | `MAIN_BUTTON_011` | `NUM_1` | **1** | `FACTORIAL` | **x!** | **Utama**: Input digit 1.<br>**Shift**: Faktorial ($x!$). |
| **3** | `MAIN_BUTTON_012` | `NUM_2` | **2** | `NPR` | **nPr** | **Utama**: Input digit 2.<br>**Shift**: Permutasi $nPr = \frac{n!}{(n-r)!}$. |
| **4** | `MAIN_BUTTON_013` | `NUM_3` | **3** | `NCR` | **nCr** | **Utama**: Input digit 3.<br>**Shift**: Kombinasi $nCr = \frac{n!}{r!(n-r)!}$. |
| **5** | `MAIN_BUTTON_014` | `ADD` | **+** | `MEMORY_PLUS` | **M+** | **Utama**: Penjumlahan ($a + b$).<br>**Shift**: Menambahkan angka/hasil aktif ke nilai memori. |
| **6** | `MAIN_BUTTON_015` | `SUBTRACT` | **−** | `MEMORY_MINUS`| **M−** | **Utama**: Pengurangan ($a - b$).<br>**Shift**: Mengurangkan angka/hasil aktif dari nilai memori. |

---

### Baris 7: Notasi Eksponen, 0, Titik Desimal, Negasi & Sama Dengan
| Posisi | ID Tag | Tombol Utama | Label Utama | Tombol Shift (2nd) | Label Shift | Efek & Penjelasan |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `MAIN_BUTTON_320` | `EXPONENT` | **EEX** | - | - | Memasukkan notasi eksponen saintifik ($10^E$, contoh `2.5E10`). |
| **2** | `MAIN_BUTTON_016` | `NUM_0` | **0** | `CONST` | **CONST** | **Utama**: Input digit 0.<br>**Shift**: Membuka dialog pemilih konstanta fisika/matematika (kecepatan cahaya, Planck, gravitasi, dll). |
| **3** | `MAIN_BUTTON_017` | `DECIMAL` | **.** | `CONVERT_UNIT` | **CONV** | **Utama**: Titik pemisah desimal.<br>**Shift**: Membuka modul konverter satuan (panjang, massa, suhu, dll). |
| **4** | `MAIN_BUTTON_018` | `NEGATE` | **±** | `STATISTIC` | **STATS** | **Utama**: Membalik tanda bilangan ($+ / -$).<br>**Shift**: Membuka kalkulator statistik (Mean, Standar Deviasi, Sum). |
| **5** | `MAIN_BUTTON_019` | `EQUALS` | **=** | `CONST_RAND` | **Random** | **Utama**: Mengeksekusi kalkulasi dan memasukkan ke riwayat.<br>**Shift**: Menghasilkan bilangan acak acak antara 0 dan 1. |

---

## 3. Komponen Layar Pendukung Mode Scientific

Pada implementasi penuh di aplikasi aslinya, layar kalkulator dilengkapi status bar indikator di atas list hitungan:
1. **Satuan Sudut (`DEG` / `RAD` / `GRAD`)**: Menampilkan satuan aktif perhitungan trigonometri.
2. **Indikator `2nd` / `SHIFT`**: Menyala saat tombol shift aktif.
3. **Indikator `HYP`**: Menyala saat fungsi hiperbolik trigonometri dipilih.
4. **Indikator Notasi (`FIX`, `SCI`, `ENG`)**: Menandakan format penulisan angka desimal.
5. **Indikator `M`**: Menandakan ada nilai memori kalkulator yang sedang tersimpan.
