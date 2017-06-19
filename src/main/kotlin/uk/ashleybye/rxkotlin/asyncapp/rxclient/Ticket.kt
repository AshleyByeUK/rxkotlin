package uk.ashleybye.rxkotlin.asyncapp.rxclient


data class Ticket(
        val forName: String,
        val airline: String,
        val flightId: String,
        val departing: GeoLocation,
        val arriving: GeoLocation
) {
    override fun toString() = "$forName\n" +
            "$airline\n" +
            "Flight #$flightId\n" +
            "Departing: $departing\n" +
            "Arriving: $arriving"
}