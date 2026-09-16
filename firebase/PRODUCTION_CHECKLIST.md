# Firebase production checklist

- Enable Firebase Authentication > Email/Password.
- Require authenticated reads in Realtime Database.
- Use Firebase App Check before public launch.
- Remove the seeded `admin/admin123` account.
- Never store passwords in Realtime Database.
- Move roles to a server-controlled profile document; do not trust a client-provided role.
- Add Cloud Functions or a Ktor service for privileged role changes and moderation.
- Add emulator tests for database rules before deployment.
