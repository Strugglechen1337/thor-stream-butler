package de.thorstream.butler.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    val message: String

    data class MissingPermission(override val message: String) : AppError
    data class NoNetwork(override val message: String = "Keine aktive Netzwerkverbindung gefunden.") : AppError
    data class InvalidInput(override val message: String) : AppError
    data class Unavailable(override val message: String) : AppError
    data class Timeout(override val message: String) : AppError
    data class Technical(override val message: String = "Die Funktion konnte nicht ausgeführt werden.") : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

