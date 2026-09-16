# YCATS Community

YCATS (Youth Counselling And Talents Show) is a mobile-first community platform for youth support, events, announcements, questions, attendance and talent activities.

## Current app

The `app/` module is an Android Jetpack Compose application with:

- Premium YCATS visual language: navy/blue/green brand palette, rounded cards, elevated dashboard surfaces and responsive content spacing.
- Local-first data access using Preferences DataStore, with cloud sync to Firebase Realtime Database.
- Login and registration with client-side validation and PBKDF2 password hashing for the local fallback mode.
- Member, guardian and administrator experiences for updates, events, Q&A/chat, profiles and attendance.

## Firebase setup

The app currently uses Firebase Realtime Database REST calls for cloud sync. Before shipping, configure Firebase security and move authentication to Firebase Authentication.

1. Create a Firebase project and enable Realtime Database.
2. Set the database region and replace `FIREBASE_URL` in `MainActivity.kt` with your project URL.
3. Apply the rules in `firebase/database.rules.json`.
4. Do not commit service-account credentials or API keys.
5. For production authentication, enable Email/Password in Firebase Authentication and migrate accounts from the local fallback store.

The current local fallback is intentionally useful for offline demos, but it should not be treated as the production identity system.

## Build

Open the repository in Android Studio with JDK 17 and run:

```bash
./gradlew :app:assembleDebug
```

Install the generated APK from `app/build/outputs/apk/debug/` on an Android 8.0+ device or emulator.

## Architecture direction

`shared/` documents the platform-neutral contract for a future desktop client or Ktor service. The Android app remains the first client, while the data models and Firebase paths are designed to be shared by mobile, desktop and web clients.

## Demo account

The local demo seed creates:

- Username: `admin`
- Password: `admin123`

Change or remove this seed before any public deployment.
