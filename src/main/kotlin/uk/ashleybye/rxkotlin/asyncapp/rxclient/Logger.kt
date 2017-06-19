package uk.ashleybye.rxkotlin.asyncapp.rxclient


object Logger {

    private val startTime = System.currentTimeMillis()

    fun log(message: Any) {
        println("${System.currentTimeMillis() - startTime} | " +
                "${Thread.currentThread().name} | " +
                message)
    }
}