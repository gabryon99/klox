package lox.backend

import lox.Lox
import lox.frontend.ast.Expr
import lox.frontend.ast.Stmt
import lox.frontend.common.Token
import lox.frontend.common.TokenType

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    private val globals = Environment()
    private var environment = globals
    private val locals = mutableMapOf<Expr, Int>()

    companion object {
        fun stringify(value: Any?): String {

            if (value == null) return "null"

            if (value is Double) {
                var text = value.toString()
                if (text.endsWith(".0")) {
                    text = text.substring(0, text.length - 2)
                }
                return text
            }

            return value.toString()
        }
    }

    init {
        globals.define("clock", object: LoxCallable {
            override fun arity(): Int = 0

            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
                return (System.currentTimeMillis().toDouble() / 1000.0)
            }
        })
        globals.define("stringify", object: LoxCallable {
            override fun arity(): Int = 1
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                return stringify(arguments[0])
            }
        })
    }

    fun interpret(statements: List<Stmt?>) {
        try {
            statements.forEach {
                it?.let { execute(it) }
            }
        }
        catch (error: RuntimeError) {
            Lox.runtimeError(error)
        }
    }

    private fun execute(it: Stmt) {
        it.accept(this)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {

        val value = evaluate(expr.value)

        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        }
        else {
            globals.assign(expr.name, value)
        }

        return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {

        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.tokenType) {
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) - (right as Double)
            }
            TokenType.PLUS -> {

                if (left is Double && right is Double) {
                    left + right
                }
                else if (left is String && right is String) {
                    left + right
                }
                else {
                    throw RuntimeError(expr.operator, "Operands must be numbers or strings!")
                }
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                if ((right as Double).equals(0.toDouble())) {
                    // PANIC!
                    Lox.error(expr.operator.line, "Right operand evaluates to 0!")
                    0.toDouble()
                }
                else {
                    (left as Double) / right
                }
            }

            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) > (right as Double)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) >= (right as Double)
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < (right as Double)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) <= (right as Double)
            }

            TokenType.BANG_EQUAL -> {
                !isEqual(left, right)
            }

            TokenType.EQUAL_EQUAL -> {
                isEqual(left, right)
            }

            else -> {
                null
            }
        }

    }

    private fun isEqual(left: Any?, right: Any?): Boolean {
        if (left == null && right == null) return true
        if (left == null) return false

        return left == right
    }

    override fun visitTernaryExpr(expr: Expr.Ternary): Any? {

        // Short-circuit evaluation
        val cond = evaluate(expr.cond)
        if (isTruthy(cond)) {
            return evaluate(expr.thenBranch)
        }

        return evaluate(expr.elseBranch)
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {

        val callee = evaluate(expr.callee)
        val arguments = mutableListOf<Any?>()

        expr.arguments.forEach {
            // Eager semantic
            arguments.add(evaluate(it))
        }

        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        }

        val function = (callee)
        if (arguments.size != function.arity()) {
            throw RuntimeError(expr.paren, "Expected ${function.arity()} arguments but got ${arguments.size}.")
        }

        return function.call(this, arguments)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.exp)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {

        val left = evaluate(expr.left)

        if (expr.operator.tokenType == TokenType.OR) {
            if (isTruthy(left)) return left
        }
        else {
            if (!isTruthy(left)) return left
        }

        return evaluate(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookupVariable(expr.name, expr)
    }

    private fun lookupVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        if (distance != null) {
            return environment.getAt(distance, name.lexeme)
        }
        return globals.get(name)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {

        val right = evaluate(expr.right)
        return when (expr.operator.tokenType) {

            TokenType.BANG -> {
                !isTruthy(right)
            }

            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }

            else -> {
                null
            }
        }
    }

    override fun visitLambdaExpr(expr: Expr.Lambda): Any {
        return LoxLambdaFunction(expr, environment)
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {

        val obj = evaluate(expr.obj)

        if (obj is LoxInstance) {
            return obj.get(expr.name)
        }

        throw RuntimeError(expr.name, "Only instances of classes have properties.")
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {

        val obj = evaluate(expr.obj)

        if (obj !is LoxInstance) {
            throw RuntimeError(expr.name, "Only instances have fields.")
        }

        val value = evaluate(expr.value)
        obj.set(expr.name, value)

        return value
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookupVariable(expr.keyword, expr)
    }

    private fun checkNumberOperand(operator: Token, right: Any?) {
        if (right !is Double) throw RuntimeError(operator, "Operand must be a number")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (right !is Double) throw RuntimeError(operator, "Right operand must be a number")
        if (left !is Double) throw RuntimeError(operator, "Left operand must be a number")
    }

    private fun isTruthy(right: Any?): Boolean {

        if (right == null) return false
        if (right is Boolean) return right

        return true
    }

    fun evaluate(exp: Expr): Any? {
        return exp.accept(this)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expr)
    }

    override fun visitIfStmt(stmt: Stmt.If) {

        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        }
        else {
            stmt.elseBranch?.let {
                execute(it)
            }
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        var value: Any? = null
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer)
        }

        environment.define(stmt.name.lexeme, value)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt, Environment(environment))
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
       try {
           while (isTruthy(evaluate(stmt.condition))) {
               execute(stmt.body)
           }
       }
       catch (rtBreak: RuntimeBreak) {
           return
        }
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        throw RuntimeBreak()
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value: Any? = if (stmt.value != null) {
            evaluate(stmt.value)
        } else { null }

        throw Return(value)
    }

    override fun visitClassStmt(stmt: Stmt.Class) {

        environment.define(stmt.name.lexeme, null)

        // Let's build metaclass
        val classMethods = mutableMapOf<String, LoxFunction>()
        stmt.classMethods.forEach {
            val function = LoxFunction(it, environment)
            classMethods[it.name.lexeme] = function
        }

        val metaclass = LoxClass("${stmt.name.lexeme}-metaclass", classMethods)

        // Build normal class
        val methods = mutableMapOf<String, LoxFunction>()
        stmt.methods.forEach {
            val function = LoxFunction(it, environment, it.name.lexeme == "init")
            methods[it.name.lexeme] = function
        }

        val clazz = LoxClass(stmt.name.lexeme, methods, metaclass)
        environment.assign(stmt.name, clazz)

        return
    }

    fun executeBlock(stmt: Stmt.Block, environment: Environment) {

        val previous = this.environment

        try {
            this.environment = environment

            stmt.stmts.forEach {
                it?.let { execute(it) }
            }

        } finally {
            this.environment = previous
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

}