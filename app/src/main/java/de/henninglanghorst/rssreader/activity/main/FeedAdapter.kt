package de.henninglanghorst.rssreader.activity.main

import android.content.Intent
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import de.henninglanghorst.rssreader.R
import de.henninglanghorst.rssreader.feed.FeedEntry

class FeedAdapter(val feedEntries: List<FeedEntry> = emptyList()) : RecyclerView.Adapter<FeedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            FeedViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.layout_feed_entry, parent, false))

    override fun getItemCount() = feedEntries.size

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        with(holder) {
            val feedEntry = feedEntries[position]
            channel = feedEntry.channel
            title = feedEntry.title
            description = feedEntry.description
            timestamp = holder.dateFormat.format(feedEntry.timestamp)
            view.setOnClickListener { view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(feedEntry.url))) }
        }
    }
}