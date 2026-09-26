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
