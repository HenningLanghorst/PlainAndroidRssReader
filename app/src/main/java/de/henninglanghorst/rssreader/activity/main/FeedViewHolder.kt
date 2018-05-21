package de.henninglanghorst.rssreader.activity.main

import android.support.v4.os.ConfigurationCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import de.henninglanghorst.rssreader.util.getValue
import de.henninglanghorst.rssreader.util.setValue
import kotlinx.android.synthetic.main.layout_feed_entry.view.*
import java.text.DateFormat
import java.util.*

data class FeedViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    private val defaultLocale: Locale = ConfigurationCompat.getLocales(view.context.resources.configuration)[0]
    val dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, defaultLocale)

    var channel by view.feed_entry_channel
    var title by view.feed_entry_title
    var description by view.feed_entry_description
    var timestamp by view.feed_entry_timestamp

}