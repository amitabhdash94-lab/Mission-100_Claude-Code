# NEUROFIT

Offline-first personal weight loss command center for a single device: a Samsung Galaxy S23 Ultra.

No account. No server. No sync. No analytics. Every byte of data stays on the phone.

**Status: Phase 1 of 12.** The repo currently builds a signed, installable APK containing one
placeholder Compose screen. Nothing else is implemented yet. That is deliberate: the build
pipeline is proven green before any feature code exists.

---

## 1. Getting to a green build

### Step 1: create the signing key (run once, ever)

You have no local JDK, so the key is generated inside GitHub Actions.

1. Push this repo to GitHub as a **private** repository, on branch `main`.
2. Go to **Actions > Bootstrap Keystore > Run workflow**. Accept the defaults.
3. When it finishes, open the run and download the **neurofit-keystore** artifact.
   It expires after 7 days. Save `release.jks` somewhere safe and offline.

If you ever lose this key or regenerate it, Android will refuse to install new builds over
the installed app, and you will have to uninstall and lose all local data. Generate it once.

### Step 2: create the four repository secrets

**Settings > Secrets and variables > Actions > New repository secret.**

| Secret name | Where the value comes from |
| --- | --- |
| `KEYSTORE_BASE64` | the entire contents of `release.jks.base64` from the artifact, one long line |
| `KEYSTORE_PASSWORD` | the value printed in `SECRETS.txt` in the artifact |
| `KEY_ALIAS` | the value printed in `SECRETS.txt` (defaults to `neurofit`) |
| `KEY_PASSWORD` | the value printed in `SECRETS.txt` (same as the store password, PKCS12 requires this) |

Set **all four or none**. If all four are absent the build still succeeds, falls back to debug
signing and logs a warning. If only some are set the build fails on purpose, because the
alternative is silently publishing a debug signed APK, and a debug signed APK will not install as
an upgrade over a release signed one. CI also verifies the finished APK's certificate, so a
mismatched `KEY_ALIAS` is caught before a release is published rather than on the phone.

### Step 3: build

Push to `main`, or run **Actions > Build APK > Run workflow**.

The workflow first verifies that every version pinned in `gradle/libs.versions.toml` actually
exists on Google's Maven repository, then runs unit tests, assembles a release APK, checks the
APK really carries the expected signing certificate, uploads it as an artifact, and publishes a
GitHub Release tagged `build-<run number>` with the `.apk` attached as a release asset.

The release asset is the delivery mechanism. Actions artifacts download as a ZIP, which is
painful on a phone. A release asset is a direct `.apk` link you can tap in mobile Chrome.

---

## 2. Installing on the Galaxy S23 Ultra

1. Open the repository **Releases** page in Chrome on the phone.
2. Tap the `neurofit-vX.Y.Z-buildNN.apk` asset. Confirm the download warning.
3. Tap the downloaded file. Android will block the install the first time.
4. Go to **Settings > Apps > Special access > Install unknown apps**, pick **Chrome**
   (or **My Files**, depending on where you tapped from), and turn on **Allow from this source**.
5. Go back and tap the APK again.
6. **Play Protect will warn** that the app was not scanned or comes from an unknown developer.
   This is expected for any sideloaded app. Tap **More details** then **Install anyway**.

### Updating

Push a change, wait for the green check, open the new release, download the new asset, install
over the top. Do not uninstall first. Your data is preserved because:

- the signing key never changes, so Android accepts the package as an upgrade, and
- `versionCode` is driven by `github.run_number`, so every new run is newer than the last.
  (Re-running an existing run reuses its number, so it reproduces the same `versionCode`
  rather than incrementing. That still installs, it is just not a newer build.)

One consequence worth knowing: `github.run_number` is keyed to the workflow's identity, so
**renaming `.github/workflows/build.yml` restarts it at 1**. That would send `versionCode`
backwards and Android would refuse every later upgrade. The file carries a comment saying so.
If it ever has to be renamed, set an explicit offset in `app/build.gradle.kts` at the same time.

