package uk.ashleybye.rxkotlin.troubleshooting

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import java.time.LocalDate
import java.util.concurrent.TimeUnit


class MyServiceWithTimeout(
        private val delegate: MyService,
        private val scheduler: Scheduler
) : MyService {

    override fun externalCall(): Observable<LocalDate> {
        return delegate
                .externalCall()
                .timeout(1, TimeUnit.SECONDS, scheduler, Observable.empty())
    }
}