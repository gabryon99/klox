package lox.backend

import lox.frontend.common.Token

class LoxInstance(private val loxClass: LoxClass) {

    private val fields = mutableMapOf<String, Any?>()

    override fun toString(): String {
        return "<class-instance ${loxClass.className}>"
    }

    fun get(name: Token): Any? {

        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }

        val method = loxClass.findMethod(name.lexeme)
        if (method != null) {
            return method.bind(this)
        }

        throw RuntimeError(name, "Undefined property ${name.lexeme}.")
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}
