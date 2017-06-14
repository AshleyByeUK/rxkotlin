package uk.ashleybye.rxkotlin.rxextensions

import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
  doExample({ CreatingObservables.exampleOne() }, "Example One: Single Subscriber")
  doExample({ CreatingObservables.exampleTwo() }, "Example Two: Single Subscriber - Custom Observable")
  doExample({ CreatingObservables.exampleThree() }, "Example Three: Multiple Subscribers")
  doExample({ CreatingObservables.exampleFour() }, "Example Four: Infinite Stream")
  doExample({ CreatingObservables.exampleFive() }, "Example Five: Interrupting a Thread")
  doExample({ CreatingObservables.exampleSix() }, "Example Six: Handling Errors - fromCallable()")
  doExample({ CreatingObservables.exampleSeven() }, "Example Seven: Timer and Interval")

  doExample({ CreatingObservables.exerciseOne() }, "Exercise One")
}

private fun doExample(example: () -> Unit, title: String = "Example") {
  println("\n$title\n")
  example.invoke()
  println()
}

/**
 * Examples of how to create `Observable` objects. It should be noted that the explicit use of
 * threads inside `create()` is discouraged. Explicitly using threads makes it possible for
 * observers to receive notifications concurrently, which the Rx Design Guidelines prohibit.
 * By assuming that observers will only process notifications one at a time allows them to be
 * written synchronously, accessed by no more than one thread, even if events originate from
 * multiple threads. The `serialize()` operator can be applied to enforce serialisation and
 * synchronisation of events that originate from multiple threads concurrently.
 */
private object CreatingObservables {

  /**
   * Example One: Single Subscriber.
   *
   * All messages are printed by the main client thread. Subscription also happens in the main
   * client thread. subscribe() blocks the client thread until all events are received.
   */
  fun exampleOne() {
    log("Before")
    Observable
        .range(5, 3)
        .subscribe { log(it) }
    log("After")
  }

  /**
   * Example Two: Single Subscriber - Custom Observable.
   *
   * As with [exampleOne], all messages are printed by the main client thread. However, this time
   * the example shows how the observable does not start emitting until subscribe is called. Note
   * that internally, the lambda expression receiving emitted items `log("Element: $it")` is
   * wrapped with `Subscriber<Int>` internally, which is passed to `create()`.
   */
  fun exampleTwo() {
    val ints = Observable.create(ObservableOnSubscribe<Int> { emitter ->
      log("Create")
      emitter.onNext(5)
      emitter.onNext(6)
      emitter.onNext(7)
      emitter.onComplete()
      log("Completed")
    })

    log("Starting")
    ints.subscribe { log("Element: $it") }
    log("Exit")
  }

  /**
   * Example Three: Multiple Subscribers.
   *
   * Every time `subscribe()` is called, the subscription handler inside `create()` is invoked.
   * Not great if there is some heavyweight computation inside `create()`, sharing of a single
   * invocation amongst subscribers can be beneficial. This can be achieved using the `cache()`
   * operator. Note that with infinite streams, `cache()` can result in an OutOfMemoryError.
   */
  fun exampleThree() {
    val intsNotCached = Observable.create<Int> { emitter ->
      log("Create")
      emitter.onNext(42)
      emitter.onComplete()
    }

    // Use of the cache() operator feeds cached values for subsequent subscribers.
    val intsCached = intsNotCached.cache()

    log("Starting - Not Cached")
    intsNotCached.subscribe { log("Subscriber A: $it") }
    intsNotCached.subscribe { log("Subscriber B: $it") }
    log("Exit - Not Cached")

    log("Starting - Cached")
    intsCached.subscribe { log("Subscriber A: $it") }
    intsCached.subscribe { log("Subscriber B: $it") }
    log("Exit - Cached")
  }

  /**
   * Example Four: Infinite Streams.
   *
   * The concept of an infinite stream can be thought of as a queue with an infinite source of
   * values. Proper implementation of an infinite stream requires a means of knowing whether
   * values should continue to be emitted for a given subscriber. The `subscribe()` method will
   * block the calling thread infinitely, so a custom thread should be initialised for each
   * subscriber (use RxKotlin to interact with threads declaratively, although this example uses
   * explicit concurrency).
   */
  fun exampleFour() {
    val naturalNumbers = Observable.create<BigInteger> { emitter ->
      val thread = Thread({
        var int: BigInteger = BigInteger.ZERO
        while (!emitter.isDisposed) {
          emitter.onNext(int)
          int = int.add(BigInteger.ONE)
        }
      })
      thread.name = "ExampleFourThread"
      thread.start()
    }

    val first = naturalNumbers.subscribe { log("First: $it") }
    val second = naturalNumbers.subscribe { log("Second: $it") }

    Thread.sleep(5)
    first.dispose()
    Thread.sleep(5)
    second.dispose()
  }

