package uk.ashleybye.rxkotlin.asyncapp.rxclient

import io.reactivex.Observable

interface TravelAgency {
    fun search(user: User, location: GeoLocation): Observable<Flight>
}
