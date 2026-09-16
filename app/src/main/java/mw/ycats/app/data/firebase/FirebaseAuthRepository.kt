package mw.ycats.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import mw.ycats.shared.model.AccountStatus
import mw.ycats.shared.model.UserProfile
import mw.ycats.shared.model.UserRole

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val users = database.reference.child("ycats/users")
    private val usernameIndex = database.reference.child("ycats/usernameIndex")

    suspend fun register(email: String, password: String, username: String, fullName: String): Result<UserProfile> = runCatching {
        val normalized = username.trim().lowercase()
        check(!usernameIndex.child(normalized).get().await().exists()) { "Username already exists." }
        val firebaseUser = requireNotNull(auth.createUserWithEmailAndPassword(email.trim(), password).await().user)
        val now = System.currentTimeMillis()
        val profile = UserProfile(firebaseUser.uid, username.trim(), fullName.trim(), email.trim(), UserRole.MEMBER, createdAt = now, updatedAt = now)
        users.child(firebaseUser.uid).setValue(profile).await()
        usernameIndex.child(normalized).setValue(mapOf("uid" to firebaseUser.uid)).await()
        firebaseUser.sendEmailVerification().await()
        profile
    }

    suspend fun login(email: String, password: String): Result<UserProfile> = runCatching {
        val firebaseUser = requireNotNull(auth.signInWithEmailAndPassword(email.trim(), password).await().user)
        loadProfile(firebaseUser.uid)
    }

    suspend fun loadProfile(uid: String = requireNotNull(auth.currentUser).uid): UserProfile {
        val snapshot = users.child(uid).get().await()
        return UserProfile(
            uid = snapshot.child("uid").getValue(String::class.java) ?: uid,
            username = snapshot.child("username").getValue(String::class.java).orEmpty(),
            fullName = snapshot.child("fullName").getValue(String::class.java).orEmpty(),
            email = snapshot.child("email").getValue(String::class.java).orEmpty(),
            role = runCatching { UserRole.valueOf(snapshot.child("role").getValue(String::class.java)?.uppercase() ?: "MEMBER") }.getOrDefault(UserRole.MEMBER),
            photoUrl = snapshot.child("photoUrl").getValue(String::class.java).orEmpty(),
            status = runCatching { AccountStatus.valueOf(snapshot.child("status").getValue(String::class.java)?.uppercase() ?: "ACTIVE") }.getOrDefault(AccountStatus.ACTIVE),
            createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
            updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
        )
    }

    suspend fun sendPasswordReset(email: String) = auth.sendPasswordResetEmail(email.trim()).await()
    fun logout() = auth.signOut()
}
