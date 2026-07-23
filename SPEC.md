# Tatara — Technical Specification v1.1

A single-user, offline-first Android app for tracking training, food, habits, and sleep.
No accounts. No server. No AI features. No cloud sync.

*Named for the traditional Japanese furnace that smelts iron sand into tamahagane — the raw
steel a blade begins as.*

---

## 0. Build constraints

| Decision | Choice | Why |
|---|---|---|
| Platform | Android only, native | Home-screen widget requires it |
| Language | Kotlin | Only real option for Compose/Glance |
| UI | Jetpack Compose (Material 3) | Declarative, fastest to vibecode |
| DB | Room (SQLite) | Local, typed, migration support |
| Widget | Glance (androidx.glance:glance-appwidget) | Compose API for widgets |
| Charts | Vico or hand-rolled Canvas | Avoid heavy chart libs |
| Distribution | GitHub Actions → signed APK → Releases | No Play Store |
| Min SDK | 26 (Android 8.0) | Glance + java.time |
| Target SDK | Latest stable |
| Notifications | **None.** Do not implement. | Explicitly out of scope |
| Network | **None**, except optional one-time DB seed | App must work in airplane mode forever |

### Repo layout

```
/app                     Android app module
/data/foods_seed.csv     Bundled food database (see §3.2)
/data/exercises_seed.csv Bundled exercise library
/data/insights.json      Insight card content (see §7.4)
/.github/workflows/      build.yml — assembleRelease, sign, attach to Release
/SPEC.md                 This file
```

### Signing

