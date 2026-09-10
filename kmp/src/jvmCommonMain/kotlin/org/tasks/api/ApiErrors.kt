package org.tasks.api

class ApiRowNotFound(val path: String, val id: Long) :
    NoSuchElementException("No row on /$path with ${TasksContract.ID} $id")

enum class ApiFailure {
    NotFound,
    ReadOnly,
    BadRequest,
    Remote,
    Unknown,
}

object ApiErrors {
    fun classify(error: Throwable): ApiFailure = when (error) {
        is ApiRowNotFound -> ApiFailure.NotFound
        is UnsupportedOperationException -> ApiFailure.ReadOnly
        is IllegalArgumentException -> ApiFailure.BadRequest
        is IllegalStateException -> ApiFailure.Remote
        else -> ApiFailure.Unknown
    }

    fun explain(error: Throwable): String {
        val detail = error.message.orEmpty()
        return when (classify(error)) {
            ApiFailure.NotFound ->
                "$detail. Nothing was written, and the same id will fail again - read the " +
                    "collection back to see what is there."

            ApiFailure.ReadOnly ->
                "That target is read-only, so the change was refused: $detail. This usually " +
                    "means the task is on a read-only shared list. Tell the user rather than " +
                    "retrying."

            ApiFailure.BadRequest ->
                "The request was malformed: $detail. Retrying it unchanged will fail again - " +
                    "check the argument names and values you sent."

            ApiFailure.Remote ->
                "A remote operation failed: $detail. This is usually the sync server rejecting " +
                    "the change."

            ApiFailure.Unknown -> "${error::class.simpleName}: $detail"
        }
    }
}
