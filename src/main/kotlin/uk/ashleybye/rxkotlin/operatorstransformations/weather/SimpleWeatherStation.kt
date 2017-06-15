package uk.ashleybye.rxkotlin.operatorstransformations.weather

import io.reactivex.Observable

/**
 * A simple weather station implementation.
 *
 * Implements [WeatherStation] with a bunch of pre-determined temperature and wind readings.
 */
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
