package de.henninglanghorst.rssreader.activity.manage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
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
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.net.URL

class SettingsFeedEntryAdapter(private val feedDao: FeedDao) : RecyclerView.Adapter<SettingsFeedEntryViewHolder>() {

    private val feedDescriptions: MutableList<Pair<Feed, FeedDescription>> = mutableListOf()

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

    private val Feed.withDescription get() = URL(url).data.feedDescription.map { this to it }

    private val Single<FeedResult>.feedDescription
        get(): Single<FeedDescription> = Maybe.merge(
                flatMapMaybe { (it parseFeedDescriptionWith ::AtomHandler) },
                flatMapMaybe { (it parseFeedDescriptionWith ::RssHandler) })
                .first(FeedDescription.none)
                .onErrorReturn { FeedDescription.none }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())


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


    private fun SettingsFeedEntryViewHolder.copyUrlToClipboard(): Boolean {
        val clipboardManager = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip = ClipData.newPlainText("text", url)
        Toast.makeText(view.context, R.string.url_copied_to_clipboard, Toast.LENGTH_LONG).show()
        return true
    }

    operator fun plusAssign(feedAndDescription: Pair<Feed, FeedDescription>) {
        feedAndDescription.insert()
                .subscribe {
                    this.feedDescriptions += feedAndDescription
                    notifyDataSetChanged()
                }
    }

    private fun Pair<Feed, FeedDescription>.insert() =
            Completable.fromAction { feedDao.insert(first) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())


    fun removeAt(index: Int, view: View) {
        val feedAndDescription = feedDescriptions[index]
        feedAndDescription.delete()
                .subscribe {
                    feedDescriptions.removeAt(index)
                    notifyItemRemoved(index)
                    view.showUndoSnackbar(feedAndDescription, index)

                }
    }

    private fun Pair<Feed, FeedDescription>.delete() =
            Completable.fromAction { feedDao.delete(first) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())

    private fun View.showUndoSnackbar(feedAndDescription: Pair<Feed, FeedDescription>, index: Int) =
            Snackbar.make(this, R.string.feed_item_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { feedAndDescription.restoreAt(index) }
                    .show()

    private fun Pair<Feed, FeedDescription>.restoreAt(index: Int) {
        insert().subscribe {
            feedDescriptions.add(index, this)
            notifyDataSetChanged()
        }
    }


}
