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
        throw RuntimeError(name, "Undefined property ${name.lexeme}.")
    }
}
