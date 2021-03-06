package lox.backend

import lox.Lox
import lox.frontend.ast.Expr
import lox.frontend.ast.Stmt
import lox.frontend.common.Token
import java.util.*
import kotlin.collections.HashMap

class Resolver(private val interpreter: Interpreter): Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    private val scopes: Stack<HashMap<String, Boolean>> = Stack()
    private val usedVariables = mutableMapOf<Token, Boolean>()

    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE
    private var loopDepth = 0

    fun globalResolve(stmts: List<Stmt?>) {
        beginScope()
        resolve(stmts)
        checkUnusedVariables()
        endScope()
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(stmts: List<Stmt?>) {
        stmts.forEach { it?.let { resolve(it) } }
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun checkUnusedVariables() {
        // Is there a variable defined but not initialized?
        usedVariables.forEach {
            if (!it.value) {
                Lox.error(it.key, "Variable '${it.key.lexeme}' declared but never used.")
            }
        }
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
        stmt.params?.forEach {
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
        markUsedVariable(expr)
        resolveLocal(expr, expr.name)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr)
    }

    override fun visitLambdaExpr(expr: Expr.Lambda) {
        resolveLambda(expr, FunctionType.FUNCTION)
    }

    override fun visitGetExpr(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitSuperExpr(expr: Expr.Super) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'super' outside of a class.")
        }
        else if (currentClass == ClassType.CLASS) {
            Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass.")
        }
        resolveLocal(expr, expr.keyword)
    }

    override fun visitThisExpr(expr: Expr.This) {

        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.")
            return
        }

        resolveLocal(expr, expr.keyword)
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
        markUsedVariable(stmt)
    }

    private fun markUsedVariable(stmt: Stmt.Var) {
        if (!usedVariables.containsKey(stmt.name)) {
            usedVariables[stmt.name] = false
        }
    }

    private fun markUsedVariable(expr: Expr.Variable) {
        if (usedVariables.containsKey(expr.name)) {
            usedVariables[expr.name] = true
        }
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

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer.")
            }
            resolve(stmt.value)
        }
    }

    override fun visitClassStmt(stmt: Stmt.Class) {

        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)

        if (stmt.superclass != null) {
            currentClass = ClassType.SUBCLASS
            resolve(stmt.superclass)
        }

        if (stmt.superclass != null) {
            beginScope()
            scopes.peek()["super"] = true
        }

        beginScope()

        scopes.peek()["this"] = true
        stmt.methods.forEach {
            val declaration = if (it.name.lexeme == "init") {
               FunctionType.INITIALIZER
            }
            else {
                FunctionType.METHOD
            }
            resolveFunction(it, declaration)
        }
        stmt.classMethods.forEach {
            resolveFunction(it, FunctionType.STATIC_METHOD)
        }

        endScope()

        if (stmt.superclass != null) {
            endScope()
        }

        define(stmt.name)
        if (stmt.superclass != null && stmt.superclass.name.lexeme == stmt.name.lexeme) {
            Lox.error(stmt.superclass.name, "A class can't inherit from itself.")
        }

        currentClass = enclosingClass
    }
}