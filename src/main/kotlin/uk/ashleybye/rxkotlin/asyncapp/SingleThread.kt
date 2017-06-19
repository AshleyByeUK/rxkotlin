package uk.ashleybye.rxkotlin.asyncapp

import org.apache.commons.io.IOUtils
import java.net.ServerSocket
import java.net.Socket

fun main(args: Array<String>) {
    val server = SingleThread()
    server.run()
}

/**
 * Simple implementation of a server using a single thread. Not concurrent at all. Returns 200 OK.
 * `serverSocket.accept()` blocks until a client establishes a connection; whilst the server is
 * handling the request, no additional requests will be received because the server is busy
 * handling the first request. These connection requests are kept in a queue, which is capped at a
 * backlog of 100. By simulating a HTTP 1.1 server, it means that connections are kept alive by
 * default. Very quickly run out of connections. Also, both the request read and response write
 * are blocking and must complete before the next client can be serviced. Barely handles one
 * concurrent connection, let alone thousands. Not very scalable!
 */
class SingleThread {
    private val PORT = 8080
    private val BACKLOG = 100
    private val RESPONSE: ByteArray = ("HTTP/1.1 200 OK\r\n" +
            "Content-length: 2\r\n" +
            "\r\n" +
            "OK").toByteArray(Charsets.UTF_8)

    fun run() {
        val serverSocket = ServerSocket(PORT, BACKLOG)
        while (!Thread.currentThread().isInterrupted) {
            val client = serverSocket.accept()
            handle(client)
        }
    }

    private fun handle(client: Socket) {
        try {
            while (!Thread.currentThread().isInterrupted) {
                readFullRequest(client)
                client.getOutputStream().write(RESPONSE)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            IOUtils.closeQuietly(client)
        }
    }

    private fun readFullRequest(client: Socket) {
        val reader = client
                .getInputStream()
                .buffered()
                .bufferedReader()

        var line: String? = reader.readLine()
        line?.let {
            while (!line!!.isEmpty()) {
                // Do nothing with request, just keep reading.
                line = reader.readLine()
            }
        }
    }
}
