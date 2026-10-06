import java.nio.ByteBuffer
import java.util.HexFormat
import kotlin.system.exitProcess

data class Ethernet(val destination: String, val source: String, val etherType: Int)

data class Ipv4(
    val version: Int,
    val ihlBytes: Int,
    val totalLength: Int,
    val identification: Int,
    val flags: String,
    val fragmentOffset: Int,
    val ttl: Int,
    val protocol: Int,
    val source: String,
    val destination: String,
    val checksumValid: Boolean,
)

data class Tcp(
    val sourcePort: Int,
    val destinationPort: Int,
    val sequenceNumber: Long,
    val acknowledgementNumber: Long,
    val dataOffsetBytes: Int,
    val flags: String,
    val window: Int,
)

data class Udp(val sourcePort: Int, val destinationPort: Int, val length: Int)

fun main() {
    try {
        val hex = System.`in`.readAllBytes().toString(Charsets.US_ASCII).replace(Regex("[\\s:]"), "")
        parse(HexFormat.of().parseHex(hex))
    } catch (e: Exception) {
        System.err.println(e.message ?: "invalid input")
        exitProcess(1)
    }
}

private fun parse(frame: ByteArray) {
    require(frame.size >= 14) { "Ethernet header is truncated" }
    val buffer = ByteBuffer.wrap(frame)

    val ethernet = Ethernet(mac(buffer), mac(buffer), buffer.u16())
    println("eth.dst ${ethernet.destination}")
    println("eth.src ${ethernet.source}")
    println("eth.ethertype 0x${ethernet.etherType.toString(16).padStart(4, '0')}")

    if (ethernet.etherType != 0x0800) return

    val ipStart = buffer.position()
    require(frame.size - ipStart >= 20) { "IPv4 header is truncated" }
    val versionAndIhl = buffer.u8()
    val version = versionAndIhl ushr 4
    val ihlBytes = (versionAndIhl and 0x0f) * 4
    buffer.get()
    val totalLength = buffer.u16()
    val identification = buffer.u16()
    val flagsAndOffset = buffer.u16()
    val ttl = buffer.u8()
    val protocol = buffer.u8()
    buffer.short
    val source = ipv4(buffer)
    val destination = ipv4(buffer)

    require(version == 4) { "Expected IPv4 packet" }
    require(ihlBytes >= 20 && totalLength >= ihlBytes) { "Invalid IPv4 header length" }
    require(ipStart + totalLength <= frame.size) { "IPv4 packet is truncated" }
    require(ipStart + ihlBytes <= frame.size) { "IPv4 options are truncated" }

    val fragmentOffset = (flagsAndOffset and 0x1fff) * 8
    val ipv4 = Ipv4(
        version,
        ihlBytes,
        totalLength,
        identification,
        ipFlags(flagsAndOffset),
        fragmentOffset,
        ttl,
        protocol,
        source,
        destination,
        checksumValid(frame, ipStart, ihlBytes),
    )
    printIpv4(ipv4)

    val transportStart = ipStart + ihlBytes
    val ipEnd = ipStart + totalLength
    buffer.position(transportStart)

    when {
        fragmentOffset != 0 -> println("payload.length ${ipEnd - transportStart}")
        protocol == 6 -> {
            require(ipEnd - transportStart >= 20) { "TCP header is truncated" }
            val tcp = tcp(buffer)
            require(tcp.dataOffsetBytes >= 20 && transportStart + tcp.dataOffsetBytes <= ipEnd) {
                "Invalid TCP header length"
            }
            printTcp(tcp)
            println("payload.length ${ipEnd - transportStart - tcp.dataOffsetBytes}")
        }
        protocol == 17 -> {
            require(ipEnd - transportStart >= 8) { "UDP header is truncated" }
            val udp = Udp(buffer.u16(), buffer.u16(), buffer.u16())
            buffer.short
            printUdp(udp)
            println("payload.length ${ipEnd - transportStart - 8}")
        }
        else -> println("payload.length ${ipEnd - transportStart}")
    }
}

private fun tcp(buffer: ByteBuffer): Tcp {
    val sourcePort = buffer.u16()
    val destinationPort = buffer.u16()
    val sequenceNumber = buffer.int.toLong() and 0xffffffffL
    val acknowledgementNumber = buffer.int.toLong() and 0xffffffffL
    val dataOffsetBytes = (buffer.u8() ushr 4) * 4
    val flags = buffer.u8()
    val window = buffer.u16()
    buffer.int
    return Tcp(
        sourcePort,
        destinationPort,
        sequenceNumber,
        acknowledgementNumber,
        dataOffsetBytes,
        tcpFlags(flags),
        window,
    )
}

private fun printIpv4(ip: Ipv4) {
    println("ip.version ${ip.version}")
    println("ip.ihl_bytes ${ip.ihlBytes}")
    println("ip.total_length ${ip.totalLength}")
    println("ip.id 0x${ip.identification.toString(16).padStart(4, '0')}")
    println("ip.flags ${ip.flags}")
    println("ip.frag_offset ${ip.fragmentOffset}")
    println("ip.ttl ${ip.ttl}")
    println("ip.protocol ${ip.protocol}")
    println("ip.src ${ip.source}")
    println("ip.dst ${ip.destination}")
    println("ip.checksum_valid ${ip.checksumValid}")
}

private fun printTcp(tcp: Tcp) {
    println("tcp.src_port ${tcp.sourcePort}")
    println("tcp.dst_port ${tcp.destinationPort}")
    println("tcp.seq ${tcp.sequenceNumber}")
    println("tcp.ack ${tcp.acknowledgementNumber}")
    println("tcp.data_offset_bytes ${tcp.dataOffsetBytes}")
    println("tcp.flags ${tcp.flags}")
    println("tcp.window ${tcp.window}")
}

private fun printUdp(udp: Udp) {
    println("udp.src_port ${udp.sourcePort}")
    println("udp.dst_port ${udp.destinationPort}")
    println("udp.length ${udp.length}")
}

private fun mac(buffer: ByteBuffer) = List(6) { buffer.u8() }.joinToString(":") {
    it.toString(16).padStart(2, '0')
}

private fun ipv4(buffer: ByteBuffer) = List(4) { buffer.u8() }.joinToString(".")

private fun ipFlags(value: Int): String {
    val df = (value and 0x4000) != 0
    val mf = (value and 0x2000) != 0
    return if (df) if (mf) "DF,MF" else "DF" else if (mf) "MF" else "none"
}

private fun tcpFlags(value: Int) = listOf("FIN", "SYN", "RST", "PSH", "ACK", "URG")
    .filterIndexed { bit, _ -> (value and (1 shl bit)) != 0 }
    .joinToString(",")
    .ifEmpty { "none" }

private fun checksumValid(bytes: ByteArray, offset: Int, length: Int): Boolean {
    var sum = 0L
    for (i in offset until offset + length step 2) {
        sum += (((bytes[i].toInt() and 0xff) shl 8) or (bytes[i + 1].toInt() and 0xff)).toLong()
        sum = (sum and 0xffff) + (sum ushr 16)
    }
    sum = (sum and 0xffff) + (sum ushr 16)
    return (sum and 0xffffL) == 0xffffL
}

private fun ByteBuffer.u8() = get().toInt() and 0xff

private fun ByteBuffer.u16() = short.toInt() and 0xffff
