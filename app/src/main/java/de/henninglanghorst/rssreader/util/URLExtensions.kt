package de.henninglanghorst.rssreader.util

import android.content.ContentValues.TAG
import android.util.Log
import de.henninglanghorst.rssreader.R
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.net.ConnectException
import java.net.URL
import java.net.UnknownHostException


val URL.data: Single<FeedResult>
    get() = Single.fromCallable<FeedResult> { feedResult }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())


private val URL.feedResult: FeedResult
    get() =
        try {
            FeedContent(openConnection().apply { connectTimeout = 10000; readTimeout = 10000 }
                    .getInputStream()
                    .bufferedReader()
                    .lineSequence()
                    .joinToString(System.lineSeparator()))

        } catch (e: UnknownHostException) {
            Log.e(TAG, "Exception: ${e.localizedMessage}", e)
            FeedError(R.string.unknown_host, toString())

        } catch (e: ConnectException) {
            Log.e(TAG, "Exception: ${e.localizedMessage}", e)
            FeedError(R.string.connect_error, toString())
        } catch (e: IOException) {
            Log.e(TAG, "Exception: ${e.localizedMessage}", e)
            FeedError(R.string.feed_error, toString())
        }


val Flowable<URL>.data: Flowable<FeedResult> get() = flatMapSingle { it.data }.cache()


sealed class FeedResult

data class FeedError(val code: Int, val url: String) : FeedResult()
data class FeedContent(val data: String) : FeedResult()
