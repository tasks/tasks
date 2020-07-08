package org.tasks.data

import com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Throws(InterruptedException::class)
fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T {
    assertNotMainThread()

    return kotlinx.coroutines.runBlocking(context, block)
}

