package lox.frontend

class Scanner(private val source: String) {

    companion object {
        val keywords = mapOf<String, TokenType>(
            "and" to TokenType.AND,
            "class" to TokenType.CLASS,
            "else" to TokenType.ELSE,
            "false" to TokenType.FALSE,
            "for" to TokenType.FOR,
            "fun" to TokenType.FUN,
            "if" to TokenType.IF,
            "nil" to TokenType.NIL,
            "or" to TokenType.OR,
            "print" to TokenType.PRINT,
            "return" to TokenType.RETURN,
            "super" to TokenType.SUPER,
            "this" to TokenType.THIS,
            "true" to TokenType.TRUE,
            "var" to TokenType.VAR,
            "while" to TokenType.WHILE
        )
    }

    private val tokens = mutableListOf<Token>()

    // Points to the first character in the lexeme being scanned
    private var start = 0
    // Points at the character currently being considered
    private var current = 0
    private var line = 1

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun advance(): Char {
        val nextChar = source[current]
        current++
        return nextChar
    }

    private fun peek(): Char {

        if (isAtEnd()) {
            return 0.toChar()
        }

        return source[current]
    }

    private fun peekNext(): Char {
        if (current + 1 >= source.length) {
            return 0.toChar()
        }
        return source[current + 1]
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        return true
    }

    private fun addToken(tokenType: TokenType) {
        addToken(tokenType, null)
    }

    private fun addToken(tokenType: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens.add(Token(tokenType, text, literal, line))
    }

    private fun isDigit(ch: Char): Boolean = ch in '0'..'9'

    private fun isAlpha(ch: Char): Boolean = ch in 'a' .. 'z' || ch in 'A' .. 'Z' || ch == '_'

    private fun isAlphaNumeric(ch: Char): Boolean = isAlpha(ch) || isDigit(ch)

    private fun lexString() {

        while (peek() != '"' &&  !isAtEnd()) {
            if (peek() == '\n') {
                line++
            }
            advance()
        }

        if (!isAtEnd()) {
            Lox.error(line, "Undetermined string.")
            return
        }

        advance()

        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    private fun lexNumber() {

        while (isDigit(peek())) advance()

        if (peek() == '.' && isDigit(peekNext())) {
            advance()

            while (isDigit(peek())) advance()
        }

        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }

    private fun lexIdentifier() {

        while (isAlphaNumeric(peek())) advance()

        val text = source.substring(start, current)

        val tokenType = keywords[text] ?: TokenType.IDENTIFIER
        addToken(tokenType)
    }

    private fun scanToken() {
        when (val ch = advance()) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)

            '!' -> addToken(if (match('=')) { TokenType.BANG_EQUAL } else { TokenType.BANG })
            '=' -> addToken(if (match('=')) { TokenType.EQUAL_EQUAL } else { TokenType.EQUAL })
            '<' -> addToken(if (match('=')) { TokenType.LESS_EQUAL } else { TokenType.LESS })
            '>' -> addToken(if (match('=')) { TokenType.GREATER_EQUAL } else { TokenType.GREATER })

            '/' -> {
                if (match('/')) {
                    // We are matching a comment, so we go until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) advance()
                }
                else {
                    addToken(TokenType.SLASH)
                }
            }

            ' ', '\r', '\t' -> {
                // Ignore whitespaces :)
            }

            '\n' -> {
                line++
            }

            '"' -> {
                lexString()
            }

            else -> {
                if (isDigit(ch)) {
                    lexNumber()
                }
                else if (isAlpha(ch)) {
                    lexIdentifier()
                }
                else {
                    Lox.error(line, "Unexpected character ($ch).")
                }
            }
        }
    }

    fun scanTokens(): List<Token> {

        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        return tokens
    }

}