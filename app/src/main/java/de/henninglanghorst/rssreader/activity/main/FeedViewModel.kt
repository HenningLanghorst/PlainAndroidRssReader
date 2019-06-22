package de.henninglanghorst.rssreader.activity.main

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import de.henninglanghorst.rssreader.R
import de.henninglanghorst.rssreader.db.Feed
import de.henninglanghorst.rssreader.db.FeedDao
import de.henninglanghorst.rssreader.db.FeedDatabase
import de.henninglanghorst.rssreader.feed.AtomHandler
import de.henninglanghorst.rssreader.feed.FeedEntry
import de.henninglanghorst.rssreader.feed.FeedHandler
import de.henninglanghorst.rssreader.feed.RssHandler
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

class FeedViewModel(application: Application) : AndroidViewModel(application) {


    companion object {
        const val TAG = "FeedViewModel"
    }

    private val initialFeeds = listOf(
            "https://www.tagesschau.de/xml/rss2",
            "http://newsfeed.zeit.de/index",
            "http://www.spiegel.de/schlagzeilen/tops/index.rss",
            "https://www.heise.de/newsticker/heise-atom.xml",
            "https://rss.golem.de/rss.php?feed=ATOM1.0"
    ).map { Feed(url = it) }


    private val database by lazy { FeedDatabase(application) }
    private val feedDao by lazy { database.feedDao }
    private val okHttpClient by lazy { OkHttpClient.Builder().build() }

    val feedEntries = MutableLiveData<List<FeedEntry>>()
    val loading = MutableLiveData<Boolean>()
    val feedsVisible = MutableLiveData<Boolean>()
    val error = MutableLiveData<String>()


    @SuppressLint("CheckResult")
    fun update() {
        loading.value = true
        Flowable.fromCallable { feedDao.feedsAfterInsertingIfNoneExist }
                .flatMapIterable { it }
                .flatMapIterable { getFeedEntries(it.url) }
                .sorted { e1, e2 -> -e1.timestamp.compareTo(e2.timestamp) }
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { result ->
                    loading.value = false
                    feedEntries.value = result
                    feedsVisible.value = true
                }
    }


    private fun getFeedEntries(url: String): List<FeedEntry> = queryRawData(url)?.let {
        try {
            it parseRawUsingHandler RssHandler()
        } catch (e1: Exception) {
            try {
                it parseRawUsingHandler AtomHandler()
            } catch (e2: Exception) {
                Log.e(TAG, "URL contains invalid stream: $url", e1)
                Log.e(TAG, "URL contains invalid stream: $url", e2)
                error.postValue(this.getApplication<Application>().getString(R.string.feed_error, url))
                emptyList<FeedEntry>()
            }
        }
    } ?: emptyList()


    private val FeedDao.feedsAfterInsertingIfNoneExist
        get(): List<Feed> {
            val feeds = getAll()
            if (feeds.isEmpty()) {
                initialFeeds.forEach { insert(it) }
                getAll()
            }
            return feeds
        }

    private fun queryRawData(url: String) = okHttpClient.newCall(
            Request.Builder()
                    .url(url)
                    .get()
                    .build())
            .execute()
            .takeIf { it.isSuccessful }
            ?.body()
            ?.string()

    private infix fun String.parseRawUsingHandler(feedHandler: FeedHandler): List<FeedEntry> {
        SAXParserFactory
                .newInstance()
                .newSAXParser()
                .xmlReader
                .also {
                    it.contentHandler = feedHandler
                    it.setFeature("http://xml.org/sax/features/namespaces", true)
                    it.setFeature("http://xml.org/sax/features/validation", false)
                }
                .parse(InputSource(StringReader(this)))

        return feedHandler.feedEntries
    }


}


