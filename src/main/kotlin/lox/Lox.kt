package lox

import lox.backend.Interpreter
import lox.backend.Resolver
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
            hadError = true
        }

        @JvmStatic
        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        @JvmStatic
        fun report(line: Int, where: String, message: String) {
            System.err.println("[klox][line: ${line}] Error ${where}: $message")
        }

        @JvmStatic
        private fun run(source: String, repl: Boolean = false) {

            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()

            val parser = Parser(tokens)

            if (repl) {
                // Check if we succeed to parse an expression
                val expr = parser.parseExpression()
                if (!hadError && expr != null) {
                    println(interpreter.stringify(interpreter.evaluate(expr)))
                    return
                }
            }

            val statements = parser.parse()

            if (hadError) return

            val resolver = Resolver(interpreter)
            resolver.resolve(statements)

            if (hadError) return

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

                run(line, true)
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
            System.err.println("[klox][line: ${error.token.line}] ${error.message}")
            hadRuntimeError = true
        }
    }
}