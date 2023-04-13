package org.tasks.location

import android.content.Context
import android.os.Bundle
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.tasks.R
import org.tasks.data.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.CaldavDao
import org.tasks.data.Place
import org.tasks.http.HttpClientFactory
import org.tasks.http.HttpException
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class PlaceSearchGoogle @Inject constructor(
        @ApplicationContext private val context: Context,
        private val httpClientFactory: HttpClientFactory,
        private val caldavDao: CaldavDao
) : PlaceSearch {
    private val url = context.getString(R.string.tasks_places_url)
    private var token: String? = null

    override fun restoreState(savedInstanceState: Bundle?) {
        token = savedInstanceState?.getString(EXTRA_SESSION_TOKEN)
    }

    override fun saveState(outState: Bundle) {
        outState.putString(EXTRA_SESSION_TOKEN, token)
    }

    override fun getAttributionRes(dark: Boolean) = if (dark) {
        R.drawable.powered_by_google_on_non_white
    } else {
        R.drawable.powered_by_google_on_white
    }

    override suspend fun search(query: String, bias: MapPosition?): List<PlaceSearchResult> {
        if (token == null) {
            token = UUID.randomUUID().toString()
        }
        val proximity = bias?.let {
            "&location=${bias.latitude},${bias.longitude}&radius=25000"
        }
        val jsonObject = execute(
                "${this.url}/maps/api/place/queryautocomplete/json?input=$query&sessiontoken=$token$proximity"
        )
        return toSearchResults(jsonObject)
    }

    override suspend fun fetch(placeSearchResult: PlaceSearchResult): Place {
        val jsonObject = execute(
                "${this.url}/maps/api/place/details/json?place_id=${placeSearchResult.id}&fields=$FIELDS&sessiontoken=$token"
        )
        return toPlace(jsonObject)
    }

    private suspend fun execute(url: String): JsonObject = withContext(Dispatchers.IO) {
        Timber.d(url)
        val account = caldavDao.getAccounts(TYPE_TASKS).firstOrNull()
                ?: throw IllegalStateException(
                        context.getString(R.string.tasks_org_account_required)
                )
        val client = httpClientFactory
                .newClient(
                        foreground = true,
                        username = account.username,
                        encryptedPassword = account.password
                )
        val response = client.newCall(Request.Builder().get().url(url).build()).execute()
        if (response.isSuccessful) {
            response.body?.string()?.toJson()?.apply { checkResult(this) }
                    ?: throw IllegalStateException("Request failed")
        } else {
            throw HttpException(response.code, response.message)
        }
    }

    companion object {
        private const val EXTRA_SESSION_TOKEN = "extra_session_token"
        private val FIELDS =
                listOf(
                        "place_id",
                        "geometry/location",
                        "formatted_address",
                        "website",
                        "name",
                        "international_phone_number"
                ).joinToString(",")

        internal fun String.toJson(): JsonObject = JsonParser.parseString(this).asJsonObject

        private fun checkResult(json: JsonObject) {
            val status = json.get("status").asString
            when {
                status == "OK" -> return
                json.has("error_message") ->
                    throw IllegalStateException(json.get("error_message").asString)
                else ->
                    throw IllegalStateException(status)
            }
        }

        internal fun toSearchResults(json: JsonObject): List<PlaceSearchResult> =
                json.get("predictions")
                        .asJsonArray
                        .map { it.asJsonObject }
                        .filter { it.has("place_id") }
                        .map { toSearchEntry(it) }

        private fun toSearchEntry(json: JsonObject): PlaceSearchResult {
            val place = json.get("structured_formatting").asJsonObject
            return PlaceSearchResult(
                    json.get("place_id").asString,
                    place.get("main_text").asString,
                    place.get("secondary_text").asString
            )
        }

        internal fun toPlace(json: JsonObject): Place {
            val result = json.get("result").asJsonObject
            val location = result.get("geometry").asJsonObject.get("location").asJsonObject
            return Place(
                name = result.get("name").asString,
                address = result.getString("formatted_address"),
                phone = result.getString("international_phone_number"),
                url = result.getString("website"),
                latitude = location.get("lat").asDouble,
                longitude = location.get("lng").asDouble,
            )
        }

        private fun JsonObject.getString(field: String): String? = if (has(field)) {
            get(field).asString
        } else {
            null
        }
    }
}