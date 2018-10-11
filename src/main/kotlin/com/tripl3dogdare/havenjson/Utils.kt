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

internal fun String.jsonUnescape() = replaceAll(mapOf(
  """\"""" to "\"",
  """\\""" to "\\",
  """\n""" to "\n",
  """\b""" to "\b",
  """\r""" to "\r",
  """\t""" to "\t",
  """\f""" to "\u000c"))
  .replace(Regex("""\\u([0-9A-Fa-f]{4})""")) {
    it.groupValues[1].toInt(16).toChar().toString()
  }
  // Anything else - return as is (without the '\').
  .replace(Regex("""\\([^"\\nbrtfu])""")) { it.groupValues[1] }

internal fun String.indent(spaces:Int=2) =
  split("\n").map { " ".repeat(spaces)+it }.joinToString("\n")

internal val String.head get() = getOrNull(0)
internal val String.tail get() = drop(1)
internal val <T, C : Collection<T>> C.head get():T? = elementAtOrNull(0)
internal val <T, C : Collection<T>> C.tail get():List<T> = drop(1)
internal operator fun Regex.contains(text:Char):Boolean = contains(text.toString())
internal operator fun Regex.contains(text:CharSequence):Boolean = this.matches(text)

private fun String.replaceAll(mapping: Map<String, String>) =
  mapping.entries.fold(this) { acc, entry ->
    acc.replace(entry.key, entry.value)
  }
