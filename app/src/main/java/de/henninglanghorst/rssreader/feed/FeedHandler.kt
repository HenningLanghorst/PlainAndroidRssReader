package de.henninglanghorst.rssreader.feed

import org.xml.sax.ContentHandler


interface FeedHandler : ContentHandler {
    val feedEntries: List<FeedEntry>
    val feedDescription:FeedDescription?
}