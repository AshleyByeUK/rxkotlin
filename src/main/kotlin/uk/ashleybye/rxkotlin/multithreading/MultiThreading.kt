package uk.ashleybye.rxkotlin.multithreading

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * Some of these examples will be best observed by commenting out the others, to prevent the output
 * from each of the others becoming intermingled.
 */
fun main(args: Array<String>) {
    val mt = MultiThreading()
    doExample({ mt.exampleOne() }, "Example One: No Concurrency")
    doExample({ mt.exampleTwo() }, "Example Two: Single Scheduler")
    doExample({ mt.exampleThree() }, "Example Three: Two Schedulers One Observable")
    doExample({ mt.exampleFour() }, "Example Four: Which Thread is Doing the Work?")
    doExample({ mt.exampleFive() }, "Example Five: Broken Attempt at Parallelisation")
    doExample({ mt.exampleSix() }, "Example Six: Fixed Attempt at Parallelisation")
    doExample({ mt.exampleSeven() }, "Example Seven: Batching with groupBy()")
    doExample({ mt.exampleEight() }, "Example Eight: observeOn()")
    doExample({ mt.exampleNine() }, "Example Nine: Using subscribeOn() and Multiple observeOn()")
    doExample({ mt.exampleTen() }, "Example Ten: Decouple Producer and Consumer Threads")

    mt.shutdown()
}

private fun doExample(example: () -> Unit, title: String = "Example") {
    println("\n$title\n")
    example.invoke()
    println()
}

/**
 * Note that `subscribeOn()` should be treated as non-idiomatic. Where possible, try to get
 * Observables that come from naturally asynchronous sources or apply their own scheduling. This is,
 * however, useful when the underlying observable is known to be synchronous, and the use of
 * `subscribeOn()` is much better than attempting to build threads in `create()`. As such, it is
 * useful for retro-fitting existing synchronous API's or libraries. The aim for new applications
 * should be to build them as truly asynchronous without use of `subscribeOn()` and schedulers.
 */
class MultiThreading {
    private var logger: Logger = Logger.getSingleton()

    private val poolA = Executors.newFixedThreadPool(10, threadFactory("Sched-A-%d"))
    private val poolB = Executors.newFixedThreadPool(10, threadFactory("Sched-B-%d"))
    private val poolC = Executors.newFixedThreadPool(10, threadFactory("Sched-C-%d"))
    private val schedulerA = Schedulers.from(poolA)
    private val schedulerB = Schedulers.from(poolB)
    private val schedulerC = Schedulers.from(poolC)

    /**
     * Example One: No Concurrency.
     *
     * Note that the order of the output is predictable, and that every line runs in the main
     * thread.
     */
    fun exampleOne() {
        logger.log("Starting")
        val observable = simple()
        logger.log("Created")

        val anotherObservable = observable
                .map { x -> x }
                .filter { true }
        logger.log("Transformed")

        val disposable = anotherObservable.subscribe(
                { logger.log("Got $it") },
                { it.printStackTrace() },
                { logger.log("Completed") }
        )
        logger.log("Exiting")
        disposable.dispose()
    }

    /**
     * Example Two: Single Scheduler.
     *
     * Note that the order of the output is no longer predictable due to concurrency.
     */
    fun exampleTwo() {
        logger.log("Starting")
        val observable = simple()
        logger.log("Created")
        observable
                .subscribeOn(schedulerA)
                .subscribe(
                        { logger.log("Got $it") },
                        { it.printStackTrace() },
                        { logger.log("Completed") }
                )
        logger.log("Exiting")
    }

    /**
     * Example Three: Two Schedulers One Observable.
     *
     * If two schedulers are applied to the same observable, the one closest to the observable
     * source wins, all others are ignored. However, the others are used for a short period of time,
     * although the work is done on the closest scheduler. Thus the others are ignored and produce a
     * small amount of overhead.
     */
    fun exampleThree() {
        logger.log("Starting")
        val observable = simple()
        logger.log("Created")
        observable
                .subscribeOn(schedulerA)
                // Many other operators.
                .subscribeOn(schedulerB)
                .subscribe(
                        { logger.log("Got $it") },
                        { it.printStackTrace() },
                        { logger.log("Completed") }
                )
        logger.log("Exiting")
    }

