# "√x" and "ⁿ√x" (n-th root) buttons

## Starting point

`SqrtNode` (`degree`/`radicand` as `SequenceNode` slots, per
`specs/editable_slots_sequencenode.md`) and its render counterpart `SqrtVisual` already existed
before either button used a degree — `radicand`-only `√x` was already wired to `btn_sqrt`, and the
model/render code for `degree` was already written but never exercised by any button. Adding
`ⁿ√x` mostly meant finishing and fixing that already-present-but-untested code path, not building
a new feature from scratch.

## Radical is a PREFIX operator, unlike fraction/power

`FractionInserter`/`PowerInserter`'s Case 1 lifts whatever operand sits right before the cursor
(so `5` then `a/b` produces `\frac{5}{}`, `5` then `xʸ` produces `5^{}`). A radical never does
this — pressing `√x` always inserts a fresh, empty radical at the cursor, regardless of what came
before it (so `5` then `√x` produces `5\sqrt{}`, never `\sqrt{5}`). This matches the original
app's own behavior and is simply how `\sqrt{}` reads in normal math notation: it applies to
whatever comes AFTER it, not before. `SqrtInserter.insertSqrt()`/`insertNthRoot()` therefore never
call `CursorNav.hasOperandBeforeCursor()` the way the fraction/power inserters do.

## `SqrtInserter` (new, mirrors `FractionInserter`/`PowerInserter`'s shape)

