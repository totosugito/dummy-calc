# General Testing Technique: Read the Debug LaTeX via `uiautomator dump`, Don't Screenshot

Not specific to any one button or feature — this is the general way to drive and verify emulator
tests for this app's expression editor, whatever button or bug is being tested.

## Why

`HyperCalActivity` already shows the current expression's `toLatexString()` output in a
`formulaTextView` at the top of the screen (see `HyperCalActivity.updateFormulaText()`) — added
originally for eyeballing during development, but it turns out to be a much faster and more
reliable way to verify behavior than taking and reading screenshots:

- **Exact, diffable output.** The LaTeX string is text you can compare directly against what you
  expect (`5^{3}`, `\frac{1}{2}`, `2 + \frac{\square}{\square}`, etc.), not something you eyeball
  off a rendered box on screen.
- **One round-trip per step**, instead of "screenshot → pull the file → crop → zoom → look at it."
- **Fewer mistakes locating buttons.** Tapping coordinates guessed from a screenshot is error-prone
  (this caused several mistaps while investigating a bug in this session, before switching to
  reading exact bounds from the view hierarchy — see the recipe below).

## An even faster tier: plain JVM unit tests, no emulator at all

The LaTeX-dump recipe below still needs a running emulator, `adb`, and (for navigation bugs) manual
counting of how many ◀/▶ presses it takes to reach a specific cursor state. But
`ExpressionEditor` and everything it delegates to (`CursorNav`, `CursorDelete`, the `*Inserter`
classes, all the `model/` classes) is **plain Java with no Android dependency** — no `View`, no
`Paint`, no `Context`. That means the exact same button-press sequences can be replayed directly
in a JUnit test running on the JVM, asserting straight against `editor.getRootSequence()
.toLatexString()` and `editor.getCursorPointer()` (a `CursorPointer(node, position)` — just compare
`.node` and `.position` directly, no rendering or screen involved).

See `app/src/test/java/com/calctastic/sample/hypercal/engine/CursorRegressionTest.java` (power
family), `FractionRegressionTest.java` (plain "a/b", mixed "a b/c", and one level of fraction/power
nesting in both directions), and `ComplexNestingRegressionTest.java` (three-four levels deep —
fraction raised to a fraction, power-of-power-of-fraction inside a fraction's denominator, a
variable-like symbol squared over another variable-like symbol, power-of-a-power, and deleting one
level out of a deep nest without disturbing the rest) — they replay button sequences from
`btn_x_power_y.md`/`btn_fraction.md`'s bug writeups (operand-lift-as-base, nested-power leftover
placeholder, whole-node delete, delete-before-wrapper, cursor-after-delete-near-operator) as
`@Test` methods. Run with:

```bash
./gradlew testDebugUnitTest --tests "com.calctastic.sample.hypercal.engine.*RegressionTest"
```

Sub-second, no adb, no emulator, no screenshot. When investigating a new cursor/insert/delete bug,
write the reproduction as one of these tests FIRST (it's usually 5–10 lines calling
`editor.appendDigit()`/`insertPower()`/`moveCursorLeft()`/`deleteChar()` etc.), confirm it fails,
fix the code, confirm it passes, then add it permanently to the suite so the bug can't silently
come back. Reach for the emulator/LaTeX-dump recipe below only once a test proves the model-level
logic is right and something is still visibly wrong — i.e. a rendering-layer question, which no
JVM test here can answer (see "When this is NOT enough" below).

**A real bug this surfaced, not just a testing convenience:** the first version of this test suite
failed in a way the emulator never showed. `NumberNode` keeps its own internal `cursorPosition`
field (used by `insertChar`/`deleteChar`), separate from the `CursorPointer` wrapper that
`CursorNav.moveLeft`/`moveRight` return. The two were only ever kept in sync as a side effect of
`HyperCalDisplayView.setCursorPointer()` running on every redraw — which happens to always occur
between two button presses in the real app, but doesn't exist at all in a headless JVM test. Fixed
by moving that sync into `ExpressionEditor`'s own private `setCursor()`, so every cursor change is
consistent regardless of whether a View is involved — a latent fragility in the "no Android
dependency" claim that this testing approach caught for free.

## Recipe

```bash
# 1. Launch the activity (once per session, or after reinstalling the APK)
adb shell am start -n com.calctastic.sample/.hypercal.HyperCalActivity

# 2. Dump the view hierarchy once to get every keypad button's exact tap coordinates
adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep resource-id
#   -> resource-id="...:id/btn_power" ... bounds="[651,962][857,1147]"
#      tap the CENTER of the bounds: x=(651+857)/2, y=(962+1147)/2

# 3. Press keypad buttons by tapping each button's center coordinate
adb shell input tap <x> <y>

# 4. Read the resulting LaTeX from the debug text view
adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep -o 'text="[^"]*"' | grep -E '\^|frac|sqrt'
```

Wrapping steps 3–4 in small shell helper functions (`tap() { adb shell input tap "$1" "$2"; sleep
0.3; }`, `getlatex() { ... }`) makes chaining a whole button sequence (e.g. "5", "+", "xʸ", "3")
and checking the result a few lines of bash.

## When this is NOT enough

The LaTeX string only reflects the **model** (the AST via `toLatexString()`), not the **render
tree**. It cannot catch rendering-layer bugs: wrong scale, wrong vertical position, a misplaced
cursor caret, overlapping boxes, etc. A rendering bug was in fact found in this app despite the
LaTeX output looking completely correct (`5^{^{}}}`, correctly nested) — a screenshot was needed to
see that the nested exponent wasn't actually being visually raised/shrunk on screen.

**Rule of thumb:**
- Testing *insert/delete/navigation logic* (did the right node end up in the right place in the
  tree?) → LaTeX dump is sufficient and much faster.
- Testing *rendering* (does it look right — size, position, cursor placement, spacing)? → still
  need a screenshot (`adb exec-out screencap -p > file.png`, then crop/zoom with e.g. Pillow to
  inspect a specific region at higher resolution).

Most keypad-button bugs in this app turn out to be insert/navigation logic bugs, so reach for the
LaTeX-dump technique first, and only fall back to screenshots once the LaTeX output already looks
correct but something still looks wrong on screen.
