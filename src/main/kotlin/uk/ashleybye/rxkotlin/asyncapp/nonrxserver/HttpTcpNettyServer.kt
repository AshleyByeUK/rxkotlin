package uk.ashleybye.rxkotlin.asyncapp.nonrxserver

fun main(args: Array<String>) {
    val server = uk.ashleybye.rxkotlin.asyncapp.nonrxserver.HttpTcpNettyServer()
    server.run()
}

/**
 * A very basic HTTP server in Netty. The crucial parts are the `bossGroup` pool, which is
 * responsible for accepting incoming connections, and `workerGroup`, which processes events. The
 * pools are small: one for `bossGroup` and close to the number of CPU cores for `workerGroup`.
 *
 * Other than specify that the server listen on port 8080, configuring what the sever should do is
 * done via a `ChannelInitializer`, which is extended by [HttpInitializer].
 */
class HttpTcpNettyServer(val port: Int = 8080, val maxBacklog: Int = 50_000) {

    fun run() {
        val bossGroup: io.netty.channel.EventLoopGroup = io.netty.channel.nio.NioEventLoopGroup(1)
        val workerGroup: io.netty.channel.EventLoopGroup = io.netty.channel.nio.NioEventLoopGroup()

        try {
            io.netty.bootstrap.ServerBootstrap()
                    .option(io.netty.channel.ChannelOption.SO_BACKLOG, maxBacklog)
                    .group(bossGroup, workerGroup)
                    .channel(io.netty.channel.socket.nio.NioServerSocketChannel::class.java)
                    .childHandler(HttpInitializer())
                    .bind(port)
                    .sync()
                    .channel()
                    .closeFuture()
                    .sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}
