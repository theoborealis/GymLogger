# GymLogger

GymLogger is a minimalist, 100% private, open-source weightlifting logger.

It's a fork of [GymLoga](https://github.com/GymLoga/GymLoga-Android) by Michael Bosse, redesigned around **Material You** (inspired by [Book Story](https://github.com/Acclorite/book-story)) with a true-black **AMOLED** dark theme, and reworked so your log **saves itself** — there is no save button.

## Features

- **Auto-saving log**: Everything you add to a day is written instantly. Add an exercise, a set, or a note and it's already saved — no "Save" button, no lost sessions.
- **Blazing fast entry**: Log your sets with simple shorthand like `135x5x3` or `20x10`.
- **Automatic PR tracking**: The app identifies your Personal Records (PRs) as you log.
- **Strength analytics**: View your exercise history and estimated One Rep Max (1RM) progress.
- **Material You**: Dynamic color on Android 12+ with a tasteful fallback palette, on a pure-black AMOLED surface.
- **Privacy first**: No tracking, no ads, no cloud syncing. Your data never leaves your device.
- **Modern native feel**: Built with Kotlin and Jetpack Compose (Material 3).

## Building from source

The repository ships a Nix shell that provides a pinned JDK 17 + Android SDK, so no global Android tooling is required:

```bash
nix-shell --run './gradlew :app:assembleDebug'
```

Run the unit tests with:

```bash
nix-shell --run './gradlew :app:testDebugUnitTest'
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

If you already have a local Android SDK (`ANDROID_HOME` set, build-tools 34, platform 34), the standard `./gradlew assembleDebug` works too.

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0) or later**. See the AGPL-3.0-or-later file for details. As a fork, it preserves the original copyright and license of GymLoga.
