import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

data class Header(val name: String, val value: String)
data class HttpMessage(
    val type: String,
    val method: String?,
    val target: String?,
    val status: String?,
    val reason: String?,
    val headers: List<Header>,
    val body: ByteArray,
)

class ParseError(val reason: String) : Exception(reason)

fun main() {
    try {
        printMessage(parse(ByteBuffer.wrap(System.`in`.readAllBytes())))
    } catch (e: ParseError) {
        println("error ${e.reason}")
        exitProcess(1)
    }
}

private fun parse(buffer: ByteBuffer): HttpMessage {
    val start = line(buffer, "bad_start_line") ?: throw ParseError("bad_start_line")
    val request = Regex("^(\\S+) (\\S+) HTTP/1\\.1$").matchEntire(start)
    val response = Regex("^HTTP/1\\.1 ([0-9]{3}) (.*)$").matchEntire(start)
    if (request == null && response == null) throw ParseError("bad_start_line")
    if (request != null && !request.groupValues[1].all(::headerNameCharacter)) throw ParseError("bad_start_line")
    val headers = mutableListOf<Header>()
    while (true) {
        val value = line(buffer, "bad_header") ?: throw ParseError("bad_header")
        if (value.isEmpty()) break
        val colon = value.indexOf(':')
        if (colon <= 0) throw ParseError("bad_header")
        val name = value.substring(0, colon)
        if (!name.all(::headerNameCharacter)) throw ParseError("bad_header")
        headers += Header(name.lowercase(), value.substring(colon + 1).trim(' ', '\t'))
    }
    val contentLengths = headers.filter { it.name == "content-length" }.map {
        if (it.value.isEmpty() || !it.value.all { char -> char in '0'..'9' }) throw ParseError("bad_header")
        it.value.toLongOrNull() ?: throw ParseError("bad_header")
    }
    if (contentLengths.distinct().size > 1) throw ParseError("bad_header")
    val lastTransferCoding = headers.asSequence()
        .filter { it.name == "transfer-encoding" }
        .flatMap { it.value.split(',').asSequence() }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .lastOrNull()
    val body = if (lastTransferCoding.equals("chunked", true)) {
        chunks(buffer)
    } else if (contentLengths.isNotEmpty()) {
        val length = contentLengths.first()
        if (length > buffer.remaining().toLong()) throw ParseError("incomplete_body")
        ByteArray(length.toInt()).also(buffer::get)
    } else {
        ByteArray(0)
    }
    return if (request != null) {
        HttpMessage("request", request.groupValues[1], request.groupValues[2], null, null, headers, body)
    } else {
        HttpMessage("response", null, null, response!!.groupValues[1], response.groupValues[2], headers, body)
    }
}

private fun chunks(buffer: ByteBuffer): ByteArray {
    val body = ByteArrayOutputStream()
    while (true) {
        val rawSize = line(buffer, "bad_chunk") ?: throw ParseError("bad_chunk")
        val sizeText = rawSize.substringBefore(';')
        if (sizeText.isEmpty() || !sizeText.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) throw ParseError("bad_chunk")
        val size = sizeText.toLongOrNull(16)?.takeIf { it <= Int.MAX_VALUE }?.toInt() ?: throw ParseError("bad_chunk")
        if (size == 0) {
            while (true) {
                val trailer = line(buffer, "bad_chunk") ?: throw ParseError("bad_chunk")
                if (trailer.isEmpty()) return body.toByteArray()
            }
        }
        if (size > buffer.remaining() - 2) throw ParseError("bad_chunk")
        val part = ByteArray(size)
        buffer.get(part)
        if (buffer.get() != 13.toByte() || buffer.get() != 10.toByte()) throw ParseError("bad_chunk")
        body.write(part)
    }
}

private fun line(buffer: ByteBuffer, error: String): String? {
    val start = buffer.position()
    var index = start
    while (index < buffer.limit()) {
        when (buffer.get(index)) {
            10.toByte() -> throw ParseError(error)
            13.toByte() -> {
                if (index + 1 >= buffer.limit() || buffer.get(index + 1) != 10.toByte()) {
                    throw ParseError(error)
                }
                val bytes = ByteArray(index - start)
                buffer.get(bytes)
                buffer.position(buffer.position() + 2)
                return String(bytes, StandardCharsets.ISO_8859_1)
            }
        }
        index++
    }
    return null
}

private fun headerNameCharacter(value: Char) = value in 'a'..'z' || value in 'A'..'Z' || value in '0'..'9' || value in "!#$%&'*+-.^_`|~"

private fun printMessage(message: HttpMessage) {
    println("type ${message.type}")
    if (message.type == "request") {
        println("method ${message.method}")
        println("target ${message.target}")
    } else {
        println("status ${message.status}")
        println("reason ${message.reason}")
    }
    println("version HTTP/1.1")
    message.headers.forEach { println("header ${it.name} ${it.value}") }
    println("body.length ${message.body.size}")
    if (message.body.isNotEmpty() && message.body.all { (it.toInt() and 255) in 0x20..0x7e }) {
        println("body.text ${String(message.body, StandardCharsets.US_ASCII)}")
    }
}
