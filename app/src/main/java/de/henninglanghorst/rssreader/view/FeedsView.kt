package de.henninglanghorst.rssreader.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Html
import de.henninglanghorst.rssreader.db.FeedDao
import de.henninglanghorst.rssreader.feed.AtomHandler
import de.henninglanghorst.rssreader.feed.FeedEntry
import de.henninglanghorst.rssreader.feed.RssHandler
import de.henninglanghorst.rssreader.util.FeedError
import de.henninglanghorst.rssreader.util.FeedResult
import de.henninglanghorst.rssreader.util.data
import de.henninglanghorst.rssreader.util.parsedWith
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.net.URL

class FeedsView(private val onUpdateViewState: (ViewState) -> Unit,
                private val feedDao: FeedDao,
                private val context: Context) {

    private val urls
        get() = Single.fromCallable { feedDao.getAll().map { URL(it.url) } }
                .flattenAsFlowable { it }
                .subscribeOn(Schedulers.io())


    fun update() {
        onUpdateViewState(Loading)
        with(urls.data) {
            (atomFeeds + rssFeeds combineWith errors)
                    .subscribe { result -> onUpdateViewState(result) }
        }
    }

    private val Flowable<FeedResult>.errors
        get() = filter { it is FeedError }.map { it as FeedError }
                .map { context.getString(it.code, it.url) }
                .toList()

    private val Flowable<FeedResult>.atomFeeds get() = flatMap { (it parsedWith ::AtomHandler) }
    private val Flowable<FeedResult>.rssFeeds get () = flatMap { (it parsedWith ::RssHandler) }

    private operator fun Flowable<FeedEntry>.plus(other: Flowable<FeedEntry>) =
            Flowable.merge(this, other)
                    .sorted { o1, o2 -> -o1.timestamp.compareTo(o2.timestamp) }
                    .toList()
                    .observeOn(AndroidSchedulers.mainThread())

    private infix fun Single<List<FeedEntry>>.combineWith(errors: Single<List<String>>) =
            zipWith(errors, BiFunction(::FeedList))

}