package lox.backend

import lox.frontend.common.Token

class Environment {

    private val values: MutableMap<String, Any?> = mutableMapOf()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        if (values.contains(name.lexeme)) {
            return values[name.lexeme]
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token, value: Any?) {
        if (values.contains(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

}