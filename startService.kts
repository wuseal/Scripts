@file:Include("https://raw.githubusercontent.com/wuseal/Scripts/main/CommandLine.kt")

import java.io.File

/**
 * kscript url
 */
val homeDir = System.getProperty("user.home")

fun obtainScriptFromUrl(url: String) = evalBash("curl -s $url").run {
    if (exitCode == 0) sout() else ""
}

"java -version".runCommand()

var script: String = obtainScriptFromUrl(args[0])

val autoStartServiceDir = File(homeDir, ".autoStartService").also { it.mkdirs() }

"rm ${autoStartServiceDir.absolutePath}/localScriptFile-${args[0].hashCode()}*".runCommand()

val localScriptFile =
    File(autoStartServiceDir, "localScriptFile-${args[0].hashCode()}-${System.currentTimeMillis()}.kts")

localScriptFile.writeText(script)

println(localScriptFile.absolutePath)

var currentProcess: Process = "kscript ${localScriptFile.absolutePath}".runCommand(0)

while (true) {
    val newScript: String = obtainScriptFromUrl(args[0])

    if (newScript.isNotEmpty() && newScript != script) {
        println("Found Job Script Changed in startService ${args[0]}, Start to execute new Job Script....")
        currentProcess.let {
            it.descendants().forEach { it.destroy() }
            it.destroy()
        }
        script = newScript
        localScriptFile.writeText(script)
        currentProcess = "kscript ${localScriptFile.absolutePath}".runCommand(0)
    } else {
        println("Job Script not Change, Delay 1 minutes to check again....")
        Thread.sleep(60 * 1000)
    }
}
