package org.tasks.feed

import kotlinx.serialization.Serializable

@Serializable
data class BlogPost(
    val title: String,
    val link: String,
    val guid: String,
    val description: String,
    val pubDate: String = "",
    val categories: List<String> = emptyList(),
)
