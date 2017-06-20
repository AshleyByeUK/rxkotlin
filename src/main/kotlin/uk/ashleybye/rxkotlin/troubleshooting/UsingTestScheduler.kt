package uk.ashleybye.rxkotlin.troubleshooting

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    UsingTestScheduler.exampleOne()
}

object UsingTestScheduler {

    /**
     * Follows from [PlusMinusMonthsSpec](/../../../../../../test/kotlin/uk/ashleybye/rxkotlin/troubleshooting/PlusMinusMonthsSpec.kt).
     *
     * Shows that unless the test scheduler is manually advanced, nothing happens. Sleeping for one
     * second intervals is irrelevant and could be omitted but highlights the independence of the
     * test scheduler from the system clock. This means that schedulers should be passed wherever
     * a scheduler can be passed, rather than relying on the default `computation()` - this can
     * still be passed as a parameter. It also highlights that it could be prudent to use dependency
     * injection and pass Schedulers from the outside.
     */
    fun exampleOne() {
        val scheduler = TestScheduler()

        val fast = Observable
                .interval(10, TimeUnit.MILLISECONDS, scheduler)
                .map { num -> "F$num" }
                .take(3)
        val slow = Observable
                .interval(50, TimeUnit.MILLISECONDS, scheduler)
                .map { num -> "S$num"}

        val stream = Observable.concat(fast, slow)
        stream.subscribe { println(it) }
        println("Subscribed")

        TimeUnit.SECONDS.sleep(1)
        println("After one second")
        scheduler.advanceTimeBy(25, TimeUnit.MILLISECONDS)

        TimeUnit.SECONDS.sleep(1)
        println("After one more second")
        scheduler.advanceTimeBy(75, TimeUnit.MILLISECONDS)

        TimeUnit.SECONDS.sleep(1)
        println("...and one more")
        scheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS)
    }
}