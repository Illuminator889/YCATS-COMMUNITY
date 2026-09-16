# Firebase setup

The Android project is configured for Firebase project `ycats-f0585` and package `mw.ycats.app`.

Enabled in Gradle:

- Firebase Authentication
- Realtime Database
- Firebase Storage
- Google Services plugin

Before running:

1. Enable Authentication → Sign-in method → Email/Password in Firebase Console.
2. Deploy `firebase/database.rules.json`.
3. Build with JDK 17: `./gradlew :app:assembleDebug`.

`google-services.json` contains client configuration, not an Admin SDK credential. Never add service-account JSON files to the Android app.
