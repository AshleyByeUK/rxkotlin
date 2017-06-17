package uk.ashleybye.rxkotlin.multithreading

import io.reactivex.schedulers.Schedulers

fun main(args: Array<String>) {
    Logger.getSingleton()

    val trampoline = Trampoline()
    trampoline.doExample()
}

/**
 * Example using the built-in trampoline scheduler. This scheduler will schedule all tasks on the
 * same thread, thus effectively blocking. Newly added tasks will not be executed until all
 * previously scheduled tasks have completed (effectively a FIFO queue). It does, however, allow for
 * recursion without infinitely growing the call stack.
 */
class Trampoline {
    private lateinit var logger: Logger

    private val scheduler = Schedulers.trampoline()
    private val worker = scheduler.createWorker()

    init {
        // Just to make the log output numbers line up nicely!
        sleepOneSecond()
    }

    fun doExample() {
        logger = Logger.getSingleton()

        logger.log("Main start")
        worker.schedule {
            logger.log("  Outer start")
            sleepOneSecond()
            worker.schedule {
                logger.log("    Middle start")
                sleepOneSecond()
                worker.schedule {
                    logger.log("      Inner start")
                    sleepOneSecond()
                    logger.log("      Inner end")
                }
                logger.log("    Middle end")
            }
            logger.log("  Outer end")
        }
        logger.log("Main end")

        worker.dispose()
    }
}
