package uk.ashleybye.rxkotlin.operatorstransformations.extensions

import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith

/**
 * Returns only odd numbered emissions.
 *
 * Returns odd emissions from the stream (i.e. every other emission), starting with the
 * first event, e.g. event 1, event 3, ... event i where `i % 2 != 0`.
 *
 * @receiver io.reactivex.Observable<T>()
 *
 * @return every other event in the upstream stream
 */
fun <T> io.reactivex.Observable<T>.odd(): Observable<T> {
    val trueFalse: Observable<Boolean> = Observable
            .just(true, false)
            .repeat()

    return this
            .zipWith(trueFalse, { t, boolean ->
                Pair<T, Boolean>(t, boolean)
            })
            .filter { (_, second) -> second }
            .map { (first) -> first }
}
