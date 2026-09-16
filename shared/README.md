# Shared YCATS contract

This directory is the platform-neutral contract for the planned desktop client and shared backend. Keep field names stable across Android, desktop and server implementations.

Recommended next implementation:

- `shared/src/commonMain/kotlin/mw/ycats/shared/Models.kt` for Kotlin Multiplatform models.
- `backend/` for a Ktor API that validates Firebase Auth ID tokens and exposes `/updates`, `/events`, `/questions`, and `/attendance`.
- Desktop Compose client consumes the same DTOs and API rather than duplicating business rules.

Firebase paths used by the current Android client:

```text
ycats/users
ycats/updates
ycats/events
ycats/questions
ycats/chats
ycats/attendance
```

Do not put passwords in these shared DTOs. Authentication belongs to Firebase Authentication; profile and role data belong in the database.
