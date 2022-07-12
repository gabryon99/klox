import lox.frontend.Scanner
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
    companion object {

        var hadError = false

        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        fun report(line: Int, where: String, message: String) {
            println("[line ${line}] Error ${where}: $message")
        }

        private fun run(source: String) {

            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()

            for (token in tokens) {
                println(token)
            }
        }

        fun runPrompt() {

            val input = InputStreamReader(System.`in`)
            val reader = BufferedReader(input)

            while (true) {

                print("> ")

                // Get next input line
                val line = reader.readLine()
                if (line.isEmpty()) {
                    break
                }

                run(line)
                hadError = false
            }
        }

        fun runFile(path: String) {
            val bytes = Files.readAllBytes(Paths.get(path))
            run(String(bytes, Charset.defaultCharset()))

            if (hadError) {
                exitProcess(65)
            }
        }
    }
}