package de.henninglanghorst.rssreader

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.net.URL


val URL.data: Single<XML>
    get() =
        Single.fromCallable {
            openStream()
                    .bufferedReader()
                    .lineSequence()
                    .joinToString(System.lineSeparator()).let { XML(it) }
        }.subscribeOn(Schedulers.io())


val List<URL>.data: Single<List<XML>>
    get() =
        Flowable.fromIterable(this).flatMap { it.data.toFlowable() }.toList()

data class XML(val data: String)
