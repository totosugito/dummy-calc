# "xʸ" Button (General Power)

## A. Status in the original code

Exists in the original code, registered as token "POWY" (`EnumC0209ia.java:433`), node type
`EnumC0300sa.dd`. Unlike `x²`/`x³` (`K`/`qB`, see `btn_x_square.md`) which only store the base as a
single child with a locked constant exponent, node `dd` has **2 real children** (base & exponent,
both actual nodes readable by index — `EA.java` `m72HiPER` ±lines 2821-2833, `m74HiPER` ±line 1749)
— so its exponent is indeed designed to be editable, consistent with our implementation.

**But:** the button-click logic (which part becomes the "base" on click, whether the base gets
wrapped in parentheses, etc.) **isn't in the `android.core` source** — already searched across all
references to `EnumC0300sa.dd` (29 files) and the token "POWY" (3 files, all just string-template
parsers, not insertion code). This package is purely the CAS/math engine (tree construction &
evaluation), not UI/keypad code. So "what happens when the xʸ button is clicked" **can't be
verified from decompile** — the design below is based on direct confirmation from the user
comparing against the real app, not from source.

## B. Behavior (confirmed by the user against the real app, 2026-09-26)

User reported a bug: the xʸ button in our app produced wrong results — it was still using the old
logic (just wrapping `cursorPointer.node` as-is, with no parentheses). After checking against the
real app:

- **Clicking xʸ with nothing to lift → produces `(□)^□`** (the empty base is **wrapped in
  parentheses**, exponent empty) — not `□^□` without parentheses.
- **User confirmation:** these base parentheses appear **consistently following the a/b Case 1/2
  pattern** — if there's an operand right before the cursor, that operand gets lifted into the base
  (wrapped in parentheses, e.g. `5` → xʸ → `(5)^□`, cursor in the exponent). If there's nothing
  (start of expression / right after an operator), both an empty base AND an empty exponent show up
  as placeholder boxes: `(□)^□`, cursor in the base (so the user fills the base first, then moves to
  the exponent).
- This differs from `x²`/`x³`/`x⁻¹`, whose base is **not** wrapped in parentheses (see
  `btn_x_square.md`) — only `xʸ` always uses parentheses on the base.

## C. Implementation

`engine/ExpressionEditor.insertPower()` (rewritten, no longer going through the `insertPowerNode`
helper used by x²/x³/x⁻¹ — different base-selection & parens):
- Operand before the cursor (`hasOperandBeforeCursor`, same as a/b) → lifted, wrapped in
  `ParenthesisNode`, becomes the base of `PowerNode(ParenthesisNode(operand), empty exponent)`,
  cursor in the exponent.
- Nothing → `PowerNode(ParenthesisNode(NumberNode("")), NumberNode(""))` inserted at the cursor,
  cursor in the base (inside the parentheses).

Tested on the emulator:
- Fresh: `xʸ` → `()^{}` (debug LaTeX representation of `(□)^□` — the empty box is rendered as an
  empty string for `NumberNode("")`, not the `\square` symbol like `EmptyNode` in fraction slots;
  this is an old convention already used by `insertSqrt`/`insertParenthesis` before this session,
  not a new change).
- Lift: `5` → `xʸ` → `3` → `(5)^{3}`.
- After an operator (no operand): `2 +` → `xʸ` → `3` → `2 + (3)^{}` (the `3` goes into the base, not
  incorrectly lifting the `+`).
- Nesting: `1/2` → `▶▶` → `xʸ` → `3` → `(\frac{1}{2})^{3}` (a completed fraction gets lifted into the
  base too, consistent with the Task 20b nesting decision in `btn_fraction.md`).

## D. Bug found & fixed along the way: the `insertPowerNode` helper (used by x²/x³/x⁻¹)

