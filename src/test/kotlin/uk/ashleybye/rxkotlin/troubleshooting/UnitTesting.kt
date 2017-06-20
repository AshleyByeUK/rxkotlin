package uk.ashleybye.rxkotlin.troubleshooting

import com.google.common.io.Files
import com.google.common.truth.Truth.assertThat
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.StringSpec
import io.reactivex.Observable
import io.reactivex.exceptions.CompositeException
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets.UTF_8

/**
 * When testing `Observable`'s, it is probably a good idea to check the following:
 *
 * - Events are emitted in correct order.
 * - Errors are properly propagated.
 * - Various operators are composed together as predicted.
 * - Events appear at right moments in time.
 * - Backpressure is supported.
 */
class UnitTesting  : StringSpec({

    "should apply concatMap in order" {
        val list: List<String> = Observable
                .range(1, 3)
                .concatMap { Observable.just(it, -it) }
                .map { it.toString() }
                .toList()
                .blockingGet()

        assertThat(list).containsExactly("1", "-1", "2", "-2", "3", "-3")
    }

    "should throw FileNotFoundException wrapped in RuntimeException when opening non-existent file" {
        val file = File("404.txt")
        val fileContents = Observable
                .fromCallable { Files.toString(file, UTF_8) }

        val exception = shouldThrow<RuntimeException> {
            fileContents.blockingFirst()
        }
        exception.cause shouldBe beInstanceOf(FileNotFoundException::class)
    }

    "See page 264 once figured out RxJava2 implementation" {
        val notifications = Observable
                .just(3, 0, 2, 0, 1, 0)
                .concatMapDelayError { Observable.fromCallable { 100 / it } }
                .materialize()

        val kinds = notifications
    }.config(enabled = false)

    "should use TestObserver" {
        val observable = Observable
                .just(3, 0, 2, 0, 1, 0)
                .concatMapDelayError { Observable.fromCallable { 100 / it } }

        val testObserver = observable.test()
        testObserver.assertValues(33, 50, 100)
//        testObserver.assertError(ArithmeticException::class.java) // Fails, actually CompositeException.
        testObserver.assertError(CompositeException::class.java)
    }
})