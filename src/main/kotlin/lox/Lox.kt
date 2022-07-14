package lox

import lox.backend.Interpreter
import lox.backend.RuntimeError
import lox.frontend.common.Token
import lox.frontend.common.TokenType
import lox.frontend.lexer.Scanner
import lox.frontend.parser.Parser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
    companion object {

        private val interpreter = Interpreter()

        private var hadError = false
        private var hadRuntimeError = false

        @JvmStatic
        fun error(token: Token, message: String) {
            if (token.tokenType == TokenType.EOF) {
                report(token.line, " at end", message)
            }
            else {
                report(token.line, "at '${token.lexeme}'", message)
            }
        }

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

            val parser = Parser(tokens)
            val statements = parser.parse()

            if (hadError) {
                return
            }

            interpreter.interpret(statements)
        }

        @JvmStatic
        fun runPrompt() {

            val input = InputStreamReader(System.`in`)
            val reader = BufferedReader(input)

            println("Digit 'quit' to exit from REPL.")

            while (true) {

                print("> ")

                // Get next input line
                val line = reader.readLine()
                if (line.isEmpty() || line == "quit") {
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
            if (hadRuntimeError) {
                exitProcess(70)
            }
        }

        fun runtimeError(error: RuntimeError) {
            System.err.println("${error.message}\n[line: ${error.token.line}]")
            hadRuntimeError = true
        }
    }
}