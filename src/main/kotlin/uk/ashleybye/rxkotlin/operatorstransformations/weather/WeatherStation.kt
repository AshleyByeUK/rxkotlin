package uk.ashleybye.rxkotlin.operatorstransformations.weather

import io.reactivex.Observable

/**
 * Specifies the contract for weather stations.
 */
interface WeatherStation {

    /**
     * Get the observed [Temperature]'s at this location.
     *
     * @return the temperatures
     */
    fun temperature(): Observable<Temperature>

    /**
     * Get the observed [Wind]'s at this location.
     *
     * @return thw winds
     */
    fun wind(): Observable<Wind>
}