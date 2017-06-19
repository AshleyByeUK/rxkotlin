package uk.ashleybye.rxkotlin.flowcontrol.backpressure

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
//    DishWashing.exampleOne()
//    DishWashing.exampleTwo()
//    DishWashing.exampleThree()
    DishWashing.exampleFour()
}

object DishWashing {

    private val dishes = Observable
            .range(1, 1_000_000_000)
            .map { Dish(it) }

    /**
     * Example One.
     *
     * Show's how range is not asynchronous, so each [Dish] is washed in the order it is created,
     * blocking until it has been washed. i.e. it is passed to an observer within the context of the
     * same thread. If the observer is slow, the producer cannot produce any new dishes until the
     * observer has finished washing the previous one. This means a backlog. In the context of a
     * restaurant, consider that this simulates a waiter being unable to leave new dishes for
     * cleaning whilst the ones currently being washed are not done. This means the waiter is unable
     * to serve customers, which means new customers cannot enter the restaurant
     */
    fun exampleOne() {
        println("\nExample One:\n")

        dishes
                .subscribe {
                    println("Washing: $it")
                    sleepMillis(50)
                }
    }

    /**
     * Example Two.
     *
     * By setting `observeOn()` to `Schedulers.io()`, the washing process is consigned to another
     * thread, freeing up the main thread to keep producing new dishes. This also shows how there is
     * some default backpressure applied: lots of events are created but they are only pushed
     * downstream in line with the default backpressure.
     */
    fun exampleTwo() {
        println("\nExample Two:\n")

        dishes
                .observeOn(Schedulers.io())
                .subscribe {
                    println("Washing: $it")
                    sleepMillis(50)
                }
    }

    /**
     * Example Three.
     *
     * Instead of using `Observable`, using `Flowable` allows manual configuration of backpressure.
     * One option is to set the buffer size and an optional warning. However, once the buffer is
     * exceeded, the message will be printed and an exception thrown.
     */
    fun exampleThree() {
        println("\nExample Three:\n")

        dishes.toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer(1000, { println("Buffer full") })
                .observeOn(Schedulers.io())
                .subscribe {
                    println("Washing: $it")
                    sleepMillis(50)
                }

        TimeUnit.SECONDS.sleep(3)
    }

    /**
     * Example Four.
     *
     * Another option for `Flowable` is to use `onBackpressureDrop()`, which will discard any items
     * following the exhaustion of the buffer. Imagine a waiter throwing away plates if the washing
     * staff are unable to wash quick enough!
     */
    fun exampleFour() {
        println("\nExample Four:\n")

        dishes.toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureDrop { println("Throw away: $it") }
                .observeOn(Schedulers.io())
                .subscribe {
                    println("Washing: $it")
                    sleepMillis(50)
                }
    }

    //TODO(Ash): I think the rest of this section needs to be applied to Flowable, page 239 onwards.

    private fun sleepMillis(period: Long) = TimeUnit.MILLISECONDS.sleep(period)
}