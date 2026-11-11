# AIO Tracker

An all-in-one personal Android app for **Salat**, **workouts**, **money**, **to-dos** and **notes**, with
optional **AI summaries from Google Gemini**. Everything is stored only on your phone.

Built with Kotlin, Jetpack Compose and Material 3. It has a custom sky-blue and gold theme, light and dark
modes, and optional Material You dynamic colour.

## Features

| Area | What you can do |
|---|---|
| **Salat** | Enter your local mosque times for Fajr, Dhuhr, Asr, Maghrib and Isha, and change them whenever the timetable changes. Log each prayer as *Jama'ah / On time / Late / Missed*. See the next prayer with a countdown, a 7-day strip, streaks, 7- and 30-day completion and a per-prayer breakdown. |
| **Jumu'ah** | Set a default Jumu'ah time and **a different time for any specific Friday**, with an optional note (e.g. the mosque). On Fridays Jumu'ah replaces Dhuhr (you can turn this off). |
| **Reminders** | Per-prayer notifications, at the prayer time or 5 to 60 minutes before. Each one has a **"Mark as prayed"** button. Reminders survive reboots, time-zone changes and app updates. |
| **Fitness** | Log workouts (strength, run, walk, cycling, swim, HIIT, yoga, sports, …) with duration, calories, distance, intensity, notes and exercises (sets × reps @ kg). Shows a weekly goal ring, a 7-day chart, monthly totals and a streak. |
| **Money** | Expenses and income with categories, notes and dates. Monthly net balance, budget meter, spending donut, 6-month trend. **Lending**: money you lent or borrowed, due dates, overdue flags and partial repayments. |
| **Planner: To-Do** | Quick-add, due dates, priorities, filters (Today / Upcoming / Anytime / All / Done). Swipe right to complete, swipe left to delete (with undo). |
| **Planner: Notes** | Notes in a staggered grid with search, pinning and colours. The full-screen editor auto-saves as you type. |
| **AI Insights** | Paste a Google AI Studio (Gemini) API key. Generate a summary of everything, or of one area, for 7, 30 or 90 days, or ask your own question. Results are rendered and saved in History. |
| **Backup** | Export everything to a JSON file and import it again. This is fully local, using the Android file picker. |

## Security and privacy

* The **API key is encrypted with AES-256-GCM** using a key held in the **Android Keystore** (hardware-backed on
  most phones). It is never shown again after saving: the app only shows a masked hint such as `AIza••••9xQ2`.
  It is decrypted in memory only when you request a summary.
* The key is sent to Google in the `x-goog-api-key` header over HTTPS, never in the URL.
* The key entry dialog blocks screenshots and screen recording.
* The key is **not included in exports**. Android cloud backup and device-to-device transfer are **disabled**.
* The AI only receives a compact text digest (counts, totals and short entries), not your raw database.

## Getting the app

Every push is built by GitHub Actions (`.github/workflows/android.yml`):

1. Open the repository's **Actions** tab, then the latest **Android build** run.
2. Download the **AIO-Tracker-N** artifact and unzip it.
3. Install `AIO-Tracker-N.apk` on your phone. You may need to allow "Install unknown apps".

Pushes to the default branch also publish a **GitHub Release** with the APK attached, so you can download it
directly on your phone.

`AIO-Tracker-N-debug.apk` is a debug build that installs alongside the main app as "com.aio.tracker.debug".
It is only useful for troubleshooting.

### Signing (so updates keep your data)

No signing key is stored in this repository. Without one, CI signs with a throwaway debug key, and Android
will refuse to install a newer build over an older one. You would have to uninstall first and lose your data.
**Set up your own key once** so every build updates in place:

```bash
keytool -genkeypair -v -keystore aio-release.jks -alias aio -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 aio-release.jks > aio-release.jks.b64
```

Then, in GitHub → *Settings → Secrets and variables → Actions*, add:

| Secret | Value |
|---|---|
| `AIO_KEYSTORE_BASE64` | contents of `aio-release.jks.b64` |
| `AIO_KEYSTORE_PASSWORD` | the keystore password you chose |
| `AIO_KEY_ALIAS` | `aio` |
| `AIO_KEY_PASSWORD` | the key password (the same as the keystore password unless you chose otherwise) |

Keep `aio-release.jks` somewhere safe and never commit it.

## First steps in the app

1. **Salat → clock icon**: enter your mosque's times and choose reminder offsets.
2. Allow **notifications** and **Alarms & reminders** when asked. This makes reminders exact.
3. **Salat → Jumu'ah**: tap any upcoming Friday to set that day's time.
4. **Settings → AI**: tap **Get a key** (aistudio.google.com/apikey), then **Add key**. Pick a model (the default
   is `gemini-2.5-flash`; *Load available models* lists the models your key can use).
5. **Settings → Your data → Export backup** from time to time.

## Building locally

Requirements: JDK 17 and the Android SDK (API 35).

```bash
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest    # unit tests + Robolectric UI smoke test
```

### Changing the database schema

All user data lives in the local Room database, so every schema change needs a migration. Without one the app
crashes on startup for everyone who already has it installed.

Room exports the schema of each database version to `app/schemas/com.aio.tracker.data.db.AppDatabase/`. These
JSON files are committed: they are the baseline that migrations are generated from and tested against.

Whenever you change an `@Entity` (in `data/db/Entities.kt`) or the entity list in `AppDatabase`:

1. Bump `version` in `@Database` (`data/db/AppDatabase.kt`).
2. Add a migration from the previous version: either `autoMigrations = [AutoMigration(from = N, to = N + 1)]`
   in `@Database` for simple changes (new tables, new columns with a default value), or a hand-written
   `Migration` registered with `.addMigrations(...)` on the database builder.
3. Build, then commit the new `N + 1.json` that appears in `app/schemas/`. Never edit or delete the JSON of a
   version that has already been released.
4. Extend `MigrationTest` (`app/src/test`) to run the migration with `MigrationTestHelper` and check that
   existing rows survive.

## Project structure

```
app/src/main/java/com/aio/tracker
├── data/
│   ├── db/          Room entities & DAOs (salat, workouts, money, loans, todos, notes, AI history)
│   ├── prefs/       DataStore settings
│   ├── secure/      Keystore-backed API key encryption
│   ├── backup/      JSON export / import
│   └── ai/          Gemini REST client
├── domain/          Prayer schedule logic, AI digest builder
├── reminders/       AlarmManager scheduling, notifications, boot receiver
└── ui/              Compose screens: home, salat, fitness, money, planner, ai, settings
```

Sync is intentionally not included yet. The backup format (`formatVersion`) is designed so it can be added later.
