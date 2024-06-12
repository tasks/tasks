package org.tasks.filters

import co.touchlab.kermit.Logger

const val SEPARATOR_ESCAPE: String = "!PIPE!"
const val SERIALIZATION_SEPARATOR: String = "|"

fun mapToSerializedString(source: Map<String, Any>): String {
    val result = StringBuilder()
    for ((key, value) in source) {
        addSerialized(result, key, value)
    }
    return result.toString()
}

private fun addSerialized(result: StringBuilder, key: String, value: Any) {
    result
        .append(key.replace(SERIALIZATION_SEPARATOR, SEPARATOR_ESCAPE))
        .append(SERIALIZATION_SEPARATOR)
    if (value is Int) {
        result.append('i').append(value)
    } else if (value is Double) {
        result.append('d').append(value)
    } else if (value is Long) {
        result.append('l').append(value)
    } else if (value is String) {
        result
            .append('s')
            .append(value.toString().replace(SERIALIZATION_SEPARATOR, SEPARATOR_ESCAPE))
    } else if (value is Boolean) {
        result.append('b').append(value)
    } else {
        throw UnsupportedOperationException(value.javaClass.toString())
    }
    result.append(SERIALIZATION_SEPARATOR)
}

fun mapFromSerializedString(string: String?): Map<String, Any> {
    if (string == null) {
        return HashMap()
    }

    val result: MutableMap<String, Any> = HashMap()
    fromSerialized(string, result) { `object`, key, type, value ->
        when (type) {
            'i' -> `object`[key] = value.toInt()
            'd' -> `object`[key] = value.toDouble()
            'l' -> `object`[key] = value.toLong()
            's' -> `object`[key] = value.replace(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR)
            'b' -> `object`[key] = value.toBoolean()
        }
    }
    return result
}

private fun <T> fromSerialized(string: String, `object`: T, putter: SerializedPut<T>) {
    val pairs =
        string.split(("\\" + SERIALIZATION_SEPARATOR).toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
    var i = 0
    while (i < pairs.size) {
        try {
            val key = pairs[i].replace(SEPARATOR_ESCAPE.toRegex(), SERIALIZATION_SEPARATOR)
            val value = pairs[i + 1].substring(1)
            try {
                putter.put(`object`, key, pairs[i + 1][0], value)
            } catch (e: NumberFormatException) {
                // failed parse to number
                putter.put(`object`, key, 's', value)
                Logger.e(e) { e.message ?: "NumberFormatException" }
            }
        } catch (e: IndexOutOfBoundsException) {
            Logger.e(e) { e.message ?: "IndexOutOfBoundsException" }
        }
        i += 2
    }
}

internal fun interface SerializedPut<T> {
    @Throws(NumberFormatException::class)
    fun put(`object`: T, key: String, type: Char, value: String)
}
