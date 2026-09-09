# Tatara — what the app does and how progression works

Based on the implementation released as **v0.1.2**, checked on September 26, 2026. This describes the current app, including implementation limitations; it is not a list of planned features. Nutrition and sleep formulas below describe app calculations, not personalised health advice.

## 1. The overall idea

Tatara is a single-user, offline Android app that combines food, training, habits, and sleep tracking with a shared progression system. Your logged actions earn XP. XP determines your level, and groups of levels form permanent tiers.

There are three different kinds of progress:

| System | What changes | What drives it |
| --- | --- | --- |
| Overall progression | XP, levels 1–100, and nine tiers | Scored activities and missed-habit penalties |
| Habit development | Automaticity, consistency, and habit stage | Each habit's completions, misses, and freezes |
| Training progression | Prescribed weights and performance history | Confirmed working sets and rep targets |

These are connected through your logs, but they are not the same score. A high sleep regularity score does not multiply XP, and lifting a heavier weight does not directly award more workout XP.

The app has five tabs:

| Tab | Purpose |
| --- | --- |
| Dashboard | Tier, level, animated hero world, XP to next tier, today's completion, history, research, and weekly reviews |
| Food | Food and meal logging, calories and macros, body weight, and adaptive nutrition targets |
| Train | Routines, prescribed sets, confirmed performance, weight progression, and workout history |
| Habits | Up to five developing habits, ongoing Hygiene routines, freezes, and habit metrics |
| Sleep | Bed/wake logs, duration, regularity, quality, screen curfew, and sleep-environment checklists |

Settings handles theme selection, profile and target settings, training blocks, and backup/import. There are no accounts, cloud sync, social rankings, or runtime AI coaching. The illustrated art is bundled in the APK; it does not require an AI service or network connection while you use the app.

## 2. The daily loop

1. Log food, habits, sleep, body weight, and any training.
2. The dashboard reflects today's three completion conditions.
3. You can correct ordinary entries for today, yesterday, and the day before.
4. When a day leaves that editing window, the app settles its XP and habit changes once.
5. XP changes your level; reaching a new tier records a permanent level floor.
6. Weekly reviews collect trends and explain nutrition adjustments.

**Most XP is delayed.** If today is Thursday, Monday and earlier are eligible to close; Tuesday, Wednesday, and Thursday remain editable. Closure runs when the app's activity is created/opened, not through a guaranteed midnight background job. Returning to an already-running app may not immediately rerun that processing.

The exception is opening an unread weekly review: its XP is awarded immediately. Today's completion animation is therefore not proof that today's XP has already been added.

## 3. XP: the scoring table

| Action | XP | Conditions |
| --- | ---: | --- |
| Complete one developing habit | +12 | Per habit, per closed day, while it is on the **Habits** list |
| Close the daily ring | +40 | All three daily conditions below must be satisfied |
| Log sleep | +8 | One sleep record for that date; duration/quality do not scale the award |
| Log body weight | +5 | A weight entry exists for the date when it closes |
| Log a workout | +30 for the first of the week | Later sessions receive diminishing returns |
| Open an unread weekly review | +25 | Once per review; reopening gives nothing extra |
| Miss a developing habit repeatedly | Variable negative XP | Explained in section 6 |

There is no separate XP award for each food entry, hitting protein, doing a heavier set, keeping a screen curfew, completing a sleep checklist, or ticking a Hygiene item. Food contributes to the +40 daily-ring award.

### Workout scoring

For workout number `n` in the Monday–Sunday week:

```text
workout XP = round(30 / (1 + 0.35 × (n − 1)))
```

| Session number that week | XP |
| --- | ---: |
| 1 | 30 |
| 2 | 22 |
| 3 | 18 |
| 4 | 15 |
| 5 | 13 |
| 6 | 11 |
| 7 | 10 |

Multiple sessions on the same day still count toward the weekly session number. The rate resets on Monday. XP is based on stored sessions, not workout duration, volume, or estimated strength. Finishing an empty session discards it, but day-close does not independently check a separate “finished” flag.

### Example earning rates

Assume every developing habit is completed, the daily ring closes, sleep and body weight are logged, and no penalties apply:

