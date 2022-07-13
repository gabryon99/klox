package lox.frontend.ast

import lox.frontend.common.Token
import lox.frontend.common.TokenType

/***
 * Pretty print an Expr AST in Infix Polish Notation (IPN) way.
 */
class AstIpnPrinter : Expr.Visitor<String> {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exp = Expr.Binary(
                Expr.Grouping(
                    Expr.Binary(
                        Expr.Literal(3),
                        Token(TokenType.STAR, "*", null, 1),
                        Expr.Literal(4)
                    )
                ),
                Token(TokenType.PLUS, "+", null, 1),
                Expr.Literal(30)
            )

            println(AstIpnPrinter().print(exp))
        }
    }

    fun print(expr: Expr) : String = expr.accept(this)

    override fun visitBinaryExpr(expr: Expr.Binary): String {

        val leftResult = expr.left.accept(this)
        val rightResult = expr.right.accept(this)

        return "$leftResult ${expr.operator.lexeme} $rightResult"
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        val exprVal = expr.exp.accept(this)
        return "( $exprVal )"
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        if (expr.value == null) return "nil";
        return expr.value.toString()
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        val resOperand = expr.right.accept(this)
        return "${expr.operator.lexeme}$resOperand"
    }

}