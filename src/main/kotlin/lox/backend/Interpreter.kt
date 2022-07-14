package lox.backend

import lox.Lox
import lox.frontend.ast.Expr
import lox.frontend.ast.Stmt
import lox.frontend.common.Token
import lox.frontend.common.TokenType

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    private val environment = Environment()

    fun interpret(statements: List<Stmt?>) {
        try {
            statements.forEach {
                it?.let { execute(it) }
            }
        }
        catch (error: RuntimeError) {
            Lox.runtimeError(error)
        }
    }

    private fun execute(it: Stmt) {
        it.accept(this)
    }

    private fun stringify(value: Any?): String {

        if (value == null) return "null"

        if (value is Double) {
            var text = value.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
            }
            return text
        }

        return value.toString()
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {

        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.tokenType) {
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) - (right as Double)
            }
            TokenType.PLUS -> {

                if (left is Double && right is Double) {
                    left + right
                }
                else if (left is String && right is String) {
                    left + right
                }
                else {
                    throw RuntimeError(expr.operator, "Operands must be numbers or strings!")
                }
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                if ((right as Double).equals(0.toDouble())) {
                    // PANIC!
                    Lox.error(expr.operator.line, "Right operand evaluates to 0!")
                    0.toDouble()
                }
                else {
                    (left as Double) / right
                }
            }

            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) > (right as Double)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) >= (right as Double)
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < (right as Double)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) <= (right as Double)
            }

            TokenType.BANG_EQUAL -> {
                !isEqual(left, right)
            }

            TokenType.EQUAL_EQUAL -> {
                isEqual(left, right)
            }

            else -> {
                null
            }
        }

    }

    private fun isEqual(left: Any?, right: Any?): Boolean {
        if (left == null && right == null) return true
        if (left == null) return false

        return left == right
    }

    override fun visitTernaryExpr(expr: Expr.Ternary): Any? {

        // Short-circuit evaluation
        val cond = evaluate(expr.cond)
        if (isTruthy(cond)) {
            return evaluate(expr.thenBranch)
        }

        return evaluate(expr.elseBranch)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.exp)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return environment.get(expr.name)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {

        val right = evaluate(expr.right)
        return when (expr.operator.tokenType) {

            TokenType.BANG -> {
                !isTruthy(right)
            }

            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }

            else -> {
                null
            }
        }
    }

    private fun checkNumberOperand(operator: Token, right: Any?) {
        if (right !is Double) throw RuntimeError(operator, "Operand must be a number")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (right !is Double) throw RuntimeError(operator, "Right operand must be a number")
        if (left !is Double) throw RuntimeError(operator, "Left operand must be a number")
    }

    private fun isTruthy(right: Any?): Boolean {

        if (right == null) return false
        if (right is Boolean) {
            return right
        }

        return true
    }

    private fun evaluate(exp: Expr): Any? {
        return exp.accept(this)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        var value: Any? = null
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer)
        }

        environment.define(stmt.name.lexeme, value)
    }

}