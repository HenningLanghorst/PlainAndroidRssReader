package de.henninglanghorst.rssreader.activity.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.os.ConfigurationCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import de.henninglanghorst.rssreader.R
import de.henninglanghorst.rssreader.db.Feed
import de.henninglanghorst.rssreader.db.FeedDatabase
import de.henninglanghorst.rssreader.feed.FeedEntry
import de.henninglanghorst.rssreader.activity.manage.FeedManagementActivity
import de.henninglanghorst.rssreader.util.getValue
import de.henninglanghorst.rssreader.util.setValue
import de.henninglanghorst.rssreader.view.FeedList
import de.henninglanghorst.rssreader.view.FeedsView
import de.henninglanghorst.rssreader.view.Loading
import de.henninglanghorst.rssreader.view.ViewState
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_feed_entry.view.*
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


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when {
                item.itemId == R.id.action_settings -> {
                    startActivity(Intent(this, FeedManagementActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feeds.setHasFixedSize(true)
        feeds.layoutManager = LinearLayoutManager(this)
        feeds.adapter = FeedAdapter()
        feeds.visibility = View.GONE

        val feedsView = FeedsView(this::updateState, feedDatabase.feedDao, this)


        Flowable.fromCallable {
            if (feedDatabase.feedDao.getAll().isEmpty()) {
                initialUrls.forEach { feedDatabase.feedDao.insert(it) }
            }
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { feedsView.update() }



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
        Log.d(TAG, "Feeds loaded")
        feeds.visibility = View.VISIBLE
        feeds.adapter = FeedAdapter(feedList.feeds)
        swipe_container.isRefreshing = false
    }

}


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


data class FeedViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    private val defaultLocale: Locale = ConfigurationCompat.getLocales(view.context.resources.configuration)[0]
    val dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, defaultLocale)

    var channel by view.feed_entry_channel
    var title by view.feed_entry_title
    var description by view.feed_entry_description
    var timestamp by view.feed_entry_timestamp

}

