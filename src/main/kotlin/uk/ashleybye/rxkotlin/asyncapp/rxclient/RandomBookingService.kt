package uk.ashleybye.rxkotlin.asyncapp.rxclient

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import java.util.*
import java.util.concurrent.TimeUnit


class RandomBookingService : BookingService {

    override fun findById(id: UUID): Maybe<User> {
        return Data
                .getUsers()
                .filter { (userId) -> userId == id }
                .singleElement()
                .doOnSubscribe { Logger.log("Users: start find ID $id") }
                .doOnSuccess { Logger.log("Users: exit find ID $id") }
    }

    override fun locate(): Observable<GeoLocation> {
        val GEOLOCATION_DELAY: Long = 3000
        val random = Random()

        val locations = GeoLocation.values()
        val randomLocation = locations[random.nextInt(locations.size)]

        return Observable
                .just(randomLocation)
                .delay(GEOLOCATION_DELAY, TimeUnit.MILLISECONDS)
                .doOnSubscribe { Logger.log("GeoLocating: start") }
                .doOnComplete { Logger.log("GeoLocating: Finish") }
    }

    override fun book(user: Observable<User>, flight: Observable<Flight>): Observable<Ticket> {
        return flight
                .zipWith(user,
                        { (id, airline, depart, arrive), (_, firstName, lastName) ->
                            Ticket("$firstName $lastName",
                                    airline,
                                    id,
                                    depart,
                                    arrive)
                        })
                .doOnSubscribe { Logger.log("Book: start") }
                .doOnComplete { Logger.log("Book: exit") }
    }

    fun bookRandomTicketFor(userId: UUID): Observable<Ticket> {
        val agencies = travelAgents()
        val user = findById(userId)
        val location = locate()

        return Observable
                .just(user.toObservable())
                .flatMap { usr ->
                    try {
                        usr.zipWith(location, { aUser, location ->
                            agencies
                                    .flatMap { agency ->
                                        agency
                                                .search(aUser, location) // Observable<Flight>.
                                                .toList() // Convert to List<Flight>.
                                                .toObservable() // And to Observable<List<Flight>>.
                                                .flatMap { flights -> // So it can be shuffled,
                                                    Observable.just( // giving a random order.
                                                            shuffle(flights as MutableList<Flight>)[0]
                                                    )
                                                }
                                    }.firstElement() // Now take the first randomly shuffled Flight.
                        }).flatMap { flight ->
                            book(user.toObservable(), flight.toObservable())
                        }
                    } catch (ex: Exception) {
                        Observable.empty<Ticket>()
                    }
                }
                .doOnSubscribe { Logger.log("Random ticket: start for $userId") }
                .doOnComplete { Logger.log("Random ticket: exit for $userId") }
    }

    private fun <T : Comparable<T>> shuffle(items: MutableList<T>): List<T> {
        val random = Random()
        for (i in 0..items.size - 1) {
            val randomPosition = random.nextInt(items.size)
            val temp = items[i]
            items[i] = items[randomPosition]
            items[randomPosition] = temp
        }

        return items
    }

    private fun travelAgents(): Observable<TravelAgency> {
        return Observable
                .just(
                        TravelAgencyOne(),
                        TravelAgencyTwo(),
                        TravelAgencyThree()
                )
    }
}