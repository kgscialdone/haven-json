package com.tripl3dogdare.havenjson

internal fun String.jsonEscape() = fold("") { acc, c ->
  acc + when (c) {
    '\"' -> """\""""
    '\\' -> """\\"""
    '\n' -> """\n"""
    '\b' -> """\b"""
    '\r' -> """\r"""
    '\t' -> """\t"""
    '\u000C' -> """\f"""
    else -> c
  }
}

internal fun String.jsonUnescape() = this
  .replace(Regex("""\\([^u])""")) {
    // Single character escapes except \u
    when (it.groupValues[1]) {
      "n" -> "\n"
      "b" -> "\b"
      "r" -> "\r"
      "t" -> "\t"
      "f" -> "\u000c"
      else -> it.groupValues[1] // Also covers the cases of "\"", "\\"
    }
  }
  .replace(Regex("""\\u([0-9A-Fa-f]{4})""")) {
    // Unicode
    it.groupValues[1].toInt(16).toChar().toString()
  }

internal fun String.indent(spaces:Int=2) =
  split("\n").map { " ".repeat(spaces)+it }.joinToString("\n")

internal val String.head get() = getOrNull(0)
internal val String.tail get() = drop(1)
internal val <T, C : Collection<T>> C.head get():T? = elementAtOrNull(0)
internal val <T, C : Collection<T>> C.tail get():List<T> = drop(1)
internal operator fun Regex.contains(text:Char):Boolean = contains(text.toString())
internal operator fun Regex.contains(text:CharSequence):Boolean = this.matches(text)

internal fun String.camelize(sep:Char):String =
  this.toLowerCase().replace(Regex("$sep(\\w)")) { it.groups[1]!!.value.toUpperCase() }
internal fun String.decamelize(sep:Char):String =
  this.replace(Regex("(?!^)([A-Z])"), "$sep$1").toLowerCase()