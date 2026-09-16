# Firebase production checklist

Before release:

1. Publish `firebase/database.rules.json` in Firebase Console.
2. Enable Email/Password authentication.
3. Create the first admin profile manually in `ycats/users/{uid}` with role `ADMIN`, or use a trusted server/Cloud Function.
4. Remove any public time-based rules.
5. Enable App Check and restrict the Android API key to package `mw.ycats.app` and the correct SHA-1 fingerprints.
6. Build a release APK/AAB and test rules using the Firebase Emulator Suite.
7. Do not ship service-account credentials in the Android app.

The Android app now has Firebase-backed repositories for Auth, Realtime Database listeners, and Storage profile photo uploads. The old local `YcatsDb` file remains only as legacy code and should be deleted after the Firebase UI migration is verified.
