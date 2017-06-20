package uk.ashleybye.rxkotlin.troubleshooting

import io.reactivex.functions.Function
import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith
import java.time.LocalDate
import java.time.Month
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


fun main(args: Array<String>) {
//    ErrorHandling.exampleOne()
//    ErrorHandling.exampleTwo()
//    ErrorHandling.exampleThree()
//    ErrorHandling.exampleFour()
//    ErrorHandling.exampleFive()
//    ErrorHandling.exampleSix()
//    ErrorHandling.exampleSeven()
//    ErrorHandling.exampleEight()
//    ErrorHandling.exampleNine()
//    ErrorHandling.exampleTen()
    ErrorHandling.exampleEleven()
}

object ErrorHandling {

    /**
     * Example One: Deal with errors. If it's going to be thrown somewhere, catch it and send it
     * downstream as an error. Make sure `subscribe()` does something with `onError()`.
     */
    fun exampleOne() {
        println("\nDeal with Errors:\n")

        Observable
                .just(1, 0, 3)
                .flatMap { num ->
                    if (num == 0) {
                        Observable.error(ArithmeticException("Zero :-("))
                    } else {
                        Observable.just(10 / num)
                    }
                }.subscribe(
                { println(it) },
                { throwable -> println(throwable) },
                { println("Completed") }
        )
    }

    /**
     * Example Two: Do something with errors. Note that in [exampleOne], once an error is
     * encountered the execution stops. That may not be what is required, in which case, let's make
     * the stream more resilient to errors. Using `onErrorReturn()` is a convenient way to use
     * some default value or value from another source. This can be used in place of a `try/catch`,
     * but it is still up to the programmer to log and monitor errors. Also note that execution
     */
    fun exampleTwo() {
        println("\nDo Something with Errors:\n")

        Observable
                .just(1, 0, 3)
                .flatMap { num ->
                    if (num == 0) {
                        Observable.error(ArithmeticException("Zero :-("))
                    } else {
                        Observable.just(10 / num)
                    }
                }
                .onErrorReturn {
                    println("From onErrorReturn(0): $it")
                    0
                }
                .subscribe(
                        { println(it) },
                        { println("Should have been dealt with in `onErrorReturn()`: $it") },
                        { println("Completed") }
                )
    }

    /**
     * Example Three: Keep going if errors occur - use an alternative source or use the same source
     * but a different method / invocation. Could even use recursion to re-connect to the same
     * stream - likely more effective if hot observable or not getting values from the start again!
     * In this simple scenario, it would probably make sense to filter out zero values.
     */
    fun exampleThree() {
        fun divideTenBy(number: Int): Observable<Int> {
            return if (number == 0) {
                Observable.error(ArithmeticException("Zero :-("))
            } else {
                Observable.just(10 / number)
            }
        }

        println("\nKeep going if errors occur:\n")

        val numbers = Observable.just(1, 0, 3, 0, 5)
        val alternateNumbers = Observable.just(3, 5)

        numbers
                .flatMap { divideTenBy(it) }
                .onErrorResumeNext { throwable: Throwable ->
                    when (throwable) {
                        is ArithmeticException -> alternateNumbers.flatMap { divideTenBy(it) }
                        else -> Observable.error(Exception(throwable))
                    }
                }
                .subscribe(
                        { println(it) },
                        { it.printStackTrace() },
                        { println("Completed") }
                )
    }

    /**
     * Example Four: Using Timeouts. Using a fictitious service that calculates when the next solar
     * eclipse after a given date will occur, uses two different timeout values. If the initial
     * computation (which takes longer than the others) does not complete within 1000ms (here, the
     * initial interval is set at 500ms) or the subsequent computations within 100ms (example is set
     * to 50ms), a [TimeoutException] will occur and the stream will return an error and complete.
     */
    fun exampleFour() {
        println("\nUsing Timeouts:\n")

        nextSolarEclipse(LocalDate.now())
                .timeout<Long, Long>(
                        Observable.timer(400, TimeUnit.MILLISECONDS),
                        Function { Observable.timer(40, TimeUnit.MILLISECONDS) }
                )
                .subscribe(
                        { println(it) },
                        { it.printStackTrace() },
                        { println("Completed") }
                )

        TimeUnit.MILLISECONDS.sleep(2000)
    }

    /**
     * Example Five: Using TimeInterval, it is possible to track how long much time elapsed since
     * the previous event. This example will use the same eclipse service.
     */
    fun exampleFive() {
        println("\nUsing TimeInterval:\n")

        nextSolarEclipse(LocalDate.now())
                .timeInterval()
                .subscribe(
                        { println(it) },
                        { it.printStackTrace() },
                        { println("Completed") }
                )

        TimeUnit.MILLISECONDS.sleep(2000)
    }

    /**
     * Example Six: Retry when errors in the stream occur. Different to a transaction failure, for
     * which it is probably sensible to create an abstract TransactionResult class or interface, and
     * implement concrete Success and Failure versions.
     */
    fun exampleSix() {
        println("\nRetry:\n")

        risky()
                .timeout(1, TimeUnit.SECONDS)
                .doOnError { throwable -> println("Will retry: $throwable") }
                .retry()
                .subscribe { println(it) }
    }

