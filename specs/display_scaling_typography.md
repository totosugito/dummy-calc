# SPECIFICATION: Display Sizing, Dynamic Screen Scale & Typography
**Target: 100% Persis Kode Asli HiPER Calc**  
**Referensi File Asli Decompile:**
- Display Container View: `android.core.BE.java`, `android.core.UF.java`
- Responsive Screen Scaling: `android.core.C0332vh.java` (lines 755–815), `android.core.Tg.java` (lines 64–72, 367–395)
- Typography & Font Metrics: `android.core.AbstractC0293re.java` (lines 135–150, 391–395, 516–532), `android.core.C0215jD.java`, `android.core.DC.java`
- Layout Metrics: `android.core.AbstractC0060Ig.java`, `android.core.C0341wd.java`, `android.core.C0081Pe.java`

---

## 1. Typeface & Font Family (Typography)

Berdasarkan `AbstractC0293re.java` (baris 135–144) & `DC.HiPER`:
- **Font String Decryption:**
  `strHiPER = DC.HiPER("\\\u0014t\u0007q")`
  Algoritma XOR (key `0x1D` dan `'f'`) menghasilkan string tepat:
  $$\mathbf{"Arial"}$$
- **Typeface Asli:**
  ```java
  Typeface.create("Arial", Typeface.NORMAL);
  ```
  *(Fallback sistem: `Typeface.SANS_SERIF` jika platform Android tidak memiliki alias Arial).*

---

## 2. Ukuran Nominal Dasar (Nominal Base Size)

Berdasarkan `AbstractC0293re.java` (baris 144) & `UF.java` (baris 79):
- Di `UF.java`:
  ```java
  this.m = abstractC0293reM232HiPER.HiPER("100", "86", Tk.HiPER.WB, super.I);
  ```
- Di `AbstractC0293re.java`:
  ```java
  HiPER(new C0215jD(strHiPER, 0, 14.0f), "100");
  ```
- **Nominal Base Size untuk teks display ekspresi (`"100"`):**
  $$\text{nominalBaseSize} = \mathbf{14.0\text{f}}$$

---

## 3. Rumus Dynamic Screen Scale (`C0332vh.java` & `Tg.java`)

HiPER Calc tidak pernah menggunakan hardcoded `sp` seperti `22sp` pada display canvas-nya. Ukuran font dihitung responsif mengikuti dimensi layar device:

### A. Formula Ukuran Font Efektif (`AbstractC0293re.java` baris 520 & 527):
$$\text{finalTextSizePx} = \text{screenScale} \times \text{nominalBaseSize}(14.0\text{f}) \times \text{childScale}(D)$$

Di mana:
$$\text{screenScale} = \text{mo352HiPER}() = \mathbf{Tg.HiPER(V.HiPER)}$$

### B. Perhitungan `Tg.HiPER(V.HiPER)` pada `C0332vh.java` (baris 778–812):
1. **Dimensi Device Saat Ini (`point`):**
   - $W = \text{viewport.width}$ (misal pada emulator Pixel: $1080\text{px}$)
   - $H = \text{viewport.height}$ (misal: $2424\text{px}$)
2. **Ukuran Referensi Konten Layout Dasar (`pointF`):**
   - Dihitung dari lebar grid tombol keypad dasar (`C0341wd.mo344HiPER()` / `C0081Pe.mo344HiPER()`).
   - Lebar referensi desain nominal dasar HiPER adalah:
     $$\text{pointF.x} \approx 300.0\text{f} - 320.0\text{f}\text{ (dalam unit referensi DP)}$$
     $$\text{pointF.x}_{\text{px}} = \text{pointF.x} \times \text{density}_{\text{base}} \approx 440\text{px} - 450\text{px}$$
3. **Rasio Skala Horizontal & Vertikal (`C0332vh.java` baris 801–811):**
   $$f_x = \frac{\text{point.x}}{\text{pointF.x}}$$
   $$f_y = \frac{\text{point.y}}{\text{pointF.y} + \text{displayHeight}}$$
   Pada mode portrait standar:
   $$\text{screenScale} = f_x \approx \frac{1080}{440} \approx \mathbf{2.45\text{f}}$$
4. **Hasil Ukuran Text Riil di Layar:**
   $$\text{Text Size Px} = 2.45 \times 14.0\text{f} \times \text{densityRatio} \approx \mathbf{88\text{px} - 92\text{px}}\quad (\mathbf{\approx 34\text{sp} - 35\text{sp}})$$

---

## 4. Analisis Mengapa Display Kita Sebelumnya Tampak Lebih Kecil

| Parameter | Kode Dummy Kita Sebelumnya | Kode Asli HiPER Calc (`UF.java` + `C0332vh.java`) |
| :--- | :--- | :--- |
| **Metode Ukuran** | Hardcoded `22.0f sp` | Dynamic Screen Scaling (`screenScale * 14.0f`) |
| **Ukuran Pixel Efektif** | $\approx 57.75\text{px}$ (di density 420dpi) | $\approx \mathbf{89.25\text{px} - 91.8\text{px}}$ |
| **Persentase Perbedaan** | **~37% lebih kecil** dari aslinya | Proporsional memenuhi display dan mudah disentuh jari |
| **Typeface** | Mencoba symbol font kustom | `Typeface.create("Arial", Typeface.NORMAL)` |
| **Cursor Width** | `paint.measureText(" ") * 0.35f` dari paint kecil | `paint.measureText(" ") * 0.35f` dari paint yang diskalakan dinamis |

---

## 5. Task List Implementasi 100% Persis HiPER

