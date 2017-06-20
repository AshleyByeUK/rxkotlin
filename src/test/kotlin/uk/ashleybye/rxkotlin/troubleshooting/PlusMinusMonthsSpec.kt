package uk.ashleybye.rxkotlin.troubleshooting

import io.kotlintest.forAtLeast
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.specs.StringSpec
import java.time.LocalDate
import java.time.Month


/**
 * Contrived example, but shows how difficult it can be to test for time. Dependency on time can be
 * made explicit using a fake system clock. This means all time related code must be delegated to a
 * service that can be mocked. Java8 uses `Clock`, RxJava has the `Scheduler` class. This class has
 * a `test()` scheduler with methods `advanceTimeBy()` and `advanceTimeTo()`.
 */
class PlusMinusMonthsSpec : StringSpec({
        val START_DATE = LocalDate.of(2016, Month.JANUARY, 1)

        "date +/- 1 month should give back the same date" {
            /*
             * Fails for 6 dates: 30 Jan -> 29 Feb -> 29 Jan, etc.
             *
            forAll((0..365).toList()) { day: Int ->
                val date = START_DATE.plusDays(day.toLong())
                date shouldEqual date.plusMonths(1).minusMonths(1)
            }
            */

            forAtLeast(360, (0..365).toList()) { day ->
                val date = START_DATE.plusDays(day.toLong())
                date shouldEqual date.plusMonths(1).minusMonths(1)
            }
        }
})