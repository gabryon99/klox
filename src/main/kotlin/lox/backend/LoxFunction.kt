package lox.backend

import lox.frontend.ast.Stmt

class LoxFunction(private val declaration: Stmt.Function) : LoxCallable {

    override fun arity(): Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {

        val environment = Environment(interpreter.globals)
        for (i in 0 until declaration.params.size) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }

        interpreter.executeBlock(Stmt.Block(declaration.body), environment)

        return null
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }


}