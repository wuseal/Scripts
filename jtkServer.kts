//usr/bin/env echo '
/**** BOOTSTRAP kscript ****\'>/dev/null
command -v kscript >/dev/null 2>&1 || source /dev/stdin <<< "$(curl -L https://git.io/fpF1K)"
exec kscript $0 "$@"
\*** IMPORTANT: Any code including imports and annotations must come after this line ***/

@file:DependsOn("io.ktor:ktor-server-core-jvm:2.0.2")
@file:DependsOn("io.ktor:ktor-client-core-jvm:2.0.2")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:2.0.2")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm::2.0.2")
@file:DependsOn("io.ktor:ktor-server-netty-jvm:2.0.2")
@file:DependsOn("io.ktor:ktor-network-tls-certificates-jvm:2.0.2")
@file:DependsOn("ch.qos.logback:logback-classic:1.2.11")
@file:DependsOn("io.ktor:ktor-server-freemarker:2.0.2")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json-jvm:2.0.2")
@file:DependsOn("io.ktor:ktor-server-content-negotiation-jvm:2.0.2")


@file:CompilerOpts("-jvm-target 11")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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
val hostName = if (System.getenv("GITHUB_USER_NAME").isNullOrBlank()) "jsontokotlin.sealwu.com:8443" else "localhost"
val protocal = if (System.getenv("GITHUB_USER_NAME").isNullOrBlank()) "https" else "http"
val client = HttpClient(CIO) {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        json()
    }
}
fun writeExceptionLog(exceptionInfo: String) {
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
    val dayDir = File(jtkServerExceptionLogDir, day)
    if (!dayDir.exists()) {
        dayDir.mkdirs()
    }
    File(dayDir, Date().time.toString() + ".log").writeText(exceptionInfo)
}
data class Message(
        val role: String, val content: String, val name: String? = null
)

data class Error(
        val code: Any?,
        val message: String,
        val `param`: Any?,
        val type: String
)
data class ChatCompletion(
        val model: String,
        val messages: List<Message>,
        val temperature: Double? = null,
        val top_p: Double? = null,
        val n: Int? = null,
        val stream: Boolean? = null,
        val stop: List<String>? = null,
        val max_tokens: Int? = null,
        val presence_penalty: Double? = null,
        val frequency_penalty: Double? = null,
        val logit_bias: Map<String, Double>? = null,
        val user: String? = null
)

data class ResponseData(
        val id: String?,
        val `object`: String?,
        val created: Long?,
        val choices: List<Choice>?,
        val usage: Usage?,
        val error: Error? = null
)

data class Choice(
        val index: Int,
        val message: Message,
        val finish_reason: String
)

data class Usage(
        val prompt_tokens: Int,
        val completion_tokens: Int,
        val total_tokens: Int
)

val apiModule: Application.() -> Unit = {
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        json()
    }
    routing {
        post("/sendExceptionInfo") {
            println("Deal with api sendExceptionInfo")
            val exceptionLog = call.receive<String>()
            writeExceptionLog(exceptionLog)
            call.respond(HttpStatusCode.OK)
        }
        post("/gpt4"){
            val question = call.receive<String>()
            val message = Message("user", question)
            val chatCompletion = ChatCompletion("gpt-4", listOf(message), max_tokens = 1500, temperature = 0.1)
            client.post("https://api.openai.com/v1/chat/completions"){
                headers {
                    append("Authorization", "Bearer ${System.getenv("OPENAI_API_KEY")}")
                }
                setBody(chatCompletion)
            }.let {
                call.respond(it)
            }
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
