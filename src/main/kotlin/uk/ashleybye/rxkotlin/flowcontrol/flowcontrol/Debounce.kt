package uk.ashleybye.rxkotlin.flowcontrol.flowcontrol

import io.reactivex.Observable
import java.math.BigDecimal
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    println("debounce():\n")
    debounce()
}

/**
 * `debounce()` discards all events that are shortly followed by another event within a specified
 * time window. If another event does not follow within this window, the event is emitted downstream.
 * The following example uses a fictitious stock trading platform to emit stock prices. If the
 * stock price is above 150, send it downstream if no other event is emitted from upstream within
 * 10ms, otherwise wait to see if another event is emitted within 100ms.
 */
private fun debounce() {
    fun randomDelay(value: Long) = Observable
            .just(value)
            .delay((Math.random() * 100).toLong(), TimeUnit.MILLISECONDS)

    fun randomStockPrice(value: Long) = 100 + Math.random() * 10 + (Math.sin(value / 100.0)) * 60

    fun pricesOf(ticker: String) = Observable
            .interval(50, TimeUnit.MILLISECONDS)
            .flatMap { randomDelay(it) }
            .map { randomStockPrice(it) }
            .map { BigDecimal.valueOf(it) }

    val prices = pricesOf("NFLX")
    prices
            .debounce { price ->
                val goodPrice = price > BigDecimal.valueOf(150)
                Observable
                        .empty<BigDecimal>()
                        .delay(
                                when (goodPrice) {
                                    true -> 10L
                                    false -> 100L
                                },
                                TimeUnit.MILLISECONDS)
            }
            .subscribe { println(it) }

    TimeUnit.SECONDS.sleep(5)
}