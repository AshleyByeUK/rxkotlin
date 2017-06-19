package uk.ashleybye.rxkotlin.asyncapp.rxserver

import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.util.CharsetUtil.UTF_8
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.netty.protocol.tcp.server.TcpServer
import java.math.BigDecimal
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    val server = EurUsdCurrencyTcpServer()
    server.run()
}

/**
 * Simple TCP server which can be accessed by `telnet localhost 8080`. Type a number and get a
 * response. Type several numbers in quick succession to see the asynchronous request/response
 * handling at work.
 *
 * Server is configured to listen on port 8080 by default. Next, the pipeline must be set up. This
 * is done by first adding a handler to convert `ByteBuf` sequences into lines, and second each line
 * into a string. For each connection, we write and flush the output, which is an `Observable`. To
 * compute the response, we are notified asynchronously whenever a new line of input is received,
 * which is of type `Observable<String>`. This is then mapped to a `BigDecimal` and converted to
 * USD based on our fixed conversion rate. Note that it is necessary to convert the result of the
 * currency conversion to an RxJava 1.x Observable due to RxNetty not currently being compatible
 * with RxJava 2.x.
 */
class EurUsdCurrencyTcpServer(val port: Int = 8080, val maxFrameLength: Int = 1024) {

    private val RATE = BigDecimal("1.06448")

    // @formatter:off
    fun run() {
        TcpServer
                .newServer(port)
                .pipelineConfigurator<String, String> { pipeline ->
                    pipeline.addLast(LineBasedFrameDecoder(maxFrameLength))
                    pipeline.addLast(StringDecoder(UTF_8))
                }.start { connection ->
                    connection.writeAndFlushOnEach(
                        connection
                            .input
                            .map { BigDecimal(it) }
                            .flatMap { eur ->
                                RxJavaInterop.toV1Observable( // RxNetty not compatible Rx 2.x
                                        eurToUsd(eur),
                                        BackpressureStrategy.MISSING)
                            }
                    )
                }.awaitShutdown()
    }
    // @formatter:on

    private fun eurToUsd(eur: BigDecimal) = Observable
            .just(eur.multiply(RATE))
            .map { amount -> "$eur EUR is $amount USD\n" }
            .delay(1, TimeUnit.SECONDS)
}
