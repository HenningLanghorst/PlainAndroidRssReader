package de.henninglanghorst.rssreader.activity.manage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import de.henninglanghorst.rssreader.R
import de.henninglanghorst.rssreader.db.Feed
import de.henninglanghorst.rssreader.db.FeedDao
import de.henninglanghorst.rssreader.feed.AtomHandler
import de.henninglanghorst.rssreader.feed.FeedDescription
import de.henninglanghorst.rssreader.feed.RssHandler
import de.henninglanghorst.rssreader.util.FeedResult
import de.henninglanghorst.rssreader.util.data
import de.henninglanghorst.rssreader.util.parseFeedDescriptionWith
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.net.URL

class SettingsFeedEntryAdapter(context: Context, private val feedDao: FeedDao) : RecyclerView.Adapter<SettingsFeedEntryViewHolder>() {

    init {
        Single.fromCallable { feedDao.getAll() }
                .subscribeOn(Schedulers.io())
                .flattenAsFlowable { it }
                .concatMapSingle { it.withDescription }
                .observeOn(AndroidSchedulers.mainThread())
                .toList()
                .subscribe { result ->
                    feedDescriptions += result
                    notifyDataSetChanged()
                }

    }

    private val feedDescriptions: MutableList<Pair<Feed, FeedDescription>> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SettingsFeedEntryViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.layout_settings_entry, parent, false))

    override fun getItemCount() = feedDescriptions.size

    override fun onBindViewHolder(holder: SettingsFeedEntryViewHolder, position: Int) {
        with(holder) {
            url = feedDescriptions[position].first.url
            title = feedDescriptions[position].second.title
            description = feedDescriptions[position].second.description
            view.setOnLongClickListener { copyUrlToClipboard() }
        }
    }

    private val Feed.withDescription get() = URL(url).data.feedDescription.map { this to it }

    private val Single<FeedResult>.feedDescription
        get(): Single<FeedDescription> = Maybe.merge(
                flatMapMaybe { (it parseFeedDescriptionWith ::AtomHandler) },
                flatMapMaybe { (it parseFeedDescriptionWith ::RssHandler) })
                .first(FeedDescription.none)
                .onErrorReturn { FeedDescription.none }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())


    private fun SettingsFeedEntryViewHolder.copyUrlToClipboard(): Boolean {
        val clipboardManager = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip = ClipData.newPlainText("text", url)
        Toast.makeText(view.context, R.string.url_copied_to_clipboard, Toast.LENGTH_LONG).show()
        return true
    }

    operator fun plusAssign(feedAndDescription: Pair<Feed, FeedDescription>) {
        Single.just(feedAndDescription.first)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnSuccess { feed -> feedDao.insert(feed) }
                .observeOn(AndroidSchedulers.mainThread())
                .ignoreElement().subscribe {
                    this.feedDescriptions += feedAndDescription
                    notifyDataSetChanged()
                }


    }

    fun removeAt(index: Int) {
        Single.just(feedDescriptions[index].first)
                .observeOn(Schedulers.io())
                .doOnSuccess { feed -> feedDao.delete(feed) }
                .observeOn(AndroidSchedulers.mainThread())
                .ignoreElement().subscribe {
                    feedDescriptions.removeAt(index)
                    notifyItemRemoved(index)
                }

    }


}

class LoggingFeedDao(val delegate: FeedDao) : FeedDao by delegate {
    override fun delete(feed: Feed) {
        Log.d("FeedDao", "Delete feed")
        delegate.delete(feed)
    }
}