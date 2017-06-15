package uk.ashleybye.rxkotlin.operatorstransformations.vacation

import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.zipWith
import uk.ashleybye.rxkotlin.operatorstransformations.location.City
import uk.ashleybye.rxkotlin.operatorstransformations.weather.SimpleWeatherStation
import uk.ashleybye.rxkotlin.operatorstransformations.weather.Weather
import uk.ashleybye.rxkotlin.operatorstransformations.weather.WeatherStation
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * A simple vacation planner implementation.
 *
 * Implements [VacationPlanner], using generated values for [Weather], [Flight]'s and [Hotel]'s. The
 * main functionality resides in [getOptions].
 */
class SimpleVacationPlanner : VacationPlanner {
    companion object {
        val random = Random()
    }

    private val weatherStation: WeatherStation = SimpleWeatherStation()

    /**
     * Gets the vacation options.
     *
     * Gets the vacation options for the given criteria. Prices are in pounds sterling, but for a
     * real application these should be currency agnostic.
     *
     * First, calculates the difference between the earliest and latest start dates. This is used
     * as the range to value when generating an observable which emits all the possible vacation
     * days within the client's specified range. Next, generate an observable which emits all the
     * possible cities excluding the city of departure:
     * `.fromIterable(City.values().toList()).filter { it != fromCity }`. Then using `flatMap()`,
     * map each city to each of the possible vacation dates. 'flatMap()` is again used to map
     * each city/date option to a zipped observable, which is the real meat of this method and the
     * point of the entire example.
     *
     * Remember that zip assumes each event emitted by the upstream observable is related to each of
     * the events emitted by each of the other upstream observables in the zip operation. As such,
     * three streams are zipped, each emitting zero or one event. Provided all three emit an event,
     * the result is used to build the vacation and emit it downstream. If one of the streams does
     * not emit an event, the others will be discarded and the next event processed. If one of the
     * streams completes, this is propagated downstream and all others complete too. Although this
     * example uses the events emitted from each stream to build the [Vacation] object, an
     * alternate implementation may completely disregard each of these results. in Kotlin, `_` can
     * be used to indicate that a lambda input value is not used, e.g.: `{ _, _, _ -> ::Vacation }`.
     *
     * @param fromCity - the city of departure
     * @param earliestDate - the earliest possible vacation start date
     * @param latestDate - the latest possible vacation start date
     * @param maxFlightCostSterling - the maximum cost of a flight in pounds sterling, default is £300
     * @param maxHotelCostSterling - the maximum cost of a hotel in pounds sterling, default is 500
     *
     * @return the vacations meeting the specified criteria
     */
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

    /**
     * Gets an observable of [Weather].
     *
     * Gets an observable of [Weather] information. This example method zips together [Temperature]
     * and [Wind], based on the information provided by [WeatherStation] which here is implemented
     * by [SimpleWeatherStation]. These are randomly made up.
     *
     * In a real application, there would be a proper weather data feed to provide this information,
     * and which could be filtered according to the client's needs.
     *
     * @return the weather
     */
    private fun weather(): Observable<Weather> = weatherStation.temperature()
            .zipWith(
                    weatherStation.wind(),
                    { temp, wind ->
                        Weather(temp, wind)
                    })

    /**
     * Gets an observable of [Flight].
     *
     * Gets an observable of [Flight]'s that cost no more than maxCostSterling. Because this is just an
     * example, the flights between cities are randomly generated as is the number of flights
     * between each of the cities.
     *
     * For a real application, there would be a flights observable which can be filtered based on
     * departure location, arrival location and then the cost. It's be sensible to make
     * `maxCostSterling` currency agnostic too.
     *
     * @param fromCity - thi city of departure
     * @param toCity - the city of arrival
     * @param maxCostSterling - the maximum cost of the flight
     *
     * @return the flights matching the specified criteria
     */
    private fun cheapFlight(fromCity: City, toCity: City, maxCostSterling: Int): Observable<Flight> {
        return Observable
                .range(1, Math.round(random.nextFloat() * 10))
                .map { _ -> Flight(fromCity, toCity) }
                .filter { it.cost <= maxCostSterling }
    }

    /**
     * Gets an observable of [Hotel].
     *
     * Gets an observable of [Hotel]'s that cost no more than maxCostSterling. Because this is just an
     * example, the names of hotels are randomly generated based on the city name and a partial
     * name from [Hotel]. So that the same hotels are not always available, the number of hotels
     * available is further restricted by randomly choosing a range between one and
     * `Hotel.partialNames.size`.
     *
     * For a real application, there would be a hotels observable which can then be filtered based
     * on [City] then `maxCostSterling`. It'd also be sensible to make it currency agnostic.
     *
     * @param inCity - the city the hotel will be in
     * @param maxCostSterling - the maximum cost of the hotel
     *
     * @return the hotels matching the specified criteria
     */
    private fun cheapHotel(inCity: City, maxCostSterling: Int): Observable<Hotel> {
        return Observable
                .range(1, Math.round(random.nextFloat() * Hotel.partialNames.size))
                .map { i -> Hotel(Hotel.partialNames[i], inCity) }
                .filter { it.cost <= maxCostSterling }
    }
}
