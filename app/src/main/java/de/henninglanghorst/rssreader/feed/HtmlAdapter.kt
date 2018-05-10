package de.henninglanghorst.rssreader.feed

import android.graphics.drawable.Drawable
import android.text.Html
import java.net.URL

object HtmlAdapter {

    private val imageGetter: Html.ImageGetter get() = Html.ImageGetter { this.loadImage(it) }
    private fun loadImage(source: String?) = Drawable.createFromStream(URL(source).openStream(), source)?.apply { setBounds(0, 0, intrinsicWidth, intrinsicHeight) }

    @Suppress("HasPlatformType", "DEPRECATION")
    fun fromHtml(s: String) = Html.fromHtml(s, imageGetter, null)
}