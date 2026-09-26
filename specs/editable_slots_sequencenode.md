# GLOBAL PRINCIPLE: Every Editable Slot Must Be a SequenceNode (GA)

This is not a per-button spec — it's a design principle that applies to **any node with a slot the
user can fill with an expression** (fractions, `xʸ`/`x²`/`x³`/`x⁻¹`, `√`, `(...)`, and any node
added down the line). Written 2026-09-26 after discovering: nesting an expression inside a node's
slot can silently fail (the insert ends up stuck at the end of the root expression instead of
landing in the intended slot) whenever that node's slot isn't modeled as a `SequenceNode`.

## Evidence from the original code (decompile, `temp/app/src/main/java/android/core/`)

- **`GA.java`** (`public final class GA extends ZB`, field `HiPER = "BinarySequence"`): a generic
  sequence holding a run of tokens joined by binary operators — exactly the representation we ported
  as `SequenceNode`.
- **`C0067Lb.java`**: EVERY operator kind (fraction, power, sqrt, parenthesis, etc.) uses the SAME
  generic class (`C0067Lb extends ZB`), distinguished only by `EnumC0300sa k` (the operator-type
  enum), with an `ArrayList c` (children) whose entries can be a `GA` (when that slot needs to hold
  a free-form expression) or an atomic node (`QA`/number) when the slot is genuinely atomic.

**Conclusion:** the original HiPER design uses `GA` as a UNIVERSAL wrapper for every slot that can
hold a free-form expression, regardless of operator kind. If a node has a slot that must be able to
hold an expression (including nested expressions: a fraction inside a fraction, a power inside a
power, etc.), that slot MUST be a `SequenceNode` (the equivalent of `GA` in the original code) — not
a single `ExpressionNode`.

## The rule

**Every slot a user can fill with a free-form expression must be typed as `SequenceNode`, not a
plain `ExpressionNode`.** An empty slot must still contain a single `EmptyNode` inside that
sequence (not a truly empty sequence with no content), so the placeholder box stays visible.

This applies to every editable slot on any node, now and in the future: base/exponent on a power
node, radicand/degree on a root node, content on a parenthesis node, and similar slots on whatever
nodes come next.

## Why this must be a global principle, not patched per node/button

1. **One insert/nav/delete path for every slot.** When every slot is consistently `SequenceNode`,
   the existing insert/navigation/DEL logic (`CursorNav`, the per-button-family inserter classes,
   `CursorDelete`, `insertAtCursor`) automatically applies uniformly to every slot through the same
   guard (`parent instanceof SequenceNode`) — no per-button logic needed for "what's allowed to nest
   here."
2. **No duplicate helpers needed.** A slot that isn't a `SequenceNode` forces the creation of a
   separate generic helper (a "single node" version of `startOf`/`endOf`) just to handle that case —
   duplicated logic that wouldn't be needed at all if every slot were consistently a `SequenceNode`
   from the start.
3. **New features automatically get full nesting.** A new node (e.g. square root) whose slots are
   made `SequenceNode` from the start can immediately hold any expression — including nested
   expressions — with no extra work, and without the risk of the "insert lands in the wrong place"
   bug that can happen if the slot were built as a plain `ExpressionNode`.

## Implementation impact (whenever a node needs an editable slot)

- The slot field on the node must be typed `SequenceNode`, using a `toSlot()`-style helper: an empty
  slot is automatically filled with a single `EmptyNode` inside the sequence.
- Rendering (the visual class for the related node): the visual field for that slot must be a
  `SequenceVisual` (not a single-node visual). `VisualTreeBuilder`/`VisualTree.find` **need no new
  code** — both are already generic over `SequenceNode`/`SequenceVisual` via the existing
  `instanceof SequenceVisual` branch.
- `CursorNav`/the related inserter: simply delegate to `startOf`/`endOf` (the `SequenceNode`
  version) to enter/exit the slot — no need for a generic "single node" helper or extra branching
  per parent-node type.
- Full regression needed every time this principle is applied to a new node: nesting across every
  expression kind, DEL/unwrap, left/right/up/down navigation, nested combinations.
