package de.henninglanghorst.rssreader

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.MaybeTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.core.Persister
import java.io.IOException
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object RssClient {

    private val dateFormat = object : ThreadLocal<DateFormat>() {
        override fun initialValue() = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ", Locale.US)
    }

    fun loadRssFeeds(urls: List<String>): Single<List<FeedEntry>> =
            Flowable.fromIterable(urls)
                    .flatMap { loadRssFeedFromChannel(it).toFlowable() }
                    .flatMap { Flowable.fromIterable(it) }
                    .sorted { o1, o2 -> -o1.timestamp.compareTo(o2.timestamp) }
                    .toList()

    private fun loadRssFeedFromChannel(url: String) =
            Maybe.fromCallable { getRssFeedOrNull(url) }
                    .compose(asAndroidBackgroundTask())
                    .map { it.feedEntries }
                    .defaultIfEmpty(Collections.emptyList())

    private fun getRssFeedOrNull(url: String) = URL(url).content()?.toRssFeed()

    private val RssFeed.feedEntries
        get() = channel.items.map {
            FeedEntry(
                    channel = this.channel.title ?: "",
                    title = it.title ?: "",
                    description = it.description ?: "",
                    timestamp = dateFormat.get().parse(it.pubDate!!),
                    url = it.link ?: ""
            )
        }


    private fun URL.content() =
            try {
                openStream().bufferedReader().lineSequence().joinToString(System.lineSeparator())
            } catch (e: IOException) {
                null
            }

    private fun String.toRssFeed() = Persister().read(RssFeed::class.java, this)


    private fun <T> asAndroidBackgroundTask(): MaybeTransformer<T, T> =
            MaybeTransformer {
                it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            }
}


@Root(name = "rss", strict = false)
data class RssFeed(
        @get:Attribute(name = "version", required = false)
        @param:Attribute(name = "version", required = false)
        val version: String?,
        @get:Element(name = "channel")
        @param:Element(name = "channel")
        val channel: Channel
)

@Root(name = "channel", strict = false)
data class Channel(
        @get:Element(name = "title")
        @param:Element(name = "title")
        val title: String? = null,
//        @get:Element(name = "link")
//        @param:Element(name = "link")
//        val link: String? = null,
        @get:Element(name = "description")
        @param:Element(name = "description")
        val description: String?,
        @get:ElementList(name = "item", required = true, inline = true)
        @param:ElementList(name = "item", required = true, inline = true)
        val items: List<Item>
)

@Root(name = "item", strict = false)
data class Item(
        @get:Element(name = "title")
        @param:Element(name = "title")
        val title: String? = null,
        @get:Element(name = "link")
        @param:Element(name = "link")
        val link: String? = null,
        @get:Element(name = "description")
        @param:Element(name = "description")
        val description: String? = null,
        @get:Element(name = "pubDate")
        @param:Element(name = "pubDate")
        val pubDate: String? = null
)

data class Image(
        @get:Element(name = "url")
        @param:Element(name = "url")
        val url: String,
        @get:Element(name = "title")
        @param:Element(name = "title")
        val title: String,
        @get:Element(name = "link")
        @param:Element(name = "link")
        val link: String
)