package de.henninglanghorst.rssreader.feed

import android.util.Log
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class RssHandler : DefaultHandler(), FeedHandler {

    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ", Locale.US)
    private val stack: Deque<String> = LinkedList<String>()
    private val title: StringBuilder = StringBuilder()
    private val description: StringBuilder = StringBuilder()
    private val items: MutableList<Item> = mutableListOf()

    private var currentItem: Item? = null
    private var addCurrentItem: () -> Unit = {}
    private var createFeedDescription: () -> FeedDescription? = { null }

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        stack.offerLast(localName)
        when (stack.joinToString("/")) {
            "rss" -> {
                addCurrentItem = {
                    items.add(currentItem ?: throw NoSuchElementException("current item is null"))
                }
                createFeedDescription = { FeedDescription(title.toString(), HtmlAdapter.fromHtml(description.toString())) }
            }
            "rss/channel/item" -> currentItem = Item()
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        stack.removeLast()

        if (localName == "item") {
            addCurrentItem()
            currentItem = null
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        val message = String(ch, start, length)

        when (stack.joinToString("/")) {
            "rss/channel/title" -> title.append(message)
            "rss/channel/description" -> description.append(message)
            "rss/channel/item/title" -> currentItem!!.title.append(message)
            "rss/channel/item/link" -> currentItem!!.link.append(message)
            "rss/channel/item/description" -> currentItem!!.description.append(message)
            "rss/channel/item/pubDate" -> currentItem!!.pubDate.append(message)
        }
    }

    override val feedEntries: List<FeedEntry>
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
                    description = HtmlAdapter.fromHtml(it.description.toString())
            )
        }

    override val feedDescription: FeedDescription?
        get() = createFeedDescription()

    private data class Item(
            val title: StringBuilder = StringBuilder(),
            val description: StringBuilder = StringBuilder(),
            val pubDate: StringBuilder = StringBuilder(),
            val link: StringBuilder = StringBuilder()

    )
}