| Daily activity | XP before workouts/reviews |
| --- | ---: |
| Three developing habits + ring + sleep + weight | 36 + 40 + 8 + 5 = **89/day** |
| Five developing habits + ring + sleep + weight | 60 + 40 + 8 + 5 = **113/day** |

With three workouts and one newly opened review, those become **718 XP/week** and **886 XP/week**, respectively: add `30 + 22 + 18 + 25 = 95` to the seven daily totals.

These are examples, not fixed rates or guaranteed completion times. Early on there may be no calorie target, so the ring cannot close. Habits eventually graduate to Hygiene and stop earning +12. Misses reduce XP, and the number of active habits can change.

## 4. Levels: 100 in total

The app displays **Level 1 through Level 100**. There is no prestige/reset system after Level 100.

The cost associated with reaching level `n` is:

```text
cost(n) = 80 + 15n
cumulative XP for level L = 80L + 15L(L + 1) / 2
```

| Level reached | Cumulative XP threshold |
| --- | ---: |
| 1 | Starts here, even at 0 or negative XP |
| 2 | 205 |
| 5 | 625 |
| 10 | 1,625 |
| 12 | 2,130 |
| 25 | 6,875 |
| 50 | 23,125 |
| 75 | 48,750 |
| 100 | **83,750** |

**Level 1 has a special display rule:** the formula includes a 95-XP Level 1 cost, but the displayed level is never below 1. Consequently you need 205 total XP to first display Level 2, not just 110 XP from zero.

Later levels need more XP: going from Level 50 to 51 takes 845 XP; going from 99 to 100 takes 1,580 XP. Total XP can go negative, and the app does not erase that deficit just because the displayed level is clamped to 1 or to a tier floor.

## 5. Tiers and themes

There are **nine tiers**. The first eight span 11 levels each; the last spans 12. Themes share the same XP and progression rules. Switching themes changes the visual world and tier names, not your progress.

| Tier | Levels | XP to enter | Samskara | Nie | Astra |
| --- | --- | ---: | --- | --- | --- |
| 1 | 1–11 | Starting tier | Ārambha | Tamahagane | Agneyastra |
| 2 | 12–22 | 2,130 | Abhyāsa | Orikaeshi | Vayavyastra |
| 3 | 23–33 | 5,980 | Tapas | Tsuchioki | Nagastra |
| 4 | 34–44 | 11,645 | Sthiti | Yaki-ire | Varunastra |
| 5 | 45–55 | 19,125 | Saṃskāra | Hamon | Vajra |
| 6 | 56–66 | 28,420 | Dhyāna | Togi | Garudastra |
| 7 | 67–77 | 39,530 | Sthitaprajña | Mei | Brahmastra |
| 8 | 78–88 | 52,455 | Siddhi | Meibutsu | Pashupatastra |
| 9 | 89–100 | 67,195 | Svabhāva | Kokuhō | Narayanastra |

### Permanent tier floors

Once a tier crossing is recorded, its starting level becomes your minimum displayed level. For example, entering Abhyāsa makes Level 12 your floor. You may fall from Level 18 to 17 after losing XP, but you cannot fall below that recorded Level 12 floor.

The floor protects the level, **not the XP balance**. Lost XP must still be earned back before reaching later thresholds. The dashboard's “XP to next tier” is the next tier's cumulative threshold minus current XP, clamped at zero. The final tier has no next-tier target, even though levels continue up to 100.

### Visual progression

- **Samskara:** tree growth. Tier 1 now uses an illustrated moonlit forest and separate sparse sapling, with animated mist, glow, motes, and an ornamental ring.
- **Nie:** a blade that becomes more refined with tier.
- **Astra:** a celestial arrow that becomes more elaborate with tier.

The Samskara illustrated redesign currently covers **Ārambha only**. Higher tiers still use the older procedural tree artwork. Tree refinement is tier-based, not a new tree model for every level. Today’s completion affects illumination and particle activity; it does not award bonus XP or speed up progression. A full roots/branches/foliage transformation cinematic is not implemented yet.

## 6. Misses, penalties, and freezes

The penalty is tracked **separately for each developing habit**, using consecutive misses:

