import lox.frontend.Scanner
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
    companion object {

        private var hadError = false

        @JvmStatic
        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        @JvmStatic
        fun report(line: Int, where: String, message: String) {
            println("[line ${line}] Error ${where}: $message")
        }

        @JvmStatic
        private fun run(source: String) {

            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()

            for (token in tokens) {
                println(token)
            }
        }

        @JvmStatic
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

        @JvmStatic
        fun runFromString(source: String) {
            run(source)

            if (hadError) {
                exitProcess(65)
            }
        }

        @JvmStatic
        fun runFile(path: String) {
            val bytes = Files.readAllBytes(Paths.get(path))
            run(String(bytes, Charset.defaultCharset()))

            if (hadError) {
                exitProcess(65)
            }
        }
    }
}