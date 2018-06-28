package com.tripl3dogdare.havenjson

object JsonParser {
  sealed class Token(val text: kotlin.String) {
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

    object Noop : Token("")
  }

  class JsonParseError(message: String) : Exception(message)

  fun parse(from: String): Json {
    val (parsed, tail) = parse(lex(from))
    if (tail.any { it != Token.Noop })
      throw JsonParseError("Input string contains unexpected tokens before EOF")
    return parsed
  }

  private tailrec fun lex(from: String, tokens: List<Token> = emptyList()): List<Token> = when (from.head) {
    null -> tokens + Token.Noop
    in Regex("\\s") -> lex(from.trim(), tokens)
    '{' -> lex(from.tail, tokens + Token.ObjectBegin)
    '}' -> lex(from.tail, tokens + Token.ObjectEnd)
    '[' -> lex(from.tail, tokens + Token.ArrayBegin)
    ']' -> lex(from.tail, tokens + Token.ArrayEnd)
    ',' -> lex(from.tail, tokens + Token.Comma)
    ':' -> lex(from.tail, tokens + Token.Colon)

    '"' -> {
      var last = '\u0000'
      val tail = from.tail.takeWhile { val t = last == '\\' || it != '"'; last = it; t }
      lex(from.drop(tail.length + 2), tokens + Token.String(tail))
    }

    in Regex("\\d"), '-' -> {
      var acc = ""
      val tail = from.dropWhile {
        when {
          (it == '-' && acc != "") ||
            (it == '.' && acc.contains('.')) ||
            ((it == 'e' || it == 'E') && acc.contains('e', true)) ->
            throw JsonParseError("Unexpected $it while parsing number")
          it in Regex("[-.eE\\d]") -> {
            acc += it; true
          }
          else -> false
        }
      }

      if (acc.contains(Regex("[.eE]"))) lex(tail, tokens + Token.Float(acc))
      else lex(tail, tokens + Token.Int(acc))
    }

    else -> when {
      from.startsWith("true") -> lex(from.drop(4), tokens + Token.True)
      from.startsWith("false") -> lex(from.drop(5), tokens + Token.False)
      from.startsWith("null") -> lex(from.drop(4), tokens + Token.Null)
      else -> throw JsonParseError("Unexpected ${from.head} when parsing JSON")
    }
  }

  private tailrec fun parse(tokens: List<Token>): Pair<Json, List<Token>> = when (tokens.head) {
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
    Token.Noop -> parse(tokens.tail)

    is Token.Int -> JsonInt(tokens.head!!.text.toInt()) to tokens.tail
    is Token.Float -> JsonFloat(tokens.head!!.text.toFloat()) to tokens.tail
    is Token.String -> JsonString(tokens.head!!.text.jsonUnescape()) to tokens.tail

    else ->
      throw JsonParseError("Unexpected ${tokens.head} when parsing JSON")
  }
}
