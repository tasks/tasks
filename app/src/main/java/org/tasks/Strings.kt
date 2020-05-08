package org.tasks

object Strings {
    @JvmStatic
    fun isNullOrEmpty(string: String?) = string?.isEmpty() ?: true
}