| Consecutive miss | Automaticity change | XP deduction |
| --- | --- | --- |
| First | None | None |
| Second | Lose 2% of current automaticity | Rounded 2% of the current level's cost |
| Third | Lose 5% of current automaticity | Rounded 5% of the current level's cost |
| Fourth and later | Lose 8% of current automaticity | Rounded 8% of the current level's cost |

For example, Level 20 has a cost of 380 XP. One habit's second, third, and fourth consecutive misses deduct approximately **8, 19, and 30 XP**, respectively. Multiple missed habits can each incur a penalty. The current effective level is evaluated as the day is processed, so crossing a level boundary during processing can change later deductions.

A completion resets that habit's consecutive-miss counter. A first miss is free of direct deductions, but it still prevents the all-habits daily-ring condition, so the +40 bonus can be lost.

There are **two freeze tokens per calendar month**, shared across habit/date freezes—not two per habit. Freezing one habit on one date consumes one token. Unused tokens do not carry over. Within the edit window, unfreezing removes the freeze and restores that month's available count.

A frozen habit/date:

- Earns no habit XP and does not change automaticity.
- Does not break the daily ring's habit condition.
- Preserves a streak without extending it.
- Is excluded from consistency's denominator.
- Pauses, but does not reset, the consecutive-miss counter.

## 7. Daily completion: what closes the ring?

The three requirements are:

1. **Food:** calories are within ±10% of the calorie target effective for that date.
2. **Habits:** every developing habit is completed or frozen.
3. **Sleep:** a sleep record exists for the date.

Each requirement contributes one third of today's displayed completion. Closing all three gives **+40 XP when that day settles**. Training and body weight are separate XP sources; neither is required to close the ring. Protein targets, sleep duration, and reported sleep quality are not additional ring gates.

A calorie target must actually exist. Merely logging food does not satisfy the food condition before the first adaptive target is established.

The dashboard also shows a historical completion grid and daily research content with a source attribution. These do not create extra scoring multipliers.

## 8. Habit development

You can have up to **five developing habits**. The **Hygiene** list is uncapped and holds ongoing routines, including graduated habits. Stacks group habits around a shared context or cue.

### Automaticity

Each settled completion moves automaticity toward 100:

```text
new automaticity = old automaticity + 0.045 × (100 − old automaticity)
```

From zero, assuming uninterrupted completions:

- About 50% after 15 completions.
- About 80% after 35 completions.
- About 95% after 66 completions.

The improvement per completion gets smaller as the score rises. This is a tracking model, not a measurement of your brain or a guarantee about how long a real habit takes to form.

| Automaticity | Stage label |
| --- | --- |
| Below 30% | Initiation |
| 30% to below 60% | Learning |
| 60% to below 85% | Stabilising |
| 85% and above | Automatic |

At **95%**, a developing habit automatically moves to Hygiene and frees a slot. The completion that triggers graduation still earns the developing-habit award. Later Hygiene completions do not earn that award. Hygiene habits still undergo automaticity updates, but misses do not cause overall XP deductions or break the ring. Falling below 95% does not automatically move a Hygiene habit back into Habits.

### Other habit metrics

- **Consistency:** completed days divided by eligible past days in the recent window, excluding freezes and days before creation. Today is excluded because it is still in progress.
- **Streaks:** consecutive completed days, with frozen days neutral. Streak length does not multiply XP.
- **Context stability:** variability in completion time, requiring at least five timed completions. Standard deviation ≤30 minutes is Tight, ≤90 is Variable, and above 90 is Scattered. Backdated completions do not get an artificial completion timestamp.

## 9. Food, body weight, and adaptive targets

Food logging uses a bundled food library, custom foods, quantities, and saved meals. Macro totals are calculated from the logged quantities. The app tracks calories, protein, carbohydrates, fat, and saturated fat, and shows remaining budgets and fat/protein pacing feedback.

Body weight has a longer editing window: **today plus the previous seven days**. Its trend is smoothed using:

```text
new smoothed weight = 0.25 × new weight + 0.75 × previous smoothed weight
```

### Weekly calorie adjustment

The weekly calculation becomes due on **Sunday at 23:00**, in local time, and is processed on app-open catch-up. Successful new targets take effect the following Monday.

It requires:

- An initial baseline of approximately 14 calendar days: the precise code gate is at least 13 date-days between the first food/weight log and the Sunday being assessed.
- At least five food-logged days in the assessed week.
- At least four weigh-ins in that week.

