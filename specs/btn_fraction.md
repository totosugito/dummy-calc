# SPECIFICATION: "a/b" Button (Fraction)
**Target: 100% Faithful to the Original HiPER Calc**
**Original Decompile Reference Files:**
- AST Model: `android.core.C0067Lb.java`, `android.core.QA.java`, `android.core.EnumC0300sa.java` (`EnumC0300sa.mB`), `android.core.EA.java`, `android.core.ZB.java`
- Visual Renderer: `android.core.Qg.java`, `android.core.C0357yG.java` (Empty Placeholder Box), `android.core.PH.java`
- Touch & Cursor Caret: `android.core.AbstractC0335wD.java`, `android.core.UF.java`, `android.core.BE.java`
- Keypad Action & Navigation: `android.core.C0196hc.java`, `android.core.GA.java`

---

## 1. Variables & Data Storage (AST Model)
In HiPER Calc, a fraction isn't an ad-hoc class, but a nested function/operator:
- **Node type:** `C0067Lb` with operator type `EnumC0300sa.mB` (*FRACTION*).
- **Child list (`c`):**
  - Index `0`: `numerator` -> type `ZB`. If empty, holds a **`QA`** (*Empty Placeholder Node*).
  - Index `1`: `denominator` -> type `ZB`. If empty, holds a **`QA`** (*Empty Placeholder Node*).
- **Linear mode (`a`):** Boolean flag `a` on `Qg.java`: if the screen is narrow or linear mode is
  active, renders as `a/b` inline with a `'/'` character. If vertical, renders stacked
  $\frac{a}{b}$.
- **Nested indicator (`m$3()`):** `m$3()` is `0.0f` if this isn't a nested fraction, and
  `AbstractC0335wD.HiPER(paint, 1.0f)` if there's a fraction inside a fraction.

---

## 2. Reaction to Clicking the a/b Button (Keypad Insertion Action)
Based on `C0196hc.java` & `EA.m72HiPER`:
- **Case 1: A number/token exists right before the cursor (e.g. type `5` then press `a/b`):**
  - The `5` is automatically lifted into the **numerator** at index `0`.
  - An empty **denominator** of type `QA` is created.
  - **The cursor moves directly to the denominator** at index 0.
- **Case 2: No number before the cursor (empty display or right after an operator):**
  - A fraction is created with an empty `QA` numerator and an empty `QA` denominator.
  - **The cursor is placed in the numerator** at index 0.
- **Case 3: The cursor is currently inside the fraction (e.g. in the numerator):**
  - Pressing `a/b` moves the cursor to the denominator.

---

## 3. Empty Placeholder Box Renderer (`C0357yG.java`)
When the numerator or denominator is an empty node (`QA`), HiPER **doesn't leave it invisibly
empty**, but renders it via `C0357yG`:
- **Empty box dimensions (`C0357yG.java` line 72):**
  $$\text{boxWidth} = \text{paint.measureText}("0") \times 1.2\text{f}$$
  $$\text{boxHeight} = \text{paint.getTextSize}() \times 0.9\text{f}$$
- **Box style (`C0357yG.java` lines 204–218):**
  - Outline stroke thickness: `paint.measureText("0") * 0.1f` (Stroke style).
  - Inset margin: `f = 0.15f * paint.measureText("0")`.
  - Color: follows the secondary expression text color (`#80FFFFFF` or theme color).
  - Canvas command:
    ```java
    canvas.drawRect(left + f, top, right - f, bottom, paintStroke);
    ```
- **Cursor in the placeholder:**
  When the cursor is inside a `QA` node, the cursor blinks inside that box.

---

## 4. Font Size & Visual Scale
Based on `Qg.java` lines 34–63:
- **Fraction child font scale (`this.D`):**
  $$\text{childScale} = D \times 0.8\text{f}$$
  (Must be exactly $80\%$ of the base expression font size).
- **Font face:** Uses the `Arial` font (`Typeface.create("Arial", Typeface.NORMAL)`).

---

