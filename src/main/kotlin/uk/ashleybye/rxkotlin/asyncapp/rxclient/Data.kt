package uk.ashleybye.rxkotlin.asyncapp.rxclient

import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit


object Data {

    private val MAX_LATENCY = 2000

    val users = listOf(
            User(UUID.fromString("9c1ae1d2-e0fb-4234-9248-21ca4949cb0c"), "Bob", "Jones"),
            User(UUID.fromString("e49b6eef-fab0-4391-8238-6cfb2873d60c"), "Sally", "Jones"),
            User(UUID.fromString("c1647d83-ddf2-4cf0-a5ca-ac7bb477e69c"), "Jane", "Doe"),
            User(UUID.fromString("f08df86c-b2ac-4616-806b-d0d8ea7ee43c"), "Alice", "Smith"),
            User(UUID.fromString("39339c17-78c5-4fa4-a88b-0077527a9195"), "Dave", "Walker")
    )

    private val flights = listOf(
            Flight("BA111", "British Airways", GeoLocation.LONDON, GeoLocation.NEWYORK),
            Flight("BA199", "British Airways", GeoLocation.NEWYORK, GeoLocation.LONDON),
            Flight("BA222", "British Airways", GeoLocation.LONDON, GeoLocation.PARIS),
            Flight("BA299", "British Airways", GeoLocation.PARIS, GeoLocation.LONDON),
            Flight("BA333", "British Airways", GeoLocation.LONDON, GeoLocation.SYDNEY),
            Flight("BA399", "British Airways", GeoLocation.SYDNEY, GeoLocation.LONDON),
            Flight("FR987", "Air France", GeoLocation.PARIS, GeoLocation.LONDON),
            Flight("FR789", "Air France", GeoLocation.LONDON, GeoLocation.PARIS),
            Flight("FR654", "Air France", GeoLocation.PARIS, GeoLocation.NEWYORK),
            Flight("FR456", "Air France", GeoLocation.NEWYORK, GeoLocation.PARIS),
            Flight("FR321", "Air France", GeoLocation.PARIS, GeoLocation.SYDNEY),
            Flight("FR123", "Air France", GeoLocation.SYDNEY, GeoLocation.PARIS),
            Flight("UA999", "United Airlines", GeoLocation.NEWYORK, GeoLocation.LONDON),
            Flight("UA666", "United Airlines", GeoLocation.LONDON, GeoLocation.NEWYORK),
            Flight("UA888", "United Airlines", GeoLocation.NEWYORK, GeoLocation.PARIS),
            Flight("UA555", "United Airlines", GeoLocation.PARIS, GeoLocation.NEWYORK),
            Flight("UA777", "United Airlines", GeoLocation.NEWYORK, GeoLocation.SYDNEY),
            Flight("UA444", "United Airlines", GeoLocation.SYDNEY, GeoLocation.NEWYORK),
            Flight("QA147", "Qantas", GeoLocation.SYDNEY, GeoLocation.LONDON),
            Flight("QA741", "Qantas", GeoLocation.LONDON, GeoLocation.SYDNEY),
            Flight("QA258", "Qantas", GeoLocation.SYDNEY, GeoLocation.NEWYORK),
            Flight("QA852", "Qantas", GeoLocation.NEWYORK, GeoLocation.SYDNEY),
            Flight("QA369", "Qantas", GeoLocation.SYDNEY, GeoLocation.PARIS),
            Flight("QA963", "Qantas", GeoLocation.PARIS, GeoLocation.SYDNEY)
    )

    private val random = Random()

    fun getUsers(): Observable<User> {
        Logger.log("Users: starting simulated database request")

        val users = Observable
                .fromIterable(users)
                .delay(random.nextInt(MAX_LATENCY).toLong(), TimeUnit.MILLISECONDS)
                .doOnSubscribe { Logger.log("Users: start simulated database access") }
                .doOnComplete { Logger.log("Users: end simulated database transmission") }

        Logger.log("Users: exiting simulated database request")

        return users
    }

    fun getFlights(): Observable<Flight> {
        Logger.log("Flights: starting simulated database request")

        val flights = Observable
                .fromIterable(flights)
                .delay(random.nextInt(MAX_LATENCY).toLong(), TimeUnit.MILLISECONDS)
                .doOnSubscribe { Logger.log("Flights: start simulated database access") }
                .doOnComplete { Logger.log("Flights: end simulated database transmission") }

        Logger.log("Flights: exiting simulated database request")

        return flights
    }
}