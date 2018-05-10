package de.henninglanghorst.rssreader.activity.manage

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.InputType
import android.view.View
import android.widget.EditText
import de.henninglanghorst.rssreader.R
import de.henninglanghorst.rssreader.db.Feed
import de.henninglanghorst.rssreader.db.FeedDatabase
import de.henninglanghorst.rssreader.feed.AtomHandler
import de.henninglanghorst.rssreader.feed.FeedDescription
import de.henninglanghorst.rssreader.feed.RssHandler
import de.henninglanghorst.rssreader.util.FeedResult
import de.henninglanghorst.rssreader.util.data
import de.henninglanghorst.rssreader.util.parseFeedDescriptionWith
import de.henninglanghorst.rssreader.util.withDescription
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_feed_administration.*
import kotlinx.android.synthetic.main.content_settings.*
import java.net.URL


class FeedManagementActivity : AppCompatActivity() {

    private val feedDatabase by lazy { FeedDatabase(applicationContext) }
    private val settingsFeedEntryAdapter by lazy { SettingsFeedEntryAdapter(this, feedDatabase.feedDao) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_feed_administration)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view -> view.addNewItem() }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        settings_entry.setHasFixedSize(true)
        settings_entry.layoutManager = LinearLayoutManager(this)
        settings_entry.adapter = settingsFeedEntryAdapter

        ItemTouchHelper(SwipeToDeleteCallback(this, settingsFeedEntryAdapter::removeAt))
                .attachToRecyclerView(settings_entry)

    }


    private fun View.addNewItem() {
        val editText = EditText(context).apply { inputType = InputType.TYPE_TEXT_VARIATION_URI }
        AlertDialog.Builder(context).setTitle(context.getString(R.string.add_new_feed_item))
                .setView(editText)
                .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                    Feed(url = editText.text.toString())
                            .withDescription
                            .subscribe { pair -> settingsFeedEntryAdapter += pair }
                }
                .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
                .show()
    }
}


