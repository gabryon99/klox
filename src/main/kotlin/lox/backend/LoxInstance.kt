package lox.backend

class LoxInstance(private val loxClass: LoxClass) {

    override fun toString(): String {
        return "<class-instance ${loxClass.className}>"
    }
}