    /**
     * Example Seven: Limit the number of retry attempts, or risk saturating resources.
     */
    fun exampleSeven() {
        println("\nLimit Number of Retry's:\n")

        risky()
                .timeout(1, TimeUnit.SECONDS)
                .doOnError { throwable -> println("Will retry: $throwable") }
                .retry(10)
                .subscribe { println(it) }
    }

    /**
     * Example Eight: Limit the number of retry attempts based on the number of previous attempts
     * made and the exception thrown.
     */
    fun exampleEight() {
        println("\nMore Retrying:\n")

        risky()
                .timeout(1, TimeUnit.SECONDS)
                .doOnError { throwable -> println("Will retry $throwable") }
                .retry { attempt, e -> attempt <= 10 && e !is TimeoutException }
                .subscribe { println(it) }
    }

    /**
     * Example Nine: Delay Retry, so that we wait a second before the next attempt. This is done
     * using `retryWhen()`, which takes an Observable that emits a Throwable every time there is an
     * upstream failure. By delaying that event, which is the signal to `retryWhen()` that it should
     * attempt to retry, it is possible to pause between attempts.
     */
    fun exampleNine() {
        println("\nDelay Retry:\n")

        risky()
                .timeout(1, TimeUnit.SECONDS)
                .doOnError { throwable -> println("Will retry $throwable") }
                .retryWhen { failure -> failure.delay(1, TimeUnit.SECONDS) }
                .subscribe { println(it) }

        TimeUnit.SECONDS.sleep(11)
    }

    /**
     * Example Ten: Delay Retry and Retain Error Notification. Using `retryWhen().take(10)` has a
     * similar effect to `retry(10)`. However, in this case, errors are swallowed and after the
     * tenth attempt a completion message is sent. To retain details of the error, it is
     * necessary to propagate the error message downstream.
     */
    fun exampleTen() {
        println("\nDelay Retry and Retain Error Notification:\n")

        val MAX_ATTEMPTS = 11

        risky()
                .timeout(1, TimeUnit.SECONDS)
                .retryWhen { failures ->
                    failures.zipWith(
                            Observable.range(1, MAX_ATTEMPTS),
                            { error, attempt ->
                                if (attempt < MAX_ATTEMPTS) {
                                    Observable.timer(1, TimeUnit.SECONDS)
                                } else {
                                    Observable.error(error)
                                }
                            })
                            .flatMap { it }
                }
                .subscribe(
                        { println(it) },
                        { it.printStackTrace() },
                        { println("Completed") }
                )

        TimeUnit.SECONDS.sleep(11)
    }

    /**
     * Example Eleven: Adjust Delay Time Between Events. Using the concept demonstrated in
     * [exampleTen], it is possible to adjust the delay time. In this example, the first
     * retry occurs immediately and subsequent delays grow exponentially.
     */
    fun exampleEleven() {
        println("\nAdjust Delay Time Between Events\n")

        val MAX_ATTEMPTS = 11

        fun handleRetryAttempt(error: Throwable, attempt: Int): Observable<Long> {
            return when (attempt) {
                1 -> Observable.timer(0, TimeUnit.MILLISECONDS)
                MAX_ATTEMPTS -> Observable.error(error)
                else -> {
                    val exponentialDelay = Math.pow(2.0, attempt - 2.0).toLong()
                    Observable.timer(exponentialDelay, TimeUnit.SECONDS)
                }
            }
        }

        risky()
                .timeout(1, TimeUnit.SECONDS)
//                .doOnError { println("Hmm... $it") }
                .retryWhen { failures ->
                    failures
                            .zipWith(
                                    Observable.range(1, MAX_ATTEMPTS),
                                    { error, attempt ->
                                        handleRetryAttempt(error, attempt)
                                    })
                            .flatMap { it }
                }
                .subscribe(
                        { println(it) },
                        { it.printStackTrace() },
                        { println("Completed") }
                )

        TimeUnit.SECONDS.sleep(30)
    }

    private fun nextSolarEclipse(after: LocalDate): Observable<LocalDate> {
        return Observable
                .just(
                        LocalDate.of(2016, Month.MARCH, 9),
                        LocalDate.of(2016, Month.SEPTEMBER, 1),
                        LocalDate.of(2017, Month.FEBRUARY, 26),
                        LocalDate.of(2017, Month.AUGUST, 21),
                        LocalDate.of(2018, Month.FEBRUARY, 15),
                        LocalDate.of(2018, Month.JULY, 13),
                        LocalDate.of(2018, Month.AUGUST, 11),
                        LocalDate.of(2019, Month.JANUARY, 6),
                        LocalDate.of(2019, Month.JULY, 2),
                        LocalDate.of(2019, Month.DECEMBER, 26)
                )
                .skipWhile { date ->
                    !date.isAfter(after)
                }
                .zipWith(
                        Observable.interval(500, 50, TimeUnit.MILLISECONDS),
                        { date, _ -> date }
                )
    }

    // Observable that misbehaves: don't want to do this for a real Observable!
    private fun risky(): Observable<String> {
        return Observable
                .fromCallable {
                    if (Math.random() < 0.1) {
                        Thread.sleep((Math.random() * 2_000).toLong())
                        "OK"
                    } else {
                        throw RuntimeException("Transient")
                    }
                }
    }
}