package uk.ashleybye.rxkotlin.flowcontrol.flowcontrol

import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    println("Buffer():\n")
    buffer()

    println("\nbufferWindow():\n")
    bufferWindow()

    println("\nmovingAverage()\n")
    movingAverage()

    println("\nbufferTimePeriod()\n")
    bufferTimePeriod()

    println("\nbufferCountEvents()\n")
    bufferCountEvents()

    println("\nbufferFullControl():\n")
    bufferFullControl()

    println("\nwindowCountEvents():\n")
    windowCountEvents()
}

/**
 * `buffer()` ggregates batches of events in real time into a `List`. Emits several lists grouping
 * some number of subsequent events rather than all events at once (like `toList()`). Simplest form
 * groups upstream values into equally sized lists. Achieved by buffering events until the buffer
 * size is reached, then emits them downstream. If completed before buffer size reached, emits what
 * it has.
 */
private fun buffer() {
    Observable
            .range(1, 7)
            .buffer(3)
            .subscribe { println(it) }
}

/**
 * An overloaded version of `buffer()` enables configuration of how many previous values to drop
 * when it pushes the list downstream. This makes it possible to look at the event stream in
 * windows of a given size.
 */
private fun bufferWindow() {
    Observable
            .range(1, 7)
            .buffer(3, 1)
            .subscribe { println(it) }
}

/**
 * Generates 1,000 random doubles and calculates a moving average over 100 of them. Note the Kotlin
 * built-in helper method `average()` makes this very simple!
 */
private fun movingAverage() {
    val random = Random()
    Observable
            .defer { Observable.just(random.nextGaussian()) }
            .repeat(1_000)
            .buffer(100, 1)
            .map { it.average() }
            .subscribe { println(it) }
}

/**
 * Another overloaded version of `buffer()` accepts a time period and buffers events as they occur
 * within it.
 */
private fun bufferTimePeriod() {
    val names = Observable.just("Mary", "Patricia", "Linda", "Barbara", "Elizabeth", "Jennifer",
            "Maria", "Susan", "Margaret", "Dorothy")
    val absoluteDelayMillis = Observable
            .just(0.1, 0.6, 0.9, 1.1, 3.3, 3.4, 3.5, 3.6, 4.4, 4.8)
            .map { d -> (d * 1_000).toLong() }
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
            .buffer(1, TimeUnit.SECONDS)
            .subscribe { println(it) }

    TimeUnit.SECONDS.sleep(5)
}

/**
 * Using the `buffer()` time period example, it is possible to count the number of events that
 * occurred during each buffer interval. Note that the `window()` operator is more efficient.
 */
private fun bufferCountEvents() {
    val names = Observable.just("Mary", "Patricia", "Linda", "Barbara", "Elizabeth", "Jennifer",
            "Maria", "Susan", "Margaret", "Dorothy")
    val absoluteDelayMillis = Observable
            .just(0.1, 0.6, 0.9, 1.1, 3.3, 3.4, 3.5, 3.6, 4.4, 4.8)
            .map { d -> (d * 1_000).toLong() }
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
            .buffer(1, TimeUnit.SECONDS)
            .map { it.size }
            .subscribe { println(it) }

    TimeUnit.SECONDS.sleep(5)
}

/**
 * Shows how to use `Duration` to control time period window of buffer. Can't get example to run,
 * maybe difference in Rx v1 and v2, or due to use of Kotlin. Again, can lead to memory leaks,
 * `window()` may be a better option.
 */
//TODO(Ash): Why does this not work?
private fun bufferFullControl() {
    data class TeleData(val data: String = "Some data")

    fun upstream() = Observable.fromIterable(listOf<TeleData>())

    fun isBusinessHour(): Boolean {
        val BUSINESS_START = LocalTime.of(9, 0)
        val BUSINESS_END = LocalTime.of(17, 0)

        val zone = ZoneId.of("Europe/London")
        val zonedDateTime = ZonedDateTime.now(zone)
        val localTime = zonedDateTime.toLocalTime()

        return !localTime.isBefore(BUSINESS_START) && !localTime.isAfter(BUSINESS_END)
    }
    val insideBusinessHours = Observable
            .interval(1, TimeUnit.SECONDS)
            .filter{ isBusinessHour() }
            .map { Duration.ofMillis(100) }
    val outsideBusinessHours = Observable
            .interval(5, TimeUnit.SECONDS)
            .filter { !isBusinessHour() }
            .map { Duration.ofMillis(200) }

    val openings = Observable
            .merge(insideBusinessHours, outsideBusinessHours)

    // See page 220, as can't figure this out at the moment.
    val samples = upstream()
            .buffer(openings)
    println("Example not currently working")
}

/**
 * Shows how to use `window()` instead of `buffer()` to count events in a given window. Has similar
 * overload options to `buffer()`/
 */
private fun windowCountEvents() {
    val names = Observable.just("Mary", "Patricia", "Linda", "Barbara", "Elizabeth", "Jennifer",
            "Maria", "Susan", "Margaret", "Dorothy")
    val absoluteDelayMillis = Observable
            .just(0.1, 0.6, 0.9, 1.1, 3.3, 3.4, 3.5, 3.6, 4.4, 4.8)
            .map { d -> (d * 1_000).toLong() }
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
            .window(1, TimeUnit.SECONDS)
            .flatMapSingle { it.count() }
            .subscribe { println(it) }

    TimeUnit.SECONDS.sleep(5)
}