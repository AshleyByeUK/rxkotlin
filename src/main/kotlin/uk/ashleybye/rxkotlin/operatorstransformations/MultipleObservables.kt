package uk.ashleybye.rxkotlin.operatorstransformations

import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith
import java.time.LocalDate
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    doExample({ MultipleObservables.exampleOne() }, "Example One: Merge")
    doExample({ MultipleObservables.exampleTwo() }, "Example Two: Zip")
    doExample({ MultipleObservables.exampleThree() })
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
         * Take more than one stream and zip the results together.
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
    }
}
