package mw.ycats.app

/** Centralized input validation used by the authentication screens. */
object AuthValidation {
    private val usernamePattern = Regex("^[a-zA-Z0-9._-]{3,24}$")

    fun username(value: String): String? = when {
        value.isBlank() -> "Enter your username."
        !usernamePattern.matches(value.trim()) -> "Use 3–24 letters, numbers, dots, dashes or underscores."
        else -> null
    }

    fun fullName(value: String): String? = when {
        value.trim().length < 2 -> "Enter your full name."
        value.trim().length > 80 -> "Name must be 80 characters or fewer."
        else -> null
    }

    fun password(value: String): String? = when {
        value.length < 8 -> "Password must be at least 8 characters."
        value.none { it.isUpperCase() } -> "Add at least one uppercase letter."
        value.none { it.isDigit() } -> "Add at least one number."
        else -> null
    }
}
