package org.tasks.compose

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.throttleLatest(period: Long) = flow {
    conflate().collect {
        emit(it)
        delay(period)
    }
}