  /**
   * Example Five: Interrupting a Thread.
   *
   * When a thread is interrupted, an `InterruptedException` is thrown inside `sleep()`. The
   * thread stops immediately and `emitter.isDisposed == false`.
   */
  fun exampleFive() {
    fun sleep(timeout: Long, unit: TimeUnit) {
      try {
        unit.sleep(timeout)
      } catch (ignore: InterruptedException) {
        // Ignored.
      }
    }

    fun <T> delayed(t: T): Observable<T> = Observable.create { emitter ->
      val runnable = Runnable {
        sleep(10, TimeUnit.SECONDS)
        if (!emitter.isDisposed) {
          emitter.onNext(t)
          emitter.onComplete()
        }
      }
      val thread = Thread(runnable)
      thread.name = "ExampleFiveThread"
      thread.start()
      emitter.setCancellable { thread::interrupt }
    }

    log("Starting")
    delayed(42).subscribe { log(it) }
    log("Exit")
  }

  /**
   * Example Six: `Observable.fromCallable()`.
   *
   * Errors need to be caught within the observable and should be emitted to all subscribers. The
   * `fromCallable()` method below is equivalent to the `create()` method.
   */
  fun exampleSix() {
    val observable = Observable.create<Int> { emitter ->
      try {
        emitter.onNext(Random().nextInt())
        emitter.onComplete()
      } catch (e: Exception) {
        emitter.onError(e)
      }
    }
    val equivalent = Observable.fromCallable<Int> { Random().nextInt() }

    log("Starting - Observable")
    observable.subscribe { log(it) }
    log("Exit - Observable")

    log("Starting - Equivalent")
    equivalent.subscribe { log(it) }
    log("Exit - Equivalent")
  }

  /**
   * Example Seven: Timer and Interval.
   *
   * Both `timer` anf `interval` create an `Observable` that uses threads underneath. Timer
   * emits a Long value of zero after a specified delay then completes. Interval produces a
   * sequence of Long, starting with zero, and places a fixed delay before each event including
   * the first zero.
   */
  fun exampleSeven() {
    log("Starting - Timer")
    Observable
        .timer(1, TimeUnit.SECONDS)
        .subscribe { log(it.toString()) }
    log("Exit - Timer")

    log("Starting - Interval")
    val interval = Observable
        .interval(1_000_000 / 60, TimeUnit.MICROSECONDS)
        .subscribe { log(it.toString()) }
    Thread.sleep(150)
    interval.dispose()
    log("Exit - Interval")
  }

  /**
   * First exercise.
   *
   * Using just `create()`, implement methods:
   *
   * just(item) - emits a single value `item` to an observer and then completes immediately.
   * never() - never emits to an observer.
   * empty() - emits no items to an observer and then completes immediately.
   * range(start, count) - emits a range to an observer and then completes immediately.
   */
  fun exerciseOne() {
    fun <T> just(item: T): Observable<T> = Observable.create { emitter ->
      emitter.onNext(item)
      emitter.onComplete()
    }

    fun <T> never(): Observable<T> = Observable.create { }

    fun <T> empty(): Observable<T> = Observable.create { subscriber ->
      subscriber.onComplete()
    }

    fun range(start: Int, count: Int): Observable<Int> = Observable.create { emitter ->
      val ints = IntRange(start, start + count - 1).asSequence()
      for (int in ints) {
        emitter.onNext(int)
      }
      emitter.onComplete()
    }

    just(10).subscribe { log("just(10): $it") }
    never<Int>().subscribe { log("never(): $it") }
    empty<Int>().subscribe { log("empty(): $it") }
    range(10, 5).subscribe { log("range(10, 5): $it") }
  }

  /**
   * Helper method.
   *
   * Prints the current thread and a message to the standard output stream.
   *
   * @param msg - the message
   */
  private fun log(msg: Any) = println("${Thread.currentThread().name}: $msg")
}
