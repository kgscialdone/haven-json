package com.tripl3dogdare.havenjson

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

/**
 * Base class for deserializable objects.
 * Inheriting from this marks a class as a valid target for [JsonValue.Companion.deserialize].
 */
interface JsonSchema {
  companion object {
    /** Name converter function that leaves names as-is. */
    val AS_WRITTEN: NameConverter = { it }
    /** Name converter function that converts names to uppercase. */
    val UPPERCASE: NameConverter = String::toUpperCase
    /** Name converter function that converts names to lowercase. */
    val LOWERCASE: NameConverter = String::toLowerCase
    /** Name converter function that converts names from camelCase to snake_case. */
    val CAMEL_TO_SNAKE: NameConverter =
      { it.replace(Regex("([A-Z])"), "_$1").toLowerCase() }
    /** Name converter function that converts names from snake_case to camelCase. */
    val SNAKE_TO_CAMEL: NameConverter =
      { it.toLowerCase().replace(Regex("_(\\w)")) { it.groups[1]!!.value.toUpperCase() } }
  }
}

/**
 * Represents a group of custom deserializer functions.
 * For most things, you should use [Deserializers.defaultInstance]. If for some
 *  reason you need to have multiple groups of deserializers, this class is available.
 */
open class Deserializers {
  /** The underlying map of deserializers. **/
  private val underlying:MutableMap<KClass<*>, (Any) -> Any> = mutableMapOf()

  /**
   * Add a deserializer function to this group.
   * @param J The input type (subclass of [JsonValue] or valid type to be contained by [JsonValue]).
   * @param T The output type.
   * @param f The deserializer function.
   */
  inline fun <J, reified T: Any> add(noinline f: (J) -> T) = add(T::class, f)
  fun <J, T: Any> add(clazz:KClass<T>, f:(J) -> T):Deserializers {
    underlying.put(clazz, f as (Any) -> Any)
    return this
  }

  /**
   * Get a deserializer function from this group.
   * @param t The class you want to get the deserializer for
   */
  fun <T: Any> get(t:KClass<T>) = underlying[t] as ((Any) -> T)?

  /**
   * Returns true if there is a deserializer registered for the given class.
   * @param t The class to check for.
   */
  fun <T: Any> has(t:KClass<T>) = underlying.containsKey(t)

  /**
   * Remove a deserializer function from this group.
   * @param t The class you want to remove the deserializer for
   */
  fun <T: Any> remove(t:KClass<T>):Deserializers {
    underlying.remove(t)
    return this
  }

  /**
   * Attempt to deserialize a value with this group.
   * @param t The class of the output type
   * @param raw The undeserialized value
   */
  fun <T: Any> deserialize(t:KClass<T>, raw:Json):T {
    val intyp = get(t)!!.reflect()!!.parameters[0].type
    val incls = intyp.jvmErasure
    return t.cast(get(t)!!(incls.cast(if(intyp.isSubtypeOf(jsonType)) raw else raw.value!!)))
  }

  /** The default Deserializers instance. */
  companion object defaultInstance : Deserializers()
}

/** Deserialize a JSON string via the primary constructor of the given class. */
fun <T: JsonSchema> JsonValue.Companion.deserialize(
  clazz:KClass<T>,
  raw:String,
  nameConverter:NameConverter = JsonSchema.AS_WRITTEN,
  deserializers:Deserializers = Deserializers
) =
  deserialize(clazz, Json.parse(raw), nameConverter, deserializers)

/** Deserialize a [JsonValue] via the primary constructor of the given class. */
fun <T: JsonSchema> JsonValue.Companion.deserialize(
  clazz:KClass<T>,
  raw:Json,
  nameConverter:NameConverter = JsonSchema.AS_WRITTEN,
  deserializers:Deserializers = Deserializers
) =
  deserialize(clazz.primaryConstructor!!, raw, nameConverter, deserializers)

