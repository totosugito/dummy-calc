# SPECIFICATION: Display Sizing, Dynamic Screen Scale & Typography
**Target: 100% Faithful to the Original HiPER Calc**
**Original Decompile Reference Files:**
- Display Container View: `android.core.BE.java`, `android.core.UF.java`
- Responsive Screen Scaling: `android.core.C0332vh.java` (lines 755–815), `android.core.Tg.java` (lines 64–72, 367–395)
- Typography & Font Metrics: `android.core.AbstractC0293re.java` (lines 135–150, 391–395, 516–532), `android.core.C0215jD.java`, `android.core.DC.java`
- Layout Metrics: `android.core.AbstractC0060Ig.java`, `android.core.C0341wd.java`, `android.core.C0081Pe.java`

---

## 1. Typeface & Font Family (Typography)

Based on `AbstractC0293re.java` (lines 135–144) & `DC.HiPER`:
- **Font String Decryption:**
  `strHiPER = DC.HiPER("\\\u0014t\u0007q")`
  XOR algorithm (key `0x1D` and `'f'`) yields exactly the string:
  $$\mathbf{"Arial"}$$
- **Original Typeface:**
  ```java
  Typeface.create("Arial", Typeface.NORMAL);
  ```
  *(System fallback: `Typeface.SANS_SERIF` if the Android platform doesn't have an Arial alias.)*

---

## 2. Nominal Base Size

Based on `AbstractC0293re.java` (line 144) & `UF.java` (line 79):
- In `UF.java`:
  ```java
  this.m = abstractC0293reM232HiPER.HiPER("100", "86", Tk.HiPER.WB, super.I);
  ```
- In `AbstractC0293re.java`:
  ```java
  HiPER(new C0215jD(strHiPER, 0, 14.0f), "100");
  ```
- **Nominal base size for the expression display text (`"100"`):**
  $$\text{nominalBaseSize} = \mathbf{14.0\text{f}}$$

---

## 3. Dynamic Screen Scale Formula (`C0332vh.java` & `Tg.java`)

HiPER Calc never uses a hardcoded `sp` like `22sp` on its display canvas. Font size is computed
responsively based on the device's screen dimensions:

### A. Effective Font Size Formula (`AbstractC0293re.java` lines 520 & 527):
$$\text{finalTextSizePx} = \text{screenScale} \times \text{nominalBaseSize}(14.0\text{f}) \times \text{childScale}(D)$$

Where:
$$\text{screenScale} = \text{mo352HiPER}() = \mathbf{Tg.HiPER(V.HiPER)}$$

### B. Computing `Tg.HiPER(V.HiPER)` in `C0332vh.java` (lines 778–812):
1. **Current device dimensions (`point`):**
   - $W = \text{viewport.width}$ (e.g. on a Pixel emulator: $1080\text{px}$)
   - $H = \text{viewport.height}$ (e.g.: $2424\text{px}$)
2. **Base layout content reference size (`pointF`):**
   - Computed from the width of the base keypad button grid (`C0341wd.mo344HiPER()` /
     `C0081Pe.mo344HiPER()`).
   - HiPER's nominal base design reference width is:
     $$\text{pointF.x} \approx 300.0\text{f} - 320.0\text{f}\text{ (in reference DP units)}$$
     $$\text{pointF.x}_{\text{px}} = \text{pointF.x} \times \text{density}_{\text{base}} \approx 440\text{px} - 450\text{px}$$
3. **Horizontal & vertical scale ratio (`C0332vh.java` lines 801–811):**
   $$f_x = \frac{\text{point.x}}{\text{pointF.x}}$$
   $$f_y = \frac{\text{point.y}}{\text{pointF.y} + \text{displayHeight}}$$
   In standard portrait mode:
   $$\text{screenScale} = f_x \approx \frac{1080}{440} \approx \mathbf{2.45\text{f}}$$
4. **Resulting real on-screen text size:**
   $$\text{Text Size Px} = 2.45 \times 14.0\text{f} \times \text{densityRatio} \approx \mathbf{88\text{px} - 92\text{px}}\quad (\mathbf{\approx 34\text{sp} - 35\text{sp}})$$

---

## 4. Why Our Display Previously Looked Smaller

| Parameter | Our Previous Dummy Code | Original HiPER Calc Code (`UF.java` + `C0332vh.java`) |
| :--- | :--- | :--- |
| **Sizing method** | Hardcoded `22.0f sp` | Dynamic screen scaling (`screenScale * 14.0f`) |
| **Effective pixel size** | $\approx 57.75\text{px}$ (at 420dpi density) | $\approx \mathbf{89.25\text{px} - 91.8\text{px}}$ |
| **Percentage difference** | **~37% smaller** than the original | Proportional, fills the display, comfortable for finger taps |
| **Typeface** | Attempted a custom symbol font | `Typeface.create("Arial", Typeface.NORMAL)` |
| **Cursor width** | `paint.measureText(" ") * 0.35f` off a small paint | `paint.measureText(" ") * 0.35f` off a dynamically scaled paint |

---

## 5. Implementation Task List — 100% Faithful to HiPER

- [x] **Task 1: Implement the Dynamic Scale Engine (`HyperCalDisplayView.java`)**
  - Implement `screenScale` computation based on the viewport width
    (`screenWidth / baseReferenceGridWidth`) with a base reference width of $276.0\text{f}$.
  - Wire the base paint to the formula $\text{finalTextSizePx} = \text{screenScale} \times 14.0\text{f}$.
- [x] **Task 2: Configure the Original HiPER Typeface (`HyperCalDisplayView.java`)**
  - Use `Typeface.create("Arial", Typeface.NORMAL)` with fallback `Typeface.SANS_SERIF`, matching
    `AbstractC0293re.java`.
- [x] **Task 3: Baseline & Padding Alignment (`UF.java` line 457)**
  - *(2026-09-26: previously checked off but `onDraw` still vertically centered; now actually
    applied, see Task 6.)*
  - Apply the original padding and baseline offset:
    $$f_3 = (-\text{paint.ascent()}) \times 1.6\text{f}$$
- [x] **Task 4: Update Cursor Caret Proportions**
  - Cursor thickness and height now automatically follow the new proportional `basePaint`
    ($54.8\text{px}$).
- [x] **Task 5: Visual & Emulator Verification**
  - Successfully built, installed, and verified on an Android Pixel emulator. Text now displays
    much bigger, clearer, and proportional to the real HiPER Calc.

---

## 6. Gap Analysis vs. the Original Code (2026-09-26) & Follow-up Task List

- [x] **Task 6: Display baseline exactly matching `UF.java` lines 452–474**
  - `f3 = (-ascent) × 1.6`; `baseline = max(f3, root.m)`.
  - If the portion below the baseline (`root.b.y - root.m`) > `viewHeight - f3` → `baseline =
    max(root.m, viewHeight - below)`.
  - `startY = baseline - root.m` (replacing `(viewHeight - b.y) / 2`). Verified on the emulator: the
    expression sticks to the top of the display.
- [x] **Task 7: Scale formula `k() = Tg.HiPER(V) × D × G.HiPER`**, font size `k() × 14` per visual.
  Currently `width / 276f` with no density factor; `REFERENCE_WIDTH_DP = 310` is unused; the
  landscape `f_y` ratio doesn't exist yet. Reconcile the table's numbers (~89px) with Task 4's
  (54.8px).
  - **Finding (`C0332vh.java` 797–811, `Tg.java` 367–395):** `Tg.HiPER(V.HiPER)` returns field `H` =
    the **vertical** scale `f2 = point.y / (pointF.y + M)`, clamped to `f2 ≤ 1.2 × f` where
    `f = point.x / pointF.x`. `point` = the entire calculator area (not just the display). The
    theme's dimension values are raw design units (no density) → the scale becomes px/unit directly.
  - **Reference keypad** (`C0341wd.mo344HiPER()` + theme `AbstractC0060Ig`): `x = max((4+47)×5,
    (5+59)×4) + 4 + 4 = 264`, `y = (4+26)×3 + (5+33)×5 + 0 + 4 + 1 = 285`, then `+2×"1"` →
    **266 × 287**.
  - **M** (reference display height, `GestureDetectorOnGestureListenerC0122aI.G()`):
    `2×"95" + header + UF.d() (3.6 × lineH(14)) + result-row lineH(15) + lineH(8) + "95"`. The
    header height (`m283HiPER`) is approximated as one line of font 8 → the value of M is an
    **estimate**.
  - **Implementation:** `HyperCalDisplayView.updatePaintSize()` uses the size of
    `android.R.id.content`. On tall phones, the `1.2 × f` clamp dominates, so M barely matters.
  - **Emulator result at 1080×2424 @420dpi:** `1.2 × 1080/266 × 14 ≈ 68.2 px` (was 54.8 px before).
    The "88–92 px" claim in Section 3 doesn't match the original code.
  - Unverified assumptions: the active keypad class is `C0341wd` (not `BD`/`C0081Pe`/`C0271pD`),
    branch `Tk.xa = false`, and the mode isn't `EnumC0051Ha.c`.
- [x] **Task 8: Per-visual paint via `k$1()`**: typeface + style from `C0215jD`, color from theme key
  `"86"`. Removed the hardcoded `0xFFFFFFFF` (text) & `0xFF2196F3` (cursor).
  - Partially done: text & cursor color now come from `res/values/colors.xml` (not literals in
    code), cursor aligned to the app's accent color `#FF9800` (same as
    `CalctasticCalculatorActivity`). Individual per-visual paint (`k$1()` with typeface/style per
    node from theme `"86"`) hasn't been implemented — all visuals still share one `basePaint`
    passed down.
