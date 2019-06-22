package de.henninglanghorst.rssreader.activity.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import de.henninglanghorst.rssreader.feed.FeedEntry

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    val feedEntries = MutableLiveData<List<FeedEntry>>()
}