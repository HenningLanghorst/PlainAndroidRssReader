package de.henninglanghorst.rssreader.feed

import java.util.*

data class FeedEntry(
        val channel: String,
        val title: String,
        val description: String,
        val timestamp: Date,
        val url: String
)