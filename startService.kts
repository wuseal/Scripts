/**
 * this script used to start kscript service with kscript url,
 * With this entry point used, when kscript url's script changed, the kscript url's process will auto restart and execute the new script
 * This function maybe called hot-update script code
 *
 *  Demo: kscript https://raw.githubusercontent.com/wuseal/Scripts/master/startService.kts [YOU_SCRIPT_URL]
 */

@file:Include("https://raw.githubusercontent.com/wuseal/Scripts/main/CommandLine.kt")
@file:CompilerOpts("-jvm-target 9")
@file:KotlinOpts("-J-server")

import java.io.File


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
"kscript ${localScriptFile.absolutePath}".runCommand(0)

while (true) {
    val newScript: String = obtainScriptFromUrl(args[0])
    if (newScript.isNotEmpty() && newScript != script) {
        println("Found Service Script Changed in startService ${args[0]}, Start to execute new Job Script....")
        ProcessHandle.current().descendants().forEach { it.destroy() }
        script = newScript
        localScriptFile.writeText(script)
        "kscript ${localScriptFile.absolutePath}".runCommand(0)
    } else {
        println("Service Script not Change, Delay 1 minutes to check again....")
        Thread.sleep(60 * 1000)
    }
}
