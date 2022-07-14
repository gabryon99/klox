package lox.backend

data class Return(val value: Any?) : RuntimeException(null, null, false, false)
