package lox.backend

import lox.frontend.common.Token

class RuntimeError(val token: Token, override val message: String) : RuntimeException(message) {

}