package mw.ycats.shared

/** Firebase-safe, password-free data contract for all YCATS clients. */
data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val role: String = "member",
    val photoUrl: String = "",
    val createdAt: Long = 0L
)

data class CommunityUpdate(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val authorUid: String = ""
)

data class CommunityEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val createdAt: Long = 0L
)

data class QuestionThread(
    val id: String = "",
    val authorUid: String = "",
    val question: String = "",
    val answer: String = "",
    val answeredBy: String = "",
    val createdAt: Long = 0L,
    val answeredAt: Long = 0L
)
