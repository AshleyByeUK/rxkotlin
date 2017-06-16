package uk.ashleybye.rxkotlin.operatorstransformations

import io.reactivex.Observable
import uk.ashleybye.rxkotlin.operatorstransformations.extensions.odd

fun main(args: Array<String>) {
    doExample({ CustomOperators.exampleOne() }, "Example One: Extension Function")
}

private fun doExample(example: () -> Unit, title: String = "Example") {
    println("\n$title\n")
    example.invoke()
    println()
}

class CustomOperators {
    companion object {

        /**
         * Example One: Simple Extension Function.
         *
         * This simplistic example demonstrates how to use an extension function for `Observable`.
         * The function, [odd], will only emit odd numbered elements in the stream (that is, the
         * odd event emissions, not the odd values in the upstream stream, i.e. event 1, 3, 5, ...).
         * Use of the extension function is shown with two examples, numbers and characters.
         */
        fun exampleOne() {
            val upstreamNumbers = Observable.range(2, 20)
            val upstreamCharacters = Observable
                    .range(0, 'Z' - 'A' + 1)
                    .map { char -> 'A' + char }

            println("Odd Emitted Numbers:\n")
            upstreamNumbers.odd().subscribe(
                    { print("$it ") },
                    { println("Error!") },
                    { println() }
            )

            println("\nOdd Emitted Characters:\n")
            upstreamCharacters.odd().subscribe(
                    { print("$it ") },
                    { println("Error!") },
                    { println() }
            )
        }
    }
}