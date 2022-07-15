package lox.backend

class LoxClass(val className: String) : LoxCallable {

    override fun arity(): Int = 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        return LoxInstance(this)
    }

    override fun toString(): String {
        return "<class $className>"
    }
}
