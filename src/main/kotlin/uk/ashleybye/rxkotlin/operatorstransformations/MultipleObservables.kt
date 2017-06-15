package uk.ashleybye.rxkotlin.operatorstransformations

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.rxkotlin.zipWith
import uk.ashleybye.rxkotlin.operatorstransformations.location.City
import uk.ashleybye.rxkotlin.operatorstransformations.vacation.SimpleVacationPlanner
import uk.ashleybye.rxkotlin.operatorstransformations.vacation.VacationPlanner
import java.time.LocalDate
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    doExample({ MultipleObservables.exampleOne() }, "Example One: Merge")
    doExample({ MultipleObservables.exampleTwo() }, "Example Two: Zip")
    doExample({ MultipleObservables.exampleThree() }, "Example Three: Vacation Planner")
    doExample({ MultipleObservables.exampleFour() }, "Example Four: Zipping In-Sync Observables")
    doExample({ MultipleObservables.exampleFive() }, "Example Five: Zipping Out of-Sync Observables")
    doExample({ MultipleObservables.exampleSix() }, "Example Six: Using combineLatest()")
    doExample({ MultipleObservables.exampleSeven() }, "Example Seven: Using withLatestFrom()")
    doExample({ MultipleObservables.exampleEight() }, "Example Eight: Using withLatestFrom() and startWith()")
    doExample({ MultipleObservables.exampleNine() }, "Example Nine: Using amb() and ambWith()")
}

private fun doExample(example: () -> Unit, title: String = "Example") {
    println("\n$title\n")
    example.invoke()
    println()
}

