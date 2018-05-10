package de.henninglanghorst.rssreader.util

import android.app.Activity
import android.widget.Toast


fun Activity.toastLong(text: CharSequence) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
fun Activity.toastShort(text: CharSequence) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()