package de.henninglanghorst.rssreader.util

import android.widget.TextView
import kotlin.reflect.KProperty


operator fun TextView.setValue(feedViewHolder: Any, property: KProperty<*>, value: CharSequence) {
    text = value
}

operator fun TextView.getValue(feedViewHolder: Any, property: KProperty<*>): CharSequence = text