The app estimates maintenance from average logged intake and smoothed weight change:

```text
observed maintenance kcal/day = average logged kcal/day
    − (smoothed weight change in kg × 7,700 / elapsed anchor days)

proposed daily target = observed maintenance
    + (goal rate % / 100 × smoothed weight kg × 7,700 / 7)
```

The default goal rate is **−0.5% body weight per week**. The user can change it. Where a prior target exists, the weekly change is limited to ±10%. With sufficient profile data, the app also clamps the result to BMR through 2.5 × BMR; those bounds are applied after the weekly-change limit.

It shows a separate formula-based maintenance estimate using Mifflin–St Jeor BMR and an activity multiplier. This is distinct from the observed estimate derived from your logs.

### Macro allocation

- Protein defaults to **1.8 g/kg** of the ratcheted weight. The ratchet keeps the highest smoothed weight used by successful adjustments in the current block, so protein does not decrease automatically during that block. Starting a new block resets that reference.
- Fat starts at 30% of target calories, with bounds based on 20% of calories, 0.7 g/kg, and a 35% calorie ceiling. When the gram floor exceeds the ceiling, the implementation orders the bounds to keep the calculation valid rather than treating both limits as simultaneously achievable.
- Carbs receive the remaining calories after protein and fat, floored at zero.

Weeks are examined once. Later backfills do not rewrite an already-computed nutrition adjustment. A skipped week does not create a new target. Fat/protein pacing messages are guidance based on the remaining budget; they do not award or subtract XP.

## 10. Training progression

Training separates **prescription** from **execution**: a routine defines what you intend to do; confirmed sets record what actually happened. Routines can prescribe set counts, rep ranges, weight increments, rest, and target effort. Freestyle sessions are also supported.

### Double progression

At session finish, a routine slot's weight increases by its configured increment only if:

1. At least the prescribed number of working sets was confirmed.
2. Every working set reached the top of the prescribed rep range.

Warm-up sets do not count toward that test. After a weight increase, the next session's suggested reps return to the bottom of the range.

Example: for three working sets of 8–12 reps and a 2.5 kg increment, completing 12/12/12 earns the increase. Completing only two sets, or 12/12/10, does not.

Estimated one-rep max uses Epley's formula:

```text
estimated 1RM = weight × (1 + reps / 30)
```

A stall is flagged when neither of the latest two session bests beats the best from three sessions back. A 10% deload is a suggestion, not an automatic reduction. Weekly muscle-group volume counts working sets, not total tonnage. A plate calculator supports loading a target barbell weight.

## 11. Sleep

Sleep logs include bed time, wake time, time to fall asleep, wake count, and quality from 1–5. Duration is the bed-to-wake interval across midnight minus time to fall asleep, floored at zero. Wake count is recorded but does not subtract an estimated awake duration.

Defaults are a **23:00 bedtime**, **07:00 wake time**, and a **90-minute screen curfew** before bed. Targets are dated, so later changes do not overwrite the historical target records. Curfew settings accept 30–180 minutes.

Regularity measures the variation in sleep midpoints:

```text
regularity = clamp(100 − 1.5 × standard deviation of midpoints in minutes, 0, 100)
```

At least two nights are needed. A higher score means more consistent timing. Curfew and environment checklists can be compared with reported quality; a comparison is suppressed until there are at least five observations on each side. These are descriptive averages, not causal findings.

The XP award remains **+8 for a logged night** regardless of its regularity, duration, or quality.

## 12. Weekly reviews, storage, and backups

Weekly reviews become due after Sunday 23:00 and are generated during catch-up. They show weight trends, calorie and protein logging, nutrition-adjustment arithmetic, habit states, sleep metrics, training volume, and XP for the week. First opening earns +25 XP.

All data is stored locally in Room/SQLite. JSON export includes logs, settings, routines, XP events, tier crossings, and review records. Import is a **full replacement**, not a merge; the file is parsed and migrated before replacement occurs in a database transaction. Theme selection and the “already celebrated” tier marker are device preferences separate from the main exported database.

A home-screen widget gives a lightweight summary. Distribution is through signed APKs on GitHub Releases; there is no Play Store or background cloud service in this implementation.

