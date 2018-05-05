package de.henninglanghorst.rssreader

import org.xml.sax.ContentHandler


interface FeedHandler : ContentHandler {
    val feedEntries: List<FeedEntry>
}