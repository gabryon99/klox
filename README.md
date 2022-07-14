## klox

This is an implementation of the _Lox_ programming language, invented by _Robert Nystrom_ in his
_Crafting Interpreters_ book.

The interpreter is implemented using Kotlin instead of Java, this explains why the _k_ before the name.

### Example

```lox
fun main() {
    print "42 is the ultimate answer.";
}
```
### Grammar

```
program         ::=     declaration* EOF

declaration     ::=     varDecl | funDecl | statement

varDecl         ::=     "var" IDENTIFIER ("=" expression)? ";"
funDecl         ::      "fun" function

statement       ::=     exprStmr | printStmt | ifStmt | whileStmt | block | alterFlowStmt | returnStmt

block           ::=     "{" declaration* "}"
exprStmt        ::=     expression ";"
printStmt       ::=     "print" expression ";"
ifStmt          ::=     "if" "(" expression ")" statement ("else" statement)?
whileStmt       ::=     "while" "(" expression ")" statement
forStmt         ::=     "for" "(" (varDecl | expression) ";" expression? ";" expression? ")" loopStmt
alterFlowStmt   ::=     "break" ";" 
returnStmt      ::=     "return" expression? ";"

function        ::=     IDENTIFIER "(" paramaters? ")" block
paramaters      ::=     IDENTIFIER ("," IDENTIFIER)*

expression      ::=     assignment
assignment      ::=     IDENTIFIER "=" assignment | equality | comparison "?" expression ":" expression | logic_or
logic_or        ::=     logic_and ("or" logic_and)*
logic_and       ::=     equality ("and" equality)*
equality        ::=     comparison (("==" | "!=") comparison)*
comparison      ::=     term (("<" | "<=" | ">" | ">=") term)*
term            ::=     factor (("-" | "+") factor)*
factor          ::=     unary (("/" | "*") unary)*
unary           ::=     ("!" | "-") unary | call
call            ::=     primary ( "(" arguments ? ")" )*
arguments       ::=     expression ("," expression)*
primary         ::=     NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER | lambda
lambda          ::=     "fun" "(" parameters? ")" block
```