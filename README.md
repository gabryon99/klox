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
program     ::=     declaration* EOF

declaration ::=     varDecl | statement

varDecl     ::=     "var" IDENTIFIER ("=" expression)? ";"

statement   ::=     exprStmr | printStmt | block

block       ::=     "{" declaration* "}"
exprStmt    ::=     expression ";"
printStmt   ::=     "print" expression ";"

expression  ::=     assignment
assignment  ::=     IDENTIFIER "=" assignment | equality | comparison "?" expression ":" expression
equality    ::=     comparison (("==" | "!=") comparison)*
comparison  ::=     term (("<" | "<=" | ">" | ">=") term)*
term        ::=     factor (("-" | "+") factor)*
factor      ::=     unary (("/" | "*") unary)*
unary       ::=     ("!" | "-") unary | primary
primary     ::=     NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER
```