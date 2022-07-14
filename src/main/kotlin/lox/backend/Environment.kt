package lox.backend

import lox.frontend.common.Token

class Environment(val enclosing: Environment? = null) {

    private val values: MutableMap<String, Any?> = mutableMapOf()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {

        if (values.contains(name.lexeme)) {
            // If variable wasn't initialized raise a runtime error
            return values[name.lexeme] ?: throw RuntimeError(name, "Variable '${name.lexeme}' was not initialized.")
        }

        if (enclosing != null) {
            return enclosing.get(name)
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token, value: Any?) {

        if (values.contains(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

}