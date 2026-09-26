# Cross-button TODO: Empty Box Should Be Navigable Left/Right

This is not a per-button spec — it's a separate file because the issue applies to **all kinds of
empty boxes** in the app: fraction slots (`a/b`, `a b/c`, see `btn_fraction.md`/`btn_ab_c.md`), base
& exponent of `xʸ`/`x²`/`x³`/`x⁻¹` (see `btn_x_power_y.md`, `btn_x_square.md`), `√` content, and
`(...)` content.

## Context

While working on `xʸ` (see Section F–H of `specs/btn_x_power_y.md`), we found & fixed a real bug
for empty boxes (cursor position & hit-test **outside the box bounds**, e.g. tapping far to the
right of the whole expression incorrectly landed on the left of the box). But after that the user
tested further and realized: **clicking INSIDE an empty box (left vs. right side of the box
itself) always produces the same position.** Re-verified on the emulator: tapping the left side vs.
the right side of an empty numerator box (`\frac{□}{□}`) produces identical cursor screenshots; the
same symptom also occurs in the `xʸ` box.

**This is not a bug** — already confirmed via decompile research (twice, for the fraction case and
for `xʸ`): `C0357yG.mo359HiPER` (the original cursor-position method for placeholder boxes) returns
the exact same point for any index when the box is empty, and this class also never overrides the
hit-test method at all — hit-test for an empty box is 100% inherited from the base class, which for
a leaf with no children also doesn't split left/right based on tap position. So our code already
behaves **exactly the same** as the original at this point, consistently across all box types
(fraction and `xʸ` alike) — not a new problem, just a consequence of how the original code simply
doesn't distinguish left/right for a truly empty slot.

## User's idea

Add a feature so an empty box can STILL have 2 distinct cursor positions (left & right):
- **Click in the box:** clicking the left side of the box → cursor goes to the left position;
  clicking the right side of the box → cursor goes to the right position.
- **◀/▶ arrow buttons:** one press = shift one position within the box (left↔right), only the
  next press exits the box to the neighboring element/parent slot.

This would be a **deliberate deviation from the original code** (HiPER itself has no such
distinction for empty boxes) — not a porting fix, purely for more precise/predictable UX.

## Estimated difficulty: medium

Needs changes in 3 places at once to stay consistent:

1. **Navigation (`ExpressionEditor.moveCursorLeft/Right`):** `EmptyNode` (used in fraction slots)
   is currently always forced to position 0 via `afterNode()` (`if (node instanceof EmptyNode)
   return new CursorPointer(node, 0);`) — needs to be treated similarly to `NumberNode`, which can
   move position-by-position (here just 2 positions: 0=left, 1=right), before continuing out to the
   parent sequence/slot.
2. **Hit-test (`PlaceholderVisual.hitTest` & `NumberVisual.hitTest` for empty text):** both
   currently `return new CursorPointer(node, 0)` unconditionally — needs to compare `point.x`
   against the box's midpoint (`b.x / 2`) to pick position 0 (left) or 1 (right).
3. **DEL (`ExpressionEditor.deleteChar`):** the DEL logic for fractions (Task 19/20b in
   `btn_fraction.md`, fairly intricate & already tuned after several iterations) assumes
   `EmptyNode` only has 1 position — needs to be reviewed so DEL while the cursor is at position 1
   (right side of the empty box) doesn't break the already-tested unwrap/nesting behavior.

## Consistency across box types

Fraction slots use `EmptyNode` (inside a `SequenceNode`), while the empty content of
`xʸ`/`√`/`(...)` uses a plain `NumberNode("")` (not `EmptyNode`) — the implementation needs to
behave the same for both representations so we don't reintroduce the kind of inconsistency seen in
earlier bugs in `btn_x_power_y.md` (Section F–H).

## Status

**Not yet implemented.** Waiting on an explicit decision from the user on whether this deviation
from original behavior is approved before implementation starts.
