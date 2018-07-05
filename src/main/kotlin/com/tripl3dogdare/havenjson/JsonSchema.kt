package com.tripl3dogdare.havenjson

import kotlin.reflect.*
import kotlin.reflect.full.*

interface JsonSchema {
  companion object {
    val AS_WRITTEN:NameConverter = { it }
    val UPPERCASE:NameConverter = String::toUpperCase
    val LOWERCASE:NameConverter = String::toLowerCase
    val CAMEL_TO_SNAKE:NameConverter =
      { it.replace(Regex("([A-Z])"), "_$1").toLowerCase() }
    val SNAKE_TO_CAMEL:NameConverter =
      { it.toLowerCase().replace(Regex("_(\\w)")) { it.groups[1]!!.value.toUpperCase() }}
  }
}

fun <T: JsonSchema> JsonValue.Companion.deserialize(clazz:KClass<T>, raw:String) = deserialize(clazz, Json.parse(raw))
fun <T: JsonSchema> JsonValue.Companion.deserialize(clazz:KClass<T>, raw:Json) = deserialize(clazz, raw, JsonSchema.AS_WRITTEN)
fun <T: JsonSchema> JsonValue.Companion.deserialize(clazz:KClass<T>, raw:String, nameConverter:NameConverter) = deserialize(clazz.primaryConstructor!!, Json.parse(raw), nameConverter)
fun <T: JsonSchema> JsonValue.Companion.deserialize(clazz:KClass<T>, raw:Json, nameConverter:NameConverter) = deserialize(clazz.primaryConstructor!!, raw, nameConverter)
fun <T: JsonSchema> JsonValue.Companion.deserialize(constructor:KFunction<T>, raw:String) = deserialize(constructor, Json.parse(raw))
fun <T: JsonSchema> JsonValue.Companion.deserialize(constructor:KFunction<T>, raw:String, nameConverter:NameConverter) = deserialize(constructor, Json.parse(raw), nameConverter)
fun <T: JsonSchema> JsonValue.Companion.deserialize(constructor:KFunction<T>, raw:Json) = deserialize(constructor, raw, JsonSchema.AS_WRITTEN)
fun <T: JsonSchema> JsonValue.Companion.deserialize(constructor:KFunction<T>, raw:Json, nameConverter:NameConverter):T {
  val params = raw.asMap!!.keys.map { name ->
    val json = raw[name]
    val it = constructor.parameters.find { it.name == nameConverter(name) }
      ?: throw NullPointerException("Could not deserialize JSON parameter $name, no constructor parameter found by name ${nameConverter(name)}")

    Pair(it, when {
      it.type == jsonType -> json
      it.type == jsonNull && json is JsonNull -> json
      it.type.isMarkedNullable && json is JsonNull -> null
      json is JsonNull ->
        throw NullPointerException("Cannot cast null JSON parameter $name to non-nullable type ${it.type}")

      it.type.isSupertypeOf(intType) && json is JsonInt -> json.asInt
      it.type.isSupertypeOf(floatType) && json is JsonFloat -> json.asFloat
      it.type.isSupertypeOf(stringType) && json is JsonString -> json.asString
      it.type.isSupertypeOf(booleanType) && json is JsonBoolean -> json.asBoolean

      it.type == jsonInt && json is JsonInt -> json
      it.type == jsonFloat && json is JsonFloat -> json
      it.type == jsonString && json is JsonString -> json
      it.type == jsonBoolean && json is JsonBoolean -> json
      it.type == jsonArray && json is JsonArray -> json
      it.type == jsonObject && json is JsonObject -> json

      json is JsonArray -> {
        val listTypeRaw = it.type.arguments.first().type!!
        val listType = listTypeRaw.withNullability(false)

        val list = when {
          listType.isSupertypeOf(intType) -> json.value.map { it.asInt }
          listType.isSupertypeOf(floatType) -> json.value.map { it.asFloat }
          listType.isSupertypeOf(stringType) -> json.value.map { it.asString }
          listType.isSupertypeOf(booleanType) -> json.value.map { it.asBoolean }

          listType.isSupertypeOf(jsonType) -> json.value
          listType.isSupertypeOf(jsonInt) -> json.value.map { it as JsonInt }
          listType.isSupertypeOf(jsonFloat) -> json.value.map { it as JsonFloat }
          listType.isSupertypeOf(jsonString) -> json.value.map { it as JsonString }
          listType.isSupertypeOf(jsonBoolean) -> json.value.map { it as JsonBoolean }
          listType.isSupertypeOf(jsonArray) -> json.value.map { it as JsonArray }
          listType.isSupertypeOf(jsonObject) -> json.value.map { it as JsonObject }
          listType.isSupertypeOf(jsonNull) -> json.value.map { it as JsonNull }

          listType.isSubtypeOf(jdeserType) ->
            json.value.mapIndexed { i, j ->
              if(listTypeRaw.isMarkedNullable && j.value == null) null else {
                if(!j::class.createType().isSubtypeOf(jsonObject))
                  throw ClassCastException("Cannot cast value of JSON parameter $name[$i] to ${listType}")
                deserialize(listType.classifier as JDKlass, j, nameConverter)
              }
            }

          else ->
            throw ClassCastException("Cannot cast value of JSON parameter $name to ${it.type}")
        }

        if(listTypeRaw.isMarkedNullable) list else list.map { it!! }
      }

      it.type.isSubtypeOf(jdeserType) && json::class.isSubclassOf(JsonObject::class) ->
        deserialize(it.type.classifier as JDKlass, json, nameConverter)

      else ->
        throw ClassCastException("Cannot cast value of JSON parameter $name to ${it.type}")
    })
  }

  return constructor.callBy(params.toMap())
}

private typealias NameConverter = (String) -> String
private typealias JDKlass = KClass<JsonSchema>

private val intType = Int::class.createType()
private val floatType = Float::class.createType()
private val stringType = String::class.createType()
private val booleanType = Boolean::class.createType()
private val jdeserType = JsonSchema::class.createType()
private val jsonType = JsonValue::class.createType(listOf(KTypeProjection.STAR))
private val jsonInt = JsonInt::class.createType()
private val jsonFloat = JsonFloat::class.createType()
private val jsonString = JsonString::class.createType()
private val jsonBoolean = JsonBoolean::class.createType()
private val jsonArray = JsonArray::class.createType()
private val jsonObject = JsonObject::class.createType()
private val jsonNull = JsonNull::class.createType()
