package org.tasks.googleapis

import com.google.api.client.http.HttpResponseException
import java.io.IOException

class HttpNotFoundException(e: HttpResponseException) : IOException(e.message)
