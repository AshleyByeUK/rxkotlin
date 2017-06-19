package uk.ashleybye.rxkotlin.flowcontrol.backpressure

/**
 * A `Dish` is a large object with an identifier. They have a [oneKb] buffer which simulates some
 * extra memory utilisation.
 */
class Dish(private val id: Int) {

    private val oneKb = kotlin.ByteArray(1_024)

    init {
        println("Created $id")
    }

    override fun toString() = id.toString()
}