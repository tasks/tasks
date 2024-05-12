package org.tasks.location

import android.content.Context
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import org.tasks.R
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.Place
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

        internal fun String.toJson(): JsonObject = Json.parseToJsonElement(this).jsonObject

        private fun checkResult(json: JsonObject) {
            val status = json["status"]?.jsonPrimitive?.content
            when {
                status == "OK" -> return
                else -> throw IllegalStateException(
                    json["error_message"]?.jsonPrimitive?.content ?: status
                )
            }
        }

        internal fun toSearchResults(json: JsonObject): List<PlaceSearchResult> =
                json["predictions"]!!
                        .jsonArray
                        .map { it.jsonObject }
                        .filter { it.contains("place_id") }
                        .map { toSearchEntry(it) }

        private fun toSearchEntry(json: JsonObject): PlaceSearchResult {
            val place = json["structured_formatting"]!!.jsonObject
            return PlaceSearchResult(
                    json["place_id"]!!.jsonPrimitive.content,
                    place["main_text"]!!.jsonPrimitive.content,
                    place["secondary_text"]!!.jsonPrimitive.content,
            )
        }

        internal fun toPlace(json: JsonObject): Place {
            val result = json["result"]!!.jsonObject
            val location = result["geometry"]!!.jsonObject["location"]!!.jsonObject
            return Place(
                name = result["name"]!!.jsonPrimitive.content,
                address = result["formatted_address"]?.jsonPrimitive?.content,
                phone = result["international_phone_number"]?.jsonPrimitive?.content,
                url = result["website"]?.jsonPrimitive?.content,
                latitude = location["lat"]!!.jsonPrimitive.double,
                longitude = location["lng"]!!.jsonPrimitive.double,
            )
        }
    }
}