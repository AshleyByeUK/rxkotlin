package uk.ashleybye.rxkotlin.operatorstransformations.weather

import java.util.*

/**
 * Holds weather information.
 *
 * Holds information about weather observations (would be for a specific place in a real
 * application).
 *
 * @property temperature - the [Temperature] for this observation
 * @property wind - the [Wind] for this observation
 * @property isSunny - true if it is sunny at the observed location; false, otherwise
 */
data class Weather(val temperature: Temperature, val wind: Wind) {
    companion object {
        private val random = Random()
    }

    val isSunny = random.nextBoolean()

    override fun toString() = "Temp: ${temperature.degreesCelsius} Celsius, " +
            "Wind: ${wind.speedKnots}, " +
            "Sunny: ${if (isSunny) "Yes" else "No"}"
}
