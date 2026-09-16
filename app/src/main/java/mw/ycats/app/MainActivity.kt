package mw.ycats.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.dataStore by preferencesDataStore("ycats_settings")
private const val FIREBASE_URL = "https://ycats-community-default-rtdb.firebaseio.com"

data class User(
    val username: String,
    val password: String,
    val role: String,
    val fullName: String,
    val photo: String = ""
)

data class Update(
    val id: Long = 0,
    val title: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class Event(
    val id: Long = 0,
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class Question(
    val id: Long = 0,
    val username: String,
    val question: String,
    val answer: String = "",
    val answeredBy: String = "",
    val answeredAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: Long = 0,
    val questionId: Long,
    val sender: String,
    val role: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class Attendance(
    val id: Long = 0,
    val username: String,
    val date: String,
    val status: String
)

class YcatsDb(private val context: Context) {
    private val keyUsers = stringPreferencesKey("users_json")
    private val keyUpdates = stringPreferencesKey("updates_json")
    private val keyEvents = stringPreferencesKey("events_json")
    private val keyQuestions = stringPreferencesKey("questions_json")
    private val keyChats = stringPreferencesKey("chats_json")
    private val keyAttendance = stringPreferencesKey("attendance_json")

    private val gson = Gson()

    private suspend inline fun <reified T> read(key: Preferences.Key<String>, fallback: T): T =
        context.dataStore.data.first()[key]?.let {
            gson.fromJson(it, object : TypeToken<T>() {}.type)
        } ?: fallback

    private suspend fun <T> write(key: Preferences.Key<String>, value: T) {
        context.dataStore.edit { it[key] = gson.toJson(value) }
    }

    suspend fun seed() {
        val users: List<User> = read(keyUsers, emptyList())
        if (users.isEmpty()) {
            write(keyUsers, listOf(User("admin", hashPassword("admin123"), "admin", "YCATS Administrator")))
        }

        if (read<List<Update>>(keyUpdates, emptyList()).isEmpty()) {
            write(
                keyUpdates,
                listOf(
                    Update(title = "Welcome to YCATS", message = "Welcome to the YCATS community!"),
                    Update(title = "Counselling session", message = "Support services are now available for members."),
                )
            )
        }

        if (read<List<Event>>(keyEvents, emptyList()).isEmpty()) {
            write(
                keyEvents,
                listOf(
                    Event(
                        title = "Youth Talent Showcase",
                        description = "Performances, speeches and community recognition.",
                        date = "2026-12-20",
                        location = "Main Hall"
                    )
                )
            )
        }
    }

    suspend fun register(username: String, password: String, fullName: String): Pair<Boolean, String> {
        if (username.length < 3 || password.length < 6 || fullName.isBlank()) {
            return false to "Please complete all fields. Username 3+ chars and password 6+ chars."
        }

        val users: MutableList<User> = read(keyUsers, mutableListOf())
        if (users.any { it.username.equals(username, ignoreCase = true) }) {
            return false to "Username already exists."
        }

        users.add(User(username, hashPassword(password), "member", fullName))
        write(keyUsers, users)
        return true to "Account created successfully."
    }

    suspend fun login(username: String, password: String): User? {
        val users: List<User> = read(keyUsers, emptyList())
        return users.find { it.username.equals(username, true) && verifyPassword(password, it.password) }
    }

    suspend fun users(): List<User> = read(keyUsers, emptyList())
    suspend fun updates(): List<Update> = read(keyUpdates, emptyList()).sortedByDescending { it.createdAt }
    suspend fun events(): List<Event> = read(keyEvents, emptyList()).sortedBy { it.date }
    suspend fun questions(): List<Question> = read(keyQuestions, emptyList()).sortedByDescending { it.createdAt }
    suspend fun chats(qid: Long): List<ChatMessage> = read(keyChats, emptyList()).filter { it.questionId == qid }.sortedBy { it.createdAt }
    suspend fun attendance(): List<Attendance> = read(keyAttendance, emptyList()).sortedByDescending { it.date }

    suspend fun addUpdate(title: String, message: String) {
        val x = read<List<Update>>(keyUpdates, emptyList()).toMutableList()
        x.add(Update(title = title, message = message))
        write(keyUpdates, x)
    }

    suspend fun deleteUpdate(id: Long) {
        write(keyUpdates, read<List<Update>>(keyUpdates, emptyList()).filter { it.id != id })
    }

    suspend fun addEvent(title: String, description: String, date: String, location: String) {
        val x = read<List<Event>>(keyEvents, emptyList()).toMutableList()
        x.add(Event(title = title, description = description, date = date, location = location))
        write(keyEvents, x)
    }

    suspend fun deleteEvent(id: Long) {
        write(keyEvents, read<List<Event>>(keyEvents, emptyList()).filter { it.id != id })
    }

    suspend fun addQuestion(user: String, q: String) {
        val x = read<List<Question>>(keyQuestions, emptyList()).toMutableList()
        x.add(Question(username = user, question = q))
        write(keyQuestions, x)
    }

    suspend fun answerQuestion(id: Long, answer: String, by: String) {
        val x = read<List<Question>>(keyQuestions, emptyList()).map {
            if (it.id == id) it.copy(answer = answer, answeredBy = by, answeredAt = System.currentTimeMillis()) else it
        }
        write(keyQuestions, x)
    }

    suspend fun addChat(qid: Long, user: String, role: String, msg: String) {
        val x = read<List<ChatMessage>>(keyChats, emptyList()).toMutableList()
        x.add(ChatMessage(questionId = qid, sender = user, role = role, message = msg))
        write(keyChats, x)
    }

    suspend fun setRole(username: String, role: String) {
        val x = read<List<User>>(keyUsers, emptyList()).map {
            if (it.username == username) it.copy(role = role) else it
        }
        write(keyUsers, x)
    }

    suspend fun updatePhoto(username: String, photo: String) {
        val x = read<List<User>>(keyUsers, emptyList()).map {
            if (it.username == username) it.copy(photo = photo) else it
        }
        write(keyUsers, x)
    }

    suspend fun markAttendance(user: String, date: String, status: String) {
        val x = read<List<Attendance>>(keyAttendance, emptyList()).toMutableList()
        x.removeAll { it.username == user && it.date == date }
        x.add(Attendance(username = user, date = date, status = status))
        write(keyAttendance, x)
    }

    suspend fun snapshot(): Map<String, Any> {
        return mapOf(
            "users" to users(),
            "updates" to updates(),
            "events" to events(),
            "questions" to questions(),
            "attendance" to attendance()
        )
    }
}

private fun hashPassword(password: String): String {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    val spec = PBEKeySpec(password.toCharArray(), salt, 120000, 256)
    val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    return "pbkdf2$" + salt.toHex() + "$" + hash.toHex()
}

private fun verifyPassword(password: String, stored: String): Boolean {
    return try {
        val parts = stored.split("$")
        if (parts.size != 3) return false

        val salt = parts[1].hexToBytes()
        val expected = parts[2].hexToBytes()
        val spec = PBEKeySpec(password.toCharArray(), salt, 120000, expected.size * 8)
        val computed = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        MessageDigest.isEqual(expected, computed)
    } catch (_: Exception) {
        false
    }
}

private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
private fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

class FirebaseSync {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun push(data: Map<String, Any>, callback: (Boolean, String) -> Unit) {
        Thread {
            try {
                val body = gson.toJson(data).toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url("$FIREBASE_URL/ycats.json").put(body).build()
                client.newCall(request).execute().use { response ->
                    callback(response.isSuccessful, if (response.isSuccessful) "Cloud sync complete" else "Cloud sync failed: ${response.code}")
                }
            } catch (e: Exception) {
                callback(false, "Cloud sync error: ${e.message}")
            }
        }.start()
    }
}

class MainViewModel(private val db: YcatsDb) : ViewModel() {
    var user by mutableStateOf<User?>(null)
        private set

    var screen by mutableStateOf("home")
    var message by mutableStateOf("")
    var updates by mutableStateOf(emptyList<Update>())
    var events by mutableStateOf(emptyList<Event>())
    var questions by mutableStateOf(emptyList<Question>())
    var users by mutableStateOf(emptyList<User>())
    var attendance by mutableStateOf(emptyList<Attendance>())
    var chats by mutableStateOf(emptyList<ChatMessage>())
    var online by mutableStateOf(false)

    private val sync = FirebaseSync()

    fun init() = viewModelScope.launch {
        db.seed()
        loadAll()
    }

    private suspend fun loadAll() {
        updates = db.updates()
        events = db.events()
        questions = db.questions()
        users = db.users()
        attendance = db.attendance()
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val result = db.login(username, password)
            if (result != null) {
                user = result
                screen = "home"
                loadAll()
                message = "Welcome, ${result.fullName}"
            } else {
                message = "Invalid username or password."
            }
        }
    }

    fun register(username: String, password: String, fullName: String) {
        viewModelScope.launch {
            val result = db.register(username, password, fullName)
            message = result.second
            if (result.first) {
                user = db.login(username, password)
                screen = "home"
                loadAll()
            }
        }
    }

    fun logout() {
        user = null
        screen = "login"
    }

    fun refresh() = viewModelScope.launch { loadAll() }
    fun addUpdate(title: String, message: String) = viewModelScope.launch { db.addUpdate(title, message); loadAll() }
    fun addEvent(title: String, description: String, date: String, location: String) = viewModelScope.launch { db.addEvent(title, description, date, location); loadAll() }
    fun addQuestion(question: String) {
        if (question.isBlank() || user == null) return
        viewModelScope.launch {
            db.addQuestion(user!!.username, question)
            loadAll()
            screen = "ask"
        }
    }

    fun openQuestion(question: Question) = viewModelScope.launch {
        chats = db.chats(question.id)
        screen = "chat"
    }

    fun sendChat(questionId: Long, msg: String) {
        if (msg.isBlank() || user == null) return
        viewModelScope.launch {
            db.addChat(questionId, user!!.username, user!!.role, msg)
            chats = db.chats(questionId)
        }
    }

    fun answerQuestion(questionId: Long, answer: String) {
        if (answer.isBlank() || user == null) return
        viewModelScope.launch {
            db.answerQuestion(questionId, answer, user!!.username)
            loadAll()
        }
    }

    fun setRole(username: String, role: String) = viewModelScope.launch {
        if (username != user?.username) db.setRole(username, role)
        loadAll()
    }

    fun updatePhoto(photo: String) = viewModelScope.launch {
        if (user == null) return@launch
        db.updatePhoto(user!!.username, photo)
        user = db.users().find { it.username == user!!.username }
        loadAll()
    }

    fun markAttendance(username: String, status: String) = viewModelScope.launch {
        db.markAttendance(username, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()), status)
        loadAll()
    }

    fun syncCloud() = viewModelScope.launch {
        sync.push(db.snapshot()) { _, msg ->
            message = msg
        }
    }

    fun checkNetwork(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        online = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}

private fun date(ms: Long): String =
    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(ms))

@Composable
fun YCATSTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF1565C0),
        secondary = Color(0xFF00A86B),
        background = Color(0xFFF5F7FB),
        surface = Color.White,
        onPrimary = Color.White,
        onSurface = Color(0xFF132238)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

