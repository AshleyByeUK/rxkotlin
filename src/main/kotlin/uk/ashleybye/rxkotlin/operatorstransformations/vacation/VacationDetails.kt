package uk.ashleybye.rxkotlin.operatorstransformations.vacation

import uk.ashleybye.rxkotlin.operatorstransformations.weather.Weather

class VacationDetails private constructor(val weather: Weather, val flight: Flight, val hotel: Hotel) {
    companion object {
        fun build(weather: Weather, flight: Flight, hotel: Hotel): VacationDetails {
            return VacationDetails(weather, flight, hotel)
        }
    }
}
