import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.random.Random
import kotlin.system.exitProcess

data class DnsAnswer(val type: String, val value: String, val ttl: Long)
data class DnsResponse(val status: String, val answers: List<DnsAnswer>, val minimumTtl: Long?)
data class CacheEntry(val response: DnsResponse, val expiresAt: Long)
data class Name(val value: String, val end: Int)

val types = mapOf("A" to 1, "NS" to 2, "CNAME" to 5, "MX" to 15, "TXT" to 16, "AAAA" to 28)

fun main(args: Array<String>) {
    val server: InetAddress
    val port: Int
    try {
        require(args.size == 2) { "usage: <server> <port>" }
        server = InetAddress.getByName(args[0])
        port = args[1].toInt()
        require(port in 1..65535) { "invalid port" }
    } catch (e: Exception) {
        System.err.println(e.message ?: "invalid arguments")
        exitProcess(1)
    }
    val cache = mutableMapOf<Pair<String, Int>, CacheEntry>()
    DatagramSocket().use { socket ->
        for (rawLine in System.`in`.bufferedReader().lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val fields = line.split(Regex("\\s+"))
            if (fields.size != 2) {
                System.err.println("invalid query: $line")
                continue
            }
            val name = fields[0]
            val typeName = fields[1].uppercase()
            val type = types[typeName]
            if (type == null) {
                System.err.println("unsupported type: ${fields[1]}")
                continue
            }
            val key = name.lowercase().trimEnd('.') to type
            val now = System.nanoTime()
            val cached = cache[key]?.takeIf { it.expiresAt > now }
            val response = try {
                cached?.response ?: request(socket, server, port, name, type)
            } catch (e: Exception) {
                System.err.println(e.message ?: "invalid query: $line")
                continue
            }
            println("query $line")
            if (response == null) {
                println("status TIMEOUT")
                println("end")
                exitProcess(1)
            }
            if (cached == null && response.status == "NOERROR" && response.minimumTtl != null && response.minimumTtl > 0) {
                cache[key] = CacheEntry(response, now + response.minimumTtl.coerceAtMost(Long.MAX_VALUE / 1_000_000_000L) * 1_000_000_000L)
            }
            println("status ${response.status}")
            response.answers.forEach { println("answer ${it.type} ${it.value} ${it.ttl}") }
            println("end")
        }
    }
}

