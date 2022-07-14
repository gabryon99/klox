package lox.backend

import lox.frontend.ast.Expr
import lox.frontend.ast.Stmt

class LoxLambdaFunction(private val declaration: Expr.Lambda, private val closure: Environment? = null) : LoxCallable {

    override fun arity(): Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {

        val environment = Environment(closure)
        for (i in 0 until declaration.params.size) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }

        try {
            interpreter.executeBlock(Stmt.Block(declaration.body), environment)
        }
        catch (returnValue: Return) {
            return returnValue.value
        }

        return null
    }

    override fun toString(): String {
        return "<fn anonymous>"
    }


}