package uk.ashleybye.rxkotlin.asyncapp.rxserver

import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.netty.protocol.http.server.HttpServer
import java.math.BigDecimal

fun main(args: Array<String>) {
    val server = RestCurrencyServer()
    server.run()
}

/**
 * A simple REST server for currency conversion. Try it: `curl -v localhost:8080/10.99`.
 */
class RestCurrencyServer(val port: Int = 8080) {
    private val RATE = BigDecimal(1.06448)

    fun run() {
        HttpServer
                .newServer(port)
                .start { request, response ->
                    val amountStr = request.decodedPath.substring(1)
                    val amount = BigDecimal(amountStr)
                    response.writeString(
                            RxJavaInterop.toV1Observable(
                                    Observable
                                            .just(amount)
                                            .map { eur -> eur.multiply(RATE) }
                                            .map { usd -> "{\"EUR\": $amount, \"USD\": $usd}" },
                                    BackpressureStrategy.MISSING
                            )
                    )
                }.awaitShutdown()
    }
}
