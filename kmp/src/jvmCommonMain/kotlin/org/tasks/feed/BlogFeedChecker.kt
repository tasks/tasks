package org.tasks.feed

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import org.tasks.analytics.CrashReporting
import org.tasks.http.OkHttpClientFactory
import org.tasks.jobs.WorkResult
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.time.DateTimeUtils2
import java.text.SimpleDateFormat
import java.util.Locale

class BlogFeedChecker(
    private val httpClientFactory: OkHttpClientFactory,
    private val tasksPreferences: TasksPreferences,
    private val appPreferences: AppPreferences,
    private val crashReporting: CrashReporting,
    private val feedUrl: String = FEED_URL,
) {
    suspend fun check(): WorkResult {
        val mode = BlogFeedMode.fromValue(
            tasksPreferences.get(TasksPreferences.blogFeedMode, BlogFeedMode.ANNOUNCEMENTS.value)
        )
        if (mode == BlogFeedMode.NONE) {
            logger.i { "Blog check disabled" }
            tasksPreferences.set(TasksPreferences.blogPendingPost, "")
            tasksPreferences.set(TasksPreferences.blogLastChecked, DateTimeUtils2.currentTimeMillis())
            return WorkResult.Success
        }
        val xml = fetchFeed() ?: return WorkResult.Fail
        val posts = try {
            RssParser.parse(xml)
        } catch (e: Exception) {
            logger.w(e) { "Failed to parse blog feed" }
            crashReporting.reportException(e)
            return WorkResult.Fail
        }
        tasksPreferences.set(TasksPreferences.blogLastChecked, DateTimeUtils2.currentTimeMillis())
        val installDate = appPreferences.getInstallDate()
        val latest = posts
            .let { if (mode == BlogFeedMode.ANNOUNCEMENTS) it.filter { p -> "announcement" in p.categories } else it }
            .mapNotNull { post -> parseRfc2822(post.pubDate)?.let { ts -> post to ts } }
            .filter { (_, ts) -> ts > installDate }
            .maxByOrNull { (_, ts) -> ts }
            ?.first
        if (latest == null) {
            logger.d { "No new announcement posts found" }
            tasksPreferences.set(TasksPreferences.blogPendingPost, "")
            return WorkResult.Success
        }
        val dismissedId = tasksPreferences.get(TasksPreferences.blogDismissedPostId, "")
        if (latest.guid == dismissedId) {
            logger.d { "Latest announcement already dismissed: ${latest.guid}" }
            tasksPreferences.set(TasksPreferences.blogPendingPost, "")
            return WorkResult.Success
        }
        logger.d { "New announcement: ${latest.title}" }
        tasksPreferences.set(TasksPreferences.blogPendingPost, Json.encodeToString(latest))
        return WorkResult.Success
    }

    private suspend fun fetchFeed(): String? = withContext(Dispatchers.IO) {
        try {
            val client = httpClientFactory.newClient()
            val request = Request.Builder()
                .url(feedUrl)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body.string()
                } else {
                    logger.w { "Feed fetch failed: ${response.code}" }
                    null
                }
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to fetch blog feed" }
            null
        }
    }

    companion object {
        private val logger = Logger.withTag("BlogFeedChecker")
        const val FEED_URL = "https://tasks.org/blog/rss.xml"

        internal fun parseRfc2822(date: String): Long? = try {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).parse(date)?.time
        } catch (_: Exception) {
            null
        }
    }
}
