package uk.ashleybye.rxkotlin.operatorstransformations.location

/**
 * Enum representing city locations.
 */
enum class City(val text: String) {
    LONDON("London"),
    NEWYORK("New York"),
    PARIS("Paris"),
    WARSAW("Warsaw");

    /**
     * Get the human readable string representation.
     *
     * Get the human readable string representation of the city.
     *
     * @return the string representation
     */
    override fun toString() = text
}
