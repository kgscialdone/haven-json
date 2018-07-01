package com.tripl3dogdare.havenjson

/**
 * The main wrapper for a JSON-compatible value.
 * Implemented for each of the different JSON-supported data types.
 *
 * @author Connor Scialdone
 */
abstract class JsonValue<T> {
  /** The raw value wrapped by a given instance. */
  abstract val value:T

  /**
   * Get a value from a [JsonArray]. Returns JsonNull by default.
   * @param key The index of the array to access
   * @return [JsonNull]
   */
  open operator fun get(key:Int):Json = JsonNull

  /**
   * Get a value from a [JsonObject]. Returns JsonNull by default.
   * @param key The string key of the object to access
   * @return [JsonNull]
   */
  open operator fun get(key:String):Json = JsonNull

  /**
   * Get the wrapped value as a specific type.
   * Returns null if the value cannot be cast to the given type.
   * @param K The type to cast to
   * @return value as K / null
   */
  inline fun <reified K> value() =
    if(value is K) value as K else null

  /** Get the wrapped value as an Int (convenience method for [value]). */
  val asInt:Int? get() = value<Int>()
  /** Get the wrapped value as a Float (convenience method for [value]). */
  val asFloat:Float? get() = value<Float>()
  /** Get the wrapped value as a String (convenience method for [value]). */
  val asString:String? get() = value<String>()
  /** Get the wrapped value as a Boolean (convenience method for [value]). */
  val asBoolean:Boolean? get() = value<Boolean>()
  /** Get the wrapped value as a List<Json> (convenience method for [value]). */
  val asList:List<Json>? get() = value<List<Json>>()
  /** Get the wrapped value as a Map<String, Json> (convenience method for [value]). */
  val asMap:Map<String, Json>? get() = value<Map<String, Json>>()

  /**
   * Convert this JSON node to a string containing valid JSON.
   * @param indent The indent depth in spaces (default: 2, 0 = minify)
   * @return The converted string
   */
  open fun mkString(indent:Int=2) = value.toString()

  override fun equals(other:Any?) =
    super.equals(other) || value == other ||
    other is Json && value == other.value

  companion object {
    /**
     * DSL method for wrapping a value.
     * Recursively calls itself for elements of Lists/Maps.
     * Calls [toString] on any unknown types.
     *
     * @param v The value to wrap
     * @return The wrapped [JsonValue]
     */
    operator fun invoke(v:Any?):Json = when(v) {
      is Json -> v
      is Int -> JsonInt(v)
      is Float -> JsonFloat(v)
      is String -> JsonString(v)
      is Boolean -> JsonBoolean(v)
      is Collection<*> -> JsonArray(v.map(::invoke))
      is Map<*,*> -> JsonObject(v.entries.associate { it.key.toString() to invoke(it.value) })
      null -> JsonNull
      else -> JsonString(v.toString())
    }

    /**
     * DSL method for wrapping a value.
     * Removes the need to wrap contents in [mapOf] when creating a [JsonObject].
     * @see [invoke]
     */
    operator fun invoke(vararg vs:Pair<String, Any?>):Json = invoke(vs.toMap())

    /**
     * DSL method for wrapping a value.
     * Removes the need to wrap contents in [listOf] when creating a [JsonArray].
     * @see [invoke]
     */
    operator fun invoke(vararg vs:Any?):Json = invoke(vs.toList())

    /**
     * Parses a JSON value from a string.
     * @see [JsonParser.parse]
     */
    fun parse(from:String) = JsonParser.parse(from)
  }
}

/** Type alias for [JsonValue]<*> */
typealias Json = JsonValue<*>
