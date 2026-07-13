# GymLogger

GymLogger is a minimalist, 100% private, open-source weightlifting logger.

It's a fork of [GymLoga](https://github.com/GymLoga/GymLoga-Android) by Michael Bosse, redesigned around **Material You** (inspired by [Book Story](https://github.com/Acclorite/book-story)) with a true-black **AMOLED** dark theme, and reworked so your log **saves itself** — there is no save button.

## Features

- **Auto-saving log**: Everything you add to a day is written instantly. Add an exercise, a set, or a note and it's already saved — no "Save" button, no lost sessions.
- **Blazing fast entry**: Log your sets with simple shorthand like `135*5*2` or `20*10` (weight * reps * sets).
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

If you already have a local Android SDK (`ANDROID_HOME` set, build-tools 36.0.0, platform 36), the standard `./gradlew assembleDebug` works too.

## Testing / profiling

`emulator.nix` provisions a headless Android emulator (JDK 17 + SDK 34 + an
x86_64 system image) so the app can be driven and profiled without a physical
device. `scripts/emulator.sh` wraps the lifecycle:

```bash
# boot a KVM-accelerated emulator in the background, then wait for it
nix-shell emulator.nix --run './scripts/emulator.sh boot' &
nix-shell emulator.nix --run './scripts/emulator.sh wait'

# install, launch, screenshot
nix-shell emulator.nix --run './scripts/emulator.sh install'
nix-shell emulator.nix --run './scripts/emulator.sh launch'
nix-shell emulator.nix --run './scripts/emulator.sh shot today'   # -> .emulator/shots/today.png

nix-shell emulator.nix --run './scripts/emulator.sh down'         # stop it
```

`scripts/jank-test.sh` drives a fixed typing workload and prints the `gfxinfo`
summary. Note the bundled emulator renders with SwiftShader (software GL), so
frame-level jank numbers are dominated by the renderer and are *not*
representative of real hardware. To measure the auto-save cost specifically, the
most reliable signal is render-independent — e.g. counting DataStore writes with
`inotifyd files/datastore` during a typing burst (debounced saves collapse a
burst of keystrokes into a single write).

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0) or later**. See the AGPL-3.0-or-later file for details. As a fork, it preserves the original copyright and license of GymLoga.
