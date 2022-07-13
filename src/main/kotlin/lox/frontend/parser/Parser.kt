package lox.frontend.parser

import lox.Lox
import lox.frontend.ast.Expr
import lox.frontend.common.Token
import lox.frontend.common.TokenType
import kotlin.math.exp

class Parser(private val tokens: List<Token>) {

    class ParseError: Throwable()

    private var current = 0

    fun parse(): Expr? {
        return try {
            expression()
        } catch (error: ParseError) {
            null
        }
    }

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
        }
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().tokenType == TokenType.EOF

    private fun peek(): Token = tokens[current]

    private fun previous(): Token = tokens[current - 1]

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) {
            return false
        }
        return peek().tokenType == type
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun expression(): Expr {
        return equality();
    }

    private fun equality(): Expr {

        var expr = comparison()

        // Check for ternary operator
        if (match(TokenType.QUESTION)) {
            val thenBranch = equality()
            if (match(TokenType.COLON)) {
                val elseBranch = equality()
                expr = Expr.Ternary(expr, thenBranch, elseBranch)
            }
        }
        else {
            while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
                val operator = previous()
                val right = comparison()
                expr = Expr.Binary(expr, operator, right)
            }
        }

        return expr
    }

    private fun comparison(): Expr {

        var expr = term()

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {

        var expr = factor()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {

        var expr = unary()

        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }


        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }

    private fun primary(): Expr {

        if (match(TokenType.FALSE)) return Expr.Literal(false)
        if (match(TokenType.TRUE)) return Expr.Literal(true)
        if (match(TokenType.NIL)) return Expr.Literal(null)

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return Expr.Literal(previous().literal)
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        throw error(peek(), "Expect expression.")
    }

    private fun consume(expectedToken: TokenType, message: String): Token {
        if (check(expectedToken)) return advance()
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {

            if (previous().tokenType == TokenType.SEMICOLON) return

            when (peek().tokenType) {
                TokenType.CLASS, TokenType.FOR, TokenType.FUN, TokenType.IF, TokenType.PRINT,
                    TokenType.RETURN, TokenType.VAR, TokenType.WHILE -> {
                        return
                    }
                else -> {
                    /* Do nothing :) */
                }
            }
        }

        advance()
    }


}