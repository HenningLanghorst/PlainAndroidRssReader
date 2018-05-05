package de.henninglanghorst.rssreader

import org.joda.time.format.ISODateTimeFormat
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.*

class AtomHandler : DefaultHandler(), FeedHandler {

    private val dateFormat = ISODateTimeFormat.dateTimeNoMillis()

    private val stack: Deque<String> = LinkedList<String>()
    private val title: StringBuilder = StringBuilder()
    private val items: MutableList<Item> = mutableListOf()

    private var currentItem: Item? = null
    private var isAtom = false

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        stack.offerLast(localName)
        when (stack.joinToString("/")) {
            "feed" -> isAtom = true
            "feed/entry" -> currentItem = Item()
            "feed/entry/link" -> currentItem!!.link.append(attributes.getValue("href"))
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        stack.removeLast()

        if (isAtom && localName == "entry") {
            items.add(currentItem ?: throw NoSuchElementException("current item is null"))
            currentItem = null
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        val message = String(ch, start, length)

        when (stack.joinToString("/")) {
            "feed/title" -> title.append(message)
            "feed/entry/title" -> currentItem!!.title.append(message)
            "feed/entry/summary" -> currentItem!!.description.append(message)
            "feed/entry/updated" -> currentItem!!.pubDate.append(message)
        }
    }


    override val feedEntries: List<FeedEntry>
        get () = items.map {
            FeedEntry(
                    channel = title.toString(),
                    title = it.title.toString(),
                    url = it.link.toString(),
                    timestamp = dateFormat.parseDateTime(it.pubDate.toString()).toDate(),
                    description = it.description.toString()
            )
        }

    private data class Item(
            val title: StringBuilder = StringBuilder(),
            val description: StringBuilder = StringBuilder(),
            val pubDate: StringBuilder = StringBuilder(),
            val link: StringBuilder = StringBuilder()

    )

}