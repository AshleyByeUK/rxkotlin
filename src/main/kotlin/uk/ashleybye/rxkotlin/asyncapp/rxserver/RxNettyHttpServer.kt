package uk.ashleybye.rxkotlin.asyncapp.rxserver

import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.netty.handler.codec.http.HttpHeaderNames
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.netty.protocol.http.server.HttpServer


fun main(args: Array<String>) {
    val server = RxNettyHttpServer()
    server.run()
}

/**
 * Note how easy it is to setup a simple HTTP server that does nothing but emit 200 OK responses.
 */
class RxNettyHttpServer(val port: Int = 8080) {
    private val RESPONSE_OK = Observable.just("OK")

    fun run() {
        HttpServer
                .newServer(port)
                .start { _, response ->
                    response
                            .setHeader(HttpHeaderNames.CONTENT_LENGTH, 2)
                            .writeStringAndFlushOnEach(
                                    RxJavaInterop.toV1Observable(
                                            RESPONSE_OK,
                                            BackpressureStrategy.MISSING))
                }.awaitShutdown()
    }
}
