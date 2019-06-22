package de.henninglanghorst.rssreader.util

import android.util.Log
import de.henninglanghorst.rssreader.db.Feed
import de.henninglanghorst.rssreader.feed.*
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.xml.sax.InputSource
import org.xml.sax.XMLReader
import java.io.StringReader
import java.net.URL
import javax.xml.parsers.SAXParserFactory

val Feed.withDescription get() = URL(url).data.feedDescription.map { this to it }

val Single<FeedResult>.feedDescription
    get(): Single<FeedDescription> = Maybe.merge(
            flatMapMaybe { (it parseFeedDescriptionWith ::AtomHandler) },
            flatMapMaybe { (it parseFeedDescriptionWith ::RssHandler) })
            .first(FeedDescription.none)
            .onErrorReturn { FeedDescription.none }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

infix fun FeedResult.parsedWith(feedHandler: () -> FeedHandler): Flowable<FeedEntry> =
        when (this) {
            is FeedContent -> Single.fromCallable {
                val feedHandler = feedHandler()
                feedHandler.xmlReader.parse(InputSource(StringReader(data)))
                feedHandler.feedEntries
            }
                    .subscribeOn(Schedulers.io())
                    .doOnError { Log.e("XmlHandling", "An error occured", it) }
                    .onErrorReturn { emptyList() }
                    .flattenAsFlowable { it }
            is FeedError -> Flowable.empty()
        }


infix fun FeedResult.parseFeedDescriptionWith(feedHandler: () -> FeedHandler): Maybe<FeedDescription> =
        when (this) {
            is FeedContent -> Maybe.fromCallable<FeedDescription> {
                val handler = feedHandler()
                handler.xmlReader.parse(InputSource(StringReader(data)))
                handler.feedDescription
            }
                    .subscribeOn(Schedulers.io())
                    .doOnError { Log.e("XmlHandling", "An error occured", it) }
                    .onErrorComplete()
                    .cache()
            is FeedError -> Maybe.empty()
        }


private val FeedHandler.xmlReader: XMLReader
    get() =
        SAXParserFactory
                .newInstance()
                .newSAXParser()
                .xmlReader
                .also {
                    it.contentHandler = this
                    it.setFeature("http://xml.org/sax/features/namespaces", true)
                    it.setFeature("http://xml.org/sax/features/validation", false)
                }



