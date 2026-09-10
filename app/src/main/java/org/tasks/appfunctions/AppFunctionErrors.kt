package org.tasks.appfunctions

import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionDeniedException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import org.tasks.api.ApiErrors
import org.tasks.api.ApiFailure

internal fun Throwable.toAppFunctionException(): AppFunctionException {
    val message = ApiErrors.explain(this)
    return when (ApiErrors.classify(this)) {
        ApiFailure.NotFound -> AppFunctionElementNotFoundException(message)
        ApiFailure.ReadOnly -> AppFunctionDeniedException(message)
        ApiFailure.BadRequest -> AppFunctionInvalidArgumentException(message)
        ApiFailure.Remote, ApiFailure.Unknown -> AppFunctionAppUnknownException(message)
    }
}
