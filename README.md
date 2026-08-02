![Tatara banner](assets/banner.svg)

# Tatara

Tatara is a private, offline-first Android application for tracking training, nutrition, habits, sleep, and body weight. It is designed for one user, with no account, server, advertising, analytics, or cloud dependency.

## Product principles

- Data stays on the device
- Core workflows remain available in airplane mode
- Daily logging takes only a few taps
- Historical edits use explicit time windows
- Backups use a versioned, portable JSON format
- Progress calculations remain transparent and testable

## Features

- Dashboard with daily status and insight cards
- Food logging, macro calculations, and adaptive TDEE
- Training routines and progression tracking
- Habit streaks and automaticity metrics
- Sleep logging and consistency metrics
- Home-screen widget built with Glance
- Automatic and manual local backups

## Technology

- Kotlin
- Jetpack Compose and Material 3
- Room over SQLite
- Glance app widgets
- Gradle Kotlin DSL

## Build

Create `local.properties` with your Android SDK path, then run:

```bash
./gradlew test
./gradlew assembleDebug
```

The debug APK is written below `app/build/outputs/apk/debug/`.

## Repository layout

```text
app/src/main/java/com/tatara/data/   Domain logic and persistence
app/src/main/java/com/tatara/ui/     Compose screens and design system
app/src/main/java/com/tatara/ui/widget/  Home-screen widget
data/                                Bundled seed data
docs/APP-GUIDE.md                    User and feature guide
SPEC.md                              Product and technical specification
```

## Privacy

Tatara does not require network access for normal operation. Export files may contain personal health and activity data and should be stored accordingly.
