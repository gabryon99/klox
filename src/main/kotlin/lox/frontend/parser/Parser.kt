package lox.frontend.parser

import lox.Lox
import lox.frontend.ast.Expr
import lox.frontend.ast.Stmt
import lox.frontend.common.Token
import lox.frontend.common.TokenType

private const val MAX_ARGUMENTS_NUMBER = 255

class Parser(private val tokens: List<Token>) {

    class ParseError(msg: String = ""): Throwable(msg)

    private var current = 0

    fun parse(): List<Stmt?> {

       val statements = mutableListOf<Stmt?>()

        while (!isAtEnd()) {
            statements.add(declaration())
        }

        return statements
    }

    fun parseExpression(): Expr? {
        return try {
            expression()
        }
        catch (error: ParseError) {
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

    private fun declaration(): Stmt? {
        return try {

            if (match(TokenType.FUN)) {
                return function("function")
            }

            if (match(TokenType.CLASS)) {
                return classDeclaration()
            }

            if (match(TokenType.VAR)) {
                varDeclaration()
            }
            else {
                statement()
            }
        }
        catch (error: ParseError) {
            synchronize()
            null
        }
    }

    private fun classDeclaration(): Stmt {

        val name = consume(TokenType.IDENTIFIER, "Expect a class name after 'class' keyword.")
        consume(TokenType.LEFT_BRACE, "Expect '{' after class name '${name.lexeme}'.")

        val methods = mutableListOf<Stmt.Function>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class ${name.lexeme} declaration.")

        return Stmt.Class(name, methods)
    }

    private fun function(kind: String): Stmt.Function {

        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name")

        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= MAX_ARGUMENTS_NUMBER) {
                    error(peek(), "Cant have more than $MAX_ARGUMENTS_NUMBER parameters.")
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters list.")
        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")

        val body = block()

        return Stmt.Function(name, parameters, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")

        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) {
            initializer = expression()
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration")

        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {

        if (match(TokenType.PRINT)) {
            return printStatement()
        }
        if (match(TokenType.LEFT_BRACE)) {
            return Stmt.Block(block())
        }
        if (match(TokenType.IF)) {
            return ifStatement()
        }
        if (match(TokenType.WHILE)) {
            return whileStatement()
        }
        if (match(TokenType.FOR)) {
            return forStatement()
        }
        if (match(TokenType.BREAK)) {
            return breakStatement()
        }
        if (match(TokenType.RETURN)) {
            return returnStatement()
        }


        val exp = expressionStatement()
        if (exp.expr is Expr.Lambda) {
            throw error(previous(), "Anonymous function cannot be declared in top-level.")
        }

        return exp
    }

    private fun returnStatement(): Stmt {

        val keyword = previous()
        var value: Expr? = null

        if (!check(TokenType.SEMICOLON)) {
            value = expression()
        }

        consume(TokenType.SEMICOLON, "Expect ';' after return value.")

        return Stmt.Return(keyword, value)
    }

    private fun breakStatement(): Stmt {
        consume(TokenType.SEMICOLON, "Expect ';' after break statement.")
        return Stmt.Break(previous())
    }

    private fun forStatement(): Stmt {

        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
        val initializer = if (match(TokenType.SEMICOLON)) {
            null
        } else if (match(TokenType.VAR)) {
            varDeclaration()
        } else {
            expressionStatement()
        }

        var condition = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            null
        }

        consume(TokenType.SEMICOLON, "Expect ';' after loop condition")

        val increment = if (!check(TokenType.RIGHT_PAREN)) {
            expression()
        } else {
            null
        }

        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = statement()

        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }

        if (condition == null) {
            condition = Expr.Literal(true)
        }

        body = Stmt.While(condition, body)

        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }

        return body
    }

    private fun whileStatement(): Stmt {

        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'while'.")

        val whileBody: Stmt = statement()

        return Stmt.While(condition, whileBody)
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN,"Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN,"Expect ')' after 'if'.")

        val thenBranch = statement()
        var elseBranch: Stmt? = null
        if (match(TokenType.ELSE)) {
            elseBranch = statement()
        }

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun block(): List<Stmt?> {

        val stmts = mutableListOf<Stmt?>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            stmts.add(declaration())
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")

        return stmts
    }

    private fun expressionStatement(): Stmt.Expression {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected ';' after value.")
        return Stmt.Print(value)
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {

        val expr = or()

        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
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

        return call()
    }

    private fun call(): Expr {

        var expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            }
            else {
                break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {

        val arguments = mutableListOf<Expr>()

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= MAX_ARGUMENTS_NUMBER) {
                    // We don't kick panic mode
                    error(peek(), "Can't have more than 255 arguments")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }

        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr {

        if (match(TokenType.FALSE)) return Expr.Literal(false)
        if (match(TokenType.TRUE)) return Expr.Literal(true)
        if (match(TokenType.NIL)) return Expr.Literal(null)

        if (match(TokenType.IDENTIFIER)) return Expr.Variable(previous())

        if (match(TokenType.FUN)) {
            return lambdaExpression()
        }

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

    private fun lambdaExpression(): Expr {

        consume(TokenType.LEFT_PAREN, "Expect '(' after lambda function.")

        // TODO: refactoring
        val parameters = mutableListOf<Token>()

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= MAX_ARGUMENTS_NUMBER) {
                    error(peek(), "Cant have more than $MAX_ARGUMENTS_NUMBER parameters.")
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters list.")
        consume(TokenType.LEFT_BRACE, "Expect '{' before lambda body.")

        val body = block()

        return Expr.Lambda(parameters, body)
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