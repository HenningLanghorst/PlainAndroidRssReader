package de.henninglanghorst.rssreader

import android.content.Context
import android.util.Log
import android.widget.Toast
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.SAXParserFactory


class RssClient(private val handleError: (Throwable) -> Unit) {


    fun loadRssFeeds(urls: List<String>) =
            Flowable.fromIterable(urls)
                    .flatMap { feedEntriesFrom(it) }

    private fun feedEntriesFrom(url: String) =
            Flowable.fromCallable { parseRssFrom(url) }
                    .subscribeOn((Schedulers.io()))
                    .onErrorReturn { emptyList() }
                    .flatMap { Flowable.fromIterable(it) }

    private fun parseRssFrom(url: String) =
            URL(url).openStream().use { parseRssFrom(it) }


    private fun parseRssFrom(inputStream: InputStream) =
            RssHandler().also { rssHandler ->
                SAXParserFactory.newInstance().newSAXParser().xmlReader.apply {
                    contentHandler = rssHandler
                    setFeature("http://xml.org/sax/features/namespaces", true)
                    setFeature("http://xml.org/sax/features/validation", false)
                    parse(InputSource(inputStream))
                }
            }.feedEntries


}

private class RssHandler : DefaultHandler() {

    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ", Locale.US)

    private val stack: Deque<String> = LinkedList<String>()
    val title: StringBuilder = StringBuilder()
    val items: MutableList<Item> = mutableListOf()

    private var currentItem: Item? = null

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        stack.offerLast(localName)
        if (localName == "item") {
            currentItem = Item()
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        stack.removeLast()

        if (localName == "item") {
            items.add(currentItem ?: throw NoSuchElementException("current item is null"))
            currentItem = null
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        val message = String(ch, start, length).trim()

        when (stack.joinToString("/")) {
            "rss/channel/title" -> title.append(message)
            "rss/channel/item/title" -> currentItem!!.title.append(message)
            "rss/channel/item/link" -> currentItem!!.link.append(message)
            "rss/channel/item/description" -> currentItem!!.description.append(message)
            "rss/channel/item/pubDate" -> currentItem!!.pubDate.append(message)
        }
    }

    val feedEntries: List<FeedEntry>
        get () = items.map {
            FeedEntry(
                    channel = title.toString(),
                    title = it.title.toString(),
                    url = it.link.toString(),
                    timestamp = try {
                        dateFormat.parse(it.pubDate.toString())
                    } catch (e: ParseException) {
                        Log.e("Wrong date format", "channel=$title title=${it.title}", e)
                        Date()
                    },
                    description = it.description.toString()
            )
        }

}


private data class Item(
        val title: StringBuilder = StringBuilder(),
        val description: StringBuilder = StringBuilder(),
        val pubDate: StringBuilder = StringBuilder(),
        val link: StringBuilder = StringBuilder()

)