    /**
     * Example Four: Which Thread is Doing the Work?
     *
     * Note how only a single worker is used to process the entire pipeline, which guarantees the
     * sequential processing of events. Thus, a single broken operator can slow down an entire
     * pipeline. This is a Rx anti-pattern: operators should be non-blocking, fast and as pure as
     * possible.
     */
    fun exampleFour() {
        logger.log("Starting")
        val observable = simple()
        logger.log("Created")
        observable
                .doOnNext { logger.log(it) }
                .map { "${it}1" }
                .doOnNext { logger.log(it) }
                .map { "${it}2" }
                .subscribeOn(schedulerA)
                .doOnNext { logger.log(it) }
                .subscribe(
                        { logger.log("Got $it") },
                        { it.printStackTrace() },
                        { logger.log("Completed") }
                )
        logger.log("Exiting")
    }

    /**
     * Example Five: Broken Attempt at Parallelisation.
     *
     * Note how using `map()`, the output is entirely sequential. Not at all what is wanted; each
     * purchase blocks until it is complete. Using `flatMap()` in place of `map()` doesn't help
     * either - the result is exactly the same.
     */
    fun exampleFive() {
        val rxGroceries = RxGroceries()
        val totalPrice: Single<BigDecimal> = Observable
                .just("bread", "butter", "milk", "tomato", "cheese")
                .subscribeOn(schedulerA) // BROKEN!!!
//                .map { product -> rxGroceries.doPurchase(product, 1) }
                .flatMap { product -> rxGroceries.purchase(product, 1) }
                .reduce { totalPrice, productPrice -> totalPrice.add(productPrice) }
                .toSingle()

        totalPrice.subscribe()
    }

    /**
     * Example Six: Fixed Attempt at Parallelisation.
     *
     * Following on from [exampleFive], the idiomatic solution is very similar to the above attempt
     * using `flatMap()`. The main observable emitting products cannot be parallelised, but for each
     * product, a new independent observable can be made which can be safely scheduled concurrently.
     *
     * Uses a second observable, `productPrices` to randomly generate a "price" - this is also used
     * to induce a delay in processing time (i.e. more products, more processing time) so that the
     * concurrency of the observable can be seen. This is zipped with each of the products. To make
     * the example simpler, the zip can be removed and replace `quantity` with 1.
     *
     * Run it a few times to see the effect.
     */
    fun exampleSix() {
        val rxGroceries = RxGroceries()
        val productPrices = Observable
                .just(1)
                .repeat()
                .map { Random().nextInt(3) }
                .filter { it != 0 }
        val totalPrice: Single<BigDecimal> = Observable
                .just("bread", "butter", "milk", "tomato", "cheese")
                .zipWith(productPrices, { product, price -> Pair<String, Int>(product, price) })
                .flatMap { (product, quantity) ->
                    rxGroceries
                            .purchase(product, quantity)
                            .subscribeOn(schedulerA)
                }.reduce { totalPrice, productPrice -> totalPrice.add(productPrice) }
                .toSingle()

        totalPrice.subscribe()
    }

    /**
     * Example Seven: Batching with groupBy().
     *
     * This example uses `groupBy()` to batch multiple products together, rather than using a
     * separate observable with quantities, as seen in [exampleSix]. Note the need to convert the
     * `Single` returned by `count()` into an `Observable`, which in this example is done after
     * mapping the count to a `Pair`.
     */
    fun exampleSeven() {
        val rxGroceries = RxGroceries()

        // @formatter:off
        val totalPrice = Observable
                .just("bread", "butter", "egg", "milk", "tomato", "cheese", "tomato", "egg", "egg")
                .groupBy { product -> product }
                .flatMap { grouped -> grouped
                        .count()
                        .map { quantity -> Pair(grouped.key, quantity) }
                        .toObservable()
                }.flatMap { (product, quantity) -> rxGroceries
                        .purchase(product, quantity.toInt())
                        .subscribeOn(schedulerA)
                }.reduce { totalPrice, productPrice -> totalPrice.add(productPrice) }
                .toSingle()
        // @formatter:on

        totalPrice.subscribe()
    }

    /**
     * Example Eight: observeOn().
     *
     * The position of `observeOn()` is important. Anything operators before it will occur on the
     * scheduler running before it or the main client thread if no scheduler was supplied (as per
     * this example). However, everything after `observeOn()` uses the the scheduler supplied to it.
     */
    fun exampleEight() {
        logger.log("Starting")
        val observable = simple()
        logger.log("Created")
        observable
                .doOnNext { logger.log("Found 1: $it") }
                .observeOn(schedulerA)
                .doOnNext { logger.log("Found 2: $it") }
                .subscribe(
                        { logger.log("Got: $it") },
                        { it.printStackTrace() },
                        { logger.log("Completed") }
                )
        logger.log("Exiting")
    }

