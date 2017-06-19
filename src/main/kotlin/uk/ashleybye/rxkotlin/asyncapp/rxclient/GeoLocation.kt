package uk.ashleybye.rxkotlin.asyncapp.rxclient


enum class GeoLocation(val location: String) {
    LONDON("London"),
    NEWYORK("New York"),
    PARIS("Paris"),
    SYDNEY("Sydney");

    override fun toString() = location
}
