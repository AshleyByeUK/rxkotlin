package uk.ashleybye.rxkotlin.flowcontrol.flowcontrol

import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    println("Sampling:\n")
    sample()

    println("\nMore Sampling:\n")
    anotherSample()

    println("\nThrottling:\n")
    throttleFirst()
}

private fun sample() {
    val startTime = System.currentTimeMillis()
    Observable
            .interval(7, TimeUnit.MILLISECONDS)
            .timestamp()
            .sample(1, TimeUnit.SECONDS)
            .map { timestamp -> "${timestamp.time() - startTime}ms: ${timestamp.value()}"}
            .take(5)
            .subscribe { println(it) }

    TimeUnit.SECONDS.sleep(6)
}

/**
 * Note that `sample()` picks the last seen value every second. Thus, the output will be Linda,
 * Barbara, and Susan. To see Dorothy, can `concatWith()` a delayed empty `Observable`.
 */
private fun anotherSample() {
    val names = Observable.just("Mary", "Patricia", "Linda", "Barbara", "Elizabeth", "Jennifer",
            "Maria", "Susan", "Margaret", "Dorothy")
    val absoluteDelayMillis = Observable
            .just(0.1, 0.6, 0.9, 1.1, 3.3, 3.4, 3.5, 3.6, 4.4, 4.8)
            .map { d -> (d * 1_000).toLong()}
    val delayedNames = names
            .zipWith(
                    absoluteDelayMillis,
                    { name, delay ->
                        Observable
                                .just(name)
                                .delay(delay, TimeUnit.MILLISECONDS)
                    }
            ).flatMap { it }
    delayedNames
            .concatWith(Observable.empty<String>().delay(1, TimeUnit.SECONDS)) // If we want to see Dorothy
            .sample(1, TimeUnit.SECONDS)
            .subscribe { println(it) }

    TimeUnit.SECONDS.sleep(5)
}

/**
 * Takes the first value emitted in the last sampling period. `throttleLast()` has the same effect
 * as `sample()`.
 */
private fun throttleFirst() {
    val names = Observable.just("Mary", "Patricia", "Linda", "Barbara", "Elizabeth", "Jennifer",
            "Maria", "Susan", "Margaret", "Dorothy")
    val absoluteDelayMillis = Observable
            .just(0.1, 0.6, 0.9, 1.1, 3.3, 3.4, 3.5, 3.6, 4.4, 4.8)
            .map { d -> (d * 1_000).toLong()}
    val delayedNames = names
            .zipWith(
                    absoluteDelayMillis,
                    { name, delay ->
                        Observable
                                .just(name)
                                .delay(delay, TimeUnit.MILLISECONDS)
                    }
            ).flatMap { it }
    delayedNames
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe { println(it) }

    TimeUnit.SECONDS.sleep(5)
}