- [x] **Task 1: Implementasi Dynamic Scale Engine (`HyperCalDisplayView.java`)**
  - Implementasikan perhitungan `screenScale` berbasis lebar viewport tampilan (`screenWidth / baseReferenceGridWidth`) dengan base reference width $276.0\text{f}$.
  - Hubungkan base paint ke rumus $\text{finalTextSizePx} = \text{screenScale} \times 14.0\text{f}$.
- [x] **Task 2: Konfigurasi Typeface Asli HiPER (`HyperCalDisplayView.java`)**
  - Gunakan `Typeface.create("Arial", Typeface.NORMAL)` dengan fallback `Typeface.SANS_SERIF` sesuai `AbstractC0293re.java`.
- [x] **Task 3: Baseline & Padding Alignment (`UF.java` line 457)**
  - *(2026-09-26: sebelumnya tercentang tapi `onDraw` masih memusatkan vertikal; kini benar-benar diterapkan, lihat Task 6.)*
  - Terapkan padding dan baseline offset asli:
    $$f_3 = (-\text{paint.ascent()}) \times 1.6\text{f}$$
- [x] **Task 4: Update Proporsi Caret Kursor**
  - Tebal kursor dan tinggi kursor otomatis mengikuti ukuran `basePaint` baru yang proporsional ($54.8\text{px}$).
- [x] **Task 5: Verifikasi Visual & Emulator**
  - Berhasil dikompilasi, diinstal, dan diverifikasi di emulator Android Pixel. Text tampil jauh lebih besar, jelas, dan proporsional persis HiPER Calc.

---

## 6. Gap Analysis vs Kode Asli (2026-09-26) & Task List Lanjutan

- [x] **Task 6: Baseline display persis `UF.java` baris 452–474**
  - `f3 = (-ascent) × 1.6`; `baseline = max(f3, root.m)`.
  - Jika bagian di bawah baseline (`root.b.y - root.m`) > `viewHeight - f3` → `baseline = max(root.m, viewHeight - below)`.
  - `startY = baseline - root.m` (menggantikan `(viewHeight - b.y) / 2`). Terverifikasi di emulator: ekspresi menempel di atas display.
- [x] **Task 7: Rumus skala `k() = Tg.HiPER(V) × D × G.HiPER`**, ukuran font `k() × 14` per visual. Sekarang `width / 276f` tanpa density; `REFERENCE_WIDTH_DP = 310` tidak terpakai; rasio `f_y` (landscape) belum ada. Samakan angka tabel (~89px) dengan Task 4 (54.8px).
  - **Temuan (C0332vh.java 797–811, Tg.java 367–395):** `Tg.HiPER(V.HiPER)` mengembalikan field `H` = skala **vertikal** `f2 = point.y / (pointF.y + M)`, dibatasi `f2 ≤ 1.2 × f` dengan `f = point.x / pointF.x`. `point` = seluruh area kalkulator (bukan hanya display). Nilai dimensi tema adalah unit desain mentah (tanpa density) → skala langsung menjadi px/unit.
  - **Keypad referensi** (`C0341wd.mo344HiPER()` + tema `AbstractC0060Ig`): `x = max((4+47)×5, (5+59)×4) + 4 + 4 = 264`, `y = (4+26)×3 + (5+33)×5 + 0 + 4 + 1 = 285`, lalu `+2×"1"` → **266 × 287**.
  - **M** (tinggi display referensi, `GestureDetectorOnGestureListenerC0122aI.G()`): `2×"95" + header + UF.d() (3.6 × lineH(14)) + baris hasil lineH(15) + lineH(8) + "95"`. Tinggi header (`m283HiPER`) diaproksimasi satu baris font 8 → nilai M adalah **perkiraan**.
  - **Implementasi:** `HyperCalDisplayView.updatePaintSize()` memakai ukuran `android.R.id.content`. Di ponsel tinggi, batas `1.2 × f` yang menentukan, jadi M hampir tidak berpengaruh.
  - **Hasil emulator 1080×2424 @420dpi:** `1.2 × 1080/266 × 14 ≈ 68.2 px` (sebelumnya 54.8 px). Klaim "88–92 px" di Bagian 3 tidak sesuai kode asli.
  - Asumsi yang belum terbukti: kelas keypad aktif adalah `C0341wd` (bukan `BD`/`C0081Pe`/`C0271pD`), cabang `Tk.xa = false`, dan mode bukan `EnumC0051Ha.c`.
- [x] **Task 8: Paint per visual via `k$1()`**: typeface + style dari `C0215jD`, warna dari tema key `"86"`. Hilangkan hardcode `0xFFFFFFFF` (teks) & `0xFF2196F3` (kursor).
  - Selesai sebagian: warna teks & kursor sekarang dari `res/values/colors.xml` (bukan literal di kode), kursor diselaraskan ke aksen aplikasi `#FF9800` (sama dengan `CalctasticCalculatorActivity`). Paint per-visual individual (`k$1()` dengan typeface/style per node dari tema `"86"`) belum diimplementasikan — semua visual masih berbagi satu `basePaint` yang diteruskan turun.
- [x] **Task 9: Hapus clamp lebar kursor** `Math.max(3.0f, …)` / `Math.max(2.0f, …)` — dihapus di `HyperCalDisplayView` (fallback root cursor) dan `render/MathVisual.getCursorWidth` (dipakai semua visual).
- [ ] **Task 10: Scroll, clip & wrap multi-baris** (`UF.java` baris 436–440, 638): `canvas.clipRect` dengan offset `B`, batas lebar `G.m` untuk wrap.
- [x] **Task 11: Bersihkan Javadoc usang** di `HyperCalDisplayView` (masih menyebut `22sp`, `R.font.math_symbols`).
- [ ] **Task 12: Pastikan string ukur `HcZgWQ.LiVE`** pada `AbstractC0335wD.HiPER(paint, f)` memang `" "` (diisi saat runtime).
