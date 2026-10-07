package org.tasks.resources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString as composeGetPluralString
import org.jetbrains.compose.resources.getString as composeGetString
import org.jetbrains.compose.resources.getStringArray as composeGetStringArray

suspend fun getString(resource: StringResource): String =
    withContext(Dispatchers.Default) { composeGetString(resource) }

suspend fun getString(resource: StringResource, vararg formatArgs: Any): String =
    withContext(Dispatchers.Default) { composeGetString(resource, *formatArgs) }

suspend fun getPluralString(resource: PluralStringResource, quantity: Int): String =
    withContext(Dispatchers.Default) { composeGetPluralString(resource, quantity) }

suspend fun getPluralString(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String =
    withContext(Dispatchers.Default) { composeGetPluralString(resource, quantity, *formatArgs) }

suspend fun getStringArray(resource: StringArrayResource): List<String> =
    withContext(Dispatchers.Default) { composeGetStringArray(resource) }
