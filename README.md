# SUYZISS FEED

Native Android social feed application.

## Current status: Foundation prototype

- Kotlin + Jetpack Compose
- Minimum Android version: 10 (API 29)
- Compile SDK 36 / Target SDK 36
- Bottom navigation: Home, Explore, Create, Inbox, Profile
- Swipe-first vertical feed with local sample data
- Dark and Light theme toggle
- Local-only prototype; no backend connection yet

## Build

Open the repository in Android Studio with JDK 17 and Android SDK 36 installed. Sync Gradle, then run the app.

Command-line build (with a compatible Gradle installation):

    gradle :app:assembleDebug

APK output:

    app/build/outputs/apk/debug/app-debug.apk

## Package

com.suyziss.feed

## Roadmap

1. Add Media3 video playback and lifecycle-aware player management.
2. Implement photo/video picker, post composer, and drafts.
3. Add comments, likes, follow actions, and profile editing.
4. Add Room caching and loading/error states.
5. Connect Supabase after the local client foundation is stable.

The current feed is a UI foundation, not a finished video platform. Video playback, publishing, authentication, and remote sync are still pending.