## 5. Layout & Line Geometry Algorithm (Exact, 100% `Qg.java` Math)
Based on `Qg.java` lines 113–142:
- **Total width (`b.x`):**
  $$\text{width} = (m\$3() \times 2.0\text{f}) + \max(\text{numW}, \text{denW})$$
- **Fraction bar Y position (`this.c` / `barY`):**
  $$\text{barY} = \text{numH} + (\text{measureText}(" ") \times 0.2\text{f})$$
- **Fraction bar thickness (`thickness`):**
  $$\text{thickness} = \text{measureText}(" ") \times 0.3\text{f}$$
- **Denominator position (`fHiPER3`):**
  $$\text{denY} = \text{barY} + \text{thickness} + (\text{measureText}(" ") \times 0.2\text{f})$$
- **Total height (`b.y`):**
  $$\text{height} = \text{denY} + \text{denH}$$
- **Expression baseline point (`this.m`):**
  $$\mathbf{m = ((-paint.ascent()) \times 0.4\text{f}) + barY}$$
  *(A $+$ or $\times$ operator beside the fraction aligns precisely centered on the fraction bar.)*

---

## 6. Cursor Caret Coordinates & Size
Based on `AbstractC0335wD.mo360HiPER()` & `UF.k(Canvas)`:
- **Cursor width:**
  $$\text{cursorWidth} = \text{paintChild.measureText}(" ") \times 0.35\text{f}$$
- **Cursor height in numerator / denominator:**
  Follows the $0.8\text{f}$ child font scale, so the cursor inside a fraction is $20\%$ smaller than
  the cursor outside a fraction.
- **Cursor outside a fraction (Center / main baseline):**
  Cursor height and width follow the full $1.0\text{f}$ scale, vertically centered exactly on the
  main baseline $m$.

---

## 7. Precise 2D Hit-Testing (Horizontal Bounds & Outer Slot Center)
Based on `Qg.java` lines 144–186:
- The numerator occupies the horizontal span $[\text{numX}, \text{numX} + \text{numW}]$ over the
  range $Y < \text{barY}$.
- The denominator occupies the horizontal span $[\text{denX}, \text{denX} + \text{denW}]$ over the
  range $Y \ge \text{barY}$.
- **Horizontal check:**
  - If the tap is **to the left of the fraction ($X < \text{margin}$)**:
    The cursor lands in the **parent sequence before the fraction** (`index = indexInParent`). The
    cursor sits at the **Center position (aligned with the fraction bar)**.
  - If the tap is **to the right of the fraction ($X > \text{fractionWidth} - \text{margin}$)**:
    The cursor lands in the **parent sequence after the fraction** (`index = indexInParent + 1`).
    The cursor sits at the **Center position (aligned with the fraction bar)**.
  - If the tap is within the horizontal range:
    - $Y < \text{barY} \rightarrow$ lands in the numerator.
    - $Y \ge \text{barY} \rightarrow$ lands in the denominator.

---

## 8. Cursor Navigation & Deletion (DEL / Left / Right)
Based on `C0067Lb.java` & `ZB.java`:
- **Right arrow (`btn_cursor_right`):**
  - From Center before the fraction $\rightarrow$ jumps to the start of the numerator.
  - From the end of the numerator $\rightarrow$ jumps to the start of the denominator.
  - From the end of the denominator $\rightarrow$ jumps to **Center right after the fraction
    $\frac{a}{b}|$**.
  - From Center after the fraction $\rightarrow$ jumps to the next token in the sequence.
- **Left arrow (`btn_cursor_left`):**
  - From right after the fraction $\rightarrow$ jumps to Center right after the fraction.
  - From Center after the fraction $\rightarrow$ jumps to the end of the denominator.
  - From the start of the denominator $\rightarrow$ jumps to the end of the numerator.
  - From the start of the numerator $\rightarrow$ jumps to **Center right before the fraction
    $|\frac{a}{b}$**.
  - From Center before the fraction $\rightarrow$ jumps to the previous token.
