import kotlin.system.exitProcess

fun main(args: Array<String>) {
    when (args.size) {
        0 -> {
            Lox.runPrompt() // Start REPL
        }
        1 -> {
            Lox.runFile(args[1])
        }
        else -> {
            println("usage: klox [script]")
            exitProcess(64)
        }
    }
}