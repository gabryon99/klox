package tool

import java.io.PrintWriter
import kotlin.system.exitProcess

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    writer.println("\tclass $className($fieldList) : $baseName() {")
    writer.println("\t\toverride fun <R> accept(visitor: Visitor<R>): R = visitor.visit$className$baseName(this)")
    writer.println("\t}\n")
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
    writer.println("\tinterface Visitor<R> {")

    for (type in types) {
        val className = type.split(";")[0].trim()
        writer.println("\t\tfun visit$className$baseName(${baseName.lowercase()}: $className): R")
    }

    writer.println("\t}\n")
}

fun defineAst(outputDirectory: String, filename: String, types: List<String>) {

    val path = "$outputDirectory/$filename.kt"
    val writer = PrintWriter(path, "UTF-8")

    writer.println("package lox.frontend.ast\n")
    writer.println("import lox.frontend.common.Token\n")
    writer.println("abstract class $filename {\n ")

    defineVisitor(writer, filename, types)

    for (type in types) {

        val split = type.split(";")
        val className = split[0].trim()
        val params = split[1].trim()
        defineType(writer, filename, className, params)
    }

    // The base accept method
    writer.println("\tabstract fun <R> accept(visitor: Visitor<R>): R")

    writer.println("}")

    writer.close()
}

fun main(args: Array<String>) {

    if (args.size != 1) {
        System.err.println("usage: generate_ast <output_directory>")
        exitProcess(64)
    }

    val outputDir = args[0]
    defineAst(outputDir, "Expr", listOf(
        "Binary    ; val left: Expr, val operator: Token, val right: Expr",
        "Ternary   ; val cond: Expr, val thenBranch: Expr, val elseBranch: Expr",
        "Grouping  ; val exp: Expr",
        "Literal   ; val value: Any?",
        "Unary     ; val operator: Token, val right: Expr"
    ))

    defineAst(outputDir, "Stmt", listOf(
        "Expression    ; val expr: Expr",
        "Print         ; val expr: Expr"
    ))
}