//usr/bin/env echo '
/**** BOOTSTRAP kscript ****\'>/dev/null
command -v kscript >/dev/null 2>&1 || source /dev/stdin <<< "$(curl -L https://git.io/fpF1K)"
exec kscript $0 "$@"
\*** IMPORTANT: Any code including imports and annotations must come after this line ***/

@file:DependsOn("io.ktor:ktor-server-core-jvm:2.0.2")
@file:DependsOn("io.ktor:ktor-server-netty-jvm:2.0.2")
@file:DependsOn("io.ktor:ktor-network-tls-certificates-jvm:2.0.2")
@file:DependsOn("ch.qos.logback:logback-classic:1.2.11")
@file:DependsOn("io.ktor:ktor-server-freemarker:2.0.2")
@file:CompilerOpts("-jvm-target 11")

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
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
val hostName = if (System.getenv("GITHUB_USER_NAME").isNullOrBlank().not()) "jsontokotlin.sealwu.com" else "localhost"
val protocal = if (System.getenv("GITHUB_USER_NAME").isNullOrBlank().not()) "https" else "http"

fun writeExceptionLog(exceptionInfo: String) {
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
    val dayDir = File(jtkServerExceptionLogDir, day)
    if (!dayDir.exists()) {
        dayDir.mkdirs()
    }
    File(dayDir, Date().time.toString()).writeText(exceptionInfo)
}

val apiModule: Application.() -> Unit = {
    routing {
        post("/sendExceptionInfo") {
            println("Deal with api sendExceptionInfo")
            val exceptionLog = call.receive<String>()
            writeExceptionLog(exceptionLog)
            call.respond(HttpStatusCode.OK)
        }
        get("/") {
            val responseContent =
                """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>JSONToKotlinClass Error Log</title>
                    </head>
                    <body style="text-align: center; font-family: sans-serif">
                    ${
                    jtkServerExceptionLogDir.listFiles().joinToString("\n") {
                        """
                   <br> <a href="$protocal://$hostName/listLogs/${it.name}">${it.name}</a></br>
                """.trimIndent()
                    }
                }
                    </body>
                    </html>
                """.trimIndent()
            call.respondText(responseContent, ContentType.Text.Html)
        }

        get("/listLogs/{logDate}") {
            val dateDay = call.parameters["logDate"]
            val exceptionLogNum = File(jtkServerExceptionLogDir, dateDay).listFiles().size
            val responseContent = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>JSONToKotlinClass Error Log</title>
                </head>
                <body style="text-align: center; font-family: sans-serif">
                <h1>There are $exceptionLogNum errors in $dateDay</h1>
                ${
                File(jtkServerExceptionLogDir, dateDay).listFiles().joinToString("\n") {
                    """
                   <br> <a href="$protocal://$hostName/static/$dateDay/${it.name}">${it.name}</a>  </br>
                    """.trimIndent()
                }
            }
            </body>
            </html>
            """.trimIndent()
            call.respondText(responseContent, ContentType.Text.Html)
        }
    }

}
val webModule: Application.() -> Unit = {
    routing {
        static("/static") {
            files(jtkServerExceptionLogDir)
        }
    }
}

val environment = applicationEngineEnvironment {
    log = LoggerFactory.getLogger("ktor.application")

    val keyStoreFile = File(homeDir, "jtkserver.jks")
    if (keyStoreFile.exists().not()) {
        throw IllegalStateException("ssl certificator must exist!")
    }
    connector {
        port = 80
    }
    sslConnector(
        keyStore = KeyStore.getInstance(keyStoreFile, sslFilePassWord.toCharArray()),
        keyAlias = "alias-key",
        keyStorePassword = { sslFilePassWord.toCharArray() },
        privateKeyPassword = { sslFilePassWord.toCharArray() }) {
        port = 8443
        keyStorePath = keyStoreFile
    }
    module(apiModule)
    module(webModule)
}


embeddedServer(Netty, environment).start(wait = true)