Generate a keystore once. Store it base64-encoded as a GitHub Actions secret
(`KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Never commit the
keystore. If the keystore is lost, upgrades over an installed APK will fail — back it up
somewhere other than the phone.

---

## 1. Navigation

Bottom navigation, five destinations. This is the maximum Android's bottom bar supports
legibly — do not add a sixth.

```
Dashboard  |  Food  |  Train  |  Habits  |  Sleep
```

State is preserved per tab. No nested bottom nav. Every primary action reachable in ≤3 taps
from the tab root.

---

## 2. Global rules

### 2.1 Retroactive editing window

**Entries may be created or edited for today, yesterday, and the day before. Nothing older.**

- Days older than D-2 render read-only with a lock icon.
- The date picker on any log screen is hard-bounded to `[today - 2, today]`.
- Rationale: the adaptive TDEE calculation (§3.5) reads completed weeks. Allowing edits to
  a week already used for an adjustment would silently corrupt the back-calculation.
- Exception: **body weight** may be back-filled up to 7 days, because a missing weight
  breaks the trend more than a late weight does. Back-filled weights are flagged and
  excluded from that week's TDEE calculation if entered after the Sunday recalculation.

### 2.2 Data export

- **Automatic:** on every app cold start, if the last export is >24h old, write
  `tatara-backup-YYYY-MM-DD.json` to `Documents/Tatara/`. Keep the last 14; delete older.
- **Manual:** a Share button on the Settings screen producing the same JSON via
  `ACTION_SEND` — send it to yourself, or commit it to the repo.
- **Import:** file picker, full replace with a confirmation dialog. Never merge.

### 2.3 Schema versioning

Every export file opens with:

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-07-23T09:14:00+05:30",
  "appVersion": "1.0.0",
  "data": { ... }
}
```

**What this is for:** in three months you will add a field or rename a table. An old backup
file will no longer match what the app expects, and importing it will crash or silently drop
data. The version number lets the import routine say *"this is a v1 file, I am now on v3,
run the v1→v2 and v2→v3 converters before loading."*

Rules:
- Increment `schemaVersion` on **any** change to the exported shape.
- Keep a `migrations/` folder with one pure function per step: `fun v1_to_v2(json): json`.
- Import chains them: `while (file.schemaVersion < CURRENT) applyNext()`.
- Refuse to import a file with a **higher** version than the app — tell the user to update.
- Separately, Room needs its own `Migration` objects for the on-device DB. Never use
  `fallbackToDestructiveMigration()`. That flag deletes everything on schema change.

### 2.4 Performance

- All DB access on `Dispatchers.IO` via suspend functions / Flow. No main-thread queries.
- Cold start to interactive: <400ms. There are no loading screens in this app; if one
  appears, something is wrong.
- Animations ≤200ms. No animation on the critical logging path.
- Aggregates (weekly totals, streaks, automaticity) are computed in SQL, cached in a
  `daily_rollup` table, and recomputed only for days that changed.

### 2.5 Design system — *Nie*

The whole visual language comes from one fact about a finished katana: **it is two steels
with a visible boundary between them.** The hard edge (*ha*) is bright, near-white. The
softer spine (*ji*) is dark blue-grey with visible grain. The wavy line where they meet is
the *hamon*, and the crystalline glitter along it is *nie*.

That boundary is the app's progress metaphor. Bright = earned. Dark = pending. The hamon is
the line between them, and it moves.

#### Palette

Dark only. No light theme — a blade is read by reflected light against darkness.

| Token | Hex | Role |
|---|---|---|
| `sumi` | `#0F1418` | Background. Charcoal, not black — has a blue cast |
| `ji` | `#232C33` | Surfaces, cards, the unearned portion of any indicator |
| `jihada` | `#33414A` | Borders, hairlines, chart gridlines |
| `tsuchi` | `#8C8070` | Clay. Muted text, disabled, secondary labels |
| `ha` | `#E9EFF3` | Primary text, the earned portion of any indicator |
| `hi` | `#C4562A` | **Fire.** Forge orange |
| `mizu` | `#6FA0BE` | **Quench.** Cold steel blue |

**The two accents are not decorative — they encode temperature, literally.** Steel is orange
while it is being worked and blue-white after it is quenched. So:

- `hi` = in progress, effortful, at risk, over budget, stalled, missed
- `mizu` = complete, earned, locked, on pace

Nothing in the app is `hi` permanently. Everything warm is something not yet done.

Pace indicator (§3.6) is the one place a third hue appears — amber sits between them
naturally, so use `hi` at 60% opacity rather than introducing a new colour.

#### Type

Three faces, three jobs. Bundle as variable fonts (~300KB total).

| Role | Face | Setting |
|---|---|---|
| Display | **Fraunces** | `SOFT=0, WONK=0, opsz=144`, weight 300–400 |
| Body / UI | **Inter Tight** | 400 / 500 |
| Data | **JetBrains Mono** | 400, tabular figures |

Fraunces at zero softness and maximum optical size collapses to hairline-thin strokes with
hard wedge serifs — it reads as *cut*, not written. Use it only for the tier name, the level
number, and screen titles. Never for body copy.

**Every logged number is monospace.** Weights, macros, reps, times. They are measurements
and should align in columns like a record, not flow like prose. This is the single most
load-bearing typographic rule in the app.

Avoid brush-script or faux-Japanese display faces entirely. The subject is metallurgy, not
calligraphy.

#### Structure

- **Vertical spine.** Every screen hangs off a 1dp `jihada` rule running down the left at
  24dp inset. Content aligns to it. A katana is a vertical object; the layout should be too.
- **Tapered dividers.** Section dividers are 1dp at the left edge, fading to 0 opacity at the
  right — modelled on the *bo-hi*, the fuller groove ground into a blade. Encodes direction
  of reading. Use nowhere else.
- **No cards with borders on all four sides.** Surfaces are defined by `ji` fill and a single
  top hairline. Boxed cards fight the spine.
- Corner radius: **2dp**. Not zero — that reads as brutalist broadsheet. Not 12dp — that
  reads as every other Material app.
- Tap targets ≥48dp. Primary actions in the bottom third — one-handed use.
- Charts readable without a legend. Axes in `jihada`, data in `ha`, the current value in
  `mizu`.

#### The signature: one blade, two readings

The Dashboard hero is a **single vertical blade silhouette**, roughly 90dp × 320dp, and it
carries both the daily and the lifetime state at once:

- **Its finish is your tier.** At Tamahagane it is a formless lump with a rough edge. It
  gains shape through Orikaeshi and Tsuchioki. At Yaki-ire the outline sharpens. At Hamon
  (level 45) the temper line appears for the first time. Through Togi it polishes — the
  surface gains reflectivity. At Mei a signature appears on the tang. At Kokuhō it is
  complete.
- **Its fill is today.** The hamon rises from the tip toward the tang as the day's
  requirements complete. Empty day, the blade is entirely `ji`. Ring closed, it is fully
  `ha` with a `mizu` hamon line.

One object, both timescales. **This replaces the daily ring** in §7.1 — the completion logic
is unchanged, only the rendering.

Draw it as a Compose `Canvas` path, not an image asset, so tier and fill are both parameters.
Nine tier variants × continuous fill.

#### Motion: the quench

**One animation, used everywhere, nothing else moves.**

When anything completes — a habit ticked, a set logged, a day closed — the element flashes
to `hi` for 60ms, then interpolates to `mizu` over 140ms with a decelerate easing. Bright,
then cool. That is what a quench looks like and it takes 200ms.

No other transitions. No shared-element flourishes, no parallax, no staggered list reveals.
The quench is the app's entire motion vocabulary, and it stays memorable because it is the
only one. Respect `Settings.Global.ANIMATOR_DURATION_SCALE` for reduced motion.

#### Voice

Cold, short, declarative. The app reports; it does not encourage.

- Empty food log: **"Nothing logged."** Not "Let's get started!"
- Missed habit: **"Missed. Second in a row."** Not "Don't worry, tomorrow's a new day."
- Tier crossing: **"Yaki-ire. Level 34 is now your floor."**
- Sat fat over: **"~34g saturated. Ceiling is 29g."**
- Stall flag: **"Bench e1RM flat for 3 sessions."**

No exclamation marks anywhere in the app. No emoji. Errors state what happened and what to
do, in one line.

---

## 3. FOOD

The hardest module. Build it second, after the data layer.

### 3.1 Text-first logging

A single text input at the top of the tab. Parse `quantity + unit + food`:

```
60g rice                → 60 grams, food "rice"
2 katori daal           → 2 portions, food "daal", portion unit "katori"
150 chicken breast      → 150 grams (bare number defaults to grams)
1.5 roti                → 1.5 units, food "roti"
3 eggs                  → 3 units, food "egg" (plural stripped)
```

- Units: `g`, `ml`, or any **named portion** defined on the food itself.
- Matching: case-insensitive fuzzy match (Levenshtein ≤2 or prefix match) against food names
  and user-defined aliases. Rank by frequency of use, then recency, then alphabetically.
- Ambiguity → inline chip row of candidates, one tap to disambiguate. Never a dialog.
- No match → "Create *chicken tikka*?" button, opening the custom food form pre-filled.

**Speed features (these matter more than the parser):**
- **Repeat yesterday** — clones the previous day's log wholesale.
- **Repeat meal** — clones any named meal from history.
- **Recent** — a horizontally scrolling row of the 15 most-logged foods, one tap to add at
  last-used quantity.
- **Saved meals** — name a set of entries ("post-gym", "office lunch"), log as one item.

### 3.2 Food database

**Seed source: IFCT 2017** (Indian Food Composition Tables, ICMR-NIN Hyderabad) — 528 Indian
foods, regionally composited. Available as an npm package (`ifct2017`), a Kaggle CSV, and a
JSON corpus. NIN explicitly encourages reuse of the data.

Trim to macros only and bundle as a CSV asset, seeded into Room on first run.

Supplement with USDA FoodData Central for anything IFCT lacks (protein powders, imported
goods) — but only what is actually needed. **Do not bundle the whole USDA database.**

**Schema:**

```
Food
  id              Long
  name            String
  aliases         String        comma-separated, for matching
  isCustom        Bool
  isEstimated     Bool          true = user-guessed, shows a • in the UI
  unitType        Enum          GRAM | PORTION
  portionName     String?       "katori", "piece", "roti", "scoop" — null if GRAM
  kcal            Float         per 100g if GRAM, per 1 portion if PORTION
  protein         Float
  carbs           Float
  fat             Float
  fatSource       Enum          see §3.3
  lastUsedAt      Instant?
  useCount        Int
```

**Custom foods are flat.** Name + four macros + unit type. No ingredient list required. This
is the primary path for restaurant food and for anything already estimated by hand.

**PORTION unit type is essential** — restaurant food cannot be weighed. A food defined as
`1 katori = 180 kcal / 9P / 22C / 6F` is logged as `2 katori daal`.

### 3.3 Saturated fat — derived, not entered

Saturated fat is **never typed as a number**. Each food carries one `fatSource` tag, and the
app derives saturated fat as a fixed percentage of that food's total fat.

| Tag | Sat % of fat | Examples |
|---|---|---|
| `COCONUT_PALM` | 85% | coconut oil, coconut chutney, palm oil |
| `DAIRY` | 65% | ghee, butter, paneer, cream, curd, cheese |
| `RED_MEAT` | 40% | mutton, keema, beef |
| `EGG` | 32% | eggs |
| `POULTRY` | 30% | chicken |
| `FISH` | 25% | all fish and seafood |
| `GRAIN_LEGUME` | 20% | daal, rice, atta, roti, oats |
| `SEED_OIL` | 15% | sunflower, mustard, groundnut, soybean |
| `NUTS` | 12% | almonds, peanuts, cashews, seeds |
| `MIXED` | 35% | default for restaurant food of unknown composition |

`satFat = fat × satPercent[fatSource]`

- Displayed everywhere with a `~` prefix to mark it as derived.
- Target: **under 10% of total calories** (US Dietary Guidelines). Show a secondary,
  stricter marker at 6% (American Heart Association) but do not warn on it.
- Warning fires only on the daily total, never per-entry.

### 3.4 Targets

| Macro | Rule |
|---|---|
| Calories | From adaptive TDEE (§3.5) |
| Protein | Fixed at user-set g/kg bodyweight. Default **1.8 g/kg** |
| Fat | Target **30%** of calories. **Floor 20%**, hard ceiling 35% |
| Carbs | Remainder |
| Fibre | 14g per 1000 kcal |
| Sat fat | <10% of calories (derived, §3.3) |

**The 20% floor is the load-bearing constraint, not the ceiling.** Below roughly 20% of
calories from fat, fat-soluble vitamin (A, D, E, K) absorption is impaired, and
meta-analytic evidence associates very low-fat diets with modestly reduced total
testosterone in men. The 30% target and 35% ceiling are budgeting decisions — fat is 9
kcal/g and displaces training carbs. The 20–35% band is the IOM's AMDR; the app should not
present the ceiling as a health threshold, because it isn't one.

Worked example at 2900 kcal:
- Fat target 30% = 870 kcal = **96.7g**
- Fat floor 20% = 580 kcal = **64.4g**
- Fat ceiling 35% = 1015 kcal = **112.8g**

### 3.4.1 What scales with bodyweight, and what doesn't

Every target recalculates on Sunday with no manual input beyond logged weight. But the
dependency is not uniform:

| Target | Depends on | Behaviour |
|---|---|---|
| Calories | Weight **change** (via TDEE back-calc, §3.5) | Not tied to weight level at all |
| Protein | Weight **level** (g/kg) | Ratcheted — see below |
| Fat floor | Weight **level** (0.7 g/kg) | Scales down as weight falls |
| Fat target/ceiling | % of calories | Follows calories |
| Goal rate | Weight **level** (% bodyweight/week) | Scales down as weight falls |
| Carbs | Remainder | Absorbs everything else |

**All g/kg and %-bodyweight maths uses the EWMA weight, never the raw weigh-in.** A 1kg
water swing must not move the protein target by 2g.

**The protein ratchet.** Naïve `1.8 g/kg × current weight` is backwards during a cut: weight
falls, so the protein target falls — but protein requirements go *up* in a deficit, not
down. Fix:

```
proteinTarget = proteinPerKg × max(ewmaWeight over the current block)
```

Protein is pegged to the **highest** smoothed weight seen since the block started. It never
drops mid-cut. It only resets when the user explicitly starts a new block (a button in
Settings: "Start new block", which clears the ratchet to current weight).

The same ratchet does **not** apply to the fat floor — that one should track current weight
honestly, since it's an absorption threshold, not a performance target.

#### How weight propagates to targets

Protein and the fat floor are defined per kg, so they do scale with bodyweight — but
**targets never recalculate on a raw weight entry.**

```
raw weight log  →  feeds EWMA  →  EWMA feeds Sunday recalculation  →  new targets
```

Any g/kg calculation reads **`ewma`, never `weight`.**

Reason: raw daily weight swings 1–2kg on water alone. At 1.8 g/kg, a 1.5kg overnight swing
would move the protein target by 2.7g and shift every downstream number for the day. That is
chasing noise, and it makes the targets feel arbitrary — which is exactly what erodes trust
in them.

So: log weight daily, targets change weekly. One exception — if smoothed weight has moved
**more than 2%** since the last recalculation, offer a mid-week refresh as a dismissible
prompt. Relevant mainly in the first month or after a long gap in logging.

The weekly review shows the weight figure the targets were computed from, so the connection
is always visible.

### 3.5 Adaptive TDEE

Runs **every Sunday at 23:00** local, on the trailing 7 days.

**Inputs:** daily logged kcal, daily body weight.

**Weigh-in frequency is a hard requirement: at least 4 days per week, ideally daily, same
time, post-void, pre-food.** A single weekly weigh-in cannot drive this system — day-to-day
fluctuation from water, sodium, glycogen and gut contents runs ±1–1.5kg, which is larger
than a full week's actual change on a 0.5% cut. One reading per week is mostly noise, and
feeding it into the back-calculation produces targets that swing wildly in the wrong
direction. If fewer than 4 weights are logged in a week, **skip the adjustment entirely** and
say so plainly on the review card.

**1. Smooth the weight.** Exponentially weighted moving average, α = 0.25:

```
ewma[0] = weight[0]
ewma[n] = 0.25 × weight[n] + 0.75 × ewma[n-1]
```

Raw daily weight is water noise. Never adjust on raw weight.

**2. Back-calculate maintenance:**

```
deltaKg  = ewma[end] - ewma[start]
tdee     = meanDailyKcal - (deltaKg × 7700 / daysElapsed)
```

7700 kcal ≈ 1kg of body mass. This is an approximation and it does not need to be exact —
it is applied consistently, so systematic error cancels across weeks.

**3. New calorie target:**

```
weeklyRateKcal = goalRatePercent × bodyweightKg × 7700
newTarget      = tdee ± (weeklyRateKcal / 7)
```

`goalRatePercent` is user-set as % bodyweight/week. Defaults: **0.5% cut**, **0.25% gain**,
0% maintain.

**4. Redistribute macros:** protein from g/kg, fat at 30% (clamped to the 20–35% band),
carbs take the remainder.

**Guardrails — all mandatory:**
- Require **≥5 logged days** in the week. Fewer → skip, show "not enough data".
- **No adjustment in the first 14 days.** The app needs a baseline first.
- Cap any single weekly change at **±10%** of the current target.
- Cap absolute calories at [1.0 × BMR, 2.5 × BMR] as a sanity bound.
- If weight change contradicts the goal for 3 consecutive weeks, surface a plain note:
  *"Three weeks without movement. Either intake is being under-logged or the rate is too
  slow — pick one."*

**Show the arithmetic.** The weekly review must display every intermediate number: mean
intake, EWMA start/end, delta, implied TDEE, applied rate, new target. No black boxes.

**On estimated food:** restaurant estimates are systematically low — everyone under-counts
restaurant oil and ghee. This does not break the model. A consistent 15% undercount shows up
as a proportionally lower apparent TDEE and the targets self-correct. **Consistency of error
matters, accuracy does not.** Corollary: do not change estimation method mid-week, and if a
dry-weight-plus-penalty system is in use, do not change the penalty percentage mid-block —
that breaks the back-calculation for that week.

### 3.6 Fat pace indicator

**A fixed "warn me at 82g" threshold is wrong**, because 82g at 9pm with the day finished is
fine, and 82g at 1pm is a problem. The metric is the fat density of the calories remaining:

```
pace = remainingFatGrams / remainingKcal
```

| State | g/kcal | Colour | Message |
|---|---|---|---|
| Fat-light | > 0.045 | blue | "Room for fat — add oil or nuts" |
| On pace | 0.020 – 0.045 | green | — |
| Fat-loaded | 0.012 – 0.020 | amber | "Lean choices from here" |
| Spent | < 0.012 | red | "Chicken and rice territory" |
| Over | negative | grey | "Over on fat — tomorrow's problem" |

Mirror the same logic for protein, inverted: flag when *behind* pace late in the day.

**Display:** one horizontal bar under the calorie ring, with tick marks at the 20% floor and
30% target, plus a single line of text — *"1,240 kcal left, 14g fat — go lean."* That is the
entire feature. Do not build a dashboard for it.

### 3.7 Recipe builder (optional, low priority)

For home-cooked food only. Build once from ingredients, enter **final cooked weight**, app
computes per-100g macros and saves the result as a normal Food row. Thereafter it is logged
like anything else. Ten recipes covers most of a home rotation. Skip entirely if custom
foods are sufficient.

---

## 4. TRAIN

### 4.1 Data model

```
Exercise      id, name, muscleGroup, equipment
Routine       id, name, order            e.g. Push / Pull / Legs / Full Body
RoutineItem   id, routineId, exerciseId, targetSets, order,
              repRangeLow, repRangeHigh, incrementKg,
              currentWeightKg            progression state lives HERE
Session       id, date, routineId?, durationMin, notes
SetEntry      id, sessionId, exerciseId, routineItemId?, setIndex,
              weightKg, reps, rpe?, isWarmup
```

Seed the exercise library from `exercises_seed.csv` — around 80 common lifts. Fully editable.

**Rep range, increment, and progression state belong to the `RoutineItem`, not the
`Exercise`.** This matters for any split where a lift appears on more than one day — a
Push/Pull/Legs/Full Body rotation puts squat on both Legs and Full Body, and bench on both
Push and Full Body. Those slots are not the same lift in progression terms: the Full Body
instance is typically lighter, higher-rep, and lower-volume. Keying progression to the
exercise globally would make the two days fight each other, each undoing the other's
increment.

So: **each slot progresses independently.** Squat on Legs can be running 5×5 at 100kg while
squat on Full Body runs 3×10 at 70kg, and neither touches the other.

**The one thing that stays global is e1RM.** A 100kg×5 is the same evidence about strength
whichever day it happened on. e1RM history, PRs, and stall detection aggregate across all
slots for that exercise; only the working weight and rep target are per-slot.

**Routines are a cycle, not a calendar.** `order` determines the rotation and the app simply
advances to the next routine after each completed session. Do not bind routines to weekdays
— miss a Tuesday and a weekday-bound schedule desynchronises permanently, whereas a cycle
just resumes.

The Train tab's default state is a single large button: **the next routine in the cycle**,
with "Repeat last" behaviour (§4.2.1) already loaded. Anything else is one tap further.

### 4.2 Live logging

- Start a session from a routine, or freestyle.
- **Previous session's numbers shown inline** next to each set as ghost text. This is the
  single most important feature in this module.
- Rest timer auto-starts on set completion, per-exercise default duration, tap to skip.
- Plate calculator: given target weight and bar weight, show the per-side plate stack.
- One-tap "same as last set" to duplicate the previous entry.

### 4.2.1 Repeat session

The primary way a session starts. One button: **"Repeat last"** on any routine, or on any
past session in history.

- Loads the full session structure — every exercise, every set — with **weight and reps
  pre-filled from last time and fully editable.** No typing unless something changed.
- Weight fields carry the previous load. Change one and it propagates to the remaining sets
  of that exercise, which you can then override individually.
- If a double-progression trigger fired (§4.3), the pre-filled weight is already the
  incremented one, marked with a small `hi` dot so you know the app moved it.

**Guardrail: pre-filled sets are unconfirmed until tapped.** They render dimmed and are
**not saved, not counted toward volume, and not fed into e1RM or progression** until you tap
each one to confirm. One tap per set — still far faster than typing — but the app never
records a set you didn't do. Bail out mid-session and only confirmed sets persist.

"Same as last set" duplicates the previous set within the current session, already confirmed.

### 4.3 Progression — deterministic, no AI

**Double progression.** For each exercise with rep range `[low, high]`:

- Hit `high` reps on **all** working sets → increase load by `incrementKg` next session, and
  drop back to `low`.
- Otherwise → keep the load, aim for more reps.

**Estimated 1RM** (Epley):

```
e1RM = weight × (1 + reps / 30)
```

Track the best e1RM per exercise per session, plot the trend.

**Stall detection:** if best e1RM has not improved across 3 consecutive sessions for an
exercise, flag it and suggest a deload — drop to 90% of current load, rebuild. Suggestion
only; never auto-apply.

### 4.4 Views

- Session history, reverse chronological.
- Per-exercise page: e1RM trend line, best set, total volume by week, PR list.
- Weekly volume by muscle group (sets, not tonnage).

---

## 5. HABITS

### 5.1 The split — this is the core structural decision

Two lists in one tab, on separate segmented tabs.

**HYGIENE** — behaviours that are near-automatic or externally anchored. Tracked, feed the
daily ring, appear on the widget. **They cannot cost XP or levels.**

- brush (morning), skincare (morning), micronutrient pills
- brush (night), skincare (night)
- bath

**HABITS** — genuinely effortful, cue-dependent, still being built. **These and only these
drive XP, levels, and the automaticity engine.**

- mindfulness 10 min
- pray 10 min
- journal
- read 30 min

Rationale: if six guaranteed wins are inside the level calculation, the consistency figure
floors around 60% permanently and stops carrying information. Failure is only informative on
the hard four.

**Hard cap: 5 items in HABITS.** Hygiene is uncapped. A habit reaching 95% automaticity
graduates to Hygiene automatically and frees its slot.

### 5.2 Stacks

Habits group into stacks; the widget and the Habits list render stacks as rows, not
individual checkboxes. Chaining is better practice than isolated tracking — a completed
prior action serves as the cue for the next, and preparatory habits raise habit strength.

| Stack | Items |
|---|---|
| **Morning** | brush · skincare · pills |
| **Night** | brush · skincare · journal |
| **Sit** | pray 10 · mindfulness 10 |
| **Solo** | bath · read 30 |

Four rows fit a widget. Eleven do not.

### 5.3 Per-habit fields

```
Habit
  id, name, list (HYGIENE|HABIT), stackId?
  cue           String   "after I wake", "after dinner"
  intendedTime  LocalTime?
  location      String?
  intention     String   "When I [cue], I will [habit]"
  automaticity  Float    0–100
  consecutiveMisses Int
  createdAt, graduatedAt?
```

The cue/time/location fields are not decoration. Context stability — same cue, same place,
same time — is a stronger predictor of automaticity than streak length, and the app scores
it (§5.6).

### 5.4 Automaticity engine

**Growth is asymptotic, not linear.** Early repetitions buy far more automaticity than later
ones. A flat ±1.6%/day is the wrong shape.

On a **completed** day:

```
automaticity += k × (100 - automaticity)      where k = 0.045
```

This yields ≈50% at 15 days, ≈80% at 35, ≈95% at 65 — consistent with the evidence, and it
self-limits so 100% is never reached.

On a **missed** day, penalty escalates with *consecutive* misses:

| Consecutive misses | Penalty |
|---|---|
| 1 | 0% — free |
| 2 | −2% relative |
| 3 | −5% relative |
| 4+ | −8% relative, each |

```
automaticity × = (1 - penalty)
```

The counter resets to zero on any completion.

**Why asymmetric:** the 2024 UniSA systematic review and meta-analysis (20 studies, 2,601
participants, *Healthcare* 12(23):2488) found median time to habit formation of 59–66 days,
means of 106–154 days, and individual variability from 4 to 335 days. A single isolated miss
does not meaningfully damage automaticity. A bad week should cost real ground; a bad Tuesday
should cost nothing.

### 5.5 Stages

Derived from automaticity, displayed as a label. Never regress a stage on one miss.

| Stage | Automaticity |
|---|---|
| Initiation | 0–29 |
| Learning | 30–59 |
| Stabilising | 60–84 |
| Automatic | 85–100 |

Show a permanent one-line footnote: *median 59–66 days, range 4–335. Variation is normal.*

### 5.6 Metrics

- **28-day consistency %** — the headline number. Not streaks.
- Current streak and longest streak — decoration only, shown small.
- **2 freeze tokens per calendar month.** Applying one to a missed day makes it neutral: no
  penalty, no XP, streak preserved. Unused tokens do not roll over.
- **Context stability score** — standard deviation of completion time-of-day over 28 days.
  Tighter is better. Display as a simple three-band label: Tight / Variable / Scattered.

**Streaks are deliberately demoted.** Breaking a long streak is a well-known abandonment
trigger; a rolling percentage degrades gracefully where a streak snaps.

### 5.7 Screen time — curfew only

**Total daily screen time is out of scope.** Screenzen handles it. No `UsageStatsManager`, no
`PACKAGE_USAGE_STATS` permission, no daily total, no trend chart. Delete all of it.

What remains is the **pre-sleep screen curfew**, which belongs to Sleep, not Habits (§6.4).

Two reasons it does not go in HABITS: it is an avoidance behaviour, and avoidance goals do
not build automaticity through the cue-response mechanism that §5.4 models. It also has a
real dependent variable — sleep quality — which makes it worth tracking as an *input to a
correlation* rather than as a thing that earns XP.

---

## 6. SLEEP

Its own tab. Not part of Habits.

### 6.1 Targets — editable

Set on the Sleep tab, editable at any time:

```
SleepTarget
  effectiveFrom   LocalDate
  targetBedTime   LocalTime     default 23:00
  targetWakeTime  LocalTime     default 07:00
  curfewMinutes   Int           default 90, range 30–180
```

**Store targets as a dated history, not a single mutable row.** Changing the target bedtime
must not retroactively re-judge past nights — a night is always evaluated against the target
that was in effect on that date. Editing the target inserts a new row with today's
`effectiveFrom`; it never overwrites.

The derived screen curfew starts at `targetBedTime - curfewMinutes`. Default 90 minutes puts
it at 21:30. The 1–1.5h range is the right one to enforce; anything shorter and the effect
gets hard to detect in the correlation.

### 6.2 Log

Manual entry: bed time, wake time, estimated time-to-fall-asleep, wake count, subjective
quality 1–5.

### 6.3 Computed

```
duration   = wake - bed - timeToFallAsleep
midpoint   = bed + duration/2
regularity = 100 - (stdDev(midpoint over trailing 7 days, in minutes) × 1.5), clamped 0–100
```

**Regularity, not duration, is the headline metric.** Consistency of sleep timing predicts
health outcomes more strongly than total duration does. Display regularity as the large
number and duration as secondary.

### 6.4 Screen curfew log

One entry per night, logged at bedtime or the next morning:

```
CurfewLog
  date            LocalDate
  curfewStart     LocalTime    snapshot of the target in effect that night
  lastScreenAt    LocalTime?   optional — actual time phone was put down
  held            Bool         derived if lastScreenAt present, else manual
```

Two ways to log, both one tap:
- **Binary:** "Held" / "Broke it" — the fast path.
- **Precise:** enter the actual last-screen time. `held = lastScreenAt <= curfewStart`.

Display on the Sleep tab as a 28-day strip: one mark per night, `mizu` for held, `hi` for
broken, `ji` for unlogged. Alongside it, one number: **nights held out of last 28.**

This feeds the ring and the correlation in §6.5. It earns **no XP and cannot affect levels.**

### 6.5 Hygiene checklist

Daily checkboxes: caffeine cutoff met · screen curfew held (§6.4) · room dark · room cool ·
no late large meal.

**Correlate the checklist against subjective quality over a trailing 30 days.** Report the
result as plain arithmetic:

> *On nights you hit the caffeine cutoff: average quality 4.1 (n=19). On nights you didn't:
> 3.2 (n=8).*

Point-biserial correlation is sufficient. Do not report significance, do not claim
causation, and suppress any comparison with fewer than 5 observations on either side. This
is the "cold maths" for sleep — one honest table, no interpretation layer.

---

## 7. DASHBOARD

### 7.1 The daily blade

Rendered as the blade hero described in §2.5 — the hamon rises from the tip as the day
completes. Not a ring.

Complete when **all three** are met:
1. Calories within ±10% of target
2. All HABITS items complete (hygiene not required)
3. Sleep logged

History renders as a 365-day contribution heatmap, `ji` → `ha`, laid out horizontally under
the blade.

### 7.2 XP and levels

Levels 1–100. XP cost to reach level *n*:

```
cost(n) = 80 + 15n
```

Total to level 100 = 83,750 XP. A strong day is ~120 XP, so level 100 lands around two years
of high consistency.

**Earning (with diminishing returns to prevent gaming):**

| Action | XP |
|---|---|
| Ring closed | 40 |
| Each HABITS item completed | 12 |
| Workout logged | 30 × dr(n) where n = workouts this week |
| Sleep logged | 8 |
| Weight logged | 5 |
| Weekly review opened | 25 |

Diminishing returns: `dr(n) = 1 / (1 + 0.35 × (n - 1))`. Fourth workout of the week is worth
noticeably less than the second.

**Losing.** Missed HABITS items cost XP on the same escalating scale as §5.4: first
consecutive miss free, then −2%, −5%, −8% of current-level XP. Hygiene misses and screen
time overruns cost nothing.

Levels can fall. XP debt below the current level's threshold demotes.

### 7.3 Tiers — the katana ladder

A blade is made by folding the same steel back on itself, over and over. Repetition is the
process, not a means to it.

| Levels | Tier | Meaning |
|---|---|---|
| 1–11 | **Tamahagane** | Raw ore from the furnace |
| 12–22 | **Orikaeshi** | The fold |
| 23–33 | **Tsuchioki** | Clay applied before the quench |
| 34–44 | **Yaki-ire** | The quench |
| 45–55 | **Hamon** | The temper line emerges |
| 56–66 | **Togi** | The polish |
| 67–77 | **Mei** | The smith's signature |
| 78–88 | **Meibutsu** | A blade with a name and a history |
| 89–100 | **Kokuhō** | National Treasure |

*Note: these are process stages arranged as a ladder rather than historical ranks. Kokuhō is
real — a legal designation held by a small number of surviving blades.*

**Tier floors.** Entering a tier permanently locks its lower bound. Reach level 34 and 34
becomes the floor forever: sliding from 43 to 34 is possible, falling back to Tsuchioki is
not. Bad weeks cost levels; they cannot cost a tier.

Two required details:
- **Show the cliff before it arrives.** Within 2 levels of the floor, the tier badge draws a
  thin warning ring.
- **Tier crossings are permanent and loud.** Full-screen moment, date recorded, entry added
  to a Tiers page that only ever grows. It is the one thing in the app nothing can take
  away.

**Visual identity:** the tier badge is the blade at that stage of completion — formless at
Tamahagane, the hamon line appearing around level 45, fully polished and signed at the top.

### 7.4 Insight cards

**Not motivational quotes.** Rotating factual cards, one per day, on the dashboard. There
are no loading screens to put them on — a local Room app opens in ~100ms, and a loading
screen would mean something was built wrong.

Content lives in `/data/insights.json`. Seed set:

- Median time to habit formation is 59–66 days. The range across individuals is 4 to 335.
  Being slow is not being behind.
- The "21 days" figure traces to Maxwell Maltz's observations about self-image adjustment,
  not to habit research.
- Morning practices and self-selected habits show greater habit strength than evening or
  assigned ones.
- Context stability — same cue, same place, same time — predicts automaticity better than
  effort does.
- Preparatory habits (laying out kit the night before) measurably raise habit strength.
- Enjoyment is a determinant of maintenance, not a bonus. A behaviour you dislike needs more
  reps.
- Habit formation is a shift from prefrontal to striatal control. The behaviour gets cheaper
  because a different system runs it.
- Raw daily weight is mostly water. Only the smoothed trend carries signal.
- Sleep timing regularity predicts health outcomes more strongly than sleep duration.
- Consistency of logging error matters more than accuracy of logging.

Each card carries a source line. **Nothing enters this file that isn't defensible** — in
particular, "66 days is all you need" is not a supportable claim and must not appear.

### 7.5 Weekly review

Generated Sunday 23:00, presented as a card on Monday. This is where the payoff lands.

Contents:
1. Weight: start EWMA, end EWMA, delta, 8-week trend line
2. Adherence: days logged, mean intake vs target, protein hit rate
3. **New macro targets with the full arithmetic shown** (§3.5)
4. Habit movements: automaticity delta per habit, any stage changes
5. Sleep: mean duration, regularity score, checklist correlation if n≥5
6. Training: sessions, weekly volume, e1RM changes, stall flags
7. XP earned, level change, distance to next tier

Reviews are archived and browsable. Opening one awards XP.

---

## 8. WIDGET

Glance app widget, 4×2.

- **Four stack rows** (Morning · Night · Sit · Solo), each showing `2/3` progress.
- Tap a row → expands to individual items, tap to tick. Writes straight to Room, no app
  launch.
- Bottom strip: calories remaining, fat pace colour dot, current tier badge.
- Updates on data change via `GlanceAppWidgetManager.update()`.

Friction is the main determinant of whether a habit gets performed. One-tap ticking from the
home screen is the highest-leverage feature in the application.

---

## 9. Build order

Strictly sequential. Do not start a step before the previous one runs.

1. **Data layer** — Room entities, DAOs, migrations, JSON export/import with
   `schemaVersion`. Verify export → wipe → import round-trips losslessly. Nothing else is
   safe to build until this works.
2. **Food** — seed DB, text parser, custom foods with PORTION units, daily totals. Hardest
   module, build it while fresh.
3. **Adaptive TDEE + fat pace** — the weekly job and the pace indicator.
4. **Habits** — split lists, stacks, automaticity engine, freeze tokens.
5. **Sleep** — log, regularity, checklist correlation.
6. **Train** — library, session logging, double progression, e1RM.
7. **Dashboard** — ring, XP, levels, tiers, insight cards, weekly review.
8. **Widget** — last. It depends on everything above.
9. **CI** — GitHub Actions signing and release.

---

## 10. Explicitly out of scope

Do not build, do not suggest, do not add later without deleting something:

- Image or photo calorie recognition
- Any AI-generated insight, coaching, or natural-language summary
- Notifications or reminders of any kind
- Accounts, login, cloud sync, social features
- Micronutrient tracking
- Streak-based primary gamification
- Any network call on the critical path
- Total daily screen time tracking, in any form. `UsageStatsManager` is not used and
  `PACKAGE_USAGE_STATS` is not requested. Screenzen owns this.
- Screen curfew earning XP or affecting levels
- More than 5 items in the HABITS list
- More than 5 bottom-nav tabs
- A light theme
- Any animation other than the quench (§2.5)
- Emoji or exclamation marks in UI copy

---

## 11. Sources

- Singh B. et al. (2024). *Time to Form a Habit: A Systematic Review and Meta-Analysis of
  Health Behaviour Habit Formation and Its Determinants.* Healthcare 12(23):2488.
- Lally P. et al. (2010). *How are habits formed: Modelling habit formation in the real
  world.* European Journal of Social Psychology.
- Longvah T., Ananthan R., Bhaskarachary K., Venkaiah K. (2017). *Indian Food Composition
  Tables.* National Institute of Nutrition, ICMR, Hyderabad.
- Institute of Medicine — Acceptable Macronutrient Distribution Ranges (fat 20–35%).
- US Dietary Guidelines (saturated fat <10% kcal); American Heart Association (<6%).
- USDA FoodData Central — Foundation Foods and SR Legacy.
