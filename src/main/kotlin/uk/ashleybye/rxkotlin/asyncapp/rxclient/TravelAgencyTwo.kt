package uk.ashleybye.rxkotlin.asyncapp.rxclient

import io.reactivex.Observable


class TravelAgencyTwo(val name: String = "Second Travel Agency") : TravelAgency {

    override fun search(user: User, location: GeoLocation): Observable<Flight> {
        Logger.log("$name: Starting search for flight departing $location (for user ID: ${user.id}")

        val flights = Data.getFlights()
                .filter { flight -> flight.depart == location }

        Logger.log("$name: Exiting search for flight departing $location (for user ID: ${user.id}")

        return flights
    }
}