package lox.backend

import lox.frontend.ast.Stmt

class LoxFunction(private val declaration: Stmt.Function, private val closure: Environment? = null, private val isInitializer: Boolean = false) : LoxCallable {

    fun isStatic() = declaration.isStatic

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
            if (isInitializer) return closure?.getAt(0, "this")
            return returnValue.value
        }

        if (isInitializer) return closure?.getAt(0, "this")

        return null
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }

    fun bind(loxInstance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", loxInstance)
        return LoxFunction(declaration, environment)
    }


}