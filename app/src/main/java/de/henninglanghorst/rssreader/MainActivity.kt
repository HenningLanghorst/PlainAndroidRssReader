package de.henninglanghorst.rssreader

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.os.ConfigurationCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import java.text.DateFormat
import java.util.*


class MainActivity : AppCompatActivity() {


    private val urls = listOf(
            "https://www.tagesschau.de/xml/rss2",
            "http://newsfeed.zeit.de/index",
            "http://www.spiegel.de/schlagzeilen/tops/index.rss",
            "https://www.heise.de/newsticker/heise-atom.xml",
            "https://rss.golem.de/rss.php?feed=ATOM1.0"
    ).map { URL(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        swipe_container.setOnRefreshListener {
            swipe_container.isRefreshing = true
            update()
        }
        feeds.setHasFixedSize(true)
        feeds.layoutManager = LinearLayoutManager(this)
        feeds.adapter = FeedAdapter()

        update()
    }


    private fun update() {

        val xmlData = urls.data.flattenAsFlowable { it }.cache()
        val atomFeeds = xmlData.flatMap { (it parsedWith ::AtomHandler) }
        val rssEntries = xmlData.flatMap { (it parsedWith ::RssHandler) }

        Flowable.concat(atomFeeds, rssEntries)
                .sorted { o1, o2 -> -o1.timestamp.compareTo(o2.timestamp) }
                .toList()
                .observeOn(AndroidSchedulers.mainThread()).subscribe { feedEntries ->
                    feeds.adapter = FeedAdapter(feedEntries)
                    swipe_container.isRefreshing = false
                }
    }

}


class FeedAdapter(private val feedEntries: List<FeedEntry> = emptyList()) : RecyclerView.Adapter<FeedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            FeedViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.layout_feed_entry, parent, false))

    override fun getItemCount() = feedEntries.size

    private val imageGetter: Html.ImageGetter get() = Html.ImageGetter { this.loadImage(it) }

    private fun loadImage(source: String?) = Drawable.createFromStream(URL(source).openStream(), source)?.apply { setBounds(0, 0, intrinsicWidth, intrinsicHeight) }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        with(holder) {
            val feedEntry = feedEntries[position]
            view.findViewById<TextView>(R.id.feed_entry_channel).text = feedEntry.channel
            view.findViewById<TextView>(R.id.feed_entry_title).text = feedEntry.title
            val description = feedEntry.description
            view.findViewById<TextView>(R.id.feed_entry_description).text = Html.escapeHtml(description)

            description.spanned.subscribe { html -> view.findViewById<TextView>(R.id.feed_entry_description).text = html }

            view.findViewById<TextView>(R.id.feed_entry_timestamp).text = holder.dateFormat.format(feedEntry.timestamp)
            view.setOnClickListener { view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(feedEntry.url))) }
        }
    }

    @Suppress("DEPRECATION")
    private val String.spanned: Single<Spanned>
        get () = Single.defer { Single.just(Html.fromHtml(this, imageGetter, null)) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
}


data class FeedViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    private val defaultLocale: Locale = ConfigurationCompat.getLocales(view.context.resources.configuration)[0]
    val dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, defaultLocale)

}

