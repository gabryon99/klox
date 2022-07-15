package lox.backend

import lox.frontend.common.Token

class LoxClass(val className: String, private val methods: Map<String, LoxFunction>) : LoxInstance(), LoxCallable {

    override fun arity(): Int {
        val initializer = findMethod("init")
        return initializer?.arity() ?: 0
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {

        val instance = LoxInstance(this)
        findMethod("init")?.bind(instance)?.call(interpreter, arguments)

        return instance
    }

    override fun toString(): String {
        return "<class $className>"
    }

    override fun get(name: Token): Any? {

        // Lookup for static method
        if (methods.containsKey(name.lexeme)) {

            val method = methods[name.lexeme]

            if (method != null) {
                if (method.isStatic()) {
                    return method
                }
                else {
                    throw RuntimeError(name, "Instance method '${name.lexeme}' cannot be invoked by class.")
                }
            }

        }

        throw RuntimeError(name, "Undefined property ${name.lexeme}.")
    }

    fun findMethod(name: String): LoxFunction? {

        if (methods.containsKey(name)) {
            return methods[name]
        }

        return null
    }
}
