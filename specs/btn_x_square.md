# "x²" Button (Square)

## A. Status: exists on both sides (our app & the original code)

Unlike `a b/c` and `1/x`, this button **already exists in our app** (`btn_square` in
`activity_hypercal.xml`, `ExpressionEditor.insertSquare()`) **and also exists in the original HiPER
Calc**. User request: check its exact original behavior before adding/changing anything on our
side.

## B. Decompile findings (via research subagent, string XOR-decode `EF.HiPER()`/`DC.HiPER()`)

- **Button registration:** `EnumC0209ia.java:431` — dedicated token "POW2", node type
  `EnumC0300sa.K`. Neighbors in the same table: `qB`=POW3 (line 432), `dd`=POWY / x^y (line 433). So
  x² is **not** syntactic sugar for a general power node with a literal "2" exponent child — it's
  its own distinct enum node, separate from x³ and from the general x^y.
- **Node structure:** node `K` **only has 1 child (base)**. The exponent value "2" is **not a
  stored child** — `EA.java` (`m74HiPER`, ±line 1885) returns a ready-made static constant ("2")
  whenever the exponent is needed, instead of reading a specific child index like the general power
  node does (`c0067Lb.L(HiPER(enumC0300sa))`). The exact same pattern applies to POW3 with the
  constant "3".
- **Consequently, the exponent "2" is LOCKED in the original code** — there is no exponent child
  node the user could click/edit. Switching to power 3 is done via the separate POW3 button, not by
  editing the existing x² exponent.
- **Internal conversion** (`C0196hc.java:255-267`): there's a conversion path between this compact
  form (K/qB) and the general power form with a literal exponent, but it's only used for certain
  display-mode needs (e.g. serializing to a "flat" form — see also `EB.java:929-934`), not something
  that happens from clicking the x² button itself.
- **Not verified** (obfuscation too deep to trace within this research budget): the exact
  target-base-selection logic (what exactly gets "lifted" as the base when x² is clicked — just the
  token right before the cursor, or possibly a larger expression), and the specific DEL/backspace
  handling for node K (whether there's an unwrap like fractions have). No evidence of a dedicated
  unwrap found in `C0067Lb.java:1004/1118`, `C0070Mc.java:266`, `EB.java:933` in this pass.

## C. Decision

Asked the user: follow the original code (lock the exponent, dedicated `SquareNode(base)` node with
no exponent child) or keep it editable as it currently is (a real
`PowerNode(base, NumberNode("2"))`)?

**User's decision (2026-09-26): keep it editable** (the recommended option). Reason: more flexible
for this sample calculator — the user can change the "2" to another number without retyping the
whole expression, and it's consistent with the `xʸ` button (`insertPower()`), whose exponent has
been editable since the start (stored as a real `NumberNode`, not a constant).

**So: no code changes.** `ExpressionEditor.insertSquare()` stays as it is — it creates a
`PowerNode(targetBase, NumberNode("2"), "x²")` whose exponent is a real child (navigable and
editable), not locked. This is documented as a **deliberate deviation** from the original code
(more permissive than HiPER), not a porting mistake.

## D. Remaining unverified points (if more precision is ever needed)

- Exact target-base-selection when x² is clicked (compared to our `insertSquare()`/`insertPower()`,
  which just takes `cursorPointer.node` as-is, including cases where it's an `EmptyNode`/
  placeholder).
- DEL behavior right after x² in the original code (unwrap to the base only, or just delete the
  whole node).

Both are blocked by the same obfuscation depth as Task 18/Display Task 12 in `btn_fraction.md`
(would need to read raw smali bytecode; `apktool`/`baksmali` isn't available in this environment).

## E. Update (2026-09-26): `insertCube()` (x³) & `insertNegativeOnePower()` (x⁻¹) added

Following the user's idea to make this "wrap base into a power" mechanism reusable, two new buttons
were added via the same helper (`ExpressionEditor.insertPowerNode`, private): `insertCube()` ("x³",
fixed exponent "3") and `insertNegativeOnePower()` ("x⁻¹", fixed exponent "-1"). Same as x²: the
exponent is *editable* (a real child `NumberNode`, not a locked constant like the original code's
`EnumC0300sa.qB`), the base is not wrapped in parentheses. ~~No UI buttons for either yet~~
**Update (2026-09-26):** UI buttons `btn_cube` ("x³") and `btn_neg_one` ("x⁻¹") are now in the first
row of `activity_hypercal.xml` — the memory button row (M+/M−/MC/MS/MR) was removed at the user's
request and its slots repurposed for x³, x⁻¹, plus %/▲/▼ moved up from the old navigation row.
Already wired up in `HyperCalActivity.onClick` → `editor.insertCube()` /
`editor.insertNegativeOnePower()`.

**Bug that briefly slipped in, then fixed:** when the `insertPowerNode` helper was first created,
the operation order was wrong and caused `5` → `x²` to produce `55^{2}` (the old node left behind in
the sequence). Root cause & fix details are in `specs/btn_x_power_y.md` Section D (found while
working on the `xʸ` button, but the bug lived in the helper shared by x²/x³/x⁻¹ here).
