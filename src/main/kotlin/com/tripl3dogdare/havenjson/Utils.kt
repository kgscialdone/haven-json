package com.tripl3dogdare.havenjson

internal tailrec fun String.jsonEscape(acc:String=""):String {
  if(this.isEmpty()) return acc
  return when(this[0]) {
    '\"' -> tail.jsonEscape("$acc\\\"")
    '\\' -> tail.jsonEscape("$acc\\\\")
    '\n' -> tail.jsonEscape("$acc\\n")
    '\b' -> tail.jsonEscape("$acc\\b")
    '\r' -> tail.jsonEscape("$acc\\r")
    '\t' -> tail.jsonEscape("$acc\\t")
    '\u000C' -> tail.jsonEscape("$acc\\f")
    else -> tail.jsonEscape(acc+this[0])
  }
}

internal tailrec fun String.jsonUnescape(acc:String=""):String {
  if(this.isEmpty()) return acc
  return if(this[0] == '\\') when(this[1]) {
    '"' -> drop(2).jsonUnescape("$acc\"")
    '\\' -> drop(2).jsonUnescape("$acc\\")
    'n' -> drop(2).jsonUnescape("$acc\n")
    'b' -> drop(2).jsonUnescape("$acc\b")
    'r' -> drop(2).jsonUnescape("$acc\r")
    't' -> drop(2).jsonUnescape("$acc\t")
    'f' -> drop(2).jsonUnescape("$acc\u000c")
    'u' -> drop(6).jsonUnescape(acc+this.drop(2).take(4).toInt(16).toChar())
    else -> drop(2).jsonUnescape(acc+this[1])
  } else tail.jsonUnescape(acc+this[0])
}

internal fun String.indent(spaces:Int=2) =
  split("\n").map { " ".repeat(spaces)+it }.joinToString("\n")

internal val String.head get() = getOrNull(0)
internal val String.tail get() = drop(1)
internal val <T, C : Collection<T>> C.head get():T? = elementAtOrNull(0)
internal val <T, C : Collection<T>> C.tail get():List<T> = drop(1)
internal operator fun Regex.contains(text:Char):Boolean = contains(text.toString())
internal operator fun Regex.contains(text:CharSequence):Boolean = this.matches(text)
