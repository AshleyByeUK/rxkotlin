package uk.ashleybye.rxkotlin.asyncapp

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import org.apache.commons.io.IOUtils

fun main(args: Array<String>) {
    Help.run()
}

object Help {

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

    fun handle(client: Socket) {
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
//        val reader = BufferedReader(
//                InputStreamReader(client.getInputStream()))
        val reader = client.getInputStream().buffered().bufferedReader()
        var line: String? = reader.readLine()
//        while (line != null && !line.isEmpty()) {
        line?.let {
            while (!line!!.isEmpty()) {
                line = reader.readLine()
            }
        }
    }
}