- `insertSqrt(rootSequence, cursorPointer)`: fresh empty radical, cursor lands in the radicand.
  (Previously this logic lived directly in `ExpressionEditor.insertSqrt()`; moved out to match
  the fraction/power family's file layout, no behavior change.)
- `insertNthRoot(rootSequence, cursorPointer)`: fresh radical with BOTH an empty degree and an
  empty radicand. Cursor lands in the degree first (typed before the radicand, e.g. "3" then ▶
  then "8" for the cube root of 8) — the degree is read/typed first even though, unlike `xʸ`'s
  exponent, it isn't the LAST slot in reading order.

`ExpressionEditor` gained `insertNthRoot()` alongside the existing `insertSqrt()`, both now just
delegating to `SqrtInserter`.

## Navigation and deletion were already generic — no new code needed there

`CursorNav.moveLeft`/`moveRight` already had `SqrtNode` degree-or-radicand boundary handling
(written proactively during the original `SequenceNode` refactor, before any button used it) —
confirmed correct on first try, no changes needed. `CursorDelete`'s `leadingSlotOwner()`
escalation (the `specs/btn_fraction.md` Section L fix) also already covered sqrt's leading slot
generically. The one gap that DID need fixing:

### Fix: `SqrtNode` had no position-aware DEL dispatch (same latent bug `PowerNode` had before Section L)

Before this feature, `CursorDelete.deleteChar()` had no `instanceof SqrtNode` branch at all, so a
cursor resting directly on a sqrt (`CursorPointer(sqrt, 0)` or `(sqrt, 1)`) fell through to the
generic "delete the whole node regardless of position" fallback — deleting the whole radical even
when the cursor was at Center-BEFORE it (should instead delete whatever precedes it). Fixed by
adding the same `PowerNode`-style branch: position 0 → `deleteBefore(sqrt, ...)`, position 1 →
delete the whole radical (matching Section J's rule, now applied to sqrt too). Parenthesis still
doesn't get this dispatch (still not a finished feature — see `specs/btn_fraction.md` Section L).

## Render-layer bugs found and fixed (2026-09-26), all pre-existing but unreached until now

Wiring `ⁿ√x` up for real was the first thing to ever actually exercise `SqrtNode.degree` /
`SqrtVisual.degreeVisual` at runtime. Three bugs surfaced immediately:

1. **`VisualTree.find()` never searched `degreeVisual`** — only `radicandVisual`. This is used by
   `HyperCalDisplayView` to locate the visual for the current cursor (for drawing the caret and
   for ▲/▼ nav); a cursor inside the degree would never be found. Fixed by searching `degreeVisual`
   first (matching reading order), then `radicandVisual`.
2. **`SqrtVisual.hitTest()` never checked `degreeVisual`** — tapping the degree box would fall
   through to the radicand's hit test instead. Fixed by giving degree priority on its own
   horizontal span, mirroring `PowerVisual.hitTest`'s base/exponent shape.
3. **`SqrtVisual.getCursorPosition()` had a broken override** — it unconditionally delegated to
   `radicandVisual` regardless of the `index` argument, ignoring the Center-before/after
   `CursorPointer(sqrtNode, 0/1)` case entirely. This bug pre-dated the degree feature (it was
   wrong for plain `√x` too, just never actually hit because nothing had reason to rest the cursor
   directly on the sqrt node until DEL's new position-aware dispatch existed). Fixed by removing
   the override entirely — the default `MathVisual` behavior (index 0/1 → just outside this
   visual's own box) is exactly right, the same reason `PowerVisual`/`FractionVisual` don't
   override it either.

## Render-layer redesign: the radical sign itself looked wrong (2026-09-26)

Independently of the degree feature, the user flagged that `√8`'s radical sign looked visibly off
compared to normal math typesetting: a too-wide gap between the check-mark and the digit, and an
oversized "flag" overshoot at the top-left where the vinculum met the rising diagonal. The
class-level doc comment claimed "100% FAITHFUL TO HiPER Calc android.core.C0102Vh.java", but
`C0102Vh`'s own decompiled draw method is internally inconsistent — one line calls a zero-arg `k()`
as if it returns a `float` (`k() * 0.9f`), while another calls a same-named `k()` that clearly
returns an `AbstractC0335wD` (a child visual). These cannot be the same method; the decompiler has
collapsed two distinct obfuscated methods onto one printed name, and the existing Kotlin/Java port
had clearly already given up on a literal translation and picked its own (visibly poor) constants
instead. Rather than guess which original formula that first call belonged to, `SqrtVisual` was
rewritten with its own proportions:

- Tick (checkmark) width and the top margin above the radicand now scale with the RADICAND'S OWN
  HEIGHT (`tickHeight * 0.32f`, `radicandH * 0.22f`) instead of a fixed multiple of the font size
  unrelated to content — so a taller root (e.g. a fraction underneath) gets a proportionally
  wider/taller hook instead of a fixed-size one that looks cramped or oversized depending on
  what's inside.
- `calculateLayout()` now stores the exact geometry it computes (`tickWidth`, `tickHeight`,
  `thickness`, `startX`, `contentGap`, `vinculumY`) as instance fields, and `draw()` reuses those
  fields directly instead of recomputing its own (previously slightly different) copies of the
  same formulas from scratch — the old version computed `fTickWidth`/`fBottomY`/`fTopY`
  independently in both methods, which could silently drift out of sync.
- Added `contentGap` (a horizontal gap between the tick and the radicand) and increased the top
  margin so content never visually touches the radical sign's strokes -- the user's reference
  screenshot (a clean LaTeX-style rendering) showed clear breathing room on both sides that the
  previous version lacked entirely.

**Iterative tuning, verified via screenshot at each step** (see `specs/testing_via_latex.md` --
rendering questions need a screenshot, not just the LaTeX dump, since the LaTeX string can't show
whether a glyph LOOKS right): first pass shrank the tick and raised the vinculum to sit near the
radicand's own top (fixed the huge gap and the oversized flag overshoot), second pass tightened
the hook's droop and width further (`tickWidth` ratio `0.42f → 0.32f`) after the first pass still
looked slightly too wide/long.

## Render-layer redesign: degree placement (2026-09-26)

The user's own reference screenshot showed the degree (e.g. the "5" in "⁵√☐") floating ABOVE the
vinculum, near the top-left of the tick — standard LaTeX `\sqrt[n]{}` placement. The first
implementation instead put it at the BOTTOM of the tick (`tickHeight - degreeHeight`), which
looked wrong and didn't match. Fixed by reworking the layout to push the whole
tick+vinculum+radicand down by `vinculumY = degreeHeight` (reserving that much room above), and
placing the degree flush at this visual's own top (`y = 0`), so its bottom edge lands exactly at
the vinculum's height.

**A real clipping bug found mid-fix:** the first attempt at this used `vinculumY = degreeHeight *
0.55f` (intending the degree to slightly overlap down into the tick region for a tighter look),
which put the degree's own top at a NEGATIVE y-coordinate (`vinculumY - degreeHeight < 0`).
`MathVisual` has no notion of a child rendering "outside" its own declared `[0, b.y]` bounding box
— the negative-y portion of the degree was silently clipped by whatever drew above it (visible
on-device as the top of the degree digit being cut off right at the display view's edge). Confirmed
via screenshot with a wider crop margin (the clipping wasn't visible in a screenshot cropped too
tight around the expected content, which looked like the degree was just small and fine — a
reminder to crop generously when checking a NEW vertical extent, not just the previously-known
one). Fixed by using the FULL `degreeHeight` for `vinculumY` (no overlap), keeping the degree's
top pinned at exactly 0.

**Follow-up (2026-09-26): flush-top looked disconnected, user wanted left + vertically centered.**
Pinning the degree flush at `y=0` (bottom exactly touching the vinculum) matched the earlier
reference image's "stay above the radical" requirement but not its exact placement -- the
reference showed the degree sitting to the LEFT of the tick's own start and roughly vertically
centered over its rising stroke, not flush against the very top edge. Fixed by reserving MORE
vertical room than the degree strictly needs (`vinculumY = degreeHeight * 1.35f` instead of
`1.0f`) and centering the degree within that band (`y = (vinculumY - degreeHeight) / 2`), and by
introducing `degreeLeftShift = degreeWidth * 0.35f` that pushes the TICK's own `startX` right by
that amount (rather than giving the degree a negative x, which would repeat the exact clipping
bug just described) so the degree visually sits to the left of the tick instead of directly above
its start.

## UI

Added `btn_nth_root` (label `"ⁿ√x"`) to `activity_hypercal.xml`, in the same row as `btn_sqrt`
(row 2: `a/b`, `a b/c`, `√x`, `ⁿ√x`, `(`, `)`) — 6 buttons instead of the previous 5. `TableLayout`
with `stretchColumns="*"` handles the differing column count per row fine (each row's own buttons
just get proportionally narrower); confirmed via `uiautomator dump` bounds and a screenshot that
all 6 fit without overflow or overlap. Wired in `HyperCalActivity`: `btn_nth_root` →
`editor.insertNthRoot()`.

## Testing

`SqrtRegressionTest.java` (headless JUnit, see `specs/testing_via_latex.md`'s "even faster tier"
section) — plain `√x` never lifting a preceding operand, `ⁿ√x`'s degree-then-radicand cursor flow
and ◀/▶ navigation between them, DEL Center-after (whole-node delete) and DEL-before (deletes the
preceding token, not the radical), and nesting in both directions with fraction and power
(`\frac{1}{\sqrt{9}}`, `\sqrt{\frac{1}{4}}`, `\sqrt[3]{2^{5}}`). All passed on first run except the
DEL-before test, written correctly the first time by directly mirroring
`CursorRegressionTest`'s already-proven power equivalent.

Render fixes (radical shape, degree placement, the clipping bug) were NOT caught by these tests —
they can't be; LaTeX output looks identical regardless of how a glyph is actually drawn. Those were
found and fixed entirely through iterative on-device screenshots, per
`specs/testing_via_latex.md`'s "When this is NOT enough" section.
