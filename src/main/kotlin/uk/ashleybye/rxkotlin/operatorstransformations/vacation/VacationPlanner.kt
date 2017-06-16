package uk.ashleybye.rxkotlin.operatorstransformations.vacation

import io.reactivex.Observable
import uk.ashleybye.rxkotlin.operatorstransformations.location.City
import java.time.LocalDate

/**
 * Specifies the contract for vacation planners.
 */
interface VacationPlanner {

    /**
     * Gets the vacation options.
     *
     * Gets the vacation options for the given criteria. Prices are in pounds sterling, but for a
     * real application these should be currency agnostic.
     *
     * @param fromCity - the city of departure
     * @param earliestDate - the earliest possible vacation start date
     * @param latestDate - the latest possible vacation start date
     * @param maxFlightCostSterling - the maximum cost of a flight in pounds sterling, default is £300
     * @param maxHotelCostSterling - the maximum cost of a hotel in pounds sterling, default is 500
     *
     * @return the vacations meeting the specified criteria
     */
    fun getOptions(
            fromCity: City,
            earliestDate: LocalDate,
            latestDate: LocalDate,
            maxFlightCostSterling: Int = 300,
            maxHotelCostSterling: Int = 500
    ): Observable<Vacation>
}