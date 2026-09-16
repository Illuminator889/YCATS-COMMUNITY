package mw.ycats.app.domain.validation

object AuthValidation {
    private val usernamePattern = Regex("^[a-zA-Z0-9._-]{3,24}$")
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    fun validateFullName(value: String): String? {
        val name = value.trim()
        return when {
            name.isEmpty() -> "Enter your full name."
            name.length < 2 -> "Your name must contain at least 2 characters."
            name.length > 80 -> "Your name must be 80 characters or fewer."
            else -> null
        }
    }

    fun validateUsername(value: String): String? {
        val username = value.trim()
        return when {
            username.isEmpty() -> "Enter a username."
            !usernamePattern.matches(username) -> "Use 3–24 letters, numbers, dots, dashes or underscores."
            else -> null
        }
    }

    fun validateEmail(value: String): String? {
        val email = value.trim()
        return when {
            email.isEmpty() -> "Enter your email address."
            !emailPattern.matches(email) -> "Enter a valid email address."
            else -> null
        }
    }

    fun validatePassword(value: String): String? {
        return when {
            value.length < 8 -> "Password must contain at least 8 characters."
            value.none { it.isUpperCase() } -> "Password must contain an uppercase letter."
            value.none { it.isLowerCase() } -> "Password must contain a lowercase letter."
            value.none { it.isDigit() } -> "Password must contain a number."
            else -> null
        }
    }

    fun validatePasswordConfirmation(password: String, confirmation: String): String? {
        return when {
            confirmation.isEmpty() -> "Confirm your password."
            password != confirmation -> "Passwords do not match."
            else -> null
        }
    }

    fun validateQuestion(value: String): String? {
        val q = value.trim()
        return when {
            q.isEmpty() -> "Write a question before sending."
            q.length < 10 -> "Your question is too short."
            q.length > 1000 -> "Question must be 1,000 characters or fewer."
            else -> null
        }
    }

    fun validateEvent(title: String, description: String, date: String, location: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (title.trim().length < 3) {
            errors["title"] = "Event title must contain at least 3 characters."
        }

        if (description.trim().length < 10) {
            errors["description"] = "Add a little more detail about this event."
        }

        if (!date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            errors["date"] = "Use the format YYYY-MM-DD."
        }

        if (location.trim().isEmpty()) {
            errors["location"] = "Enter the event location."
        }

        return errors
    }
}
