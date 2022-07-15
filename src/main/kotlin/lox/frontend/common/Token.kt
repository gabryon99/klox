package lox.frontend.common

data class Token(val tokenType: TokenType, val lexeme: String, val literal: Any?, val line: Int) {

    override fun toString(): String {
        return "Token(tokenType=$tokenType, lexeme='$lexeme', literal=$literal, line=$line)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token

        if (tokenType != other.tokenType) return false
        if (lexeme != other.lexeme) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tokenType.hashCode()
        result = 31 * result + lexeme.hashCode()
        return result
    }


}