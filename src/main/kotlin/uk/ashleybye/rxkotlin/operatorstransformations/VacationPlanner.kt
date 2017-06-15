package uk.ashleybye.rxkotlin.operatorstransformations

import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.zipWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

interface VacationPlanner {
    fun getOptions(
            fromCity: City,
            earliestDate: LocalDate,
            latestDate: LocalDate,
            maxFlightCostSterling: Int = 300,
            maxHotelCostSterling: Int = 500
    ): Observable<Vacation>
}

interface WeatherStation {
    fun temperature(): Observable<Temperature>
    fun wind(): Observable<Wind>
}

enum class City(val text: String) {
    LONDON("London"),
    NEWYORK("New York"),
    PARIS("Paris"),
    WARSAW("Warsaw");

    override fun toString() = text
}

data class Temperature(val degreesCelsius: Double)

data class Wind(val speedKnots: Int)

data class Weather(val temperature: Temperature, val wind: Wind) {
    companion object {
        private val random = Random()
    }

    val isSunny = random.nextBoolean()

    override fun toString() = "Temp: ${temperature.degreesCelsius} Celsius, " +
            "Wind: ${wind.speedKnots}, " +
            "Sunny: ${if (isSunny) "Yes" else "No"}"
}

data class Flight(private val from: City, private val to: City) {
    companion object {
        private val random = Random()
    }

    val cost = Math.round(random.nextFloat() * 300)

    override fun toString() = "$from to $to: £$cost"
}

data class Hotel(private val title: String, private val city: City) {
    companion object {
        private val random = Random()
        val partialNames = listOf("Regency", "Continental", "Ritz", "Hilton", "Travel Lodge",
                "Easy Rest", "Emporium", "World View", "Sweet Escape", "Dingy", "Paradise",
                "Nasty", "Drury", "Holiday Inn", "Marriott", "Shangri-La", "Millennium",
                "Dorchester", "Ritz-Carlton", "Premier Inn")
    }

    val name = "$city $title Hotel"
    val cost = Math.round(random.nextFloat() * 500)

    override fun toString() = "$name: £$cost"
}

class SimpleWeatherStation : WeatherStation {
    override fun temperature(): Observable<Temperature> {
        return Observable
                .range(1, 10)
                .map { i -> (20 + i).toDouble() }
                .map { temp -> Temperature(temp) }
    }

    override fun wind(): Observable<Wind> {
        return Observable
                .range(1, 10)
                .map { i -> 5 + i }
                .map { speed -> Wind(speed) }
    }

}

class Vacation private constructor(val toCity: City, val onDate: LocalDate, val details: VacationDetails) {
    companion object {
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

class VacationDetails private constructor(val weather: Weather, val flight: Flight, val hotel: Hotel) {
    companion object {
        fun build(weather: Weather, flight: Flight, hotel: Hotel): VacationDetails {
            return VacationDetails(weather, flight, hotel)
        }
    }
}

class SimpleVacationPlanner : VacationPlanner {
    companion object {
        val random = Random()
    }

    private val weatherStation: WeatherStation = SimpleWeatherStation()

    override fun getOptions(
            fromCity: City,
            earliestDate: LocalDate,
            latestDate: LocalDate,
            maxFlightCostSterling: Int,
            maxHotelCostSterling: Int): Observable<Vacation> {
        val maxDays = ChronoUnit.DAYS.between(earliestDate, latestDate).toInt()
        val vacationDays = Observable
                .range(1, maxDays)
                .map { numDays -> earliestDate.plusDays(numDays.toLong()) }

        val possibleVacations = Observable
                .fromIterable(City.values().toList())
                .filter { it != fromCity }
                .flatMap { city ->
                    vacationDays.map { date ->
                        Pair(city, date)
                    }
                }
                .flatMap { option ->
                    Observables.zip(
                            weather().filter { it.isSunny },
                            cheapFlight(fromCity, option.first, maxFlightCostSterling),
                            cheapHotel(option.first, maxHotelCostSterling),
                            { weather, flight, hotel ->
                                Vacation.build(
                                        option.first,
                                        option.second,
                                        weather,
                                        flight,
                                        hotel)
                            }
                    )
                }

        return possibleVacations
    }

    private fun weather(): Observable<Weather> = weatherStation.temperature()
            .zipWith(
                    weatherStation.wind(),
                    { temp, wind ->
                        Weather(temp, wind)
                    })

    private fun cheapFlight(fromCity: City, toCity: City, maxCostSterling: Int): Observable<Flight> {
        return Observable
                .range(1, Math.round(random.nextFloat() * 10))
                .map { _ -> Flight(fromCity, toCity) }
                .filter { it.cost <= maxCostSterling }
    }

    private fun cheapHotel(inCity: City, maxCostSterling: Int): Observable<Hotel> {
        return Observable
                .range(1, Math.round(random.nextFloat() * Hotel.partialNames.size))
                .map { i -> Hotel(Hotel.partialNames[i], inCity) }
                .filter { it.cost <= maxCostSterling }
    }
}
