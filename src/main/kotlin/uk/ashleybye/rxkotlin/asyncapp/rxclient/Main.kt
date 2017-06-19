package uk.ashleybye.rxkotlin.asyncapp.rxclient

import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit


private val tickets = mutableListOf<Ticket>()

fun main(args: Array<String>) {
    val bookingService = RandomBookingService()
    val users = Data.users.toMutableList()
    users.add(User(UUID.fromString("39339c17-78c5-4fa4-a88b-0077527a9190"), "Non", "User"))

    Observable
            .fromIterable(users)
            .flatMap { (id) -> bookingService.bookRandomTicketFor(id) }
            .subscribe(
                    {
                        Logger.log("Received: for ${it.forName}")
                        tickets.add(it)
                    },
                    { it.printStackTrace() },
                    { Logger.log("Subscribe completed")}
            )

    TimeUnit.SECONDS.sleep(10)

    println("\nTickets:")
    for (ticket in tickets) {
        println("$ticket\n")
    }
}

