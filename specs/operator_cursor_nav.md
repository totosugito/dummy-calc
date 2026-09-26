# Bug: Cursor Navigation "Stuck" When Crossing an Operator (+, −, ×, ÷)

Not a per-button spec — this is a global cursor-navigation bug, applying to every `OperatorNode`
(`+`, `−`, `×`, `÷`, and any other operator represented by the same class).

## User report

On the expression `4 + 3`, moving the cursor left (◀) took **2 clicks** to pass the `+` sign — when
it should only take 1 (an operator is just a single symbol, with no content inside it to explore).
Confirmed by the user to also happen with the other operators (`−`, `×`, `÷`), not just `+`.

## Root cause (verified via step-by-step screenshots on the emulator)

`ExpressionEditor.moveCursorLeft/Right` used to give `OperatorNode` its own two cursor positions (0
and 1, via `afterNode()`'s default `CursorPointer(node, node.getLength())`), even though:
1. Both positions **render at the exact same visual point** (because `OperatorNode` doesn't
   override `getCursorPosition`, so it uses `MathVisual`'s default formula, which for an object as
   small as an operator symbol effectively doesn't distinguish "left" vs. "right" visually).
2. Left/right navigation **never actually uses the operator's own position** — once the cursor is
   "on" the operator (at either position), the next arrow press always jumps to the sibling
   before/after it. So that second position was pure dead weight that happened to render at the
   same spot as the boundary of the neighboring token — the user felt like 1 click "did nothing"
   (even though the model state did change, just invisibly).

The first attempt (just changing `afterNode()` so `OperatorNode` always sits at position 0, mirroring
`EmptyNode`) **wasn't enough** — it just moved the problem, rather than removing it: the "stuck"
click moved to a different pair of states (`CursorPointer(operator, 0)` vs.
`CursorPointer(previousNumber, end)`), which ALSO render at the same spot (the boundary between the
number and the operator). Only discovered after comparing step-by-step screenshots one by one — a
reminder of why visual verification matters, not just reading the code.

## Final fix

Operators now **never become a cursor stop of their own** — left/right navigation at the sequence
level (`ExpressionEditor.moveCursorLeft/Right`) detects when the sibling it's about to land on is an
`OperatorNode`, and immediately jumps ONE STEP FURTHER (to the sibling on the other side of that
operator), rather than stopping on the operator itself:
- `moveCursorLeft`: if the previous sibling is an operator, jump to `afterNode(the sibling before
  that)` (or to the start of the sequence if that operator is the first token).
- `moveCursorRight`: if the next sibling is an operator, jump to `enterFromLeft(the sibling after
  that)` (new helper, a generalization of the inline "how to enter from the left" check) — or to
  the end of the sequence if that operator is the last token (e.g. an incomplete `4 +`).

`afterNode()` still gives `OperatorNode` a fallback single position (like `EmptyNode`) for other
call sites (e.g. via `endOf`/unwrap fraction) that might happen to land on an operator — just in
case, not the main path of the fix.

## Verification

Tested on the emulator with step-by-step screenshots (not just reading the LaTeX text, since that
doesn't show cursor position): `4 + 3`, pressing ◀ three times from the end of "3" → each click now
produces a **clearly different visual position** (end-of-3 → start-of-3 → end-of-4 → start-of-4), no
more clicks that visibly stay in place. Also tested the ▶ direction (mirror), the other operators
(`−`, `×`, `÷`), edge cases (operator as the first/last token in an incomplete expression), and full
regression (fractions, mixed numbers, 1/x, x², xʸ, nested combinations) — all still correct, no
crashes.