- **Delete (`btn_delete` / DEL):**
  - In the denominator: if it has a number, delete a digit. If empty (`QA`), move to the end of the
    numerator.
  - In the numerator: if empty and the denominator is also empty, delete the whole fraction. If the
    numerator is empty but the denominator has content, keep the numerator box visible.
  - If at Center after the fraction, then DEL: delete the denominator or unwrap the fraction.

---

## 9. Decompile Findings & Differences from Our Previous Code

### A. Empty Placeholder Box
- **Previous status:** We used `NumberNode("")`, which renders an empty string `""`, so no box was
  visible at all when the numerator/denominator was empty.
- **Original HiPER code (`android.core.QA.java` & `C0357yG.java`):**
  - Uses the `QA` node (`EmptyNode`).
  - Rendered by `C0357yG`:
    - `boxWidth = paint.measureText("0") * 1.2f`
    - Inset margin: `f = 0.15f * paint.measureText("0")`
    - Stroke thickness: `fMeasureText * 0.1f` with `Paint.Style.STROKE`
    - Canvas drawRect: `canvas.drawRect(left + f, top + fB, right - f, bottom + fB, paint)`
  - When a digit is typed on an `EmptyNode`, the `EmptyNode` gets replaced/filled with
    `NumberNode(digit)`.

### B. Cursor Position to the Left/Right of a Fraction (Center Slot Navigation)
- **Previous status:** Tapping or navigating near a fraction always forced the cursor into the
  numerator or denominator (`FractionNode.numerator` or `FractionNode.denominator`), and the cursor
  couldn't stop at the fraction-bar level (a Center position on the parent Sequence).
