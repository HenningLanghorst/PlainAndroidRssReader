package de.henninglanghorst.rssreader.view

import de.henninglanghorst.rssreader.feed.FeedEntry
import de.henninglanghorst.rssreader.util.FeedError

sealed class ViewState

object Loading : ViewState()
data class FeedList(val feeds: List<FeedEntry>, val feedErrors: List<String>) : ViewState()
