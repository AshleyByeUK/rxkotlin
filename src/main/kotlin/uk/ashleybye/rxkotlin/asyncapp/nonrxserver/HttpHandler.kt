package uk.ashleybye.rxkotlin.asyncapp.nonrxserver

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.util.CharsetUtil.UTF_8

/**
 * The business logic of the server. Construct the response and write back a [DefaultFullHttpResponse].
 * `writeAndFlush()` does not block but returns a `ChannelFuture` that is subscribed to and closed
 * asynchronously.
 */
@ChannelHandler.Sharable
class HttpHandler : ChannelInboundHandlerAdapter() {

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is HttpRequest) sendResponse(ctx)
    }

    private fun sendResponse(ctx: ChannelHandlerContext?) {
        val response: DefaultFullHttpResponse = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer("OK".toByteArray(UTF_8))
        )
        response.headers().add("Content-length", 2)
        ctx?.writeAndFlush(response)
                ?.addListener(ChannelFutureListener.CLOSE)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        println("Error: $cause")
        ctx?.close()
    }
}
