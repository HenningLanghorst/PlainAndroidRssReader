package de.henninglanghorst.rssreader.activity.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModelProviders
import de.henninglanghorst.rssreader.R
import de.henninglanghorst.rssreader.activity.manage.FeedManagementActivity
import de.henninglanghorst.rssreader.util.toastShort
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when {
                item.itemId == R.id.action_settings -> {
                    startActivity(Intent(this, FeedManagementActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate")

        feeds.setHasFixedSize(true)

        val feedViewModel = ViewModelProviders.of(this).get(FeedViewModel::class.java)


        feedViewModel.error.observe(this, Observer { toastShort(it) })
        feedViewModel.loading.observe(this, Observer { swipe_container.isRefreshing = it })
        feedViewModel.feedEntries.observe(this, Observer { feeds.adapter = FeedAdapter(it) })
        map(feedViewModel.feedsVisible) { v -> if (v) View.VISIBLE else View.GONE }
                .observe(this, Observer { feeds.visibility = it })
        swipe_container.setOnRefreshListener { feedViewModel.update() }

        val feedEntries = feedViewModel.feedEntries.value
        if (!feedEntries.isNullOrEmpty()) {
            feeds.visibility = View.VISIBLE
            feeds.adapter = FeedAdapter(feedEntries)
        } else {
            feedViewModel.update()
        }


    }

}


