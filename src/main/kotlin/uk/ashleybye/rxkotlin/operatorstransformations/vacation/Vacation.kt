package uk.ashleybye.rxkotlin.operatorstransformations.vacation

import uk.ashleybye.rxkotlin.operatorstransformations.location.City
import uk.ashleybye.rxkotlin.operatorstransformations.weather.Weather
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Holds vacation information.
 *
 * @constructor private - use [build]
 * @property toCity - the city travelling to
 * @property onDate - the date of travel
 * @property details - additional details about the vacation
 */
class Vacation private constructor(val toCity: City, val onDate: LocalDate, val details: VacationDetails) {
    companion object {

        /**
         * Builds an instance of `Vacation`.
         *
         * @param toCity - the city travelling to
         * @param onDate - the date of travel
         * @param weather - the weather at the city
         * @param flight - details of the flight to the city
         * @param hotel - the details of the hotel at the city
         *
         * @return the vacation information
         */
        fun build(
                toCity: City,
                onDate: LocalDate,
                weather: Weather,
                flight: Flight,
                hotel: Hotel
        ): Vacation {
            val details = VacationDetails.build(weather, flight, hotel)
            return Vacation(toCity, onDate, details)
        }
    }

    override fun toString(): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yy")
        return "Travelling to $toCity on ${onDate.format(formatter)}, " +
                "the Weather (${details.weather}), " +
                "Flying from ${details.flight}, " +
                "Staying at ${details.hotel}, " +
                "Total Cost: £${details.flight.cost + details.hotel.cost}"
    }
}