private fun request(socket: DatagramSocket, server: InetAddress, port: Int, name: String, type: Int): DnsResponse? {
    val id = Random.nextInt(0x10000)
    val query = query(id, name, type)
    socket.send(DatagramPacket(query, query.size, server, port))
    val deadline = System.nanoTime() + 5_000_000_000L
    while (true) {
        val remaining = deadline - System.nanoTime()
        if (remaining <= 0) return null
        socket.soTimeout = ((remaining + 999_999) / 1_000_000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val bytes = ByteArray(0xffff)
        val packet = DatagramPacket(bytes, bytes.size)
        try {
            socket.receive(packet)
        } catch (_: java.net.SocketTimeoutException) {
            return null
        }
        if (packet.length >= 2 && ByteBuffer.wrap(bytes, 0, packet.length).u16() == id) {
            return try {
                parseResponse(bytes.copyOf(packet.length))
            } catch (_: RuntimeException) {
                DnsResponse("FORMERR", emptyList(), null)
            }
        }
    }
}

private fun query(id: Int, name: String, type: Int): ByteArray {
    val buffer = ByteBuffer.allocate(512)
    buffer.putShort(id.toShort())
    buffer.putShort(0x0100.toShort())
    buffer.putShort(1)
    buffer.putShort(0)
    buffer.putShort(0)
    buffer.putShort(0)
    name.trimEnd('.').split('.').filter(String::isNotEmpty).forEach {
        val label = it.toByteArray(StandardCharsets.US_ASCII)
        require(label.size in 1..63) { "invalid DNS label" }
        buffer.put(label.size.toByte()).put(label)
    }
    buffer.put(0)
    buffer.putShort(type.toShort())
    buffer.putShort(1)
    return buffer.array().copyOf(buffer.position())
}

private fun parseResponse(bytes: ByteArray): DnsResponse {
    val buffer = ByteBuffer.wrap(bytes)
    require(buffer.remaining() >= 12) { "truncated DNS response" }
    buffer.u16()
    val flags = buffer.u16()
    val questions = buffer.u16()
    val answerCount = buffer.u16()
    buffer.u16()
    buffer.u16()
    repeat(questions) {
        buffer.position(readName(bytes, buffer.position()).end)
        require(buffer.remaining() >= 4) { "truncated DNS question" }
        buffer.position(buffer.position() + 4)
    }
    val answers = mutableListOf<DnsAnswer>()
    var minimumTtl: Long? = null
    repeat(answerCount) {
        buffer.position(readName(bytes, buffer.position()).end)
        require(buffer.remaining() >= 10) { "truncated DNS answer" }
        val type = buffer.u16()
        buffer.u16()
        val ttl = buffer.int.toLong() and 0xffffffffL
        val length = buffer.u16()
        val offset = buffer.position()
        require(length <= buffer.remaining()) { "truncated DNS data" }
        minimumTtl = minimumTtl?.let { minOf(it, ttl) } ?: ttl
        answer(bytes, type, ttl, offset, length)?.let(answers::add)
        buffer.position(offset + length)
    }
    return DnsResponse(status(flags and 15), answers, minimumTtl)
}

private fun answer(bytes: ByteArray, type: Int, ttl: Long, offset: Int, length: Int): DnsAnswer? {
    val data = ByteBuffer.wrap(bytes, offset, length)
    return when (type) {
        1 -> if (length == 4) DnsAnswer("A", List(4) { data.u8() }.joinToString("."), ttl) else null
        2 -> DnsAnswer("NS", readName(bytes, offset).value, ttl)
        5 -> DnsAnswer("CNAME", readName(bytes, offset).value, ttl)
        15 -> if (length >= 3) DnsAnswer("MX", "${data.u16()} ${readName(bytes, offset + 2).value}", ttl) else null
        16 -> DnsAnswer("TXT", text(bytes, offset, length), ttl)
        28 -> if (length == 16) DnsAnswer("AAAA", ipv6(data), ttl) else null
        else -> null
    }
}

private fun readName(bytes: ByteArray, start: Int): Name {
    val labels = mutableListOf<String>()
    var position = start
    var end = -1
    repeat(128) {
        require(position in bytes.indices) { "truncated DNS name" }
        val length = bytes[position].toInt() and 255
        if (length and 0xc0 == 0xc0) {
            require(position + 1 < bytes.size) { "truncated DNS pointer" }
            if (end < 0) end = position + 2
            position = ((length and 0x3f) shl 8) or (bytes[position + 1].toInt() and 255)
        } else {
            require(length <= 63 && position + 1 + length <= bytes.size) { "invalid DNS name" }
            position++
            if (length == 0) return Name(if (labels.isEmpty()) "." else labels.joinToString(".", postfix = "."), if (end < 0) position else end)
            labels += String(bytes, position, length, StandardCharsets.US_ASCII)
            position += length
        }
    }
    error("DNS compression loop")
}

private fun text(bytes: ByteArray, offset: Int, length: Int): String {
    val end = offset + length
    var position = offset
    val result = StringBuilder()
    while (position < end) {
        val size = bytes[position++].toInt() and 255
        require(position + size <= end) { "invalid TXT record" }
        result.append(String(bytes, position, size, StandardCharsets.UTF_8))
        position += size
    }
    return result.toString()
}

private fun ipv6(buffer: ByteBuffer): String {
    val groups = IntArray(8) { buffer.u16() }
    var bestStart = -1
    var bestLength = 0
    var index = 0
    while (index < groups.size) {
        if (groups[index] != 0) {
            index++
            continue
        }
        val start = index
        while (index < groups.size && groups[index] == 0) index++
        if (index - start > bestLength && index - start >= 2) {
            bestStart = start
            bestLength = index - start
        }
    }
    if (bestStart < 0) return groups.joinToString(":") { it.toString(16) }
    val left = groups.take(bestStart).joinToString(":") { it.toString(16) }
    val right = groups.drop(bestStart + bestLength).joinToString(":") { it.toString(16) }
    return "$left::$right"
}

private fun status(code: Int) = when (code) {
    0 -> "NOERROR"
    1 -> "FORMERR"
    2 -> "SERVFAIL"
    3 -> "NXDOMAIN"
    5 -> "REFUSED"
    else -> "RCODE$code"
}

private fun ByteBuffer.u8() = get().toInt() and 0xff
private fun ByteBuffer.u16() = short.toInt() and 0xffff
