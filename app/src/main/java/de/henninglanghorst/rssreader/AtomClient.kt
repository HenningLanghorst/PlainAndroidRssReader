package de.henninglanghorst.rssreader

import android.widget.Toast
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.joda.time.format.ISODateTimeFormat
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.net.URL
import java.util.*
import javax.xml.parsers.SAXParserFactory


class AtomClient(private val handleError: (Throwable) -> Unit) {

    fun loadAtomFeeds(urls: List<String>) =
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
            AtomHandler().also { handler ->
                SAXParserFactory.newInstance().newSAXParser().xmlReader.apply {
                    contentHandler = handler
                    setFeature("http://xml.org/sax/features/namespaces", true)
                    setFeature("http://xml.org/sax/features/validation", false)
                    parse(InputSource(inputStream))
                }
            }.feedEntries


}

private class AtomHandler : DefaultHandler() {

    private val dateFormat = ISODateTimeFormat.dateTimeNoMillis()

    private val stack: Deque<String> = LinkedList<String>()
    val title: StringBuilder = StringBuilder()
    val items: MutableList<Item> = mutableListOf()

    private var currentItem: Item? = null

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        stack.offerLast(localName)
        if (stack.joinToString("/") == "feed/entry") {
            currentItem = Item()
        } else if (stack.joinToString("/") == "feed/entry/link") {
            currentItem!!.link.append(attributes.getValue("href"))
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        stack.removeLast()

        if (localName == "entry") {
            items.add(currentItem ?: throw NoSuchElementException("current item is null"))
            currentItem = null
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        val message = String(ch, start, length).trim()

        when (stack.joinToString("/")) {
            "feed/title" -> title.append(message)
            "feed/entry/title" -> currentItem!!.title.append(message)
            "feed/entry/summary" -> currentItem!!.description.append(message)
            "feed/entry/updated" -> currentItem!!.pubDate.append(message)
        }
    }


    val feedEntries: List<FeedEntry>
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

