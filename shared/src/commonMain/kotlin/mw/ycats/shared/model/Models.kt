package mw.ycats.shared.model

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: UserRole = UserRole.MEMBER,
    val photoUrl: String = "",
    val status: AccountStatus = AccountStatus.ACTIVE,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

enum class UserRole {
    MEMBER,
    GUARDIAN,
    ADMIN
}

enum class AccountStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}

data class CommunityUpdate(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val authorUid: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val published: Boolean = true
)

data class CommunityEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val startTime: String = "",
    val location: String = "",
    val coverImageUrl: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val status: EventStatus = EventStatus.PUBLISHED
)

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    COMPLETED
}

data class QuestionThread(
    val id: String = "",
    val authorUid: String = "",
    val question: String = "",
    val status: QuestionStatus = QuestionStatus.OPEN,
    val assignedTo: String = "",
    val answer: String = "",
    val answeredBy: String = "",
    val createdAt: Long = 0L,
    val answeredAt: Long = 0L
)

enum class QuestionStatus {
    OPEN,
    IN_PROGRESS,
    ANSWERED,
    CLOSED
}

data class ChatMessage(
    val id: String = "",
    val questionId: String = "",
    val senderUid: String = "",
    val senderRole: UserRole = UserRole.MEMBER,
    val message: String = "",
    val createdAt: Long = 0L
)

data class AttendanceRecord(
    val uid: String = "",
    val date: String = "",
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val markedBy: String = "",
    val markedAt: Long = 0L
)

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    EXCUSED
}
