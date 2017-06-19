package uk.ashleybye.rxkotlin.asyncapp.rxserver

import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.util.CharsetUtil.UTF_8
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.netty.protocol.tcp.server.TcpServer

fun main(args: Array<String>) {
    val server = HttpTcpRxNettyServer()
    server.run()
}

/**
 * RxNetty implementation of 'HttpTcpNettyServer]. Note how much simpler its implementation is.
 */
class HttpTcpRxNettyServer(val port: Int = 8080) {

    private val RESPONSE = Observable
            .just("HTTP/1.1 200 OK\r\n" +
                    "Content-length: 2\r\n" +
                    "\r\n" +
                    "OK")

    // @formatter:off
    fun run() {
        TcpServer
                .newServer(port)
                .pipelineConfigurator<String, String> { pipeline ->
                    pipeline.addLast(LineBasedFrameDecoder(128))
                    pipeline.addLast(StringDecoder(UTF_8))
                }.start { connection ->
                    connection.writeStringAndFlushOnEach(
                        connection
                            .input
                            .flatMap { line ->
                                if (line.isEmpty()) {
                                    RxJavaInterop.toV1Observable(
                                            RESPONSE,
                                            BackpressureStrategy.MISSING)
                                } else {
                                    RxJavaInterop.toV1Observable(
                                            Observable.empty(),
                                            BackpressureStrategy.MISSING)
                                }
                            }

                    )
                }.awaitShutdown()
    }
    // @formatter:on
}