- **Original HiPER code (`Qg.java` lines 144–186, `ZB.java` mo265F & mo267HiPER,
  `AbstractC0335wD.java` mo359HiPER):**
  - **2D hit testing:**
    - If `point.x < activeLeft` (to the left of the numerator/denominator's active span): hit-test
      returns the cursor in the **parent Sequence before the fraction** (`indexInParent`, baseline
      Center aligned with the fraction bar).
    - If `point.x > activeRight` (to the right of the numerator/denominator's active span):
      hit-test returns the cursor in the **parent Sequence after the fraction**
      (`indexInParent + 1`, baseline Center aligned with the fraction bar).
  - **Cursor position & visual size:**
    - The cursor in the parent Sequence has full height $1.0\times$ and $Y = \text{Sequence.m}$
      (exactly aligned with the fraction bar/baseline of the `+` or `×` operator).
    - The cursor inside the numerator/denominator is $0.8\times$ (child scale) in size.

---

## 10. Implementation Task List & Status

- [x] **Task 1: Empty Placeholder Node (`EmptyNode.java` / `QA`)**
  - Create `EmptyNode` extending `ExpressionNode` to represent `QA.java`.
  - Implement the outline box rendering `PlaceholderVisual.java` (`C0357yG.java`) with
    `paint.measureText("0") * 1.2f` and STROKE style.
- [x] **Task 2: Integrate the placeholder into `FractionNode` & `VisualTreeBuilder`**
  - `FractionNode` defaults to `EmptyNode` for an empty slot.
  - `VisualTreeBuilder` maps `EmptyNode -> PlaceholderVisual`.
- [x] **Task 3: Precise horizontal hit-testing (`FractionVisual.hitTest`)**
  - Check the horizontal span: if the tap is outside $[ \text{numX}, \text{numX} + \text{numW} ]$
    and $[ \text{denX}, \text{denX} + \text{denW} ]$, return the cursor to the Center position in
    the parent `SequenceNode`.
- [x] **Task 4: Keypad & Cursor Navigation (`HyperCalActivity.java`)** — see Tasks 6–8 for follow-up
  fixes
  - `insertFraction()`: create `FractionNode(new EmptyNode(), new EmptyNode())` and move the cursor
    to the `numerator` (`EmptyNode`).
  - `appendDigit(char c)`: if the cursor is on an `EmptyNode`, replace the `EmptyNode` with
    `NumberNode(c)`.
  - `deleteChar()`: if all digits are deleted in the denominator, revert to `EmptyNode`. If DEL is
    pressed again on an `EmptyNode` denominator, move the cursor to the end of the numerator. If
    both numerator and denominator are `EmptyNode`, remove the fraction from the Sequence.
  - `moveCursorLeft()` & `moveCursorRight()`: support cursor transitions to the Center position in
    the parent Sequence before/after the fraction.
- [x] **Task 5: Emulator verification** — left/right tap verified 2026-09-26
  (`5 + [tap left]8 123/7 [tap right]9 + 3`)
  - Pressing the `a/b` button with an empty display $\rightarrow$ must show 2 outline boxes
    (numerator & denominator) with the cursor in the numerator box.
  - Type `7` $\rightarrow$ the numerator box becomes the number 7.
  - Press `a/b` or the right arrow $\rightarrow$ cursor moves to the denominator box.
  - Tap to the right / left of the fraction $\rightarrow$ cursor moves to the Center position
    aligned with the fraction bar.

---

## 11. Gap Analysis vs. the Original Code (2026-09-26) & Follow-up Task List

Result of re-comparing `Qg.java`, `C0357yG.java`, `QA.java`, `AbstractC0335wD.java` against our
implementation.

### A. Input Structure & Logic (done)
- [x] **Task 6: Numerator/denominator slot = `SequenceNode` (GA inside `C0067Lb`)**
  - `FractionNode.numerator/denominator` are now `SequenceNode`; an empty slot holds a single
    `EmptyNode`.
  - Operators/digits can now be typed inside a slot (`1+2` in the numerator). Previously
    `appendOperator` fell through to `rootSequence`.
  - New helpers in `HyperCalActivity`: `insertAtCursor`, `fractionOfSlot`, `startOf`/`endOf`/
    `afterNode`, `removeFromSequence`, `deleteBefore`.
  - Left/right navigation is now based on slot boundaries (start/end of sequence), not single-node
    identity.
- [x] **Task 7: `insertFraction()` after an operator = Case 2**
  - Only an operand right before the cursor (Number/Power/Sqrt/Paren/Fraction with position > 0)
    gets lifted into the numerator.
  - After an operator / on a placeholder / empty display → two empty boxes, cursor in the
    numerator.
- [x] **Task 8: DEL on an empty numerator while the denominator has content**
  - The numerator box stays visible, cursor moves to Center before the fraction (previously the
    last token of `rootSequence` got deleted along with it).
  - A number fully deleted in a slot reverts straight to `EmptyNode`.
  - DEL at Center before the fraction deletes one character/token before it (not the whole number).
- [x] **Task 9: Emulator verification** — `a/b 1 + 2 → 3` → `\frac{1 + 2}{3}`; `5 + a/b 7` →
  `5 + \frac{7}{□}`; DEL on an empty numerator → `7 + \frac{□}{3}` stays; `a/b DEL` → empty display.

### B. Not Yet Done
- [ ] **Task 10: Linear "a/b" mode** (`Qg.a`, `Qg.java` lines 96–126): render inline with `'/'`,
  children not scaled to 0.8, baseline `max(numM, -ascent, denM)`.
  - **STATUS: DISABLED / DEFERRED** at the user's request (2026-09-26). No plan yet; do not
    implement until explicitly instructed. The trigger scenario in HiPER (`Qg.a`, narrow screen or
    linear mode active from settings) hasn't been defined for this sample app.
- [x] **Task 11: Nested fraction padding `m$3()`**
  - `FractionVisual.calculateLayout`: `m3 = spaceWidth × 1.0f` when the numerator/denominator is
    itself a `FractionVisual`, else 0. `numX`/`denX` shift by `+m3`, `b.x = maxW + 2×m3`.
  - The fraction bar is still drawn `0..b.x` — since the numerator/denominator are already shifted
    `+m3`, the original's `fMin - m$3()` naturally returns to 0 and `F()` (the `b.x` estimate)
    already includes the new padding, so the formula ends up equivalent either way.
  - Emulator test: `a/b 1 ▶ a/b 2 ▶` → `\frac{1}{\frac{2}{□}}`, the outer line visibly widens to
    accommodate the nested fraction.
- [x] **Task 12: Up/down navigation** (`Qg.HiPER(PointF, Df)` / `Qg.E(PointF, Df)`): move
  numerator ↔ denominator, x clamped with margin `ZD.ab (0.2) × density`. Needs ▲/▼ buttons.
  - Done: new button row `btn_cursor_up` / `btn_cursor_down` (above ◀/▶) in
    `activity_hypercal.xml`; `HyperCalDisplayView.findVerticalCursorTarget()` finds the nearest
    fraction then hit-tests the opposite slot at the cursor's x. Test: `a/b 123 ▼ 7 ▲ 5` →
    `\frac{1253}{7}` (5 lands at the same x position).
- [x] **Task 13: Placeholder geometry exactly matching `C0357yG`**
  - `PlaceholderVisual`: `m = -ascent + 0.9 × density`, `b.y = descent + m`. The box is drawn from
    `y=0` to `y=b.y` (our version doesn't implement the vertical padding `L` from
    `AbstractC0335wD`, so `fB` is simplified to 0 — a deliberate simplification, not something
    based on the decompiled code).
  - The old formula (`textSize × 0.9`, box centered on `m ± ascent×0.9`) is no longer used.
- [ ] **Task 14: Hidden placeholder mode `C0357yG.E()` / `QA.B()` / `QA.D()`** — **NOT DONE,
  depends on a subsystem that doesn't exist in this sample**.
  - `C0357yG.E()` (re-read 2026-09-26): only checks `qa.D()`/`qa.B()` when
    `AbstractC0033Df.k.mo40HiPER() == EnumC0051Ha.HiPER` — some kind of global app mode (likely
    "function-argument fill mode", not regular calculation mode). Outside that mode, `E()` is
    always `true` (the box is never hidden).
  - `QA.B()`/`QA.D()` themselves contain fairly complex grammar checks: whether this slot is the
    last argument of a variadic function (`GA`/`C0067Lb` with `enumC0300sa.xa`), whether it's safe
    to delete without changing meaning, etc. — all depending on the function-argument AST structure
    (`GA`, error code `EnumC0300sa`) that has no counterpart at all in this sample's `engine/model`
    (`FractionNode`, `SqrtNode`, etc. are far simpler, with no concept of a "function with
    variadic/optional arguments").
  - Honestly implementing this would mean rebuilding a whole mode + function-argument-grammar
    subsystem this calculator sample doesn't need. Not a priority until a function/argument feature
    that needs this behavior comes along.
  - The ellipsis `"…"` (`qa.b()` / `HiPER()` method) is also part of the same subsystem (used when
    a function argument is hidden as a summary) — deferred along with it.
- [x] **Task 15: Line & box colors follow the theme, not hardcoded**
  - **Finding:** `AbstractC0335wD.HiPER(Paint, String)` (used by `C0357yG` for the placeholder box)
    returns the paint **as-is** when the highlight key being looked up doesn't exist in the theme
    map — i.e. the normal (not-highlighted) condition. So the placeholder box and fraction bar
    **use the regular text color**, not a separate secondary/transparent color like the old
    assumption (`#80FFFFFF`).
  - `PlaceholderVisual`: the box now inherits its color from `basePaint` (same as the text), no
    separate `setColor`.
  - `FractionVisual`: the fraction bar already inherited its color from the start (unchanged).
  - `HyperCalDisplayView`: text & cursor colors moved to `res/values/colors.xml`
    (`hypercal_text`, `hypercal_cursor`) so they follow the app theme, instead of hardcoded hex
    literals in code. Cursor aligned to the app's accent color (`#FF9800`, same as the cursor in
    `CalctasticCalculatorActivity`), replacing an unrelated blue.
  - Emulator test: cursor displays orange (matching the `=` button), placeholder box solid white
    like the text.
- [x] **Task 16: Remove the `Math.max(1.5f, …)` clamp** on gaps, line thickness (`FractionVisual`),
  and placeholder stroke (`PlaceholderVisual`). The cursor width clamp `Math.max(3.0f/2.0f, …)` in
  `MathVisual.getCursorWidth` and `HyperCalDisplayView` was also removed (bundled with Display
  Task 9).
- [x] **Task 17: Cursor geometry `mo359/mo360`**
  - `MathVisual.getCursorRect` (default) now exactly matches `AbstractC0335wD`'s base formula: full
    height `0..b.y`, width centered on `getCursorPosition(index).x`.
  - `MathVisual.getCursorPosition` default: index 0 → `-0.5w`, otherwise → `b.x + 0.5w`. Used by
    `FractionVisual` (Center before/after) and `PlaceholderVisual` (empty box).
  - **Finding:** `C0357yG` (placeholder) doesn't override `mo359HiPER`, so the cursor in an empty
    box actually sits at the left of the box (`-w..0`), not centered as before. Verified visually on
    the emulator (`a/b` on an empty display) — cursor exactly at the left edge of the numerator box.
  - `NumberVisual`'s between-digit cursor position stays as it was (not a confirmed decompile
    result — see note below).
- [ ] **Task 18: Verify `Qg.HiPER(PointF,bool,bool)` hit-test via smali** — **CANNOT BE DONE,
  blocked by tooling**.
  - JADX failed to decompile this method into Java; the remaining fragments suggest a comparison
    against the midpoint, not the children's min/max bounds like our implementation assumes.
  - Confirming this would require reading raw smali bytecode directly, but no `.smali` files or
    `apktool`/`baksmali` tooling exist in this environment (only JADX output under `temp/`). Would
    need to be regenerated from the original APK (`raw/` or `.apk`) outside this session to
    resolve.
- [x] **Task 20: a/b right after a fraction — REVERSED to nesting (2026-09-26), see Task 20b**
  - History: first implemented as "sibling" (`hasOperandBeforeCursor` excluding `FractionNode`) to
    fix an early complaint ("cursor exits the fraction, a/b ends up auto-filling the denominator").
  - Side effect of the sibling approach: two adjacent fractions ended up touching with no gap
    (original code `C0329vH.mo63HiPER()` line ~176: horizontal advance = `x += element width` only,
    with no gap mechanism between sequence elements — `mo358HiPER()`, which looked like a margin, is
    actually used for word-wrap decisions, not visual spacing, and `Qg`/Fraction doesn't override
    that).
  - After further discussion (see Task 20b): the spec text in Section 2 Case 1 ("a number/**token**
    before the cursor gets lifted into the numerator") doesn't exclude fractions → nesting is more
    consistent with that general rule, and automatically eliminates the spacing issue (no more two
    adjacent elements with no operator between them).
- [x] **Task 20b: a/b after a fraction → nesting (the old fraction becomes the new fraction's
  numerator)**
  - User's decision (2026-09-26): `hasOperandBeforeCursor()` no longer excludes `FractionNode` —
    a completed fraction is treated the same as any other token (Number/Sqrt/Power/Parenthesis).
  - Result: `a/b 1 ▶ 2 ▶ a/b 7` → `\frac{\frac{1}{2}}{7}` (no longer
    `\frac{1}{2}\frac{7}{□}`). The cursor after a/b lands directly in the new fraction's
    denominator.
  - Re-tested: DEL unwrap (Task 19), ▲/▼ navigation (Task 12), and the basic `1/2` case all still
    correct after this change.
  - Note: this reverses the original complaint ("a/b auto-fills the denominator" was considered a
    bug) — on reflection, that turned out to be the theoretically correct behavior, not a bug.
    Reference `C0196hc.java` lines 347–363 remains inconclusive (evaluation-transformation code, not
    the button action itself), so this is a design decision based on spec-rule consistency, not
    direct decompile evidence.
- [x] **Task 19: Fraction unwrapping** on DEL at Center after the fraction.
  - Empty denominator → the fraction is replaced by the numerator's content, cursor at the end of
    that content (numerator also empty → the fraction is deleted). Denominator has content →
    cursor moves into the end of the denominator (as before).
  - Test: `5 a/b ▶ DEL 3` → `53`; `2 + a/b 1 ▶ ▶ DEL` → `2 + 1`.

### C. Priority Order
1. ~~**Behavior (directly felt by the user):** Task 20 → Task 19 → Task 12 → verify left/right tap
   on fractions (remainder of Task 5).~~ ✅ done 2026-09-26
2. ~~**Display scale:** `display_scaling_typography.md` Task 7.~~ ✅ done 2026-09-26
3. ~~**Render accuracy:** Task 13 → Task 17 → Task 11 → Task 16 (+ display Task 9).~~ ✅ done
   2026-09-26
4. ~~**Theme colors:** Task 15 (+ display Task 8).~~ ✅ done 2026-09-26
5. **Additional features:** Task 10 (disabled/deferred at the user's request), Task 14 (deferred —
   depends on a subsystem that doesn't exist), Task 18 (blocked by tooling). Display Task 10
   (scroll+clip) ✅ done 2026-09-26, Display Task 12 (blocked by tooling).

### D. Remaining Work
Every task that can be done in this environment is done. What remains:
- **Task 10 (linear mode):** disabled/deferred at the user's request — do not work on it without
  explicit instruction.
- **Task 14 (hidden placeholder):** deferred — needs a mode + function-argument-grammar subsystem
  that doesn't exist in this sample.
- **Task 18 (hit-test verification via smali) & Display Task 12 (string `HcZgWQ.LiVE`):** blocked by
  lack of smali tooling (`apktool`/`baksmali`) in this environment, not a matter of effort.

### E. Code Relocation (2026-09-26): `HyperCalActivity.java` split up
`HyperCalActivity.java` was previously 599 lines, mixing Android keypad wiring with pure expression-
editing logic. All the insert/delete/left-right-navigation logic in this area (Tasks 6–9, 19, 20/20b)
**moved** into a new class:

- **`engine/ExpressionEditor.java`** (new) — contains: `appendDigit`, `toggleNegate`,
  `appendOperator`, `insertSqrt`, `insertFraction` (+ `hasOperandBeforeCursor`), `insertPower`,
  `insertSquare`, `insertReciprocal`, `insertParenthesis`, `deleteChar` (+ `deleteBefore`,
  `removeFromSequence`, `unwrapFraction`), `moveCursorLeft`, `moveCursorRight`, and the fraction-slot
  helpers (`fractionOfSlot`, `afterNode`, `startOf`, `endOf`, `insertAtCursor`). This class has no
  Android dependency (no `View`/`Paint`), so if a future task needs pure unit testing of the
  fraction logic, this is the place.
- **`HyperCalActivity.java`** (remaining 163 lines) — now just keypad wiring (`setupKeypad`,
  `onClick` delegating to `editor.xxx()`) and `moveCursorVertical` (Task 12), which **stays** in the
  Activity because it needs `HyperCalDisplayView.findVerticalCursorTarget` (requires the visual tree
  + `Paint`, can't move into the Android-free `ExpressionEditor`).

**When looking for code related to the tasks above now:** fraction insert/delete/left-right
navigation logic is in `engine/ExpressionEditor.java`, not `HyperCalActivity.java` anymore. Up/down
navigation (Task 12) stays in `HyperCalActivity.moveCursorVertical` +
`HyperCalDisplayView.findVerticalCursorTarget`.

### F. "a b/c" Button (Mixed Number)

Split into its own file (2026-09-26): **`specs/btn_ab_c.md`** — so specs in this folder stay
per-button (one file = one keypad button), instead of piling every fraction variant into one file.

### G. Cross-button TODO: empty boxes should be navigable left/right

Moved into a separate file (2026-09-26): **`specs/box_cursor_lr.md`** — this isn't specific to the
a/b button, but applies to every kind of empty box (fractions, `xʸ`, `√`, `(...)`), so it doesn't
belong in a per-button spec.

### H. Code Relocation (2026-09-26): `ExpressionEditor.java` split up again (730 lines)

`ExpressionEditor.java` had already been split once from `HyperCalActivity.java` (Section E), but
kept growing to 730 lines as more buttons were added (`a b/c`, `1/x`, `xʸ`/`x²`/`x³`/`x⁻¹`). Split
again by button family, not by individual button (too many helpers shared within a family to split
per-button without duplication):

- **`engine/CursorNav.java`** (new) — all the pure/static navigation & slot helpers shared across
  buttons: `fractionOfSlot`, `afterNode`, `startOf`/`endOf`, `enterFromLeft`, `startOfNode`/
  `endOfNode`, `hasOperandBeforeCursor`, `insertAtCursor`. Used by `ExpressionEditor` itself (DEL,
  left/right navigation) AND by the inserter classes below.
- **`engine/inserter/FractionInserter.java`** (new) — `insertFraction`, `insertMixedFraction`,
  `insertReciprocal` (buttons a/b, a b/c, 1/x — see `btn_ab_c.md`, `btn_1_per_x.md`).
- **`engine/inserter/PowerInserter.java`** (new) — `insertPower`, `insertSquare`, `insertCube`,
  `insertNegativeOnePower` + the private helper `insertPowerNode` (buttons xʸ, x², x³, x⁻¹ — see
  `btn_x_power_y.md`, `btn_x_square.md`).
- **`ExpressionEditor.java`** (down to 469 lines at that point) — delegates to the two inserter
  classes above, plus the simple buttons, DEL, and left/right navigation.

The inserter methods in `FractionInserter`/`PowerInserter` are `static`, take `rootSequence`/
`cursorPointer` as explicit parameters (not instance state), and return a new `CursorPointer` —
`ExpressionEditor` calls them and stores the result via `setCursor(...)`. No behavior changed at
all; fully re-tested: every fraction/power button, DEL/unwrap, operator navigation
(`specs/operator_cursor_nav.md`), and nested combinations — all identical to before the split.

**Further splitting (2026-09-26, same day):** the user asked whether DEL and left/right navigation
should also be split out. Checked first: both turned out to **never touch `rootSequence` at all** —
purely walking the tree via `node.getParent()` — so they could be extracted into pure static
methods (functions of `CursorPointer` alone), just like the existing `CursorNav` helpers, not just
private `ExpressionEditor` methods. Split further:

- **`moveCursorLeft`/`moveCursorRight`** moved **into `CursorNav.java`** (not a new file) as
  `CursorNav.moveLeft(CursorPointer)`/`CursorNav.moveRight(CursorPointer)` — not a "single-button
  feature" like an insert, but rather the top-level entry point for the lower-level helpers already
  in `CursorNav` (`afterNode`, `startOf`/`endOf`, `enterFromLeft`, etc.), so it made more sense to
  keep it in the same file rather than split it out again.
- **`engine/CursorDelete.java`** (new) — `deleteChar` + its private helpers (`deleteBefore`,
  `removeFromSequence`, `unwrapFraction`). A large enough, self-contained unit (~180 lines,
  including the unwrap/nesting logic from Task 19/20b), different from pure navigation, so it gets
  its own file mirroring the `FractionInserter`/`PowerInserter` pattern: static methods, take a
  `CursorPointer`, return a new `CursorPointer` (never `null` for "no change" — always returns the
  same cursor back, so the caller can always do
  `setCursor(CursorDelete.deleteChar(cursorPointer))` without a null-check).
- **`ExpressionEditor.java`** (now 146 lines) — purely holds state (`rootSequence` +
  `cursorPointer`) + one-liner buttons that don't need their own file (`appendDigit`,
  `toggleNegate`, `appendOperator`, `insertSqrt`, `insertParenthesis`) + delegation to all the
  classes above.

Fully re-tested again after this second split (regression across fractions/mixed/1x/power/sqrt/
paren/DEL unwrap/operator navigation/nested combinations, including step-by-step screenshot
verification for the operator navigation bug) — everything identical, no behavior change.

**When looking for related code now:** fraction/power buttons are in `engine/inserter/`; ◀/▶
navigation (and shared slot helpers) are in `engine/CursorNav.java`; DEL/backspace is in
`engine/CursorDelete.java`; `ExpressionEditor.java` is just thin orchestration.
