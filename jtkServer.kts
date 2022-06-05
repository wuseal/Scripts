//usr/bin/env echo '
/**** BOOTSTRAP kscript ****\'>/dev/null
command -v kscript >/dev/null 2>&1 || source /dev/stdin <<< "$(curl -L https://git.io/fpF1K)"
exec kscript $0 "$@"
\*** IMPORTANT: Any code including imports and annotations must come after this line ***/

@file:DependsOn("io.ktor:ktor-server-core-jvm:2.0.2")
@file:DependsOn("io.ktor:ktor-server-netty-jvm:2.0.2")
@file:DependsOn("io.ktor:ktor-network-tls-certificates-jvm:2.0.2")
@file:DependsOn("ch.qos.logback:logback-classic:1.2.11")
@file:CompilerOpts("-jvm-target 11")

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.*

val homeDir = System.getProperty("user.home")
val jtkServerExceptionLogDir = File(homeDir, "JSONToKotlinClass/Exceptions")
jtkServerExceptionLogDir.mkdirs()
val sslFilePassWord = System.getenv("JTK_SSL_PASSWORD")

fun writeExceptionLog(exceptionInfo: String) {
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
    val dayDir = File(jtkServerExceptionLogDir, day)
    if (!dayDir.exists()) {
        dayDir.mkdirs()
    }
    File(dayDir, Date().time.toString()).writeText(exceptionInfo)
}

val environment = applicationEngineEnvironment {
    log = LoggerFactory.getLogger("ktor.application")

    val keyStoreFile = File(homeDir, "jtkserver.jks")
    if (keyStoreFile.exists().not()) {
        throw IllegalStateException("ssl certificator must exist!")
    }
    connector {
        port = 8080
    }
    sslConnector(
        keyStore = KeyStore.getInstance(keyStoreFile, sslFilePassWord.toCharArray()),
        keyAlias = "alias-key",
        keyStorePassword = { sslFilePassWord.toCharArray() },
        privateKeyPassword = { sslFilePassWord.toCharArray() }) {
        port = 8443
        keyStorePath = keyStoreFile
    }
    module {
        routing {
            get("/") {
                call.respondText("Hello, world! From Ktor!!")
            }
            get("/{...}") {
                val uri = call.request.uri
                call.respondText("Request uri: $uri")
            }
            post("/sendExceptionInfo") {
                println("Deal with api sendExceptionInfo")
                val exceptionLog = call.receive<String>()
                writeExceptionLog(exceptionLog)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

embeddedServer(Netty, environment).start(wait = true)
