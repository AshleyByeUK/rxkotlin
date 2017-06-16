package uk.ashleybye.rxkotlin.operatorstransformations

import io.reactivex.Observable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*


fun main(args: Array<String>) {
    doExample({ AdvancedOperators.exampleOne() }, "Example One: Scan")
    doExample({ AdvancedOperators.exampleTwo() }, "Example Two: Scan with Initial Value")
    doExample({ AdvancedOperators.exampleThree() }, "Example Three: Reduce")
    doExample({ AdvancedOperators.exampleFour() }, "Example Four: Collect")
    doExample({ AdvancedOperators.exampleFive() }, "Example Five: Distinct")
}

private fun doExample(example: () -> Unit, title: String = "Example") {
    println("\n$title\n")
    example.invoke()
    println()
}

class AdvancedOperators {
    companion object {

        /**
         * Example One: Scan.
         *
         * The `scan()` operator takes teo parameters, the last result returned and the next value
         * from the upstream emitter. The first of these parameters, the accumulator, keeps track of
         * the cumulative total, which is what gets emitted each time. Each upstream value is added
         * to the accumulator. Obviously, the accumulator can be used for any purpose which requires
         * keeping track of previous values.
         */
        fun exampleOne() {
            val progress = Observable.just<Long>(10, 14, 12, 13, 14, 16, 13, 8)
            val totalProgress = progress
                    .scan { accumulator, chunk -> accumulator + chunk }
                    .map { i -> "$i%" }
            totalProgress.subscribe(::println)
        }

        /**
         * Example Two: Scan with Initial Value.
         *
         * `scan()` has an overloaded version which can take an initial value. If no initial value
         * is provided, then the return type of the variable will be the type of the accumulator. If
         * a default value is provided, the return type will be that of the initial value. It
         * follows that any values received from the upstream emitter must either be of the same
         * type as the initial value or mapped to it prior to `scan()`.
         */
        fun exampleTwo() {
            val factorials = Observable
                    .range(2, 100)
                    .scan(BigInteger.ONE,
                            { factorial, current ->
                                factorial.multiply(BigInteger.valueOf(current.toLong()))
                            })
            factorials.subscribe(
                    { print("$it ") },
                    { println("Error!") },
                    { println() }
            )
        }

        /**
         * Example Three: Reduce.
         *
         * The `reduce()` operator is similar to `scan()`, except that we don't care about the
         * intermediate values. The `reduce()` operator can either be used on its own is in the
         * first example or in conjunction with `map()` as in the second. Arguably, the second is
         * easier and certainly looks neater.
         */
        fun exampleThree() {
            data class CashTransfer(val amount: BigDecimal)

            val transfers = Observable.just(
                    CashTransfer(BigDecimal.valueOf(10_000_000)),
                    CashTransfer(BigDecimal.valueOf(329_456.72)),
                    CashTransfer(BigDecimal.valueOf(12_024_936.97)),
                    CashTransfer(BigDecimal.valueOf(123.45)),
                    CashTransfer(BigDecimal.valueOf(9_876_543.21)),
                    CashTransfer(BigDecimal.valueOf(456_123.78)),
                    CashTransfer(BigDecimal.valueOf(135_791.35)),
                    CashTransfer(BigDecimal.valueOf(4_329_286.00)),
                    CashTransfer(BigDecimal.valueOf(163_500.00))
            )

            val total1: Single<BigDecimal> = transfers
                    .reduce(BigDecimal.ZERO,
                            { total, transfer ->
                                total.add(transfer.amount)
                            })

            val total2 = transfers
                    .map(CashTransfer::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)

            total1.subscribe { x -> println("Reduce: $x") }
            total2.subscribe { x -> println("Map Reduce: $x") }
        }

        /**
         * Example Four: Collect.
         *
         * The `collect()` operator is similar to reduce, but is used to collect each event emitted
         * from upstream, e.g. to add it to a mutable collection or aggregating all events into a
         * [StringBuilder].
         */
        fun exampleFour() {
            val all: Single<MutableList<Int>> = Observable
                    .range(10, 20)
                    .collectInto(mutableListOf<Int>(), { list, item -> list.add(item) })

            all.subscribe { x -> println(x) }

            val string: Single<String> = Observable
                    .range(10, 20)
                    .collectInto(
                            StringBuilder(),
                            { builder, item -> builder.append(item).append(" - ") }
                    )
                    .map(StringBuilder::toString)

            string.subscribe { x -> println(x) }
        }

        /**
         * Example Five: Distinct.
         *
         * If it is necessary to only emit events that have not been seen prior, the `distinct()`
         * operator enables this. The following example will print the first 10 ints from a stream
         * of randomly generated integers.
         *
         * Distinct can also be passed a "key", which is used to identify an object. For example, if
         * in the Twitter example, only the first status update from each user was required, the
         * user's ID can be extracted to provide the uniqueness property:
         *
         * val tweets: Observable<Status> = //...
         * val distinctUserTweets: Observable<Status> = tweets
         *          .distinct(status ->
         *                  status.getUser().getId()
         *
         * However, `distinct()` has to remember every emitted item and can very quickly consume a
         * lot of memory. More often, what is actually required is to only emit events when some
         * upstream value has changed. The weather, for example, may be emitted every minute but
         * changes may be less regular. Use of `distinctUntilChanged()` will only forward an event
         * if it has changed, ignoring all others:
         *
         * val weather: Observable<Weather> = //...
         * val tempChanges: Observable<Weather> = weather.distinctUntilChanged(Weather::temperature)
         */
        fun exampleFive() {
            val randomInts = Observable.create<Int> { emitter ->
                val random = Random()
                while (!emitter.isDisposed) {
                    emitter.onNext(random.nextInt(1000))
                }
            }

            val uniqueRandomInts = randomInts
                    .distinct()
                    .take(10)

            uniqueRandomInts.subscribe(
                    { println("$it") },
                    { println("Error!") },
                    { println() }
            )
        }
    }
}