When `insertPower`/`insertSquare` were first refactored into one shared helper `insertPowerNode()`
(before the xʸ-parentheses issue was known), the operation order was wrong: `PowerNode` was
constructed (which reassigns the base's `parent` to the `PowerNode` itself, via the constructor)
**before** checking whether `targetBase.getParent() instanceof SequenceNode`. As a result that check
saw the already-changed parent (now `PowerNode`, no longer `SequenceNode`), took the wrong branch,
and the old node never got removed from its parent sequence — so `5` → `x²` produced `55^{2}` (the
old "5" left in place, plus the same "5" also became the power's base).

Fixed by resolving the parent/index and calling `removeChild` **before** constructing the new
`PowerNode` (the same order as the original pre-refactor code). Re-tested: `5` → `x²` → `5^{2}`
(correct). This bug was purely our own ordering mistake during the refactor, not a decompile
finding.

## E. Follow-up bug (2026-09-26): the empty exponent box was oversized, and base↔exponent navigation didn't work

Two user reports after the empty-box placeholder was added for empty `NumberNode`s (see
`btn_1_per_x.md`... no wait, see the note in `render/NumberVisual.java`):

**E.1 Empty exponent box oversized.** Checked directly against `C0357yG.java` (the original
placeholder) via subagent: the box-height formula does use `0.9f * density` — an **absolute pixel
value that doesn't shrink** when the element is scaled down (unlike ascent/descent, which
automatically follow the text size scale). This is the original formula and we use exactly the
same one (faithful), BUT it was also confirmed: `C0294rh.java` (the original `NumberVisual`) **never
draws a box for an empty number at all** — the empty box in the original code only exists in
`C0357yG`/`QA`, and `C0311tf.java` (exponent layout) has no empty-box logic at all. So the
combination "empty box inside an exponent that's been scaled down" **has no counterpart** in the
original code to reference — HiPER most likely never shows an empty box in a superscript context at
all. Since we deliberately added this feature (so an empty `x^y` exponent also shows as a box,
consistent with a/b), the fix: multiply `0.9f * density` by the element's scale factor (`D`) in
`NumberVisual.calculateLayout`, so the box shrinks proportionally when nested in a small context
(exponent, etc.) — this is a **deliberate deviation** from the original formula, not a porting
mistake, since the scenario in question doesn't exist in the original code to copy from.

**E.2 Left/right/up/down navigation couldn't move between base↔exponent.** `ExpressionEditor.
moveCursorLeft/Right` previously only knew how to enter/exit fraction slots (`FractionNode`) — for
`PowerNode` (base/exponent) and `ParenthesisNode` (content), once `node.getParent()` wasn't a
`SequenceNode`, the function just `return`ed without doing anything. Added symmetric handling like
fractions: new helpers `startOfNode`/`endOfNode` (a generic version of `startOf`/`endOf` that takes
a single node rather than just a `SequenceNode`, because our implementation's power base/exponent &
parenthesis content aren't `SequenceNode`-wrapped like fraction slots), plus new branches in both
directions for:
- Entering/exiting a `PowerNode` at Center position (`position 0` = before, `1` = after), mirroring
  the existing `FractionNode` pattern.
- Entering/exiting a `ParenthesisNode` at Center too.
- Jumping from base to exponent (and back) when exiting either slot.

Because the xʸ base is wrapped in a `ParenthesisNode` (see Section C), exiting the base needs **2
presses** of ◀/▶ (one to exit the number content, one more to exit the parentheses) before entering
the exponent — this is a natural consequence of the nested structure (exactly like a nested fraction
needing several presses to exit every level), not a bug.

**Up/down navigation (▲/▼):** also added in `view/HyperCalDisplayView.findVerticalCursorTarget`, a
new branch for `PowerVisual` mirroring the existing `FractionVisual` branch — the difference is the
exponent is drawn ABOVE the base (not below like the denominator), so ▲ = base→exponent and
▼ = exponent→base (the reverse order of fractions).

Re-tested on the emulator: the exponent box is now proportional (compared before/after via
screenshot), ◀/▶ and ▲/▼ successfully move between base↔exponent for `x²` (plain base/exponent, 1
press per boundary) and `xʸ` (base wrapped in parentheses, 2 presses to exit the base), plus full
regression of fractions/mixed/1x/sqrt/parenthesis/nested combinations — all still correct.

## F. Follow-up bug (2026-09-26): cursor sticking INSIDE the box, not beside it

The user compared against the real app again: pressing ◀/▶ should place the cursor **beside the
box** (outside it, left/right), not stuck inside/against its border. Fraction boxes (`a/b`, via
`EmptyNode`/`PlaceholderVisual`) were already correct from the start — already using the default
`MathVisual.getCursorPosition` convention (index 0 → `-0.5×cursorWidth`, i.e. to the LEFT of the
box, else → `b.x + 0.5×cursorWidth`, i.e. to the RIGHT of the box). But the new boxes rendered via
`NumberVisual` (exponent/base of `xʸ`/`x²`/`x³`, `√` content, `(...)` content) **didn't** follow this
convention — `NumberVisual.getCursorPosition` always overrides with a per-character measurement
formula, and for empty text that falls to `return new PointF(0, m)`, i.e. **x=0**, right at the left
edge INSIDE the box (since the box is drawn starting from a small `insetX`, x=0 nearly touches/
overlaps the box's border), not outside the box like `PlaceholderVisual`.

**Fix:** `NumberVisual.getCursorPosition` now delegates to `super.getCursorPosition()` (the default
`MathVisual`, the same one `PlaceholderVisual` uses) when the text is empty, and only uses the
character-measurement formula when there's actual content. Re-verified via screenshots (`√` and
`xʸ`): the cursor is now clearly outside, to the left of the box, same as fraction boxes.

## G. Follow-up bug (2026-09-26): ◀/▶ from the exponent box "stuck", and tapping an empty box

The user reported two things after Section F: (1) with the cursor to the left of an empty exponent
box, pressing ▶ couldn't move to the right of the box; (2) tapping inside an empty box couldn't pick
a left/right position based on tap location. Checked against the original code again via subagent:

**G.1 (tapping an empty box) — TURNS OUT THIS ALREADY MATCHES THE ORIGINAL CODE, NOT A BUG.**
`C0357yG.java:165-166` (`mo359HiPER`, the position-by-index method): for an empty box, this method
**always returns `(0,0)` regardless of index** — there's no branching by index at all. And
`C0357yG.java` also **doesn't** override the hit-test method (`AbstractC0335wD.HiPER(PointF,
bool,bool)`) at all — hit-test for QA is 100% inherited from the base class, which for a leaf with
no children also **doesn't** split left/right based on tap-x (`AbstractC0335wD.java` ±lines 75-207:
for a leaf with 0 children, it directly returns one single result, with no check of `pointF.x`
against the midpoint). Conclusion: **in the original code, tapping anywhere inside an empty box
always produces the cursor at the same position** — there's no left/right distinction for an empty
box. Our code (`PlaceholderVisual.hitTest` & `NumberVisual.hitTest` for empty text, both
unconditionally `return CursorPointer(node, 0)`) already matches this original behavior
**exactly**. No code changes for this point.

**G.2 (◀/▶ stuck in the exponent) — THIS IS A REAL BUG, now fixed.** Root cause: our old
`PowerVisual.getCursorPosition(index)` **delegated** index==1 ("Center after the power") to
`exponentVisual.getCursorPosition(0)` — i.e. ALWAYS to the start of the exponent, not the right edge
of the whole power. Checked against `C0311tf.java` (the original): this file **doesn't override
`mo359HiPER(int)` at all** — so index 0/1 on the power node itself (not on its exponent) purely uses
the default formula `AbstractC0335wD.mo359HiPER` (±lines 744-765): index 0 → left edge
(`-0.5×cursorWidth`), index 1 → right edge **using the power node's own width**
(`0.5×cursorWidth + this.b.x`) — NEVER delegating to any child. So the original architecture:
"Center after the power" should appear at the RIGHT edge of the entire `x^y` expression, not at the
start of the exponent box — exactly the same pattern as `Fraction`/`PlaceholderVisual` used
elsewhere (don't override, rely on `MathVisual`'s default).

**Fix:** removed the `getCursorPosition` override in `PowerVisual` entirely, back to the
`MathVisual` default (paralleling how `PlaceholderVisual` never overrode this method from the
start). Good side effect: this also fixed a similar unreported issue for `x²`/`x³` — the cursor
"after `x²`" (to continue typing) used to incorrectly appear at the start of the superscript "2",
now correctly appears at the right edge of the whole `x²`. Re-tested: before/after screenshots of ▶
from an empty exponent box (now clearly moves to the right edge of the whole expression), plus
continuing to type after `x²` (`5` → x² → `+` → `9` → `5^{2} + 9`, not incorrectly inserted in the
middle), and full regression.

## H. Follow-up bug (2026-09-26): tapping the canvas far to the right of the box still lands on the left of the box

After Section G, the user still reported: tapping the display (not the arrow buttons) to move to
the right of the box still didn't work. Tested directly on the emulator (not just reading code):
tapping the empty exponent box itself already worked correctly (always the same position — matches
G.1, faithful). But tapping **far to the right of the whole expression `(5)^{}`** (on empty canvas,
not on the box itself) **also** landed on the left of the exponent box, which makes no sense — it
should at minimum land at "Center after the power" (the right edge of the whole expression), same as
`FractionVisual`'s behavior for taps outside its content bounds.

**Root cause:** `PowerVisual.hitTest` had no boundary check (`activeLeft`/`activeRight`) like
`FractionVisual` does — the method just checked `point.x >= exponentVisual.HiPER.x` with NO upper
bound, so a tap any distance to the right was still considered "inside the exponent," and the empty
exponent always returns the same position (left of the box) — which is why it looked like it
couldn't move to the right.

**Fix:** added an `activeLeft`/`activeRight` check at the start of `PowerVisual.hitTest`, exactly
matching `FractionVisual`'s pattern (Section 7 of `btn_fraction.md`) — a tap outside the combined
base+exponent bounds returns `CursorPointer(pow, 0)` (before) or `CursorPointer(pow, 1)` (after),
and only when inside the bounds does it forward to base/exponent as before. Re-tested: tapping empty
canvas far to the right of `(5)^{}` now correctly lands at the right edge of the whole expression
(no longer stuck on the left of the exponent box); tapping inside the exponent box itself remains
consistent (single position, per G.1); full regression still correct.

## I. Change (2026-09-26): "()" removed as a real node, now purely render decoration

After Sections C–H, the user re-checked against the real app and realized: the `()` around the xʸ
base is **fake** — not real navigable parentheses (like the actual `(` `)` buttons). Agreed to
remove it from the model as a `ParenthesisNode`, replacing it with pure render decoration, using a
mechanism that already existed in the code: `PowerNode.needsParenthesesForBase()` (originally only
used by `toLatexString`, now also used for canvas rendering) — plus a new condition:
`"xʸ".equals(operationName)` always returns `true` (the xʸ base is always parenthesized regardless
of content, unlike the other conditions which only apply to cases like negative numbers or compound
expressions). `x²`/`x³`/`x⁻¹` are unaffected (their `operationName` isn't "xʸ").

**Code changes:**
- `ExpressionEditor.insertPower()` now delegates entirely to `insertPowerNode("", "xʸ", true)` —
  no more separate lift/`ParenthesisNode` branch. The base is now a plain operand, same as
  x²/x³/x⁻¹.
- `render/PowerVisual.java`: `calculateLayout` computes `parenW` (the parenthesis decoration
  width, same formula as `ParenthesisVisual`) when `needsParenthesesForBase()` is true, shifting the
  base's position right by `parenW` and adding to the total width. `draw` draws the left/right
  parenthesis arcs around the base (same arc formula as `ParenthesisVisual`) before drawing the base
  itself.
- Good side effect: ◀/▶ navigation between base and exponent for xʸ is now **1 press** (instead of
  2× as before when the base was really wrapped in a `ParenthesisNode`) — simpler & consistent with
  `x²`/`x³`.

**A serious bug found along the way (and fixed at the same time):** `insertPowerNode` (the helper
used by x²/x³/x⁻¹, and now also xʸ) turned out to **crash with a `StackOverflowError`** when called
on a truly empty expression (`cursorPointer.node` is `rootSequence` itself, not a token). The
`PowerNode` constructor reassigns the parent of `targetBase` (here, `rootSequence`) to the newly
created `PowerNode`, and that `PowerNode` then gets added as a child of the SAME `rootSequence` —
so `rootSequence` becomes a child of its own child (a cycle), causing infinite recursion in
`toLatexString`. This bug existed since `insertPowerNode` was first created, but only surfaced now
because x²/x³/xʸ were always previously tested after typing a number first ("5" then x²), never on a
truly empty state. Fixed: `insertPowerNode` now explicitly checks
`cursorPointer.node instanceof SequenceNode` (and the last node in root if that's also a
`SequenceNode`) → treats it as "nothing to lift," uses `insertAtCursor` with a fresh empty base
(not `rootSequence` itself), cursor to the base. Re-tested: `x²`, `x³`, `xʸ` all starting from an
empty state (without typing anything first) — no more crash, cursor lands correctly in the empty
base box.

## J. Change (2026-09-26): "()" removed entirely from xʸ's display

After Section I (the parentheses became render decoration, not a node), the user asked to go
further: don't show `()` at all for `xʸ`. Reverted the `"xʸ".equals(operationName)` condition just
added to `PowerNode.needsParenthesesForBase()` — this method returns to its original logic
(automatic parentheses only for cases that genuinely need them mathematically: negative base,
compound expression, operator), which is now shared identically by x²/x³/x⁻¹/xʸ with no distinction.
The render decoration mechanism in `PowerVisual` (Section I) was left unchanged — it now
automatically stops drawing parentheses because `needsParenthesesForBase()` returns `false` for an
empty base/plain number, as before. Re-tested: an empty `xʸ` is now `^{}` (not `()^{}`), `5` → `xʸ`
→ `3` is now `5^{3}` (not `(5)^{3}`); x²/x³/fractions/1x/combinations all still correct.

## K. Bug found & fixed (2026-09-26): operator lifted as the base — "5 + xʸ" → "5(+)^{}"

Found while checking that `xʸ` can now be nested inside another `xʸ`'s exponent (see
`specs/editable_slots_sequencenode.md` for that unrelated refactor): `5 + xʸ` (an operand, an
operator, then the general-power button) produced `5(+)^{}` instead of the expected `5 + ^{}`
(empty base+exponent — there's nothing before the cursor to lift, since the cursor sits right on
the `+` that was just typed).

**Root cause:** `PowerInserter.insertPowerNode()` (shared by `xʸ`/`x²`/`x³`/`x⁻¹`) decided what to
lift as the base with a weak check — `!(cursorPointer.node instanceof SequenceNode)` — which
doesn't exclude the cursor sitting directly on an operator. `ExpressionEditor.appendOperator()`
sets the cursor to `CursorPointer(op, 1)` right after inserting `+`, so this check let the `+`
`OperatorNode` itself get treated as a liftable base. `PowerNode.needsParenthesesForBase()` then
parenthesizes any bare `OperatorNode` base, producing the `(+)` seen in the output.

`FractionInserter` (`a/b`, `a b/c`, `1/x`) never had this bug because all three of its methods
already used the shared `CursorNav.hasOperandBeforeCursor()` helper, which explicitly excludes
`EmptyNode`/`OperatorNode`/`SequenceNode` and requires the cursor to be positioned *after*
something. `PowerInserter` had just never been switched over to using it — a leftover from before
that helper existed, not something introduced by any of the sections above.

There was also a second, redundant path to the same bug: a fallback branch
(`rootSequence.getChildCount() > 0 && !(lastChild instanceof SequenceNode)`) meant for the
"`cursorPointer.node` is the (empty) root sequence itself" case (e.g. right after
`ExpressionEditor#reset()`), but written to ignore cursor position entirely — so it re-grabbed the
same trailing `+` as a fallback "base" even after the primary check was fixed. It was dead code for
its actual intended case anyway (`cursorPointer.node == rootSequence` only happens when
`rootSequence` is empty, so `getChildCount() > 0` never held there), so it was removed rather than
fixed.

**Fix:** `PowerInserter.insertPowerNode()` now uses `CursorNav.hasOperandBeforeCursor(cursorPointer)`
for the base-selection check, matching `FractionInserter`; the redundant fallback branch was
deleted.

**Verified on emulator** (LaTeX-dump technique, see `specs/testing_via_latex.md`):
- `5 + xʸ` → `5 + ^{}` (fixed; was `5 + (+)^{}`)
- `5` → `xʸ` → `3` → `5^{3}` (operand-lift still works)
- `2 + x²` → `2 + ^{2}`, `2 + 1/x` → `2 + \frac{1}{}`, `2 + a/b` → `2 + \frac{\square}{\square}`
  (no regressions in the sibling buttons that already used `hasOperandBeforeCursor` correctly)
- `5` → `xʸ` → `xʸ` (pressed again with the cursor in the fresh empty exponent) → `5^{^{}}`,
  confirming `xʸ` can now nest inside another `xʸ`'s exponent (the SequenceNode conversion in
  `specs/editable_slots_sequencenode.md` working as intended at the model level).
