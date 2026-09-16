# YCATS Community

YCATS (Youth Counselling And Talents Show) is a mobile-first community platform for youth support, events, announcements, questions, attendance and talent activities.

## Current state

This repository now contains:

- a polished Android Jetpack Compose app scaffold in `app/`
- stronger client validation for auth and content screens
- a shared data contract in `shared/` for desktop/backend preparation
- Firebase security guidelines and schema documentation in `firebase/`

## App blueprint

The Android app is the first client in the YCATS platform and is organized around the following user flows:

- login and registration
- dashboard overview
- updates feed
- events list
- questions and chat support
- profile management
- attendance records
- admin dashboard

## Firebase direction

The current implementation uses a local-first data store and cloud sync placeholders. The correct production path is:

1. Firebase Authentication for secure sign-in and sign-up
2. Firebase Realtime Database or Firestore for app data
3. role-based database rules
4. server-side moderation for admin operations
5. App Check before public launch

See `firebase/database.rules.json` and `firebase/PRODUCTION_CHECKLIST.md` for the recommended security and deployment configuration.

## Shared model layer

The project includes a shared model contract in `shared/src/commonMain/kotlin/mw/ycats/shared/model/Models.kt` so the application can later share the same types with a desktop client or backend service without duplicating business logic.

## Build

Open in Android Studio with JDK 17 and run:

```bash
./gradlew :app:assembleDebug
```

## Demo account

For local demo use, the app seeds an admin account:

- username: `admin`
- password: `admin123`

This should never be used in a public or production environment.
