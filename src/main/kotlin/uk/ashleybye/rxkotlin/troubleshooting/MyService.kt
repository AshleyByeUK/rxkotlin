package uk.ashleybye.rxkotlin.troubleshooting

import io.reactivex.Observable
import java.time.LocalDate


interface MyService {
    fun externalCall(): Observable<LocalDate>
}