package de.henninglanghorst.rssreader.feed

data class FeedDescription(
        val title: String,
        val description: CharSequence) {
    companion object {
        val none: FeedDescription = FeedDescription(title = "N/A", description = "N/A")
    }
}