---

## 3. Permissions

**Phase 1 requests zero permissions.** The manifest contains no `<uses-permission>` entries at all.

Permissions will be added in later phases. Each one will be listed here with its justification
as it is introduced. The planned set, for reference:

| Permission | Phase | Why |
| --- | --- | --- |
| `CAMERA` | 5 | barcode scanning of packaged food, via CameraX. Never used for anything else, no photo upload. |
| `POST_NOTIFICATIONS` | 10 | local meal reminders and the weekly weigh-in prompt, scheduled by WorkManager on-device. |
| `USE_BIOMETRIC` | 10 | optional fingerprint app lock. Off by default. |
| `SCHEDULE_EXACT_ALARM` | 10 | only if reminder timing proves unreliable with inexact work. Preference is to avoid it. |
| Health Connect read permissions | 8 | optional daily steps and active calories. The app degrades gracefully with a manual entry fallback if declined or unavailable. |

Progress photos are captured through the system photo picker, which needs no storage permission.
Export and backup write through the Storage Access Framework, which needs no storage permission.

There is no `INTERNET` permission in Phase 1. It will be added only when the opt-in Open Food
Facts lookup is built in Phase 5, and that feature ships **off by default**.

---

## 4. Data locality

- `android:allowBackup="false"` in the manifest.
- `android:dataExtractionRules` points at `res/xml/data_extraction_rules.xml`, which excludes
  every domain from both Google cloud backup and device to device transfer.
- `res/xml/backup_rules.xml` excludes everything as well. This is the legacy
  `fullBackupContent` format, which the platform only honours on API 23 to 30, so at
  `minSdk 31` it never actually runs. It is kept because it costs nothing and stops the
  file being missing if the floor ever drops, but `allowBackup="false"` and the data
  extraction rules are what do the work.
- CI fails the build if the finished APK requests any permission at all, so the zero
  permission claim above is checked against the artifact rather than trusted.
- No analytics SDK, no crash reporting SDK, no Firebase, no ads, no telemetry.
- Room database and photos live in app-private internal storage.
- The app is fully functional in airplane mode.

---

## 5. Phase 1 decisions and assumptions

Recorded here as required, so nothing is a surprise later.

1. **AGP 8.13.2 rather than AGP 9.x.** AGP 9 changed the DSL significantly and some third party
   Gradle plugins are still catching up. Since you cannot compile locally and every red run costs
   several minutes, the last mature 8.x line is the right trade.
   The patch level is load bearing, not cosmetic: Google's AGP 8.13 release notes list exactly one
   feature for the whole line, *"Android Gradle plugin 8.13.2 uses R8 8.13.19 which supports
   Kotlin 2.3"*. Since this project is on Kotlin 2.3.20, **8.13.2 is the minimum**, and dropping to
   8.13.0 or 8.13.1 would pair Kotlin 2.3 with a toolchain that predates support for it.
   The same notes give AGP 8.13's compatibility: maximum API level 36.1 (so `compileSdk 36` is
   fine), minimum Gradle 8.13, minimum JDK 17. Gradle stays on the 8.x line because AGP 8.x
   cannot run on Gradle 9.x.
2. **`compileSdk` and `targetSdk` are 36.** That is the newest API level AGP 8.13 supports.
3. **Compose BOM `2026.03.01`**, which pairs cleanly with Kotlin 2.3.20 and `compileSdk 36`.
   Newer BOMs pull Compose versions that expect a newer AGP. This BOM resolves the Compose
   libraries to 1.12.0.
4. **The Gradle wrapper JAR is not committed.** It is a binary file and cannot be authored as
   text. The build workflow generates it with `gradle wrapper` if it is missing, using the Gradle
   version pinned in `env.GRADLE_VERSION` and in `gradle/wrapper/gradle-wrapper.properties`.
   `gradlew` and `gradlew.bat` are likewise generated in CI. This costs a few seconds per run and
   removes an entire class of "wrapper mismatch" failures.
