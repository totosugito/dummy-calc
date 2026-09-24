# Migrasi Event Tombol → Kode Asli CalcTastic

Checklist migrasi perilaku tombol dari decompiled original (`raw/`) ke sample app (`dummy-calc`).
Centang item saat selesai diimplementasi **dan** diverifikasi.

**Sumber kebenaran:**
- Label tombol: `raw/.../p007d0/f.java`
- Insert entry + auto-`(`: `raw/.../equations/Equation.java` `T()` (ordinal 68–86)
- Backspace per-entry: `raw/.../equations/Equation.java` `b()` + `AlgebraicInputHandler.L` case 27
- Plain/styled string: `raw/.../core/CalculatorCommand.java`
- Mapping tag→command: `raw/.../p007d0/g.java`, layout `keyboard_portrait_full.xml`

**Status:** `[ ]` belum · `[~]` sebagian · `[x]` selesai · `[-]` ditunda/n/a

---

## Batch 1 — Fungsi prefix + auto-paren + DEL token  (PRIORITAS)

| Status | Tombol | Asli plain | Auto `(`? | Shift | Catatan |
| :---: | --- | --- | :---: | --- | --- |
| [x] | Sin | `sin` | ya → `sin(` | asin→`asin(` | ordinal 73/76; label +`h` jika HYP |
| [x] | Cos | `cos` | ya | acos→`acos(` | ordinal 74/77 |
| [x] | Tan | `tan` | ya | atan→`atan(` | ordinal 75/78 |
| [x] | Ln | `ln` | ya | e^→`e^` **tanpa** `(` | ordinal 71/90 |
| [x] | Log | `log` | ya | 10^→`10^` **tanpa** `(` | ordinal 72/91 |
| [x] | **DEL** | — | — | MC side-effect | `backspaceEntry()`: hapus `(` + nama fungsi utuh; operator multi-char 1 entry |

**Implementasi DEL (string-based, setara entry):**
1. Jika sebelum kursor `func(` (arg kosong) → hapus seluruh `func(`
2. Jika sebelum kursor `(` dan sebelumnya nama fungsi (abs/ceil/floor/ln/log/sin/…) → hapus keduanya
3. Jika `func(x` → hapus per digit/char biasa di argumen
4. Jika `func()` dengan kursor setelah `)` → buang `)` lalu ulangi aturan 1–2
5. Selain itu → hapus 1 karakter (angka/operan multi-char: ` + ` dianggap 1 entry operator)

---

## Batch 2 — Fungsi/shift lain (auto-paren sesuai ordinal 68–86)

| Status | Tombol | Plain | Auto `(`? | Shift plain | Shift auto `(`? |
| :---: | --- | --- | :---: | --- | :---: |
| [x] | 7 | `7` | — | `ceil` | ya → `ceil(` |
| [x] | 4 | `4` | — | `floor` | ya → `floor(` |
| [x] | 5 | `5` | — | `abs` | ya → `abs(` |
| [x] | 6 | `6` | — | `arg` | ya → `arg(` |
| [x] | 8 | `8` | — | `re` | ya → `re(` |
| [x] | 9 | `9` | — | `im` | ya → `im(` |
| [x] | × shift | — | — | `conj` | ya → `conj(` |
| [x] | a/b | `/` | tidak | DMS `°` | tidak (DMS special) |
| [x] | x² | `²` | tidak (88) | `√` | tidak |
| [x] | yˣ | `^` | tidak | `√` (NTH_ROOT) | tidak |
| [x] | 1/x | `1/` | tidak (92) | — | — |
| [x] | % | `%` | tidak (93) | ` Δ% ` | tidak |
| [x] | ± | `-` | tidak (94) | STATS dialog | — |
| [x] | 1 shift | `!` | tidak (87) | — | — |
| [x] | simple √ | `√` (bukan `√(`) | tidak | — | — |
| [x] | simple 1/x | `1/` (bukan `1 / `) | tidak | — | — |