## 13. Current implementation details worth knowing

These are useful when interpreting the app's behaviour, and should not be mistaken for additional intended game rules:

1. **Zero-habit discrepancy:** today's dashboard requires at least one developing habit for its habit third to fill. Day-close considers “all habits satisfied” true when there are none, so a historical ring can close with food and sleep alone.
2. **Tier crossing persistence is deferred:** the day-close service records tier crossings. Review XP can immediately raise the calculated level, but a newly reached tier from review XP may not get its permanent crossing recorded until a later day-close pass.
3. **Weight backfill and XP use different windows:** weight can be entered seven days back, but a day already settled is not rescored. Such a late backfill may appear in weight history without receiving +5 XP for that already-closed date.
4. **Consistency window is effectively up to 27 past dates:** despite the “trailing 28” description in comments, the code iterates from today minus 27 through yesterday.
5. **Startup catch-up has prerequisites:** day-close finds its first activity from habit creation, food, or body-weight records; review generation starts from food or body-weight records. Sleep/training alone do not establish those start dates.
6. **History is not fully snapshotted:** day-close consults the currently stored habits, and review details are assembled when opened. In particular, a historical review's habit automaticity/stages reflect current stored habit values rather than a frozen weekly snapshot.
7. **No guaranteed background settlement:** automatic processing catches up when the app opens. XP and review information can require a later dashboard refresh after startup processing finishes.

## 14. Basic technical architecture

The product flow is:

```text
User logs activities
    → repositories validate dates and save to local Room tables
    → day-close settles eligible dates and appends XP events
    → Levels calculates level; recorded crossings enforce tier floors
    → dashboard displays progression through the selected theme

Weekly catch-up
    → nutrition calculations and dated target adjustments
    → review records; first review opening appends its XP award
```

The UI is Kotlin with Jetpack Compose. Repositories manage food, training, habits, and sleep operations. Pure calculation objects implement XP thresholds, habit automaticity, training progression, sleep arithmetic, and nutrition estimates. Services coordinate day-close and weekly catch-up. Room persists the records, while small device preferences hold the theme and celebration state.

The hero boundary receives the motif, tier, and daily-completion fraction. Samskara composes environment artwork, ornament, fog, glow, tree art, and particles. It does not own XP or database decisions. UI labels, levels, progress, and navigation remain actual Compose elements.

## 15. Implementation sources

These repository-relative links are the main sources used to verify this guide:

- [XP and level formulas](../app/src/main/java/com/tatara/data/dashboard/Levels.kt)
- [Day-close scoring and permanent crossings](../app/src/main/java/com/tatara/data/dashboard/DayCloseService.kt)
- [Weekly reviews](../app/src/main/java/com/tatara/data/dashboard/ReviewService.kt)
- [Edit windows](../app/src/main/java/com/tatara/data/EditWindow.kt)
- [Habit automaticity](../app/src/main/java/com/tatara/data/habit/HabitEngine.kt), [habit operations](../app/src/main/java/com/tatara/data/habit/HabitRepository.kt), and [habit metrics](../app/src/main/java/com/tatara/data/habit/HabitMetrics.kt)
- [Nutrition calculations](../app/src/main/java/com/tatara/data/tdee/TdeeCalculator.kt) and [weekly scheduling](../app/src/main/java/com/tatara/data/tdee/TdeeService.kt)
- [Training progression](../app/src/main/java/com/tatara/data/train/Progression.kt) and [session operations](../app/src/main/java/com/tatara/data/train/TrainRepository.kt)
- [Sleep calculations](../app/src/main/java/com/tatara/data/sleep/SleepMath.kt)
- [Themes and tier names](../app/src/main/java/com/tatara/ui/theme/AppTheme.kt)
- [Dashboard](../app/src/main/java/com/tatara/ui/DashboardScreen.kt), [app startup](../app/src/main/java/com/tatara/MainActivity.kt), and [backup behaviour](../app/src/main/java/com/tatara/data/backup/BackupManager.kt)
- [Illustrated asset prompts and layering](samskara-art.md)

The original [technical specification](../SPEC.md) gives additional design intent. Where it differs from the implementation, this guide follows the code and identifies important discrepancies above.
