@file:Include("https://raw.githubusercontent.com/wuseal/Scripts/main/CommandLine.kt")

/**
 * kscript url
 */

fun obtainScriptFromUrl(url: String) = evalBash(url).run {
    if (exitCode == 0) sout() else ""
}

"java -version".runCommand()

var script: String = obtainScriptFromUrl(args[0])

var currentProcess : Process = "kscript ${args[0]}".runCommand(0)

while (true) {
    val newScript: String = obtainScriptFromUrl(args[0])

    if (newScript.isNotEmpty() && newScript != script) {
        println("Found Job Script Changed in startService ${args[0]}, Start to execute new Job Script....")
        currentProcess.let {
            it.descendants().forEach { it.destroy() }
            it.destroy()
        }
        currentProcess = "kscript ${args[0]}".runCommand(0)
        script = newScript
    } else {
        println("Job Script not Change, Delay 1 minutes to check again....")
        Thread.sleep(60 * 1000)
    }
}
