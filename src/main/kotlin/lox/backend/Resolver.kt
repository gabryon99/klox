package lox.backend

import lox.Lox
import lox.frontend.ast.Expr
import lox.frontend.ast.Stmt
import lox.frontend.common.Token
import java.util.*
import kotlin.collections.HashMap

class Resolver(private val interpreter: Interpreter): Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    private val scopes: Stack<HashMap<String, Boolean>> = Stack();
    private var currentFunction = FunctionType.NONE
    private var loopDepth = 0

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    fun resolve(stmts: List<Stmt?>) {
        stmts.forEach { it?.let { resolve(it) } }
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        val scope = scopes.peek()

        // Avoid name collision in the same scope (alias: no shadowing)
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable with this name in this scope.")
        }

        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.peek()[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.size - 1 downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(stmt: Stmt.Function, type: FunctionType) {

        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        stmt.params.forEach {
            declare(it)
            define(it)
        }
        resolve(stmt.body)
        endScope()

        currentFunction = enclosingFunction
    }

    private fun resolveLambda(stmt: Expr.Lambda, type: FunctionType) {

        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        stmt.params.forEach {
            declare(it)
            define(it)
        }
        resolve(stmt.body)
        endScope()

        currentFunction = enclosingFunction

    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitTernaryExpr(expr: Expr.Ternary) {
        resolve(expr.cond)
        resolve(expr.thenBranch)
        resolve(expr.elseBranch)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        expr.arguments.forEach {
            resolve(it)
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.exp)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
        return
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr)
    }

    override fun visitLambdaExpr(expr: Expr.Lambda) {
        resolveLambda(expr, FunctionType.FUNCTION)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expr)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expr)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.stmts)
        endScope()
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        loopDepth++
        resolve(stmt.condition)
        resolve(stmt.body)
        loopDepth--
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        if (loopDepth == 0) {
            Lox.error(stmt.token, "Break statements can be inside only loops.")
        }
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {

        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.")
        }

        if (stmt.value != null) resolve(stmt.value)
    }


}