**Urutan ordinal auto-`(` = 68..86** (Equation.T:224–245):
`abs ceil floor ln log sin cos tan asin acos atan arg conj re im ones twos F08 F16`
**Bukan** auto: `! ² √ e^ 10^ 1/ % -` (87+)

---

## Batch 3 — Label dinamis tombol (f.java)

| Status | Tombol | Aturan asli | Sample sekarang |
| :---: | --- | --- | --- |
| [x] | DRG | `AngleUnit.f()` → `DEG`/`RAD`/`GRD` cycle (ScientificCalculator case 109) | ✅ cycle, label awal DEG |
| [x] | FSE | `DecimalNotation.b()` → `FIX`/`SCI`/`ENG` cycle (`DecimalNotation.f`) | ✅ FIX/SCI/ENG |
| [x] | HYP (shift DRG) | toggle; trig label `strQ+"h"` | ✅ toggle + `Sinh`/`Cos h`… |
| [x] | PRECISION (shift FSE) | label `P:`+precision (`f.java:89`, default 12) | ✅ `P:12` saat shift |
| [x] | POWER label | `f.java:55`: `zE0? "yˣ":"xʸ"`; ALGEBRAIC → **`xʸ`** | ✅ `xʸ` |
| [x] | SQUARE label | `x²` | ✅ |
| [x] | SHIFT alpha | selected state | ✅ alpha 0.7/1.0 |
| [x] | trig shift labels | asin/acos/atan di tombol saat shift | ✅ |

---

## Batch 4 — Operator & konstanta (plain string)

| Status | Tombol | Plain asli | Catatan |
| :---: | --- | --- | :---: |
| [x] | + | `" + "` | sudah |
| [x] | − | `" − "` | sudah |
| [x] | × | `" × "` | sudah |
| [x] | ÷ | `" / "` | sudah (plain pakai `/`) |
| [x] | ( ) | `(` `)` | sudah |
| [x] | digit 0–9 | `0`..`9` | sudah |
| [x] | . | `.` | sudah |
| [x] | π / e | `π` `e` | sudah |
| [x] | EEX | `E` | sudah |
| [x] | i / ∠ | `i` `∠` | sudah |
| [x] | nPr / nCr / mod / Δ% | `" nPr "` dst. | sudah |
| [x] | = | evaluate (plain `" = "` hanya display) | sudah performEquals |

---

## Batch 5 — Aksi non-insert (side-effect)

| Status | Tombol | Asli | Sample |
| :---: | --- | --- | :---: |
| [x] | CLR | clear current equation (L case 26) | ✅ |
| [x] | CLS (shift CLR) | clear equation + history (L case 25) | ✅ history.clear |
| [x] | DEL | backspace entry | ✅ `backspaceEntry()` (Batch 1) |
| [x] | ◀ ▶ | cursor ±1 entry/char | ✅ per-char |
| [x] | shift ◀ ▶ | HOME / END | ✅ |
| [x] | MS | MEMORY_SAVE — simpan operand ke reg 0 (Calc. case 44) | ✅ `memorySave()` |
| [x] | MR | MEMORY_RECALL — sisipkan memori (case 45) | ✅ insert string |
| [x] | MC (shift DEL) | MEMORY_CLEAR — hapus reg 0 (case 46) | ✅ `mMemoryValue = null` |
| [x] | M+ M− (shift + −) | MEMORY_PLUS/MINUS — `mem ±= operand` (case 48/49) | ✅ `memoryPlusMinus()` |
| [x] | simple M+/M−/MC/MS/MR | sama dengan sci | ✅ wired |
| [x] | Random (shift =) | CONST_RAND float [0,1) | ✅ Math.random |
| [ ] | CONST / CONV / STATS | buka dialog | [ ] no-op |
| [ ] | DMS shift a/b | dialog/konversi DMS | [ ] insert `°` saja |

---

## Batch 6 — Render ekspresi (bukan label tombol)

