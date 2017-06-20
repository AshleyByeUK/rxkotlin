//import io.reactivex.Observable
//import io.reactivex.rxkotlin.zipWith
//import java.time.LocalDate
//import java.time.Month
//import java.util.concurrent.TimeUnit
//
//fun nextSolarEclipse(after: LocalDate): Observable<LocalDate> {
//    return Observable
//            .just(
//                    LocalDate.of(2016, Month.MARCH, 9),
//                    LocalDate.of(2016, Month.SEPTEMBER, 1),
//                    LocalDate.of(2017, Month.FEBRUARY, 26),
//                    LocalDate.of(2017, Month.AUGUST, 21),
//                    LocalDate.of(2018, Month.FEBRUARY, 15),
//                    LocalDate.of(2018, Month.JULY, 13),
//                    LocalDate.of(2018, Month.AUGUST, 11),
//                    LocalDate.of(2019, Month.JANUARY, 6),
//                    LocalDate.of(2019, Month.JULY, 2),
//                    LocalDate.of(2019, Month.DECEMBER, 26)
//            )
//            .skipWhile { date ->
//                !date.isAfter(after)
//            }
//            .zipWith(
//                    Observable.interval(500, 50, TimeUnit.MILLISECONDS),
//                    { date, _ -> date }
//            )
//}
//
//fun main(args: Array<String>) {
//    nextSolarEclipse(LocalDate.now())
//            .timeout<Long, Long>(
//                    Observable.timer(400, TimeUnit.MILLISECONDS),
//                    Function { Observable.timer(40, TimeUnit.MILLISECONDS) }
//            )
//            .subscribe(
//                    { println(it) },
//                    { it.printStackTrace() },
//                    { println("Completed") }
//            )
//
//    TimeUnit.MILLISECONDS.sleep(2000)
//}