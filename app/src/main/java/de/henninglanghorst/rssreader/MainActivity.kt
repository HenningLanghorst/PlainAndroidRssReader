package de.henninglanghorst.rssreader

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.os.ConfigurationCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import de.henninglanghorst.rssreader.db.Feed
import de.henninglanghorst.rssreader.db.FeedDatabase
import de.henninglanghorst.rssreader.feed.AtomHandler
import de.henninglanghorst.rssreader.feed.FeedEntry
import de.henninglanghorst.rssreader.feed.RssHandler
import de.henninglanghorst.rssreader.util.*
import de.henninglanghorst.rssreader.view.FeedList
import de.henninglanghorst.rssreader.view.FeedsView
import de.henninglanghorst.rssreader.view.Loading
import de.henninglanghorst.rssreader.view.ViewState
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import java.text.DateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    private val feedDatabase: FeedDatabase by lazy { FeedDatabase(applicationContext) }

    private val initialUrls = listOf(
            "https://www.tagesschau.de/xml/rss2",
            "http://newsfeed.zeit.de/index",
            "http://www.spiegel.de/schlagzeilen/tops/index.rss",
            "https://www.heise.de/newsticker/heise-atom.xml",
            "https://rss.golem.de/rss.php?feed=ATOM1.0"
    ).map { Feed(url = it) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feeds.setHasFixedSize(true)
        feeds.layoutManager = LinearLayoutManager(this)
        feeds.adapter = FeedAdapter()
        feeds.visibility = View.GONE

        Flowable.fromCallable {
            if (feedDatabase.feedDao.getAll().isEmpty()) {
                initialUrls.forEach { feedDatabase.feedDao.insert(it) }
            }
        }.subscribeOn(Schedulers.io()).subscribe()


        val feedsView = FeedsView(this::updateState, feedDatabase.feedDao, this)

        swipe_container.setOnRefreshListener { feedsView.update() }
        feedsView.update()

    }


    private fun updateState(viewState: ViewState) =
            when (viewState) {
                is Loading -> onLoading()
                is FeedList -> onFeedsLoaded(viewState)
            }

    private fun onLoading() {
        swipe_container.isRefreshing = true

    }

    private fun onFeedsLoaded(feedList: FeedList) {
        feeds.visibility = View.VISIBLE
        feeds.adapter = FeedAdapter(feedList.feeds)
        swipe_container.isRefreshing = false
    }

}


class FeedAdapter(private val feedEntries: List<FeedEntry> = emptyList()) : RecyclerView.Adapter<FeedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            FeedViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.layout_feed_entry, parent, false))

    override fun getItemCount() = feedEntries.size

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        with(holder) {
            val feedEntry = feedEntries[position]
            view.findViewById<TextView>(R.id.feed_entry_channel).text = feedEntry.channel
            view.findViewById<TextView>(R.id.feed_entry_title).text = feedEntry.title
            view.findViewById<TextView>(R.id.feed_entry_description).text = feedEntry.description
            view.findViewById<TextView>(R.id.feed_entry_timestamp).text = holder.dateFormat.format(feedEntry.timestamp)
            view.setOnClickListener { view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(feedEntry.url))) }
        }
    }
}


data class FeedViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    private val defaultLocale: Locale = ConfigurationCompat.getLocales(view.context.resources.configuration)[0]
    val dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, defaultLocale)

}

