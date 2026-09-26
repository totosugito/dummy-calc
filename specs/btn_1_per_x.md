# "1/x" Button (Reciprocal)

## A. Status in the original code

**Doesn't exist.** Already checked while working on the fraction button (`btn_fraction.md`) and
the mixed fraction (`a b/c`) — the button registration in `EnumC0209ia.java` for the fraction group
only contains entries for `a/b` (`EnumC0300sa.mB`, line 485) and `a b/c` (`EnumC0300sa.sa`, line
487). There's no third entry for "1/x". HiPER Calc has no dedicated reciprocal button on this
keypad — if the user wants `1/x`, the way to do it is to type `a/b` then manually fill the
numerator with `1`.

So the `btn_reciprocal` ("1/x") button in our sample is **purely our own addition**, not ported
from anything in the original source. Since there's no decompile reference for its behavior, the
design below was made to be **consistent with the existing fraction button conventions** (`a/b`,
`a b/c`), not derived from reverse engineering.

## B. Design idea

This button is essentially a "shortcut" for `1/(operand)` — so its behavior mirrors Case 1 of
`a/b` (the operand right before the cursor gets lifted), but the result places that operand in the
**denominator** (not the numerator, since the numerator is always `1`):

1. **There's an operand right before the cursor** (a number, fraction, sqrt, power, a completed
   parenthesis — checked using the same helper as `a/b`, `hasOperandBeforeCursor`) → that operand
   gets lifted into the denominator of a new fraction `1/operand`, cursor moves to Center **after**
   the fraction (ready to continue typing the next operator). Example: `5` then `1/x` →
   `\frac{1}{5}`, cursor after the fraction.
   - Also applies to already-completed fractions (consistent with the Task 20b decision in
     `btn_fraction.md`): `1/2` then `1/x` → `\frac{1}{\frac{1}{2}}`.
2. **Nothing to lift** (start of expression, right after an operator, on an empty box) → insert an
   empty `1/[]`, cursor lands directly in the denominator box so the user can type right away. Same
   as `a/b` Case 2.

Why not a "cycle" like `a/b`/`a b/c` (click again to move slots)? Because `1/x` only has 1 slot the
user can fill (the denominator — the numerator is always `1`, never empty for the user to fill), so
there's no second slot to cycle to. This differs from `a/b` (2 slots: numerator→denominator) and
`a b/c` (3 slots: integer→numerator→denominator).

Why not follow the `x²`/`xʸ` pattern (which always wraps a token, with no empty-box fallback)?
Because `x²`/`xʸ` in the original code always have a base (defaulting to `NumberNode("x")` if
empty), whereas fractions (including `1/x`) are designed in this sample to have an empty
placeholder box as a valid state (see `btn_fraction.md` Section 2) — so `1/x` at the start of an
empty expression should likewise show an empty box that can be filled, rather than forcing a
default base `"x"` that wouldn't make sense for a calculator.

## C. Implementation

`engine/ExpressionEditor.insertReciprocal()`:
- If an operand right before the cursor is detected (`hasOperandBeforeCursor`, the same helper used
  by `insertFraction` and `insertMixedFraction`) → the operand is detached from its parent sequence,
  wrapped as `new FractionNode(new NumberNode("1"), target)`, reinserted at the same position,
  cursor set to `afterNode(frac)` (Center after the fraction — the same helper used elsewhere).
- No operand → `insertAtCursor` an empty `1/[]` fraction, cursor in the `NumberNode("")` denominator
  (old behavior, unchanged).

Tested on the emulator:
- `5` → `1/x` → `\frac{1}{5}` (the number gets lifted into the denominator)
- `1/x` (empty state) → `3` → `\frac{1}{3}` (empty box fallback filled)
- `2 +` → `1/x` → `4` → `2 + \frac{1}{4}` (no operand before the cursor since we're right after an
  operator → empty box fallback, not incorrectly lifting the `+`)
- `1/2` → `1/x` → `\frac{1}{\frac{1}{2}}` (fraction nesting, consistent with Task 20b)
- `5` → `1/x` → `+ 3` → `\frac{1}{5} + 3` (cursor indeed lands after the fraction, ready to keep
  typing)

No changes to `render/FractionVisual.java` or any other render file — `1/x` just produces a plain
`FractionNode` (`integerPart == null`), so it renders exactly like a regular `a/b` fraction.
