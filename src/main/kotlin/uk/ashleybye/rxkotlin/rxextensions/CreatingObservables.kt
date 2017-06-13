package uk.ashleybye.rxkotlin.rxextensions

import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe

fun main(args: Array<String>) {
    doExample({ CreatingObservables.exampleOne() }, "Example One: Single Subscriber")
    doExample({ CreatingObservables.exampleTwo() }, "Example Two: Single Subscriber - Custom Observable")
    doExample({ CreatingObservables.exampleThree() }, "Example Three: Multiple Subscribers")

    doExample({ CreatingObservables.exerciseOne() }, "Exercise One")
}

private fun doExample(example: () -> Unit, title: String = "Example") {
    println("\n$title\n")
    example.invoke()
    println()
}

private object CreatingObservables {

    /**
     * Example One: Single Subscriber.
     *
     * All messages are printed by the main client thread. Subscription also happens in the main
     * client thread. subscribe() blocks the client thread until all events are received.
     */
    fun exampleOne() {
        log("Before")
        Observable
                .range(5, 3)
                .subscribe { log(it) }
        log("After")
    }

    /**
     * Example Two: Single Subscriber - Custom Observable.
     *
     * As with [exampleOne], all messages are printed by the main client thread. However, this time
     * the example shows how the observable does not start emitting until subscribe is called. Note
     * that internally, the lambda expression receiving emitted items `log("Element: $it")` is
     * wrapped with `Subscriber<Int>` internally, which is passed to `create()`.
     */
    fun exampleTwo() {
        val ints = Observable.create(ObservableOnSubscribe<Int> { observableEmitter ->
            log("Create")
            observableEmitter.onNext(5)
            observableEmitter.onNext(6)
            observableEmitter.onNext(7)
            observableEmitter.onComplete()
            log("Completed")
        })

        log("Starting")
        ints.subscribe { log("Element: $it") }
        log("Exit")
    }

    /**
     * Example Three: Multiple Subscribers.
     *
     * Every time `subscribe()` is called, the subscription handler inside `create()` is invoked.
     * Not great if there is some heavyweight computation inside `create()`, sharing of a single
     * invocation amongst subscribers can be beneficial. This can be achieved using the `cache()`
     * operator. Note that with infinite streams, `cache()` can result in an OutOfMemoryError.
     */
    fun exampleThree() {
        val intsNotCached = Observable.create<Int> { subscriber ->
            log("Create")
            subscriber.onNext(42)
            subscriber.onComplete()
        }

        // Use of the cache() operator feeds cached values for subsequent subscribers.
        val intsCached = intsNotCached.cache()

        log("Starting - Not Cached")
        intsNotCached.subscribe { log("Subscriber A: $it") }
        intsNotCached.subscribe { log("Subscriber B: $it") }
        log("Exit - Not Cached")

        log("Starting - Cached")
        intsCached.subscribe { log("Subscriber A: $it") }
        intsCached.subscribe { log("Subscriber B: $it") }
        log("Exit - Cached")
    }

    /**
     * First exercise.
     *
     * Using just `create()`, implement methods:
     *
     * just(item) - emits a single value `item` to an observer and then completes immediately.
     * never() - never emits to an observer.
     * empty() - emits no items to an observer and then completes immediately.
     * range(start, count) - emits a range to an observer and then completes immediately.
     */
    fun exerciseOne() {
        fun <T> just(item: T): Observable<T> = Observable.create { subscriber ->
            subscriber.onNext(item)
            subscriber.onComplete()
        }

        fun <T> never(): Observable<T> = Observable.create { }

        fun <T> empty(): Observable<T> = Observable.create { subscriber ->
            subscriber.onComplete()
        }

        fun range(start: Int, count: Int): Observable<Int> = Observable.create { subscriber ->
            val ints = IntRange(start, start + count - 1).asSequence()
            for (int in ints) {
                subscriber.onNext(int)
            }
            subscriber.onComplete()
        }

        just(10).subscribe { log("just(10): $it") }
        never<Int>().subscribe { log("never(): $it") }
        empty<Int>().subscribe { log("empty(): $it") }
        range(10, 5).subscribe { log("range(10, 5): $it") }
    }

    /**
     * Helper method.
     *
     * Prints the current thread and a message to the standard output stream.
     *
     * @param msg - the message
     */
    private fun log(msg: Any) = println("${Thread.currentThread().name}: $msg")
}
