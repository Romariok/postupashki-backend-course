import java.net.HttpURLConnection
import java.net.URI
import kotlin.random.Random
import kotlin.system.exitProcess

data class Config(val url: String, val method: String, val maxAttempts: Int, val idempotencyKey: String?)
data class Attempt(val status: Int?, val error: String?, val retryAfterMs: Long?)

fun main(args: Array<String>) {
    val config = try {
        parseArgs(args)
    } catch (e: Exception) {
        System.err.println(e.message ?: "invalid arguments")
        exitProcess(1)
    }
    var attemptNumber = 0
    var successful = false
    while (attemptNumber < config.maxAttempts) {
        attemptNumber++
        val attempt = request(config)
        if (attempt.status != null) {
            println("attempt $attemptNumber status ${attempt.status}")
            successful = attempt.status in 200..399
        } else {
            println("attempt $attemptNumber error ${attempt.error}")
        }
        if (successful || !retryable(config, attempt) || attemptNumber == config.maxAttempts) break
        val delay = attempt.retryAfterMs ?: Random.nextLong(backoff(attemptNumber + 1) + 1)
        println("sleep_ms $delay")
        Thread.sleep(delay)
    }
    println("result ${if (successful) "success" else "failure"} attempts $attemptNumber")
    if (!successful) exitProcess(1)
}

private fun parseArgs(args: Array<String>): Config {
    require(args.isNotEmpty()) { "usage: <url> [options]" }
    val uri = URI(args[0])
    require(uri.scheme == "http" && uri.host != null) { "only http URLs are supported" }
    var method = "GET"
    var maxAttempts = 5
    var key: String? = null
    var index = 1
    while (index < args.size) {
        require(index + 1 < args.size) { "missing option value" }
        when (args[index]) {
            "--method" -> method = args[index + 1]
            "--max-attempts" -> maxAttempts = args[index + 1].toInt()
            "--idempotency-key" -> key = args[index + 1]
            else -> error("unknown option: ${args[index]}")
        }
        index += 2
    }
    require(maxAttempts > 0) { "max attempts must be positive" }
    return Config(args[0], method, maxAttempts, key)
}

private fun request(config: Config): Attempt {
    var connection: HttpURLConnection? = null
    return try {
        connection = URI(config.url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = config.method
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Connection", "close")
        config.idempotencyKey?.let { connection.setRequestProperty("Idempotency-Key", it) }
        val status = connection.responseCode
        (if (status >= 400) connection.errorStream else connection.inputStream)?.use { it.readAllBytes() }
        val retryAfter = connection.getHeaderField("Retry-After")?.toLongOrNull()?.takeIf { it >= 0 }?.let {
            if (it > Long.MAX_VALUE / 1000) Long.MAX_VALUE else it * 1000
        }
        Attempt(status, null, retryAfter)
    } catch (e: Exception) {
        Attempt(null, e.message?.takeIf(String::isNotBlank) ?: e.javaClass.simpleName, null)
    } finally {
        connection?.disconnect()
    }
}

private fun retryable(config: Config, attempt: Attempt): Boolean {
    val methodCanRetry = config.method.uppercase() in setOf("GET", "HEAD", "PUT", "DELETE", "OPTIONS", "TRACE") ||
        config.method.equals("POST", true) && config.idempotencyKey != null
    return methodCanRetry && (attempt.status == null || attempt.status in setOf(429, 500, 502, 503, 504))
}

private fun backoff(nextAttempt: Int) = when (nextAttempt) {
    2 -> 400L
    3 -> 800L
    4 -> 1600L
    else -> 2000L
}
