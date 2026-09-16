package mw.ycats.app.data.firebase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mw.ycats.shared.model.CommunityEvent
import mw.ycats.shared.model.CommunityUpdate
import mw.ycats.shared.model.QuestionThread
import mw.ycats.shared.model.UserProfile

class FirebaseViewModel(
    private val authRepo: FirebaseAuthRepository = FirebaseAuthRepository(),
    private val communityRepo: FirebaseCommunityRepository = FirebaseCommunityRepository()
) : ViewModel() {
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()
    private val _updates = MutableStateFlow<List<CommunityUpdate>>(emptyList())
    val updates: StateFlow<List<CommunityUpdate>> = _updates.asStateFlow()
    private val _events = MutableStateFlow<List<CommunityEvent>>(emptyList())
    val events: StateFlow<List<CommunityEvent>> = _events.asStateFlow()
    private val _questions = MutableStateFlow<List<QuestionThread>>(emptyList())
    val questions: StateFlow<List<QuestionThread>> = _questions.asStateFlow()
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    fun restoreSession() {
        val current = authRepo.currentUser()
        if (current == null) return
        viewModelScope.launch {
            runCatching { authRepo.loadProfile(current.uid) }.onSuccess { start(it) }.onFailure { _message.value = it.message.orEmpty() }
        }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        authRepo.login(email, password).onSuccess { start(it) }.onFailure { _message.value = it.message ?: "Login failed." }
    }

    fun register(email: String, password: String, username: String, fullName: String) = viewModelScope.launch {
        authRepo.register(email, password, username, fullName).onSuccess { start(it) }.onFailure { _message.value = it.message ?: "Registration failed." }
    }

    private fun start(profile: UserProfile) {
        _profile.value = profile
        viewModelScope.launch {
            communityRepo.updates().collect { _updates.value = it }
        }
        viewModelScope.launch {
            communityRepo.events().collect { _events.value = it }
        }
        viewModelScope.launch {
            communityRepo.questions(profile.uid, profile.role.name == "ADMIN").collect { _questions.value = it }
        }
    }

    fun ask(question: String) = viewModelScope.launch {
        val uid = _profile.value?.uid ?: return@launch
        runCatching { communityRepo.createQuestion(uid, question) }.onFailure { _message.value = it.message.orEmpty() }
    }

    fun publishUpdate(title: String, text: String) = viewModelScope.launch {
        val uid = _profile.value?.uid ?: return@launch
        runCatching { communityRepo.createUpdate(uid, title, text) }.onFailure { _message.value = it.message.orEmpty() }
    }

    fun publishEvent(title: String, description: String, date: String, location: String) = viewModelScope.launch {
        val uid = _profile.value?.uid ?: return@launch
        runCatching { communityRepo.createEvent(uid, title, description, date, location) }.onFailure { _message.value = it.message.orEmpty() }
    }

    fun answer(id: String, text: String) = viewModelScope.launch {
        val uid = _profile.value?.uid ?: return@launch
        runCatching { communityRepo.answerQuestion(id, text, uid) }.onFailure { _message.value = it.message.orEmpty() }
    }

    fun logout() { authRepo.logout(); _profile.value = null }
}
