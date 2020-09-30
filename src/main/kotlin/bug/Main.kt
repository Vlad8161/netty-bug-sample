package bug

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import java.io.File
import java.io.FileInputStream
import java.lang.StringBuilder
import java.security.KeyStore
import kotlin.text.toCharArray


fun main() {
    embeddedServer(Netty, applicationEngineEnvironment {
        connector {
            port = 80
            host = "localhost"
        }

        val keyStore = KeyStore.getInstance("JKS").apply {
            FileInputStream("store.jks").use {
                load(it, "12345678".toCharArray())
            }
        }
        sslConnector(
            keyStore = keyStore,
            keyAlias = "mykey",
            keyStorePassword = { "12345678".toCharArray() },
            privateKeyPassword = { "12345678".toCharArray() }
        ) {
            port = 443
            host = "localhost"
        }
        module {
            install(LogFeature)
            install(HSTS)

            routing {
                get("/") {
                    call.respond(HttpStatusCode.OK, "hello")
                }
            }
        }
    }).start(true)
}

class LogFeature {
    class Configuration

    companion object Feature : ApplicationFeature<Application, Configuration, LogFeature> {
        override val key: AttributeKey<LogFeature> = AttributeKey("LoggFeature")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): LogFeature {
            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                StringBuilder().apply {
                    appendLine()
                    appendLine("---")
                    appendLine("${call.request.origin.method} ${call.request.origin.scheme}:${call.request.origin.host}${call.request.uri} ${call.request.origin.version}")
                    for ((k, v) in call.request.headers.entries()) {
                        appendLine("$k: ${v.joinToString(", ")}")
                    }
                    println(toString())
                }
            }
            return LogFeature()
        }
    }
}