class MultipleObservables {
    companion object {

        /**
         * Example One: Merge.
         *
         * Take more than one stream and merge the results (will appear in order they are processed).
         */
        fun exampleOne() {
            val streamA = Observable
                    .interval(90, TimeUnit.MILLISECONDS)
                    .take(5)
                    .map { i -> "A$i" }
            val streamB = Observable
                    .interval(45, TimeUnit.MILLISECONDS)
                    .take(5)
                    .map { i -> "B$i" }
            val mergedStream = Observable
                    .merge(streamA, streamB)

            mergedStream.subscribe(
                    { print("$it ") },
                    { println("Error") },
                    { println() })

            TimeUnit.MILLISECONDS.sleep(450)
        }

        /**
         * Example Two: Zip.
         *
         * Take more than one stream and zip the results together. Zip operates on the assumption
         * that the events emitted from each observable are related, therefore if one does not meet
         * the requirements of any wrapping filters, etc., then the observable will not zip those
         * events and will move on to the next ones. Furthermore, if one is in error or completes,
         * then the event is propagated downstream.
         */
        fun exampleTwo() {
            val streamTemp = Observable
                    .interval(100, TimeUnit.MILLISECONDS)
                    .take(10)
                    .map { i -> "Temp Reading $i" }
            val streamWind = Observable
                    .interval(100, TimeUnit.MILLISECONDS)
                    .take(10)
                    .map { i -> "Wind Speed Reading $i" }
            // Observable.zip(streamTemp, streamWind)...
            val weather = streamTemp
                    .zipWith(streamWind, { temp, wind -> Pair(temp, wind) })

           weather.subscribe({ println(it) })

            TimeUnit.MILLISECONDS.sleep(1000)
        }

        /**
         * Example Three: Vacation Planner.
         *
         * Complex example using `zip()`, `map()`, `flatMap()` and `filter()`. See [VacationPlanner]
         * for implementation details.
         */
        fun exampleThree() {
            val planner: VacationPlanner = SimpleVacationPlanner()
            val possibleVacations = planner
                    .getOptions(
                            City.LONDON,
                            LocalDate.now(),
                            LocalDate.now().plusDays(10),
                            150,
                            200)

            possibleVacations.subscribe { println(it) }
        }

        /**
         * Example Four: Zipping In-Sync Observables.
         *
         * The assumption is that observables will always be in sync in time and frequency when
         * zipping. This is the effect, in that the difference between emission time of each
         * oscillates around zero.
         */
        fun exampleFour() {
            val red = Observable.interval(10, TimeUnit.MILLISECONDS)
            val green = Observable.interval(10, TimeUnit.MILLISECONDS)

            val disposable = Observables.zip(
                    red.timestamp(),
                    green.timestamp(),
                    { r, g -> r.time() - g.time() }
            ).forEach(::println)

            TimeUnit.MILLISECONDS.sleep(100)
            disposable.dispose()
        }

        /**
         * Example Five: Zipping Out of-Sync Observables.
         *
         * Following on from [exampleFour], if green slows down, the time difference between it and
         * red increases. Red is consumed in real-time but must wait for an emission from green.
         * This can lead to stale data or even memory leaks, given enough time.
         */
        fun exampleFive() {
            val red = Observable.interval(10, TimeUnit.MILLISECONDS)
            val green = Observable.interval(11, TimeUnit.MILLISECONDS)

            val disposable = Observables.zip(
                    red.timestamp(),
                    green.timestamp(),
                    { r, g -> r.time() - g.time() }
            ).forEach(::println)

            TimeUnit.MILLISECONDS.sleep(100)
            disposable.dispose()
        }

        /**
         * Example Six: Using combineLatest().
         *
         * Following on from [exampleFive], any time an upstream event is received, an event can be
         * transmitted downstream using the latest event from each. As seen in this example, it
         * doesn't matter which stream the event originates from, `combineLatest()` will take the
         * last value of the other stream, i.e. it is symmetric.
         *
         * Running for 10 seconds, there should be about 1000 "F events" (10 seconds / 10
         * milliseconds) and about 588 "S events".
         */
        fun exampleSix() {
            val fastStream = Observable.interval(10, TimeUnit.MILLISECONDS).map { x -> "F$x" }
            val slowStream = Observable.interval(17, TimeUnit.MILLISECONDS).map { x -> "S$x" }

            val disposable = Observables.combineLatest(
                    fastStream,
                    slowStream,
                    { fast, slow -> "$fast:$slow" }
            ).forEach(::println)

            TimeUnit.SECONDS.sleep(10)
            disposable.dispose()
        }

        /**
         * Example Seven: Using withLatestFrom().
         *
         * Following on from [exampleSix], sometimes it is only necessary to emit an event when
         * something appears in one upstream stream and the latest version from another upstream
         * stream, but not vice-versa. This can be achieved using `withLatestFrom()`, i.e. it is
         * asymmetric.
         *
         * Note that all slow events appear exactly once whilst some fast events are dropped. The
         * fast stream is just a helper, used only when slow emits.
         */
        fun exampleSeven() {
            val fastStream = Observable.interval(10, TimeUnit.MILLISECONDS).map { x -> "F$x" }
            val slowStream = Observable.interval(17, TimeUnit.MILLISECONDS).map { x -> "S$x" }

            val disposable = slowStream.withLatestFrom(
                    fastStream,
                    { slow, fast -> "$slow:$fast" }
            ).forEach(::println)

            TimeUnit.SECONDS.sleep(10)
            disposable.dispose()
        }

        /**
         * Example Eight: Using withLatestFrom() and startWith().
         *
         * Following on from [exampleSeven], if the secondary stream is slower than the primary, the
         * primary will not emit until the first event is emitted from the secondary. To force an
         * event on the secondary stream, thus preserving the primary, us `startWith()`. See how
         * this works by introducing an artificial delay of 100ms into the fastStream.
         */
        fun exampleEight() {
            val fastStream = Observable
                    .interval(10, TimeUnit.MILLISECONDS)
                    .map { x -> "F$x" }
                    .delay(100, TimeUnit.MILLISECONDS)
                    .startWith("FX")
            val slowStream = Observable.interval(17, TimeUnit.MILLISECONDS).map { x -> "S$x" }

            val disposable = slowStream.withLatestFrom(
                    fastStream,
                    { slow, fast -> "$slow:$fast" }
            ).forEach(::println)

            TimeUnit.SECONDS.sleep(2)
            disposable.dispose()
        }

        /**
         * Example Nine: Using amb().
         *
         * The `amb()` and `ambWith()` operators subscribe to all upstream observables and waits for
         * the first item emitted. When one stream emits an event, `amb()` discards all the other
         * streams and only forwards events from the first one downstream. In the following example,
         * note the `initialDelay` parameter that controls which observable emits first.
         */
        fun exampleNine() {
            fun stream(initialDelay: Long, interval: Long, name: String): Observable<String> = Observable
                    .interval(initialDelay, interval, TimeUnit.MILLISECONDS)
                    .map { x -> "$name $x" }
                    .doOnSubscribe { println("Subscribe to $name") }
                    .doOnDispose { println("Dispose of $name") }

            println("amb()")
            val first = Observable.amb(
                    mutableListOf<ObservableSource<String>>(
                            stream(100, 17, "S"),
                            stream(200, 10, "F"))
            ).subscribe { println(it) }

            TimeUnit.SECONDS.sleep(1)
            first.dispose()

            println("\nambWith()")
            val second = stream(100, 17, "S").ambWith(stream(200, 10, "F")).subscribe(::println)
            
            TimeUnit.SECONDS.sleep(1)
            second.dispose()
        }
    }
}
