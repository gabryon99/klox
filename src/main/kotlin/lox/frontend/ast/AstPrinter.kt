package lox.frontend.ast

import lox.frontend.common.Token
import lox.frontend.common.TokenType

class AstPrinter : Expr.Visitor<String> {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exp = Expr.Binary(
                Expr.Unary(
                    Token(TokenType.MINUS, "-", null, 1),
                    Expr.Literal(123)
                ),
                Token(TokenType.STAR, "*", null, 1),
                Expr.Grouping(Expr.Literal(45.67))
            )

            println(AstPrinter().print(exp))
        }
    }

    fun print(expr: Expr) : String = expr.accept(this)

    private fun parenthesize(name: String, vararg exprs: Expr): String {

        val stringBuilder = StringBuilder()
        stringBuilder.append("(").append(name)

        for (expr in exprs) {
            stringBuilder.append(" ")
            stringBuilder.append(expr.accept(this))
        }
        stringBuilder.append(")")

        return stringBuilder.toString()
    }

    override fun visitAssignExpr(expr: Expr.Assign): String {
        TODO("Not yet implemented")
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitTernaryExpr(expr: Expr.Ternary): String {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return parenthesize("group", expr.exp)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        if (expr.value == null) return "nil"
        return expr.value.toString()
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        TODO("Not yet implemented")
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

}