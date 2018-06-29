package com.tripl3dogdare.havenjson

typealias Json = JsonValue<*>
abstract class JsonValue<T> {
  abstract val value:T

  open operator fun get(key:Int):Json = JsonNull
  open operator fun get(key:String):Json = JsonNull

  inline fun <reified K> value() =
    if(value is K) value as K else null

  val asInt:Int? get() = value<Int>()
  val asFloat:Float? get() = value<Float>()
  val asString:String? get() = value<String>()
  val asBoolean:Boolean? get() = value<Boolean>()
  val asList:List<Json>? get() = value<List<Json>>()
  val asMap:Map<String, Json>? get() = value<Map<String, Json>>()

  open fun mkString(indent:Int=2) = value.toString()
  override fun equals(other:Any?) =
    super.equals(other) || value == other ||
    other is Json && value == other.value

  companion object {
    operator fun invoke(v:Any?):Json = when(v) {
      is Json -> v
      is Int -> JsonInt(v)
      is Float -> JsonFloat(v)
      is String -> JsonString(v)
      is Boolean -> JsonBoolean(v)
      is Collection<*> -> JsonArray(v.map(::invoke))
      is Map<*,*> -> JsonObject(v.entries.associate { it.key.toString() to invoke(it.value) })
      null -> JsonNull
      else -> throw IllegalArgumentException()
    }

    operator fun invoke(vararg vs:Pair<String, Any?>):Json = invoke(vs.toMap())
    operator fun invoke(vararg vs:Any?):Json = invoke(vs.toList())

    fun parse(from:String) = JsonParser.parse(from)
  }
}
