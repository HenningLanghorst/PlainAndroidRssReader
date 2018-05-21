package de.henninglanghorst.rssreader.feed

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.util.*

data class FeedEntry(
        val channel: String,
        val title: String,
        val description: CharSequence,
        val timestamp: Date,
        val url: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            Date(parcel.readLong()),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(channel)
        parcel.writeString(title)
        parcel.writeString(description.toString())
        parcel.writeLong(timestamp.time)
        parcel.writeString(url)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FeedEntry> {
        override fun createFromParcel(parcel: Parcel): FeedEntry {
            return FeedEntry(parcel)
        }

        override fun newArray(size: Int): Array<FeedEntry?> {
            return arrayOfNulls(size)
        }
    }
}