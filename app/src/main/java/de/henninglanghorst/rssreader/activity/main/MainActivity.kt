package de.henninglanghorst.rssreader.activity.main

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import de.henninglanghorst.rssreader.R
import de.henninglanghorst.rssreader.db.Feed
import de.henninglanghorst.rssreader.db.FeedDatabase
import de.henninglanghorst.rssreader.activity.manage.FeedManagementActivity
import de.henninglanghorst.rssreader.feed.FeedEntry
import de.henninglanghorst.rssreader.view.FeedList
import de.henninglanghorst.rssreader.view.FeedsView
import de.henninglanghorst.rssreader.view.Loading
import de.henninglanghorst.rssreader.view.ViewState
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.collections.ArrayList


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

    override fun onSaveInstanceState(outState: Bundle?) {
        Log.d(TAG, "onSaveInstanceState")
        val feedAdapter = feeds.adapter as FeedAdapter
        outState?.putParcelableArrayList("feedEntries", ArrayList(feedAdapter.feedEntries))

        super.onSaveInstanceState(outState)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate")
        val feedEntries = savedInstanceState?.getParcelableArrayList<FeedEntry>("feedEntries")
        Log.d(TAG, "feedEntries ${feedEntries?.size} item(s)")


        feeds.setHasFixedSize(true)
        feeds.layoutManager = LinearLayoutManager(this)
        feeds.visibility = View.GONE

        val feedsView = FeedsView(
                onUpdateViewState = this::updateState,
                feedDao = feedDatabase.feedDao,
                context = this
        )

        if (feedEntries != null) {
            onFeedsLoaded(FeedList(feedEntries, emptyList()))
        } else {
            Flowable.fromCallable {
                if (feedDatabase.feedDao.getAll().isEmpty()) {
                    initialUrls.forEach { feedDatabase.feedDao.insert(it) }
                }
            }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { feedsView.update() }

        }



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


