package uk.ashleybye.rxkotlin.operatorstransformations.vacation

import uk.ashleybye.rxkotlin.operatorstransformations.location.City
import java.util.*

/**
 * Holds flight information.
 *
 * Holds information about a flight's city of departure, arrival and cost (randomly generated).
 *
 * @constructor initialises the city of departure and arrival
 * @param from - the city of departure
 * @param to - the city of arrival
 * @property cost - the randomly generated cost of the flight
 */
data class Flight(private val from: City, private val to: City) {
    companion object {
        private val random = Random()
    }

    val cost = Math.round(random.nextFloat() * 300)

    override fun toString() = "$from to $to: £$cost"
}
