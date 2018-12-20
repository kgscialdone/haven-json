package com.tripl3dogdare.havenjson

/**
 * Provides methods for parsing JSON from a string.
 *
 * @author Connor Scialdone
 */
object JsonParser {
  private sealed class Token(val text: kotlin.String) {
    object ObjectBegin : Token("{")
    object ObjectEnd : Token("}")
    object ArrayBegin : Token("[")
    object ArrayEnd : Token("]")
    object Comma : Token(",")
    object Colon : Token(":")
    object True : Token("true")
    object False : Token("false")
    object Null : Token("null")

    class Int(text: kotlin.String) : Token(text)
    class Float(text: kotlin.String) : Token(text)
    class String(text: kotlin.String) : Token(text)

    override fun toString() = text
  }

  /** Thrown by [parse] when a parsing error occurs. */
  class JsonParseError(message: String) : Exception(message)

  /**
   * Parses JSON from a string.
   *
   * @param from The source string
   * @return The parsed [Json]
   */
  fun parse(from: String): Json {
    val (parsed, tail) = parse(lex(from))
    if(tail.isNotEmpty())
      throw JsonParseError("Input string contains unexpected tokens before EOF")
    return parsed
  }

  private tailrec fun lex(from: String, tokens: List<Token> = emptyList()): List<Token> = when (from.head) {
    null -> tokens
    in Regex("\\s") -> lex(from.trim(), tokens)
    '{' -> lex(from.tail, tokens + Token.ObjectBegin)
    '}' -> lex(from.tail, tokens + Token.ObjectEnd)
    '[' -> lex(from.tail, tokens + Token.ArrayBegin)
    ']' -> lex(from.tail, tokens + Token.ArrayEnd)
    ',' -> lex(from.tail, tokens + Token.Comma)
    ':' -> lex(from.tail, tokens + Token.Colon)

    '"' -> {
      val str = run loop@{ from.tail.fold("") { acc, c ->
        if(c == '"' && !acc.endsWith('\\')) return@loop acc
        acc + c
      }}

      lex(from.drop(str.length + 2), tokens + Token.String(str))
    }

    in Regex("\\d"), '-' -> {
      val num = run loop@{ from.tail.fold(from.head.toString()) { acc, c ->
        if(c == '-' || c in Regex("[.eE]") && acc.contains(c, true))
          throw JsonParseError("Unexpected $c while parsing number")
        if(c !in Regex("[.eE\\d]"))
          return@loop acc
        acc + c
      }}

      if(num.contains(Regex("[.eE]"))) lex(from.drop(num.length), tokens + Token.Float(num))
      else lex(from.drop(num.length), tokens + Token.Int(num))
    }

    else -> when {
      from.startsWith("true") -> lex(from.drop(4), tokens + Token.True)
      from.startsWith("false") -> lex(from.drop(5), tokens + Token.False)
      from.startsWith("null") -> lex(from.drop(4), tokens + Token.Null)
      else -> throw JsonParseError("Unexpected ${from.head} when parsing JSON")
    }
  }

  private fun parse(tokens: List<Token>): Pair<Json, List<Token>> = when (tokens.head) {
    Token.ObjectBegin -> {
      var tail = tokens.tail
      var pairs: List<Pair<String, Json>> = emptyList()

      while (tail.head !is Token.ObjectEnd) {
        if (tail.size < 4)
          throw JsonParseError("Unexpected EOF when parsing object")
        val key = tail.head
        if (key == null || key !is Token.String || tail.drop(1).head !is Token.Colon)
          throw JsonParseError("Objects must follow the structure {\"key1\":value1,\"key2\":value2}")

        val (value, newTail) = parse(tail.drop(2))
        pairs += key.text to value
        tail = if (newTail.head is Token.Comma) newTail.tail else newTail
      }

      JsonObject(pairs.toMap()) to tail.tail
    }

    Token.ArrayBegin -> {
      var tail = tokens.tail
      var list: List<Json> = emptyList()

      while (tail.head !is Token.ArrayEnd) {
        if (tail.size < 2)
          throw JsonParseError("Unexpected EOF when parsing array")

        val (value, newTail) = parse(tail)
        list += value
        tail = if (newTail.head is Token.Comma) newTail.tail else newTail
      }

      JsonArray(list) to tail.tail
    }

    Token.True -> JsonBoolean(true) to tokens.tail
    Token.False -> JsonBoolean(false) to tokens.tail
    Token.Null -> JsonNull to tokens.tail

    is Token.Int ->
      tokens.head!!.text
        .let { it.toIntOrNull()?.let(::JsonInt) ?: JsonLong(it.toLong()) }
        .to(tokens.tail)
    is Token.Float -> JsonFloat(tokens.head!!.text.toFloat()) to tokens.tail
    is Token.String -> JsonString(tokens.head!!.text.jsonUnescape()) to tokens.tail

    else ->
      throw JsonParseError("Unexpected ${tokens.head} when parsing JSON")
  }
}