@Composable
fun YCATSApp() {
    val context = LocalContext.current
    val vm = remember(context) { MainViewModel(YcatsDb(context)) }

    LaunchedEffect(Unit) {
        vm.init()
        vm.checkNetwork(context)
    }

    YCATSTheme {
        if (vm.user == null) {
            if (vm.screen == "register") RegisterScreen(vm) else LoginScreen(vm)
        } else {
            MainShell(vm)
        }
    }
}

@Composable
fun LoginScreen(vm: MainViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B1F3A),
                        Color(0xFF1565C0),
                        Color(0xFF00A86B)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("YCATS", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1F3A))
                Text("Youth Counselling And Talents Show", color = Color(0xFF4B647D))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Button(
                    onClick = { vm.login(username, password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }

                TextButton(onClick = { vm.screen = "register" }) {
                    Text("Create account")
                }

                if (vm.message.isNotBlank()) {
                    Text(vm.message, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(vm: MainViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B1F3A),
                        Color(0xFF1565C0),
                        Color(0xFF00A86B)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Create account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (6+ characters)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(
                    onClick = { vm.register(username, password, fullName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Register")
                }
                TextButton(onClick = { vm.screen = "login" }) {
                    Text("Back to login")
                }
                if (vm.message.isNotBlank()) {
                    Text(vm.message, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(vm: MainViewModel) {
    val navItems = listOf(
        "home" to Icons.Default.Home,
        "updates" to Icons.Default.Notifications,
        "events" to Icons.Default.Event,
        "ask" to Icons.Default.QuestionAnswer,
        "profile" to Icons.Default.Person,
        "attendance" to Icons.Default.CheckCircle
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YCATS Community") },
                actions = {
                    Icon(
                        imageVector = if (vm.online) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (vm.online) Color(0xFF00A86B) else Color(0xFFEA5455)
                    )
                    IconButton(onClick = { vm.syncCloud() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                    }
                    IconButton(onClick = { vm.logout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                navItems.forEach { (screen, icon) ->
                    NavigationBarItem(
                        selected = vm.screen == screen,
                        onClick = { vm.screen = screen },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(screen.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .background(Color(0xFFF5F7FB))
        ) {
            when (vm.screen) {
                "home" -> HomeScreen(vm)
                "updates" -> UpdatesScreen(vm)
                "events" -> EventsScreen(vm)
                "ask" -> QuestionsScreen(vm)
                "chat" -> ChatScreen(vm)
                "profile" -> ProfileScreen(vm)
                "attendance" -> AttendanceScreen(vm)
                "admin" -> AdminScreen(vm)
                else -> HomeScreen(vm)
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, accent: Color = Color(0xFF1565C0)) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(title, color = Color(0xFF5E6C84))
        }
    }
}

@Composable
fun HomeScreen(vm: MainViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Welcome, ${vm.user?.fullName ?: "Member"}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1F3A)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("Updates", vm.updates.size.toString())
                SummaryCard("Events", vm.events.size.toString(), Color(0xFF00A86B))
                SummaryCard("Questions", vm.questions.size.toString(), Color(0xFF8E44AD))
            }
        }

        item {
            Button(
                onClick = { vm.screen = "updates" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("View latest updates")
            }
        }

        item {
            Button(
                onClick = { vm.screen = "events" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("View events")
            }
        }

        item {
            Button(
                onClick = { vm.screen = "ask" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Ask YCATS")
            }
        }

        if (vm.user?.role == "admin") {
            item {
                Button(
                    onClick = { vm.screen = "admin" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Admin dashboard")
                }
            }
        }

        if (vm.message.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = vm.message,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF0B1F3A)
                    )
                }
            }
        }
    }
}

@Composable
fun UpdatesScreen(vm: MainViewModel) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vm.updates) { update ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(update.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0B1F3A))
                    Text(update.message, color = Color(0xFF4B647D))
                    Text(date(update.createdAt), color = Color(0xFF6A7A8F), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EventsScreen(vm: MainViewModel) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vm.events) { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(event.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF0B1F3A))
                    Text(event.description, color = Color(0xFF4B647D))
                    Text("Date: ${event.date}", color = Color(0xFF1565C0))
                    Text("Location: ${event.location}", color = Color(0xFF6A7A8F))
                }
            }
        }
    }
}

@Composable
fun QuestionsScreen(vm: MainViewModel) {
    var question by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ask the community", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Your question") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Button(
                        onClick = {
                            vm.addQuestion(question)
                            question = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Send question")
                    }
                }
            }
        }

        items(vm.questions.filter { it.username == vm.user?.username || vm.user?.role == "admin" }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { vm.openQuestion(item) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.question, fontWeight = FontWeight.SemiBold)
                    if (item.answer.isNotBlank()) {
                        Text("Answer: ${item.answer}", color = Color(0xFF00A86B))
                    } else {
                        Text("Waiting for reply…", color = Color(0xFF6A7A8F))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(vm: MainViewModel) {
    var messageText by remember { mutableStateOf("") }
    val currentQuestion = vm.questions.firstOrNull { it.id == vm.chats.firstOrNull()?.questionId } ?: vm.questions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Conversation", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vm.chats) { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${msg.sender} (${msg.role})", fontWeight = FontWeight.Bold)
                        Text(msg.message)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                label = { Text("Reply") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    currentQuestion?.let { vm.sendChat(it.id, messageText); messageText = "" }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: MainViewModel) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { vm.updatePhoto(it.toString()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3EFFD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(70.dp), tint = Color(0xFF1565C0))
        }

        Text(vm.user?.fullName ?: "Member", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("@${vm.user?.username ?: ""}", color = Color(0xFF4B647D))
        Text("Role: ${vm.user?.role ?: "member"}", color = Color(0xFF6A7A8F))

        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Choose profile photo")
        }

        if (vm.user?.role == "admin") {
            Button(
                onClick = { vm.screen = "admin" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Open admin dashboard")
            }
        }
    }
}

@Composable
fun AttendanceScreen(vm: MainViewModel) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Attendance", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        items(vm.users) { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullName, fontWeight = FontWeight.SemiBold)
                        Text(user.username, color = Color(0xFF6A7A8F))
                    }
                    TextButton(onClick = { vm.markAttendance(user.username, "Present") }) {
                        Text("Present")
                    }
                    TextButton(onClick = { vm.markAttendance(user.username, "Absent") }) {
                        Text("Absent")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("History", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        items(vm.attendance) { attendance ->
            Text("${attendance.date} • ${attendance.username} • ${attendance.status}", color = Color(0xFF4B647D))
        }
    }
}

@Composable
fun AdminScreen(vm: MainViewModel) {
    var title by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var eventTitle by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Admin Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Text("Users: ${vm.users.size}   Updates: ${vm.updates.size}   Events: ${vm.events.size}   Questions: ${vm.questions.size}")
        }

        item { Divider() }

        item {
            Text("Create update", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = msg,
                onValueChange = { msg = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        item {
            Button(
                onClick = {
                    vm.addUpdate(title, msg)
                    title = ""
                    msg = ""
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Publish update")
            }
        }

        item { Divider() }

        item {
            Text("Create event", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        item {
            OutlinedTextField(
                value = eventTitle,
                onValueChange = { eventTitle = it },
                label = { Text("Event title") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = eventDescription,
                onValueChange = { eventDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        item {
            OutlinedTextField(
                value = eventDate,
                onValueChange = { eventDate = it },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = eventLocation,
                onValueChange = { eventLocation = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = {
                    vm.addEvent(eventTitle, eventDescription, eventDate, eventLocation)
                    eventTitle = ""
                    eventDescription = ""
                    eventDate = ""
                    eventLocation = ""
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Create event")
            }
        }

        item { Divider() }

        item {
            Text("Manage users", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        items(vm.users) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${user.fullName} (@${user.username})", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { vm.setRole(user.username, "member") }) { Text("Member") }
                        TextButton(onClick = { vm.setRole(user.username, "guardian") }) { Text("Guardian") }
                        TextButton(onClick = { vm.setRole(user.username, "admin") }) { Text("Admin") }
                    }
                }
            }
        }

        item { Divider() }

        item {
            Text("Question management", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        items(vm.questions) { question ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${question.username}: ${question.question}")
                    if (question.answer.isNotBlank()) {
                        Text("Answer: ${question.answer}", color = Color(0xFF00A86B))
                    } else {
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                vm.answerQuestion(question.id, answer)
                                answer = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Reply")
                        }
                    }
                }
            }
        }

        item { Divider() }

        item {
            Text("Cloud sync", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Button(onClick = { vm.syncCloud() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text("Sync all data to Firebase")
            }
        }

        item {
            Button(onClick = { vm.refresh() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text("Refresh local data")
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YCATSApp()
        }
    }
}
