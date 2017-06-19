package uk.ashleybye.rxkotlin.asyncapp.nonrxserver

import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec

/**
 * Rather than use a single function to handle the connection, build a pipeline that processes
 * incoming `ByteBuf` instances as they arrive. The first step decodes these raw bytes into
 * higher-level HTTP request objects with [HttpServerCodec]; this handler is stateful and is also
 * used for encoding HTTP response objects. The second step, the [HttpHandler], contains the
 * business logic and can be a stateless singleton.
 */
class HttpInitializer : io.netty.channel.ChannelInitializer<SocketChannel>() {

    private val httpHandler = HttpHandler()

    override fun initChannel(ch: io.netty.channel.socket.SocketChannel) {
        ch.pipeline()
                .addLast(io.netty.handler.codec.http.HttpServerCodec())
                .addLast(httpHandler)
    }
}