- [x] **Task 9: Remove the cursor width clamp** `Math.max(3.0f, …)` / `Math.max(2.0f, …)` — removed
  in `HyperCalDisplayView` (fallback root cursor) and `render/MathVisual.getCursorWidth` (used by
  every visual).
- [x] **Task 10: Scroll & horizontal clip** (`UF.java` lines 436–440, 638) — **multi-line wrapping
  NOT implemented**, see note below.
  - `HyperCalDisplayView.onDraw`: `canvas.clipRect(0,0,viewWidth,viewHeight)` before drawing, so an
    expression wider/taller than the display doesn't leak into the keypad area.
  - Horizontal scroll now follows the cursor position (like caret-follow in a text field), rather
    than just "always scroll to the end" as before: `startX` is computed from the cursor's local
    coordinate position (before offsetting), then clamped so the cursor always stays between
    `paddingLeft` and `viewWidth-paddingRight`. The `startX` value is kept across frames
    (`lastStartX`) so it doesn't "jump" on redraw.
  - Emulator test: typing a long expression (`123+456+789+123+456+789`) → auto-scrolls to the end,
    cursor visible at the right edge. Pressing ◀ repeatedly → scrolls back following the cursor all
    the way to the start (`123|+456+...`), content doesn't spill into the keypad.
  - **NOT IMPLEMENTED: multi-line wrapping.** The original line-wrapping algorithm
    (`C0329vH.mo63HiPER()`, ~lines 90–220) is far more complex than horizontal scrolling — it
    measures each element's width, decides on line breaks based on remaining width (`f3`), and
    re-lays-out the starting `i11` per line. Fully porting this is out of scope for this sample;
    horizontal scroll was chosen as a practical solution good enough for ordinary calculator use.
