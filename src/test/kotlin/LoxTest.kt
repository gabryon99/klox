import lox.Lox

internal class LoxTest {

    @org.junit.jupiter.api.Test
    fun runPrompt() {
    }

    @org.junit.jupiter.api.Test
    fun runFromString() {

        val inputFile = """
            /* Block /* Nested */ */
        """.trimIndent()

        Lox.runFromString(inputFile)

    }
}