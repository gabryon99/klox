package lox.backend

class LoxClass(
    val className: String,
    private val methods: Map<String, LoxFunction>,
    metaclass: LoxClass? = null) : LoxInstance(metaclass), LoxCallable {

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

    fun findMethod(name: String): LoxFunction? {

        if (methods.containsKey(name)) {
            return methods[name]
        }

        return null
    }
}
