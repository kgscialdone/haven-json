package com.tripl3dogdare.havenjson

/**
 * JSON-boxed Int
 * @see [JsonValue]
 */
data class JsonInt(override val value:Int) : JsonValue<Int>()

/**
 * JSON-boxed Float
 * @see [JsonValue]
 */
data class JsonFloat(override val value:Float) : JsonValue<Float>()

/**
 * JSON-boxed Boolean
 * @see [JsonValue]
 */
data class JsonBoolean(override val value:Boolean) : JsonValue<Boolean>()

/**
 * JSON-boxed String
 * @see [JsonValue]
 */
data class JsonString(override val value:String) : JsonValue<String>() {
  override fun mkString(indent:Int) = "\"${value.jsonEscape()}\""
}

/**
 * JSON-boxed List<Json>
 * @see [JsonValue]
 */
data class JsonArray(override val value:List<Json>) : JsonValue<List<Json>>() {
  /**
   * Get a value from this array by index.
   * Returns [JsonNull] if the index does not exist.
   *
   * @param key The index to retrieve
   * @return The [JsonValue] at the given index, or [JsonNull]
   */
  override fun get(key:Int) = value.getOrElse(key){JsonNull}

  override fun mkString(indent:Int):String {
    val n = if(indent > 0) "\n" else ""
    return value
      .map { it.mkString(indent).indent(indent) }
      .joinToString(",$n", "[$n", "$n]")
  }

  override fun equals(other:Any?):Boolean {
    if(super.equals(other)) return true
    if(other !is JsonArray) return false
    return value.zip(other.value).all { it.first == it.second }
  }
  override fun hashCode() = super.hashCode()
}

/**
 * JSON-boxed Map<String, Json>
 * @see [JsonValue]
 */
data class JsonObject(override val value:Map<String, Json>) : JsonValue<Map<String, Json>>() {
  /**
   * Get a value from this object by key.
   * Returns [JsonNull] if the key does not exist.
   *
   * @param key The key to retrieve
   * @return The [JsonValue] at the given key, or [JsonNull]
   */
  override fun get(key:String) = value.getOrElse(key){JsonNull}

  override fun mkString(indent:Int):String {
    val n = if(indent > 0) "\n" else ""
    val s = if(indent > 0) " " else ""
    return value
      .map { (k,v) -> "\"$k\":$s${v.mkString(indent)}".indent(indent) }
      .joinToString(",$n", "{$n", "$n}")
  }

  override fun equals(other:Any?):Boolean {
    if(super.equals(other)) return true
    if(other !is JsonObject) return false
    return value.entries.zip(other.value.entries).all {
      it.first.key == it.second.key && it.first == it.second }
  }
  override fun hashCode() = super.hashCode()
}

/**
 * JSON-boxed null
 * @see [JsonValue]
 */
object JsonNull : JsonValue<Nothing?>() {
  override val value = null
  override fun mkString(indent:Int) = "null"
}
