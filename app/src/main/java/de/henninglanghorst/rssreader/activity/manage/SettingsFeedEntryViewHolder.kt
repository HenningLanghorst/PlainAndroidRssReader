package de.henninglanghorst.rssreader.activity.manage

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import de.henninglanghorst.rssreader.util.getValue
import de.henninglanghorst.rssreader.util.setValue
import kotlinx.android.synthetic.main.layout_settings_entry.view.*

class SettingsFeedEntryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    var title by view.settings_feed_entry_title
    var description by view.settings_feed_entry_description
    var url by view.settings_feed_entry_url
}