| Status | Item | Asli | Sample |
| :---: | --- | --- | --- |
| [x] | Superskrip sudut di ekspresi trig | `sin`+`<sup>d/r/g</sup>` (`CalculatorCommand.C`, AngleUnit.modifier) | ✅ `decorateAngleFunctions()` |
| [x] | HYP di ekspresi | `sin`+`<sup>h</sup>` | ✅ same, mode `mHyperbolic` |
| [x] | AngleUnit convert saat evaluate | `AngleUnit.e` DEG↔RAD↔GRD | ✅ `toRadians`/`fromRadians` |
| [x] | Evaluator `sin(` `ln(` `abs(` `!` `nPr` dll. | FloatingPoint / CommandEntry | ✅ `evalFunctionCalls` + helpers |
| [x] | Live preview untuk fungsi | evaluateExpression | ✅ PASS 9/9 (sin30/cos0/ln e/log100/abs/!/RAD) |

---

## Batch 7 — Font per command (f.java typeface map)

| Status | Font id | Font | Dipakai untuk |
| :---: | --- | --- | --- |
| [ ] | SANS1 | font_inter_medium | label fungsi, operator teks |
| [ ] | SANS2 | font_inter_regular | digit |
| [ ] | MONO1 | font_roboto_mono_variable | `i` `(` `)` |
| [ ] | SERI1 | font_stix_two_text_medium | `=` `π` `±` `+ − × ÷` |
| [ ] | SERI2 | font_hepta_slab_medium | `a/b` `x²` `yˣ` `1/x` `√` `.` |

Saat ini sample memakai `CalcSimpleButton.*` umum (inter medium).

---

## Legend perubahan file

- `SimpleCalculatorActivity.java` — wiring click + insert/delete logic
- `activity_simple_calculator.xml` — layout sci/simple
- `styles.xml` — style tombol/font
- (opsional nanti) token/entry model ringan agar DEL setara `Equation.b()`

---

## Log progres

| Tanggal | Batch | Catatan |
| --- | --- | --- |
| 2026-09-24 | Setup | Plan dibuat; Batch 4 sebagian besar sudah dari wiring awal |
| 2026-09-24 | Batch 1 | **Selesai + diverifikasi logcat** — `sin(` `ln(`; DEL: `sin(5)`→`sin(`→empty; SHIFT+Ln→`e^` no paren |
| 2026-09-24 | Batch 2 | **Selesai + diverifikasi logcat PASS 16/16** — `²` `1/` `%` `^` `!` `°` ` nPr ` (DEL utuh); simple `√` (bukan `√(`); simple `1/` |
| 2026-09-24 | Batch 3 | **Selesai** — POWER `xʸ` (ALGEBRAIC); FSE shift → `P:12`; HYP → `Sinh`; DRG/FSE cycle; trig shift asin/acos/atan |
| 2026-09-24 | Batch 5 | **Selesai + diverifikasi PASS 4/4** — MS/MR; M+ `5+3=8`; M− `8-2=6`; MC kosongkan memori; simple mem row wired |
| 2026-09-24 | Batch 6 | **Selesai + diverifikasi PASS 9/9** — sin(30)DEG=0.5; cos0=1; ln(e)=1; log100=2; abs(-5)=5; 5!=120; RAD sin(1)≈0.841; superskrip d/r/h |
| 2026-09-24 | Fix | **Superskrip + kursor** — decorate setelah formatEquation (cari nama fn, bukan `sin(`); kursor disesuaikan +char sup; `evaluateLive` skip ekspresi belum lengkap. Verif PASS 4/4: `sin`+`5`→`sind(5` |

---

## Definisi selesai (per tombol)

1. Insert string = `equationStringPlain` asli  
2. Auto-`(` sesuai ordinal 68–86  
3. Shift = command shift dari `SCIENTIFIC_DESIGN.md` / `g.java`  
4. Label = aturan `f.java` (termasuk dinamis DRG/FSE/HYP)  
5. DEL mengikuti aturan entry (fungsi+paren utuh)  
6. Diverifikasi di emulator `emulator-5554`