    /**
     * Example Nine: Using subscribeOn() and Multiple observeOn().
     *
     * The output shows how `observeOn()` affects the scheduler of the subscriber. Subscribing on
     * `schedulerA`, the scheduler finds both A and B. We then observe on `schedulerB`, and again
     * both A and B are found by it. We then observe on `schedulerC`, which also finds both A and B.
     * Note, however, the effect this has on the scheduler of the subscriber - it too switches to
     * `schedulerC`. This example also highlights the importance of taking care where the
     * `observeOn()` operator is used but that `subscribeOn()` can be used anywhere in the
     * pipeline.
     */
    fun exampleNine() {
        logger.log("Starting")
        val observable = simple()
        logger.log("Created")
        observable
                .doOnNext { logger.log("Found 1: $it") }
                .observeOn(schedulerB)
                .doOnNext { logger.log("Found 2: $it") }
                .observeOn(schedulerC)
                .doOnNext { logger.log("Found 3: $it") }
                .subscribeOn(schedulerA)
                .subscribe(
                        { logger.log("Got: $it") },
                        { it.printStackTrace() },
                        { logger.log("Completed") }
                )
        logger.log("Exiting")
    }

    /**
     * Example Ten: Decouple Producer and Consumer Threads.
     *
     * By default, there is no decoupling of producer (`Observable.create`) and consumer
     * (subscriber) threads; Rx simply uses the same thread. However, `observeOn()` and
     * `subscribeOn()` can be used together so that subscription and processing of the pipeline
     * are carried out on one thread, and observing carried out on another. To achieve this (and to
     * air readability), `subscribeOn()` is placed close to the original observable and
     * `observeOn()` close to `subscribe()`. This ensures that only the subscriber uses the special
     * thread for observing.
     */
    fun exampleTen() {
        fun store(s: String): Observable<UUID> = Observable
                .create { emitter ->
                    logger.log("Storing $s")
                    // Hard work.
                    emitter.onNext(UUID.randomUUID())
                    emitter.onComplete()
                }

        logger.log("Starting")
        val observable = Observable
                .create<String> { emitter ->
                    logger.log("Subscribed")
                    emitter.onNext("A")
                    emitter.onNext("B")
                    emitter.onNext("C")
                    emitter.onNext("D")
                    emitter.onComplete()
                }
        logger.log("Created")
        observable
                .subscribeOn(schedulerA)
                .flatMap { record -> store(record).subscribeOn(schedulerB) }
                .observeOn(schedulerC)
                .subscribe(
                        { logger.log("Got: $it") },
                        { it.printStackTrace() },
                        { logger.log("Completed") }
                )
        logger.log("Exiting")
    }

    fun shutdown() {
        poolA.awaitTermination(2, TimeUnit.SECONDS)
        poolB.awaitTermination(2, TimeUnit.SECONDS)
        poolC.awaitTermination(2, TimeUnit.SECONDS)
        poolA.shutdown()
        poolB.shutdown()
        poolC.shutdown()
    }

    private fun threadFactory(pattern: String): ThreadFactory = ThreadFactoryBuilder()
            .setNameFormat(pattern)
            .build()

    private fun simple() = Observable
            .create<String> { emitter ->
                logger.log("Subscribed")
                emitter.onNext("A")
                emitter.onNext("B")
                emitter.onComplete()
            }

    private class RxGroceries {
        fun purchase(productName: String, quantity: Int): Observable<BigDecimal> {
            return Observable.fromCallable { doPurchase(productName, quantity) }
        }

        fun doPurchase(productName: String, quantity: Int): BigDecimal {
            val logger = Logger.getSingleton()
            val random = Random()

            logger.log("Purchasing $quantity $productName")
            // Real logic here; simulate some length of processing time.
            TimeUnit.SECONDS.sleep(quantity.toLong())

            // Simulate some product price being calculated.
            val priceForProduct = BigDecimal.valueOf(quantity * random.nextInt(10).toLong())

            logger.log("Done $quantity $productName")

            return priceForProduct
        }
    }
}
