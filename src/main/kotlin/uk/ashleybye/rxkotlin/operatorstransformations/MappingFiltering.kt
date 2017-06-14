package uk.ashleybye.rxkotlin.operatorstransformations

import io.reactivex.Observable
import java.time.DayOfWeek
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
  doExample({ MappingFiltering.exampleOne() }, "Example One: Simple Map and Filter")
  doExample({ MappingFiltering.exampleTwo() }, "Example Two: More Filtering")
  doExample({ MappingFiltering.exampleThree() }, "Example Three: Flat Map from Iterable")
  doExample({ MappingFiltering.exampleFour() }, "Example Four: Fun FlatMap")
  doExample({ MappingFiltering.exampleFive() }, "Example Five: FlatMap with Interval")
  doExample({ MappingFiltering.exampleSix() }, "Example Six: FlatMap with Interval")
  doExample({ MappingFiltering.exampleSeven() }, "FlatMap with MaxConcurrent Parameter")
}

private fun doExample(example: () -> Unit, title: String = "Example") {
  println("\n$title\n")
  example.invoke()
  println()
}

private class MappingFiltering {
  companion object {
    /**
     * Example One: Simple Map and Filter
     *
     * Filters a stream of values, maps them to a new value, applies a second filter.
     */
    fun exampleOne() {
      val observable = Observable
          .just(8, 9, 10)
          .filter { i -> i % 3 > 0 }
          .map { i -> "#${i * 10}" }
          .filter { s -> s.length < 4 }

      observable.subscribe { println(it) }
    }

    /**
     * Example Two: More Filtering
     *
     * Filters a stream of values and applies `doOnNext()` to show the effect of filtering. The
     * stream and filters are the same as for [exampleOne].
     */
    fun exampleTwo() {
      Observable
          .just(8, 9, 10)
          .doOnNext { println("A: $it") }
          .filter { i -> i % 3 > 0 }
          .doOnNext { println("B: $it") }
          .map { i -> "#${i * 10}" }
          .doOnNext { println("C: $it") }
          .filter { s -> s.length < 4 }
          .subscribe { s -> println("D: $s") }
    }

    /**
     * Example Three: Flat Map from Iterable.
     *
     * Shows how to take an `Iterable` (customer orders) and using `flatMap()` map the customer to
     * their orders.
     */
    fun exampleThree() {
      val orders1 = listOf(
          Order("Customer 1 - Order 1"),
          Order("Customer 1 - Order 2"),
          Order("Customer 1 - Order 3"))
      val orders2 = listOf(
          Order("Customer 2 - Order 1"),
          Order("Customer 2 - Order 2"),
          Order("Customer 2 - Order 3"),
          Order("Customer 2 - Order 4"),
          Order("Customer 2 - Order 5"))
      val orders3 = listOf(
          Order("Customer 3 - Order 1"))

      val customers = Observable
          .fromIterable(listOf(
              Customer(orders1),
              Customer(orders2),
              Customer(orders3)
          ))
      val orders = customers
          .flatMapIterable(Customer::orders)
      val ordersAgain = customers.flatMapIterable { it.orders }

      println("Orders:")
      orders.subscribe { println(it.name) }
      println("\nOrders Again:")
      ordersAgain.subscribe { println(it.name) }
    }

    /**
     * Example Four: Fun FlatMap
     *
     * Another `flatMap()` example.
     */
    fun exampleFour() {
      fun toMorseCode(char: Char) = when (char) {
        'a' -> Observable.just(Sound.DI, Sound.DAH)
        'b' -> Observable.just(Sound.DAH, Sound.DI, Sound.DI, Sound.DI)
        'c' -> Observable.just(Sound.DAH, Sound.DI, Sound.DAH, Sound.DI)
        'p' -> Observable.just(Sound.DI, Sound.DAH, Sound.DAH, Sound.DI)
        'r' -> Observable.just(Sound.DI, Sound.DAH, Sound.DI)
        's' -> Observable.just(Sound.DI, Sound.DI, Sound.DI)
        't' -> Observable.just(Sound.DAH)
        else -> Observable.empty()
      }

      val sparta = "Sparta!"
      print("$sparta = ")
      Observable
          .fromIterable(sparta.asIterable())
          .map(Char::toLowerCase)
          .flatMap { toMorseCode(it) }
          .subscribe(
              { print("$it ") },
              { println("Error") },
              { println() })
    }

    /**
     * Example Five: FlatMap with Interval.
     *
     * Note how `FlatMap()` does not emit values in the order they are received. When SUNDAY is
     * mapped, a new thread begins, and MONDAY is received immediately after and a second thread is
     * started to process this. Due to the different intervals, the downstream ordering is based on
     * the time period specified:
     *
     * Mon-0 Sun-0 Mon-1 Sun-1 Mon-2 Mon-3 Sun-2 Mon-4 Sun-3 Sun-4
     */
    fun exampleFive() {
      Observable
          .just(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
          .flatMap(this@Companion::loadRecordsFor)
          .subscribe(
              { print("$it ") },
              { println("Error") },
              { println() })
      TimeUnit.SECONDS.sleep(1)
    }

    /**
     * Example Six: ConcatMap with Interval.
     *
     * If downstream ordering needs to be aligned with the upstream ordering, us `concatMap()`
     * instead.
     */
    fun exampleSix() {
      Observable
          .just(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
          .concatMap(this@Companion::loadRecordsFor)
          .subscribe(
              { print("$it ") },
              { println("Error") },
              { println() })
      TimeUnit.SECONDS.sleep(1)
    }

    /**
     * Example Seven: FlatMap with MaxConcurrent Parameter.
     *
     * It can often be necessary to limit the number of events `flatMap()` process concurrently,
     * e.g. loading a large number of user profiles from a remote server. An overload allows this
     * to be achieved by specifying the maximum number of upstream events to process concurrently.
     */
    fun exampleSeven() {
      Observable
          .just(DayOfWeek.SUNDAY,
              DayOfWeek.MONDAY,
              DayOfWeek.TUESDAY,
              DayOfWeek.WEDNESDAY,
              DayOfWeek.THURSDAY,
              DayOfWeek.FRIDAY,
              DayOfWeek.SATURDAY)
          .flatMap(this@Companion::loadRecordsFor, 3)
          .subscribe(
              { print("$it ") },
              { println("Error") },
              { println() })
      TimeUnit.SECONDS.sleep(3)
    }

    private fun getObservableFor(day: String, period: Long): Observable<String> = Observable
        .interval(period, TimeUnit.MILLISECONDS) // Generates numbers from zero at intervals of period.
        .take(5)
        .map { "$day-$it" }

    private fun loadRecordsFor(day: DayOfWeek): Observable<String> = when (day) {
      DayOfWeek.SUNDAY -> getObservableFor("Sun", 90)
      DayOfWeek.MONDAY -> getObservableFor("Mon", 65)
      DayOfWeek.TUESDAY -> getObservableFor("Tue", 50)
      DayOfWeek.WEDNESDAY -> getObservableFor("Wed", 40)
      DayOfWeek.THURSDAY -> getObservableFor("Thu", 70)
      DayOfWeek.FRIDAY -> getObservableFor("Fri", 120)
      DayOfWeek.SATURDAY -> getObservableFor("Sat", 30)
    }
  }

  data class Customer(val orders: List<Order>)
  data class Order(val name: String)
  enum class Sound(val value: String) {
    DI("di"),
    DAH("dah");

    override fun toString() = value
  }
}