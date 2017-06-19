package uk.ashleybye.rxkotlin.asyncapp.rxclient


data class Flight(
        val id: String,
        val airline: String,
        val depart: GeoLocation,
        val arrive: GeoLocation
) : Comparable<Flight> {
    override fun compareTo(other: Flight): Int {
        return compareValuesBy(
                this,
                other,
                { it.id },
                { it.depart },
                { it.arrive },
                { it.airline }
        )
    }

}
