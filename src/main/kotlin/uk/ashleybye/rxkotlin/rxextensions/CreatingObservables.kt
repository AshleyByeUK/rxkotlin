package uk.ashleybye.rxkotlin.rxextensions

import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe

fun main(args: Array<String>) {
    doExample({ CreatingObservables.exampleOne() }, "Example One")
    doExample({ CreatingObservables.exampleTwo() }, "Example Two")
}

private fun doExample(example: () -> Unit, title: String = "Example") {
    println("$title:")
    example.invoke()
    println()
}

private object CreatingObservables {

    /**
     * First example.
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
     * Second example.
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
     * Helper method.
     *
     * Prints the current thread and a message to the standard output stream.
     *
     * @param msg - the message
     */
    private fun log(msg: Any) = println("${Thread.currentThread().name}: $msg")
}
