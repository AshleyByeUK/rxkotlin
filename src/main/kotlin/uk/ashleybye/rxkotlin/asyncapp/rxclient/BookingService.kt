package uk.ashleybye.rxkotlin.asyncapp.rxclient

import io.reactivex.Maybe
import io.reactivex.Observable
import java.util.*


interface BookingService {
    fun findById(id: UUID): Maybe<User>
    fun locate(): Observable<GeoLocation>
    fun book(user: Observable<User>, flight: Observable<Flight>): Observable<Ticket>
}