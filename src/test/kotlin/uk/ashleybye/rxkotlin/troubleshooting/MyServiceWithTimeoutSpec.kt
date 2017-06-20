package uk.ashleybye.rxkotlin.troubleshooting

import io.kotlintest.specs.StringSpec
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong


class MyServiceWithTimeoutSpec : StringSpec({
    "should receive timeout after one second if externalCall() never finishes" {
        // Given.
        val testScheduler = TestScheduler()
        val mock = mockReturning(Observable.never(), testScheduler)

        // When.
        val testObserver = mock.externalCall().test()

        // Then.
        testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS)
        testObserver.assertNoTimeout()
        testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        testObserver.assertComplete()
        testObserver.assertNoValues()
    }

    "should not receive timeout prior to the specified threshold" {
        // Given.
        val testScheduler = TestScheduler()
        val slowResult = Observable
                .timer(950, TimeUnit.MILLISECONDS, testScheduler)
                .map { LocalDate.now() }
        val myService = mockReturning(slowResult, testScheduler)

        // When.
        val testObserver = myService.externalCall().test()

        // Then.
        testScheduler.advanceTimeBy(930, TimeUnit.MILLISECONDS)
        testObserver.assertNotComplete()
        testObserver.assertNoValues()
        testScheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        testObserver.assertComplete()
        testObserver.assertValueCount(1)
    }

    "should be able to control backpressure" {
        // Given.
        val atomicLong = AtomicLong(0)
        val naturals = Flowable.generate<Long> { emitter ->
            emitter.onNext(atomicLong.getAndIncrement())
        }

        // When.
        val testSubscriber = naturals.take(10).test(0)

        // Then.
        testSubscriber.assertNoValues()
        testSubscriber.requestMore(100)
        testSubscriber.assertValueCount(10)
        testSubscriber.assertComplete()
    }
})

private fun mockReturning(result: Observable<LocalDate>, testScheduler: TestScheduler): MyServiceWithTimeout {
    val mock = Mockito.mock(MyService::class.java)
    given(mock.externalCall()).willReturn(result)

    return MyServiceWithTimeout(mock, testScheduler)
}