# "a b/c" Button (Mixed Number)

Split into its own file (2026-09-26) from `btn_fraction.md` Section F, so specs in this folder stay
per-button (one file = one keypad button). For the plain "a/b" fraction and the basics of the
fraction AST model (`FractionNode`, `SequenceNode` slots, etc.), see `btn_fraction.md` — this file
only covers what's specific to the mixed number.

## A. Background

User request: add a mixed-number fraction button (integer + fraction, e.g. `1 2/3`), which our
previous implementation didn't have.

## B. Decompile findings (via research subagent, `android.core`)

- Not a separate class. HiPER uses the same generic node `C0067Lb` used for regular fractions, just
  with **3 children** (instead of 2): `L(0)`=integer part, `L(1)`=numerator, `L(2)`=denominator
  (`Rc.java:384-392`). The node kind is distinguished via the enum `EnumC0300sa.sa` (mixed) vs.
  `EnumC0300sa.mB` (plain fraction).
- The button is registered in `EnumC0209ia.java:485-487`, right after the a/b button — the
  label/drawable string can't be recovered (obfuscated), and no `res/*.xml` resource mentions
  "mixed"/"ab_c" (the only public hit, `rfMixedRB`, is a *result format* radio button, mixed vs.
  improper — not an input button).
- Non-CAS serialization: integer part + fraction are concatenated without an operator, separated by
  a thin-space character (`Rc.java:384-392`) — confirming this isn't `int + a/b` but a single
  combined token.
- Rendering/cursor navigation: `GA.java:93`, `C0067Lb.java:1004/1011/1118`, `AbstractC0033Df.java:721`
  treat `sa` and `mB` the same in almost every "is this a fraction?" predicate — so mixed numbers
  reuse the regular fraction renderer, with the integer part drawn as an extra piece.
  `C0197hd.java:734` (`iL==3 && sa`) is the branch specific to editing/DEL for this 3-child node.

## C. Our implementation

- `engine/model/FractionNode.java`: added an `integerPart` field (nullable `SequenceNode`), factory
  `FractionNode.createMixed(integerPart, num, den)`, `isMixed()`. Plain fractions are unaffected
  (`integerPart == null`).
- `engine/ExpressionEditor.insertMixedFraction()` (new button "a b/c"): the cycling follows the
  existing a/b pattern —
  - Cursor in the integer part → jumps to the numerator.
  - Cursor in the numerator → jumps to the denominator (same as plain a/b).
  - Operand before the cursor → the operand gets lifted into the **integer part** (not the
    numerator like plain a/b), numerator/denominator start empty, cursor goes to the numerator.
  - Nothing to lift → creates an all-empty mixed number, cursor in the integer part.
  - Consistent with the Task 20b decision (in `btn_fraction.md`): a completed fraction/mixed number
    counted as "operand before the cursor" also gets nested (not excluded), so `1/2` then `a b/c`
    produces `\frac{1}{2\ \frac{□}{□}}` — already tested on the emulator.
  - DEL (`deleteChar`) and unwrap (`unwrapFraction`) extended for the 3 slots: DEL in an empty
    numerator box (mixed) jumps to the end of the integer part; DEL in an empty integer-part box
    behaves like DEL before the fraction; unwrap includes the integer part + numerator tokens when
    the denominator is empty.
  - Left/right navigation (`moveCursorLeft/Right`) extended: the numerator's left boundary ↔ end of
    the integer part; the integer part's right boundary ↔ start of the numerator.
- `render/FractionVisual.java`: field `integerVisual`, drawn to the left of the numerator/
  denominator stack at full size (not scaled 0.8× like the numerator/denominator), with a small gap
  (`spaceWidth * 0.5f`) and vertically centered against the total height. The divider line
  (`barLeft`) is shifted right so it doesn't cut through the integer part. Hit-test adds a region
  for the integer part on the left side.
- `render/VisualTreeBuilder.java`: builds `integerVisual` from `frac.integerPart` when present.
- `render/VisualTree.java`: `find()` now also walks `frac.integerVisual` (not just
  numerator/denominator) — this turned into a bug at one point (see Section E below).
- UI: new button `btn_mixed_fraction` ("a b/c") in `activity_hypercal.xml`, next to `a/b`; the `%`
  button moved to the ▲/▼ navigation row (column 2) to make room. *(Update 2026-09-26: the top
  memory row was removed; %/▲/▼ are now in row 1 alongside x³/x⁻¹, see `btn_x_square.md` Section E.)*
- **Honest note:** the exact vertical/horizontal position of the integer part (scale, gap,
  baseline) is a reasonable reconstruction from the description "reuse the fraction renderer + draw
  the integer part extra on the left" — the exact pixel coordinates (the `Qg.java` mixed-number
  variant) weren't in the research report (the rendering method for the `sa` branch wasn't traced
  line-by-line like the plain fraction was). If more definitive evidence turns up later (e.g.
  smali), revisit this.
- Tested on the emulator: lifting an operand into the integer part, filling all 3 slots via button
  cycling, visual rendering (screenshot), chained DEL down to full unwrap (denominator → numerator
  → integer part → gone), nesting a/b → a b/c, and regression of the plain fraction flow (unchanged).

## D. Bug found & fixed (2026-09-26)

After the initial implementation, the cursor on a newly-created integer-part slot (`integerPart`)
incorrectly showed up at "Center after the fraction" (far right, full height) instead of in the
leftmost integer-part box. Root cause: `render/VisualTree.find()` (the helper mapping model node →
visual for the cursor) didn't yet know about the new `integerVisual` field, and only walked
`numeratorVisual`/`denominatorVisual`. When the cursor was set to `integerPart`, the lookup failed
(returned null), and `HyperCalDisplayView` fell back to rendering at the wrong position. Fixed by
adding `find(frac.integerVisual, target)` at the start of the `FractionVisual` branch in
`VisualTree.find()`. Re-tested on the emulator — the cursor now correctly lands in the integer-part
box.
