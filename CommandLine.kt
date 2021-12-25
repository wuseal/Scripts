import java.io.File
import java.util.concurrent.TimeUnit


data class BashResult(val exitCode: Int, val stdout: Iterable<String>, val stderr: Iterable<String>) {
    fun sout() = stdout.joinToString("\n").trim()

    fun serr() = stderr.joinToString("\n").trim()
}


fun evalBash(cmd: String, showOutput: Boolean = false, wd: File? = null): BashResult {
    return cmd.runCommand {
        redirectOutput(ProcessBuilder.Redirect.PIPE)
        redirectInput(ProcessBuilder.Redirect.PIPE)
        redirectError(ProcessBuilder.Redirect.PIPE)
        wd?.let { directory(it) }
    }.run {
        val exitCode = exitValue()
        val stdout = inputStream.reader().readLines()
        val stderr = errorStream.reader().readLines()
        BashResult(exitCode, stdout, stderr).also {
            if (showOutput) {
                if (exitCode == 0) {
                    println(it.sout())
                } else {
                    println(it.serr())
                }
            }
        }
    }
}


fun String.runCommand(
    timeoutValue: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.MINUTES,
    processConfig: ProcessBuilder.() -> Unit = {}
): Process {
    ProcessBuilder("/bin/bash", "-c", this).run {
        directory(File("."))
        inheritIO()
        processConfig()
        val process = start()
        if (timeoutValue > 0L) {
            process.waitFor(timeoutValue, timeoutUnit)
        } else if (timeoutValue < 0) {
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
