package org.tasks.http

import java.io.IOException

class HttpException(code: Int, message: String) : IOException("$code $message")