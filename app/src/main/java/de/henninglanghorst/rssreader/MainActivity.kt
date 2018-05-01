package de.henninglanghorst.rssreader

import android.os.Bundle
import android.support.v4.os.ConfigurationCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.henninglanghorst.rssreader.RssClient.loadRssFeeds
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    val feedUrls = listOf(
            "https://www.tagesschau.de/xml/rss2",
            "http://newsfeed.zeit.de/index",
            "http://www.spiegel.de/schlagzeilen/tops/index.rss"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feeds.setHasFixedSize(true)
        feeds.layoutManager = LinearLayoutManager(this)
        feeds.adapter = FeedAdapter()

        loadRssFeeds(feedUrls)
                .subscribe { feedEntries -> feeds.adapter = FeedAdapter(feedEntries) }


    }

}


class FeedAdapter(private val feedEntries: List<FeedEntry> = emptyList()) : RecyclerView.Adapter<FeedViewholder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewholder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_feed_entry, parent, false)
        return FeedViewholder(view)
    }

    override fun getItemCount(): Int {
        return feedEntries.size
    }

    override fun onBindViewHolder(holder: FeedViewholder, position: Int) {
        val feedEntry = feedEntries[position]
        holder.view.findViewById<TextView>(R.id.feed_entry_channel).text = feedEntry.channel
        holder.view.findViewById<TextView>(R.id.feed_entry_title).text = feedEntry.title
        holder.view.findViewById<TextView>(R.id.feed_entry_description).text = Html.fromHtml(feedEntry.description)
        holder.view.findViewById<TextView>(R.id.feed_entry_timestamp).text = holder.dateFormat.format(feedEntry.timestamp)
    }

}


data class FeedViewholder(val view: View) : RecyclerView.ViewHolder(view) {
    private val defaultLocale: Locale = ConfigurationCompat.getLocales(view.context.resources.configuration)[0]
    val dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, defaultLocale)

}

data class FeedEntry(val channel: String, val title: String, val description: String, val timestamp: Date)