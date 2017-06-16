package uk.ashleybye.rxkotlin.operatorstransformations

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    doExample({ AdvancedOperators.exampleOne() }, "Example One: Scan")
    doExample({ AdvancedOperators.exampleTwo() }, "Example Two: Scan with Initial Value")
    doExample({ AdvancedOperators.exampleThree() }, "Example Three: Reduce")
    doExample({ AdvancedOperators.exampleFour() }, "Example Four: Collect")
    doExample({ AdvancedOperators.exampleFive() }, "Example Five: Distinct")
    doExample({ AdvancedOperators.exampleSix() }, "Example Six: merge(), concat() and switchOnNext()")
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

        /**
         * Example Six: merge(), concat() and switchOnNext().
         *
         * Uses helper function [speak], see method comments for how it operates. The example is
         * meant to represent three people on a stage with a microphone, each speaking at slightly
         * different rates. It highlights how `merge()`, `concat()` and `switchOnNext()` differ.
         *
         * Merge: This simulates all three people speaking at the same time. The result is chaos
         * and only due to prefixing each word with the name of the speaker is it possible to
         * distinguish who is speaking - all that would be heard is noise. This shows how `merge()`
         * subscribes to each stream immediately and forwards each event downstream as soon as it is
         * emitted - there is no buffering or halting.
         *
         * Concat: This simulates each person speaking their phrase in turn. Concat first subscribes
         * to Alice, when she completes Bob is subscribed to, and so on.
         *
         * Switch on Next: This simulates one person starting to speak after a random period of time
         * has elapsed. After another random period of time, the next person starts speaking, and
         * the rules are that the previous person stops and cannot say any more. The random delay is
         * used to show how `switchOnNext()` works. This operator subscribes to an outer observable
         * which emits inner observables. When the first inner observable begins emitting,
         * `switchOnNext()` subscribes to it. When the next inner observable begins emitting,
         * `switchOnNext()` subscribes to it and ignores the first, even if it continues to emit new
         * events. This pattern continues for all remaining inner observables. Without using a
         * random delay, all inner observables would begin emitting at the same time and it would
         * not be possible to examine how `switchOnNext()` works. To view the result of this
         * operator, it is necessary to subscribe to an `Observable<Observable<String>>`, which is
         * the type of `quotes`.
         */
        fun exampleSix() {
            val alice = speak("To be, or not to be: that is the question", 110)
            val bob = speak("Though this be madness, yet there is method in't", 90)
            val jane = speak("There are more things in Heaven and Earth, " +
                    "Horatio, than are dreamt of in your philosophy", 100)

            // merge().
            println("\nMerge:\n")
            Observable
                    .merge(
                            alice.map { word -> "Alice: $word" },
                            bob.map { word -> "Bob: $word" },
                            jane.map { word -> "Jane: $word" }
                    ).subscribe(::println)
            TimeUnit.SECONDS.sleep(10)

            // concat().
            println("\nConcat:\n")
            Observable
                    .concat(
                            alice.map { word -> "Alice: $word" },
                            bob.map { word -> "Bob: $word" },
                            jane.map { word -> "Jane: $word" }
                    ).subscribe(::println)
            TimeUnit.SECONDS.sleep(20)

            // switchOnNext().
            println("\nSwitch on Next\n")
            val random = Random()
            val quotes = Observable
                    .just(
                            alice.map { word -> "Alice: $word" },
                            bob.map { word -> "Bob: $word" },
                            jane.map { word -> "Jane: $word" }
                    ).flatMap { innerObservable ->
                Observable.just(innerObservable)
                        .delay(random.nextInt(5).toLong(), TimeUnit.SECONDS)
            }
            Observable
                    .switchOnNext(quotes)
                    .subscribe(::println)
            TimeUnit.SECONDS.sleep(15)
        }
    }
}

/**
 * Turns strings into stream of words with a delay for each word.
 *
 * Takes a string and returns a stream of each word in the original string, with each word delayed
 * by the computed delay of number of millis per character in the word.
 *
 * Examining the implementation, first punctuation is removed from the string, which is subsequently
 * split into a list of words. For each word, the length of time taken to "say" that word is
 * calculated by multiplying the word length by `millisPerChar`. However, to ensure that the stream
 * remains in the correct order, the delays need to be cumulative. That is, if the first word takes
 * 100 millis, the second 150, the third 200 and so on, the first word needs to appear with 0 delay
 * (this is covered later), the second after 100 millis, the third after 250 millis (100 + 150), the
 * fourth after 450 millis (250 + 200), etc. This is where `scan()` is useful - it keeps an
 * accumulated value but also emits each intermediate total, exactly what is required. Finally, the
 * stream of words needs to be returned to the client with the correct delays applied to each. This
 * is achieved by zipping the words with the helper stream `absoluteDelay` to produce a
 * `Pair<String, Long>`: the word and its associated delay. The last step is to `flatMap()` the
 * string with the delay for each pair, resulting in an `Observable<String>` delayed by the
 * required amount of time, i.e. each pair is a one element `Observable` shifted in time.
 */
private fun speak(quote: String, millisPerChar: Long): Observable<String> {
    val tokens = quote.replace("[:,]", "").split(" ")
    val words = Observable.fromIterable(tokens)
    val absoluteDelay = words
            .map(String::length)
            .map { len -> len * millisPerChar }
            .scan { total, current -> total + current }

    return words
            .zipWith(absoluteDelay.startWith(0L), { word, delay ->
                Pair<String, Long>(word, delay)
            })
            // Using destructuring, the following can be re-written as shown below:
//            .flatMap { pair ->
//                Observable.just(pair.first)
//                        .delay(pair.second, TimeUnit.MILLISECONDS)
//            }
            .flatMap { (first, second) ->
                Observable.just(first)
                        .delay(second, TimeUnit.MILLISECONDS)
            }
}
