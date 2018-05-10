package de.henninglanghorst.rssreader.util

import android.util.Log
import de.henninglanghorst.rssreader.feed.FeedEntry
import de.henninglanghorst.rssreader.feed.FeedHandler
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.xml.sax.InputSource
import org.xml.sax.XMLReader
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory


infix fun FeedResult.parsedWith(feedHandler: () -> FeedHandler): Flowable<FeedEntry> =
        when (this) {
            is FeedContent -> Single.fromCallable {
                feedHandler().also {
                    SAXParserFactory
                            .newInstance()
                            .newSAXParser()
                            .xmlReader
                            .configured(it)
                            .parse(InputSource(StringReader(this.data)))
                }.feedEntries
            }
                    .subscribeOn(Schedulers.io())
                    .doOnError { Log.e("XmlHandling", "An error occured", it) }
                    .onErrorReturn { emptyList() }
                    .flattenAsFlowable { it }
            is FeedError -> Flowable.empty()
        }

private fun XMLReader.configured(feedHandler: FeedHandler) =
        this.apply {
            contentHandler = feedHandler
            setFeature("http://xml.org/sax/features/namespaces", true)
            setFeature("http://xml.org/sax/features/validation", false)
        }