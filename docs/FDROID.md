# Hosting GymLogger + a custom F-Droid repo on GitHub

Everything runs on GitHub — no external server. A tag push builds a **signed release
APK**, generates a **custom F-Droid repository**, publishes it to **GitHub Pages**
(the `gh-pages` branch), and attaches the APK to a **GitHub Release**.

```
git tag v1.3  →  Actions: build+sign APK → fdroid update (sign index) → gh-pages → Pages
                                                                      └→ GitHub Release
```

Users add one URL to their F-Droid client and get automatic updates:

```
https://theoborealis.github.io/GymLogger/fdroid/repo
```

## Moving parts

| File | Role |
| --- | --- |
| `app/build.gradle.kts` | Release signing, enabled only when `SIGNING_KEYSTORE_FILE` env is set (local builds stay unsigned and keep working). |
| `fdroid/config.yml` | `fdroidserver` repo config (URL, name, keystore + alias, passwords via env). |
| `fdroid/metadata/com.theob.gymlogger.yml` | App metadata; localized text/screenshots/icon come from `fastlane/metadata`. |
| `.github/workflows/release.yml` | The whole pipeline. |
| `scripts/make-keystore.sh` | One-time keystore generator. |

A **single PKCS12 keystore** signs both the APK and the repo index.

## One-time setup

### 1. Generate the signing key

```sh
./scripts/make-keystore.sh          # writes gymlogger.p12 (git-ignored)
```

Back this file up somewhere safe. **If you lose it you cannot ship signed updates**
— F-Droid and Android reject an APK/repo signed by a different key, and users would
have to uninstall/reinstall.

### 2. Add repo secrets

Settings → Secrets and variables → Actions → *New repository secret*:

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | `base64 -i gymlogger.p12 \| pbcopy` then paste |
| `KEYSTORE_PASSWORD` | the password you chose |
| `KEY_PASSWORD` | the same password (PKCS12 requires them equal) |
| `KEY_ALIAS` | `gymlogger` (must match `repo_keyalias` in `fdroid/config.yml`) |

### 3. Enable GitHub Pages

The first workflow run creates the `gh-pages` branch. After it succeeds:
Settings → Pages → **Build and deployment → Source: Deploy from a branch →
Branch: `gh-pages` / `/ (root)`**.

Your repo index then lives at
`https://theoborealis.github.io/GymLogger/fdroid/repo` and a landing page with
instructions at `https://theoborealis.github.io/GymLogger/`.

## Releasing a new version

1. Bump `versionCode` (and usually `versionName`) in `app/build.gradle.kts`.
   **F-Droid only offers an update when `versionCode` increases.**
2. Commit, then tag and push:
   ```sh
   git tag v1.3 && git push origin v1.3
   ```
3. The workflow builds, signs, republishes the repo (old versions are preserved),
   and creates the Release.

You can also trigger it manually from the Actions tab (**Run workflow**); a manual
run republishes but skips the Release step.

## Installing (for users)

- **F-Droid client:** Settings → Repositories → add
  `https://theoborealis.github.io/GymLogger/fdroid/repo`, then install GymLogger.
  (Neo Store / Droid-ify work the same way.)
- **No repo, just updates:** [Obtainium](https://github.com/ImranR98/Obtainium)
  pointed at the [Releases page](https://github.com/theoborealis/GymLogger/releases).

## Notes & gotchas

- **Custom repo ≠ official F-Droid.** The official repo requires reproducible builds
  and no proprietary dependencies; your own repo has none of those rules — you sign
  and serve whatever you want.
- **Pages limits:** ~1 GB branch / ~100 GB bandwidth per month — fine for a personal
  app. `archive_older: 0` in `config.yml` keeps every version; raise/lower to taste.
- **Local release builds** (`./gradlew assembleRelease` with no env) produce an
  *unsigned* APK — that's expected. CI supplies the key.