5. **The APK is renamed in the workflow, not in Gradle.** Renaming variant outputs from Gradle
   requires AGP internal classes that move between versions. A `mv` in CI is boring and cannot
   break the build. The `:app:printVersionName` task feeds the version into the filename so there
   is still a single source of truth.
6. **R8 is off.** `isMinifyEnabled = false` and `isShrinkResources = false` on the release build
   type, as instructed. `app/proguard-rules.pro` is written and kept correct, including forward
   looking rules for Room, Hilt, kotlinx.serialization, Retrofit, WorkManager and ML Kit, so the
   flags can be flipped later without a debugging session through CI logs.
7. **`buildConfig = true`** is enabled for the app module so the boot screen can display the
   installed version name and build number. That makes it obvious at a glance which build is on
   the phone.
8. **V1 signing is disabled**, V2 and V3 are enabled. `minSdk 31` means the legacy JAR signature
   scheme is dead weight.
9. **Fonts are not yet bundled.** Orbitron, JetBrains Mono and Rajdhani are binary `.ttf` files
   and cannot be authored as text. Phase 2 will add a CI step that downloads them from the Google
   Fonts repository into `app/src/main/res/font/` at build time, so no binary is committed here
   either. Phase 1 typography uses platform families behind the `DisplayFamily`, `MonoFamily` and
   `BodyFamily` aliases in `ui/theme/Type.kt`, so the swap touches one file.
10. **Launcher icon is a vector adaptive icon.** No PNG assets, for the same reason. Includes a
    `monochrome` layer so One UI themed icons work.
11. **Locale is English only** for now. The strings file exists and no composable hardcodes text,
    per the architecture rules.
12. **Configuration cache is off** in `gradle.properties`. It is easy to turn on later and it is
    a common source of obscure failures with annotation processors, which arrive in Phase 3.
13. **Every version above was checked against Google's published release notes**, not recalled:
    AGP 8.13.2 and its compatibility table, Compose BOM `2026.03.01`, `core-ktx 1.16.0`,
    `lifecycle 2.9.4` and `activity-compose 1.10.1` all appear in the official documentation.
14. **CI verifies the pinned versions before it builds.** `tools/check-pinned-versions.sh` fetches
    `maven-metadata.xml` for AGP, the Compose BOM and the androidx artifacts and fails in seconds
    if a pinned version was never published, printing the versions that do exist. A wrong version
    is otherwise a slow, opaque failure part way through a Gradle run. This matters because these
    versions cannot be checked from the machine this project is written on, only from the runner.
    If a pin turns out not to exist, the fix is one line in `gradle/libs.versions.toml`.

---

## 6. Local development (optional)

You do not need this, since CI is the only build that counts. If you ever do get an Android SDK:

```
cp keystore.properties.example keystore.properties
# fill in storeFile, storePassword, keyAlias, keyPassword

# gradlew is deliberately not committed (section 5, decision 4), so generate it once:
gradle wrapper --gradle-version 8.14.3 --distribution-type bin

./gradlew assembleRelease
```

Signing resolves in this order: environment variables, then `keystore.properties`, then debug
signing. The project always configures even with no secrets present.

---

## 7. Roadmap

| Phase | Contents | Status |
| --- | --- | --- |
| 1 | Repo, version catalog, signing, CI, one placeholder screen | delivered |
| 2 | Hilt, full design system, component gallery | pending |
| 3 | Room schema, seed data, domain calculations and tests | pending |
| 4 | Onboarding, profile, target calculation | pending |
| 5 | Food logging, custom foods, recipes, barcode, water | pending |
| 6 | Dashboard and navigation shell | pending |
| 7 | Weight, measurements, photos, trend chart, adaptive TDEE | pending |
| 8 | Exercise logging, templates, Health Connect | pending |
| 9 | Analytics and insight generator | pending |
| 10 | Notifications, export, import, backup, settings, widget | pending |
| 11 | Polish pass | pending |
| 12 | Final release | pending |
