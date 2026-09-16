package mw.ycats.app.data.firebase

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FirebaseCommunityRepository(database: FirebaseDatabase = FirebaseDatabase.getInstance()) {
    private val root = database.reference.child("ycats")
    suspend fun put(path: String, value: Any) = root.child(path).setValue(value).await()
    suspend fun read(path: String): Any? = root.child(path).get().await().value
    suspend fun delete(path: String) = root.child(path).removeValue().await()
    fun user(uid: String) = "users/$uid"
    fun update(id: String) = "updates/$id"
    fun event(id: String) = "events/$id"
    fun question(id: String) = "questions/$id"
    fun chat(questionId: String, messageId: String) = "chats/$questionId/$messageId"
    fun attendance(date: String, uid: String) = "attendance/$date/$uid"
}
