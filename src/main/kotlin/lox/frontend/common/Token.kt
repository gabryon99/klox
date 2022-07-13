package lox.frontend.common

data class Token(val tokenType: TokenType, val lexeme: String, val literal: Any?, val line: Int) {
    override fun toString(): String {
        return "Token(tokenType=$tokenType, lexeme='$lexeme', literal=$literal, line=$line)"
    }
}