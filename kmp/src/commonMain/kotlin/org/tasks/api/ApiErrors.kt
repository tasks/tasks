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
        val detail = error.message.orEmpty().trimEnd('.')
        return when (classify(error)) {
            ApiFailure.NotFound ->
                "$detail. Nothing was written, and the same id will fail again."

            ApiFailure.ReadOnly ->
                "That target is read-only, so the change was refused: $detail. This usually " +
                    "means the task is on a read-only shared list, and retrying will fail the " +
                    "same way."

            ApiFailure.BadRequest ->
                "The request was malformed: $detail. Nothing was written, and retrying it " +
                    "unchanged will fail again."

            ApiFailure.Remote ->
                "A remote operation failed: $detail. This is usually the sync server rejecting " +
                    "the change."

            ApiFailure.Unknown -> "${error::class.simpleName}: $detail"
        }
    }
}
