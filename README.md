# Email Manager & Maps Quick Access

A small, fully offline Android app (Kotlin, Jetpack Compose, Material 3, Room) for keeping your own list of
email IDs, copying one in a single tap, stepping through the list with **Prev / Next**, and jumping to
Gmail, Google Maps or the Google Account page.

* Package: `com.emailmanager.app` · min Android 8.0 (API 26) · target/compile SDK 34
* **No internet permission. No permissions at all.** Data lives in a private on-device Room database.
* Cloud backup is disabled (`allowBackup=false`) so your list is never copied to Google Drive automatically.
  Use **Export backup file** if you want a copy.

## What it does

| Area | Details |
|---|---|
| Dashboard | Total saved count (plus Unused / In Use / Archived), search box, Add Email, Import list, Gmail / Google Maps / Google Account buttons, email list |
| Entries | Email, optional label, notes, status (Unused / In Use / Archived), Copy, Edit, Delete (with confirmation) |
| Fast navigation | Pinned bottom bar: **Prev**, **Copy Email**, **Next** (wraps around, follows the current search / filter / sort). The selected entry is remembered after restart |
| Bulk import | Paste one email per line. Validates format, removes duplicates (in the pasted text and against saved emails), and shows a preview with counts before anything is saved. Also understands `mailto:`, `Name <a@b.com>`, and comma / semicolon separated values |
| Storage | Room database, sorted / filtered / searched in-app. Sort: newest, oldest, A-Z, Z-A, status |
| Backup | Export / import a JSON file you pick with the system file picker (no storage permission needed). Import also accepts a plain `.txt` with one email per line. Duplicates are skipped |
| Delete all | Menu (top right) -> Delete all data, with a confirmation dialog |
| Google links | Gmail: opens the Gmail app, or mail.google.com if it is not installed. Maps: opens the Google Maps app, or https://maps.google.com/ in the browser if the app is missing. Account: opens https://accounts.google.com/ in the browser. The app only *opens* these; it never types, posts or submits anything |

## Privacy and scope

The app never asks for or stores passwords, OTPs, recovery codes or tokens (the Notes field even says so),
makes no network requests of its own, and has no automation for creating Google accounts, solving CAPTCHAs,
verification, or posting Maps reviews.

## Build the APK

### Option A: Android Studio (easiest)
1. Install Android Studio (Koala 2024.1 or newer) from https://developer.android.com/studio.
2. **File -> Open** and choose this `EmailManager` folder. Let Gradle sync finish (first sync downloads
   about 1 GB of dependencies; needs internet once). Accept any prompt to install SDK Platform 34.
3. **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**, then click **locate** in the notification.
4. The file is `app/build/outputs/apk/debug/app-debug.apk`.

### Option B: command line
Requires JDK 17+ and the Android SDK (`ANDROID_HOME` set, platform 34 installed).

    ./gradlew testDebugUnitTest     # parser unit tests
    ./gradlew assembleDebug         # builds the APK (Windows: gradlew.bat assembleDebug)

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Option C: no local setup (GitHub Actions)
1. Create a free private repository on GitHub and upload this folder's contents.
2. Open the **Actions** tab -> **Build debug APK** -> **Run workflow** (it also runs on every push).
3. When it finishes, download the **EmailManager-debug-apk** artifact (a zip containing `app-debug.apk`).

## Install on your phone

1. Copy `app-debug.apk` to the phone: USB cable (drag into the Downloads folder), Bluetooth / Nearby Share,
   or email / cloud drive to yourself.
2. On the phone open **Files** (or **Downloads**) and tap `app-debug.apk`.
3. If asked, allow your Files / browser app to **Install unknown apps** (Settings -> Apps -> Special access).
   You can turn that permission off again afterwards.
4. Tap **Install**, then **Open**. If Play Protect warns about an unknown developer, choose
   **More details -> Install anyway** (the debug APK is signed with a debug key, not a Play Store key).

Alternative with a USB cable and USB debugging enabled: `adb install -r app-debug.apk`

To update later, install a newer APK over the old one; your data is kept (same package name and signing key
on the same computer). If you build on a different computer the debug key differs, so Android will refuse to
update: **export a backup first**, uninstall, install the new APK, then import the backup.

## Project layout

    app/src/main/java/com/emailmanager/app/
      MainActivity.kt          entry point, edge-to-edge setup
      EmailViewModel.kt        search / sort / filter state, Prev-Next logic, import & export
      data/                    Room entity, DAO, database
      util/EmailParser.kt      validation + paste parsing (pure Kotlin, unit-tested)
      util/BackupManager.kt    JSON backup read / write
      util/Launchers.kt        safe launching of Gmail / Maps / browser
      ui/                      Compose screens, dialogs, theme
    app/src/test/              JVM unit tests for the parser

## Customising
* Change the Account button URL in `util/Launchers.kt` (`ACCOUNTS_URL`), for example to
  `https://myaccount.google.com/` for account settings.
* Colours are in `ui/Theme.kt`.
