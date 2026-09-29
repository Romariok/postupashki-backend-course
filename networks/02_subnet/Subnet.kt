import java.io.File
import kotlin.system.exitProcess

data class Cidr(val address: Long, val prefix: Int) {
    val mask = if (prefix == 0) 0L else (0xffffffffL shl (32 - prefix)) and 0xffffffffL
    val network = address and mask
    val lastAddress = network or (mask xor 0xffffffffL)
}

data class Route(val cidr: Cidr, val via: String)

fun main(args: Array<String>) {
    try {
        when (args.firstOrNull()) {
            "subnet" -> {
                require(args.size == 2) { "invalid arguments" }
                subnet(parseCidr(args[1]))
            }
            "route" -> {
                require(args.size == 3) { "invalid arguments" }
                route(File(args[1]), parseIp(args[2]))
            }
            else -> error("usage: subnet <address/prefix> | route <file> <address>")
        }
    } catch (e: Exception) {
        System.err.println(e.message ?: "invalid input")
        exitProcess(1)
    }
}

private fun subnet(cidr: Cidr) {
    val special = cidr.prefix >= 31
    val first = if (special) cidr.network else cidr.network + 1
    val last = if (special) cidr.lastAddress else cidr.lastAddress - 1
    val hosts = if (special) 1L shl (32 - cidr.prefix) else (1L shl (32 - cidr.prefix)) - 2
    println("network ${formatIp(cidr.network)}")
    println("broadcast ${if (special) "none" else formatIp(cidr.lastAddress)}")
    println("netmask ${formatIp(cidr.mask)}")
    println("prefix ${cidr.prefix}")
    println("first ${formatIp(first)}")
    println("last ${formatIp(last)}")
    println("hosts $hosts")
}

private fun route(file: File, destination: Long) {
    val selected = file.readLines().asSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map {
            val fields = it.split(Regex("\\s+"))
            require(fields.size == 2) { "invalid route: $it" }
            Route(parseCidr(fields[0]), fields[1])
        }
        .filter { destination and it.cidr.mask == it.cidr.network }
        .maxByOrNull { it.cidr.prefix }
    if (selected == null) {
        println("unreachable true")
        exitProcess(1)
    }
    println("via ${selected.via}")
    println("prefix ${selected.cidr.prefix}")
}

private fun parseCidr(value: String): Cidr {
    val parts = value.split('/')
    require(parts.size == 2) { "invalid CIDR: $value" }
    val prefix = parts[1].toIntOrNull()
    require(prefix != null && prefix in 0..32) { "invalid prefix: ${parts[1]}" }
    return Cidr(parseIp(parts[0]), prefix)
}

private fun parseIp(value: String): Long {
    val parts = value.split('.')
    require(parts.size == 4) { "invalid IPv4 address: $value" }
    return parts.fold(0L) { result, part ->
        require(part.isNotEmpty() && part.all(Char::isDigit)) { "invalid IPv4 address: $value" }
        val octet = part.toIntOrNull()
        require(octet != null && octet in 0..255) { "invalid IPv4 address: $value" }
        (result shl 8) or octet.toLong()
    }
}

private fun formatIp(value: Long) = (3 downTo 0).joinToString(".") { ((value ushr (it * 8)) and 255).toString() }
