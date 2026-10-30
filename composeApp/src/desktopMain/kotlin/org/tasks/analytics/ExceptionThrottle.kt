package org.tasks.analytics

class ExceptionThrottle(
    private val perFingerprintLimit: Int = 3,
    private val globalLimit: Int = 20,
    private val windowMillis: Long = 60 * 60 * 1000L,
    private val maxFingerprints: Int = 500,
    private val now: () -> Long = System::currentTimeMillis,
) {
    sealed interface Decision {
        data class Report(val suppressed: Int) : Decision
        data object Drop : Decision
    }

    private class Bucket(var windowStart: Long, var count: Int = 0, var suppressed: Int = 0)

    private val buckets = LinkedHashMap<String, Bucket>(16, 0.75f, true)
    private var globalWindowStart = 0L
    private var globalCount = 0

    @Synchronized
    fun check(t: Throwable): Decision {
        val time = now()
        if (time - globalWindowStart >= windowMillis) {
            globalWindowStart = time
            globalCount = 0
        }
        val key = fingerprint(t)
        val bucket = buckets.getOrPut(key) {
            if (buckets.size >= maxFingerprints) {
                buckets.remove(buckets.keys.first())
            }
            Bucket(time)
        }
        if (time - bucket.windowStart >= windowMillis) {
            bucket.windowStart = time
            bucket.count = 0
        }
        if (bucket.count >= perFingerprintLimit || globalCount >= globalLimit) {
            bucket.suppressed++
            return Decision.Drop
        }
        bucket.count++
        globalCount++
        val suppressed = bucket.suppressed
        bucket.suppressed = 0
        return Decision.Report(suppressed)
    }

    companion object {
        private const val FRAMES = 5

        internal fun fingerprint(t: Throwable): String = buildString {
            append(t.javaClass.name)
            t.stackTrace.take(FRAMES).forEach { append('|').append(it) }
            t.stackTrace.firstOrNull { it.className.startsWith("org.tasks.") }
                ?.let { append("|app:").append(it) }
            t.cause?.let { append("|cause:").append(it.javaClass.name) }
        }
    }
}
