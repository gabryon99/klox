package lox.backend

class LoxClass(val className: String, private val methods: Map<String, LoxFunction>) : LoxCallable {

    override fun arity(): Int = 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        return LoxInstance(this)
    }

    override fun toString(): String {
        return "<class $className>"
    }

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]
        }

        return null
    }
}
