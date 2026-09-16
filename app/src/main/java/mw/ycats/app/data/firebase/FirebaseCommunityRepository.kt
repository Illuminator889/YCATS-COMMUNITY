package mw.ycats.app.data.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import mw.ycats.shared.model.CommunityEvent
import mw.ycats.shared.model.CommunityUpdate
import mw.ycats.shared.model.EventStatus
import mw.ycats.shared.model.QuestionStatus
import mw.ycats.shared.model.QuestionThread

class FirebaseCommunityRepository(
    database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val root = database.reference.child("ycats")

    fun updates(): Flow<List<CommunityUpdate>> = listen("updates") { it.toUpdates() }
    fun events(): Flow<List<CommunityEvent>> = listen("events") { it.toEvents() }
    fun questions(uid: String, isAdmin: Boolean): Flow<List<QuestionThread>> =
        listen("questions") { snapshot ->
            snapshot.toQuestions().filter { isAdmin || it.authorUid == uid }
        }

    suspend fun createQuestion(uid: String, question: String) {
        val ref = root.child("questions").push()
        val id = requireNotNull(ref.key)
        ref.setValue(mapOf(
            "id" to id, "authorUid" to uid, "question" to question.trim(),
            "status" to QuestionStatus.OPEN.name, "assignedTo" to "", "answer" to "",
            "answeredBy" to "", "createdAt" to System.currentTimeMillis(), "answeredAt" to 0L
        )).await()
    }

    suspend fun createUpdate(uid: String, title: String, message: String) {
        val ref = root.child("updates").push()
        val id = requireNotNull(ref.key)
        ref.setValue(mapOf("id" to id, "title" to title.trim(), "message" to message.trim(), "authorUid" to uid,
            "createdAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis(), "published" to true)).await()
    }

    suspend fun createEvent(uid: String, title: String, description: String, date: String, location: String) {
        val ref = root.child("events").push()
        val id = requireNotNull(ref.key)
        ref.setValue(mapOf("id" to id, "title" to title.trim(), "description" to description.trim(), "date" to date.trim(),
            "startTime" to "", "location" to location.trim(), "coverImageUrl" to "", "createdBy" to uid,
            "createdAt" to System.currentTimeMillis(), "status" to EventStatus.PUBLISHED.name)).await()
    }

    suspend fun answerQuestion(id: String, answer: String, adminUid: String) {
        root.child("questions").child(id).updateChildren(mapOf(
            "answer" to answer.trim(), "answeredBy" to adminUid,
            "answeredAt" to System.currentTimeMillis(), "status" to QuestionStatus.ANSWERED.name
        )).await()
    }

    private fun <T> listen(path: String, map: (DataSnapshot) -> List<T>): Flow<List<T>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(map(snapshot)) }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) { close(error.toException()) }
        }
        val ref = root.child(path)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun DataSnapshot.toUpdates() = children.mapNotNull { child ->
        CommunityUpdate(id = child.child("id").getValue(String::class.java) ?: child.key.orEmpty(),
            title = child.child("title").getValue(String::class.java).orEmpty(),
            message = child.child("message").getValue(String::class.java).orEmpty(),
            authorUid = child.child("authorUid").getValue(String::class.java).orEmpty(),
            createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L,
            updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L,
            published = child.child("published").getValue(Boolean::class.java) ?: true)
    }.sortedByDescending { it.createdAt }

    private fun DataSnapshot.toEvents() = children.mapNotNull { child ->
        CommunityEvent(id = child.child("id").getValue(String::class.java) ?: child.key.orEmpty(),
            title = child.child("title").getValue(String::class.java).orEmpty(),
            description = child.child("description").getValue(String::class.java).orEmpty(),
            date = child.child("date").getValue(String::class.java).orEmpty(),
            startTime = child.child("startTime").getValue(String::class.java).orEmpty(),
            location = child.child("location").getValue(String::class.java).orEmpty(),
            coverImageUrl = child.child("coverImageUrl").getValue(String::class.java).orEmpty(),
            createdBy = child.child("createdBy").getValue(String::class.java).orEmpty(),
            createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L,
            status = runCatching { EventStatus.valueOf(child.child("status").getValue(String::class.java) ?: "PUBLISHED") }.getOrDefault(EventStatus.PUBLISHED))
    }.sortedBy { it.date }

    private fun DataSnapshot.toQuestions() = children.mapNotNull { child ->
        QuestionThread(id = child.child("id").getValue(String::class.java) ?: child.key.orEmpty(),
            authorUid = child.child("authorUid").getValue(String::class.java).orEmpty(),
            question = child.child("question").getValue(String::class.java).orEmpty(),
            status = runCatching { QuestionStatus.valueOf(child.child("status").getValue(String::class.java) ?: "OPEN") }.getOrDefault(QuestionStatus.OPEN),
            assignedTo = child.child("assignedTo").getValue(String::class.java).orEmpty(),
            answer = child.child("answer").getValue(String::class.java).orEmpty(),
            answeredBy = child.child("answeredBy").getValue(String::class.java).orEmpty(),
            createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L,
            answeredAt = child.child("answeredAt").getValue(Long::class.java) ?: 0L)
    }.sortedByDescending { it.createdAt }
}