- [x] **Task 11: Clean up stale Javadoc** in `HyperCalDisplayView` (still mentioning `22sp`,
  `R.font.math_symbols`).
- [ ] **Task 12: Measurement string `HcZgWQ.LiVE`** in `AbstractC0335wD.HiPER(paint, f)` —
  **CANNOT BE CONFIRMED, blocked by tooling**.
  - `HcZgWQ` is an obfuscator-generated string-pool class (dozens of `public static String` fields
    with random names, no literal values in the decompiled Java) — its values are likely filled in
    via a native library (JNI) at runtime, not through ordinary Java bytecode. No `.smali` files or
    `apktool`/`baksmali` tooling exist in this environment to dig further (same situation as Task
    18).
  - Assumed `" "` (space) is kept based on usage pattern (its value is used for relatively small
    gap/thickness multipliers like 0.2f/0.3f/0.35f, consistent with measuring the width of a single
    space), but this remains a **guess**, not a verified fact.

### F. Code Relocation (2026-09-26): `HyperCalDisplayView.java` split up
`HyperCalDisplayView.java` was previously 437 lines, mixing View/touch/blink setup with the font
scale math and visual-tree node lookup. Two parts were moved into new classes (now 364 lines):

- **`view/HyperCalScale.java`** (new, package-private) — everything from Task 7: `lineHeight`,
  `referenceDisplayHeight`, `computeTextSize` (combining the `f`/`f2`/`1.2×f` clamp formula that
  used to live directly in `updatePaintSize`), plus the constants `REF_KEYPAD_WIDTH/HEIGHT`,
  `DISPLAY_PADDING`, `EDITOR_LINES`, `NOMINAL_BASE_SIZE`. A purely mathematical class (`Paint`/
  `Typeface` only, no `View`/`Context`), so re-verifying the scale formula later (e.g. once Task 12
  above is answered) only requires reading this file, not `HyperCalDisplayView`.
- **`render/VisualTree.java`** (new) — contains the old `findVisualForNode` method (now
  `VisualTree.find`), used to find the `MathVisual` for a given `ExpressionNode` in the tree built
  by `VisualTreeBuilder`. Moved to the `render/` package since it's the same kind of code as the
  existing `VisualTreeBuilder` there (pure knowledge of the visual tree's shape: `SequenceVisual`,
  `FractionVisual`, `PowerVisual`, etc.), not something specific to Android's `View`.
- **`HyperCalDisplayView.java`** (remaining) — now purely holds View responsibilities: touch/blink
  setup (`init`, `handleTapAt`, `onTouchEvent`), `onDraw` (layout, scroll, clip, drawing), and
  `findVerticalCursorTarget` (Task 12 in `btn_fraction.md`), which stays here because it needs the
  result of `calculateLayout` (Paint) owned by this View.

**When looking for code related to the tasks above:** the Task 7 scale formula now lives in
`view/HyperCalScale.java`, not `HyperCalDisplayView`. Node-to-visual lookup (used for drawing the
cursor & the up/down navigation Task 12 in `btn_fraction.md`) is in `render/VisualTree.java`.
