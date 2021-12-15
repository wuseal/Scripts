import java.io.*
import kotlin.system.exitProcess
import java.util.concurrent.TimeUnit


data class BashResult(val exitCode: Int, val stdout: Iterable<String>, val stderr: Iterable<String>) {
    fun sout() = stdout.joinToString("\n").trim()

    fun serr() = stderr.joinToString("\n").trim()
}


fun evalBash(cmd: String, showOutput: Boolean = false, wd: File? = null): BashResult {

    try {

        // optionally prefix script with working directory change
        val cmd = (if (wd != null) "cd '${wd.absolutePath}'\n" else "") + cmd


        var pb = ProcessBuilder("/bin/bash", "-c", cmd)
        if (showOutput) {
            pb.inheritIO()
        }        
        pb.directory(File("."));
        var p = pb.start();

        val outputGobbler = StreamGobbler(p.getInputStream())
        val errorGobbler = StreamGobbler(p.getErrorStream())

        // kick them off
        errorGobbler.start()
        outputGobbler.start()

        // any error???
        val exitVal = p.waitFor()
        return BashResult(exitVal, outputGobbler.sb.lines(), errorGobbler.sb.lines())
    } catch (t: Throwable) {
        throw RuntimeException(t)
    }
}


internal class StreamGobbler(var inStream: InputStream) : Thread() {
    var sb = StringBuilder()

    override fun run() {
        try {
            val isr = InputStreamReader(inStream)
            val br = BufferedReader(isr)
            for (line in br.linesJ7()) {
                sb.append(line!! + "\n")
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }
    
    // workaround missing lines() function in java7
    fun BufferedReader.linesJ7(): Iterable<String> = lineSequence().toList()


    val output: String get() = sb.toString()
}

fun String.runCommand(timeoutValue: Long = 60, timeoutUnit: TimeUnit = TimeUnit.MINUTES, processConfig: ProcessBuilder.() -> Unit = {}): Process {
    ProcessBuilder("/bin/bash", "-c", this).run {
        directory(File("."))
        redirectOutput(ProcessBuilder.Redirect.INHERIT)
        redirectError(ProcessBuilder.Redirect.INHERIT)
        processConfig()
        val process = start()
        if (timeoutValue > 0L) {
            process.waitFor(timeoutValue,timeoutUnit)
        }else if (timeoutValue < 0) {
           process.waitFor() 
        }
        return process
    }
}

fun Process.throwIfError() {
    if (this.exitValue() != 0) {
        throw kotlin.RuntimeException("Process exec error ${toString()}")
    }
}

