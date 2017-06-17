package uk.ashleybye.rxkotlin.multithreading

class Logger private constructor() {

    private var start: Long = 0

    init {
        logger = this
        start = System.currentTimeMillis()
    }

    fun log(label: Any) {
        println("${System.currentTimeMillis() - start}\t| " +
                "${Thread.currentThread().name}\t| " +
                "$label")
    }

    companion object {
        private var logger: Logger? = null

        fun getSingleton(): Logger {
            return logger ?: Logger()
        }
    }
}
