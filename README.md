# SUYZISS FEED

Android client prototype for SUYZISS FEED.

## Build environment

The project uses a fixed Android build toolchain:

| Component | Version |
|---|---:|
| JDK | 17 |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.21 |
| Android compile SDK | 36 |
| Android build tools | 36.0.0 |
| Minimum Android version | Android 10 (API 29) |

Use the same JDK and Gradle versions locally and in CI to reduce environment-specific build failures.

## Build a debug APK

Requirements:
- JDK 17
- Android SDK with platform 36
- Android Build Tools 36.0.0
- Gradle 8.13 installed and available as `gradle`

From the repository root:

```bash
gradle --no-daemon :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Continuous integration

GitHub Actions runs on pushes and pull requests targeting `main`, and can also be started manually from the Actions tab.

The workflow:
1. Installs JDK 17, Gradle 8.13, and Android SDK 36.
2. Builds the debug APK.
3. Runs Android Lint.
4. Checks that the APK exists and can be inspected by Android Build Tools.
5. Uploads the APK as the `suyziss-feed-debug-<commit>` artifact for 14 days.

Open the repository's Actions tab to inspect the latest build and download its artifact.

## Current project scope

This is a client-side foundation. The feed and profile intentionally contain no fabricated posts or engagement data. Video playback, media selection and publishing, authentication, remote data, and the backend are not implemented yet.