/** Deserialize a JSON string via the given constructor. */
fun <T: JsonSchema> JsonValue.Companion.deserialize(
  constructor:KFunction<T>,
  raw:String,
  nameConverter:NameConverter = JsonSchema.AS_WRITTEN,
  deserializers:Deserializers = Deserializers
) =
  deserialize(constructor, Json.parse(raw), nameConverter, deserializers)

/**
 * Deserialize a [JsonValue] via the given constructor.
 *
 * @param T The type to return. Must inherit from [JsonSchema].
 * @param constructor The constructor function to call.
 * @param raw The raw [JsonValue].
 * @param nameConverter The function to use to convert JSON property names into matching constructor parameter names.
 * @param deserializers The [Deserializers] instance to use for custom deserialization functions.
 * @return The results of calling the given constructor with named parameters corresponding to the property names of
 *         the given JSON object.
 *
 * @throws NullPointerException if a non-nullable parameter corresponds to a JSON property containing null.
 * @throws ClassCastException if the passed [JsonValue] is not a [JsonObject].
 * @throws ClassCastException if a parameter corresponds to a JSON property of the wrong type.
 * @throws NoSuchFieldException if a JSON property is found without a corresponding constructor parameter.
 */
fun <T: JsonSchema> JsonValue.Companion.deserialize(
  constructor:KFunction<T>,
  raw:Json,
  nameConverter:NameConverter = JsonSchema.AS_WRITTEN,
  deserializers:Deserializers = Deserializers
):T {
  if(raw !is JsonObject)
    throw ClassCastException("Cannot deserialize JsonSchema from non-object JSON value")
  val params = raw.asMap!!.keys.map { name ->
    val json = raw[name]
    val it = constructor.parameters.find {
      it.isAnnotated<JsonProperty> { name == it.name } ||
      !it.isAnnotated<JsonProperty>() && it.name == nameConverter(name)
    } ?: throw NoSuchFieldException("Cannot deserialize JSON parameter $name, no matching constructor parameter found")

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
                deserialize(listType.jvmErasure as KClass<JsonSchema>, j, nameConverter, deserializers)
              }
            }
          deserializers.has(listType.jvmErasure) ->
            json.value.mapIndexed { i, j ->
              if(listTypeRaw.isMarkedNullable && j.value == null) null else {
                try { deserializers.deserialize(listType.jvmErasure, j) }
                catch(e:Exception) {
                  throw ClassCastException("Cannot cast value of JSON parameter $name[$i] to ${listType}") }
              }
            }

          else ->
            throw ClassCastException("Cannot cast value of JSON parameter $name to ${it.type}")
        }

        if(listTypeRaw.isMarkedNullable) list else list.map { it!! }
      }

      it.type.isSubtypeOf(jdeserType) && json::class.isSubclassOf(JsonObject::class) ->
        deserialize(it.type.jvmErasure as KClass<JsonSchema>, json, nameConverter, deserializers)
      deserializers.has(it.type.jvmErasure) ->
        try { deserializers.deserialize(it.type.jvmErasure, json) }
        catch(e:Exception) {
          throw ClassCastException("Cannot cast value of JSON parameter $name to ${it.type}") }

      else ->
        throw ClassCastException("Cannot cast value of JSON parameter $name to ${it.type}")
    })
  }

  val missing = constructor.parameters.toSet() - params.map { it.first }.toSet()
  val missingNonOptional = missing.filter { !it.isOptional }
  val missingNames = missingNonOptional.map { it.name }.joinToString(", ")
  if(missingNonOptional.any())
    throw InstantiationException(
      "Cannot instantiate ${constructor.returnType} from JSON, missing non-optional parameters: $missingNames")

  return constructor.callBy(params.toMap())
}

/**
 * Marks a constructor parameter to be deserialized from a specific property name.
 * Overrides the nameConverter parameter of [deserialize].
 * @property name The name of the JSON property this parameter should take it's value from when deserialized.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
annotation class JsonProperty(val name:String)

private inline fun <reified A: Annotation> KParameter.isAnnotated(pred:(A) -> Boolean = {true}) =
  this.annotations.any { it.annotationClass == A::class && pred(it as A) }

private typealias NameConverter = (String) -> String

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
