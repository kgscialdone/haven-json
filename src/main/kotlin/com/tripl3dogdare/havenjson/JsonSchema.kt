package com.tripl3dogdare.havenjson

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

/**
 * Base class for deserializable objects.
 * Inheriting from this marks a class as a valid target for [JsonValue.Companion.deserialize].
 */
interface JsonSchema {
  @Suppress("unchecked_cast")
  fun toJson():JsonObject {
    val constructor = this::class.primaryConstructor
      ?: throw NullPointerException("Cannot convert constructorless type ${this::class.simpleName} to JSON")

    val out = constructor.parameters.map { param ->
      val prop = (this::class.declaredMemberProperties.first { it.name == param.name } as KProperty1<JsonSchema, Any?>).get(this)
      (param.findAnnotation<JsonProperty>()?.name ?: param.name!!) to when {
        prop is JsonSchema -> prop.toJson()

        Json(prop) is JsonString && prop !is String && prop !is JsonString -> {
          val className = if(prop != null) prop::class.simpleName else "null"
          throw ClassCastException("Cannot convert $className to JSON")
        }

        else -> Json(prop)
      }
    }

    return JsonObject(out.toMap())
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
  deserializers:Deserializers = Deserializers
) =
  deserialize(clazz, Json.parse(raw), deserializers)

/** Deserialize a [JsonValue] via the primary constructor of the given class. */
fun <T: JsonSchema> JsonValue.Companion.deserialize(
  clazz:KClass<T>,
  raw:Json,
  deserializers:Deserializers = Deserializers
) =
  deserialize(clazz.primaryConstructor!!, raw, deserializers)

/** Deserialize a JSON string via the given constructor. */
fun <T: JsonSchema> JsonValue.Companion.deserialize(
  constructor:KFunction<T>,
  raw:String,
  deserializers:Deserializers = Deserializers
) =
  deserialize(constructor, Json.parse(raw), deserializers)

/**
 * Deserialize a [JsonValue] via the given constructor.
 *
 * @param T The type to return. Must inherit from [JsonSchema].
 * @param constructor The constructor function to call.
 * @param raw The raw [JsonValue].
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
  deserializers:Deserializers = Deserializers
):T {
  if(raw !is JsonObject)
    throw ClassCastException("Cannot deserialize JsonSchema from non-object JSON value")
  val params = raw.asMap!!.keys.map { name ->
    val json = raw[name]
    val it = constructor.parameters.find {
      it.isAnnotated<JsonProperty> { name == it.name } ||
      !it.isAnnotated<JsonProperty>() &&
      it.name == NamePolicy.getNameConverter(constructor.returnType.jvmErasure as KClass<JsonSchema>)(name)
    } ?: throw NoSuchFieldException("Cannot deserialize JSON parameter $name, no matching constructor parameter found")

    Pair(it, when {
      it.type.isMarkedNullable && json is JsonNull -> null
      it.type.isSupertypeOf(jsonNull) && json is JsonNull -> json
      json is JsonNull ->
        throw NullPointerException("Cannot cast null JSON parameter $name to non-nullable type ${it.type}")

      it.type.isSupertypeOf(intType) && json is JsonInt -> json.asInt
      it.type.isSupertypeOf(floatType) && json is JsonFloat -> json.asFloat
      it.type.isSupertypeOf(stringType) && json is JsonString -> json.asString
      it.type.isSupertypeOf(booleanType) && json is JsonBoolean -> json.asBoolean

      it.type.isSupertypeOf(jsonInt) && json is JsonInt -> json
      it.type.isSupertypeOf(jsonFloat) && json is JsonFloat -> json
      it.type.isSupertypeOf(jsonString) && json is JsonString -> json
      it.type.isSupertypeOf(jsonBoolean) && json is JsonBoolean -> json
      it.type.isSupertypeOf(jsonArray) && json is JsonArray -> json
      it.type.isSupertypeOf(jsonObject) && json is JsonObject -> json

      it.type.isSubtypeOf(jdeserType) && json::class.isSubclassOf(JsonObject::class) ->
        deserialize(it.type.jvmErasure as KClass<JsonSchema>, json, deserializers)
      deserializers.has(it.type.jvmErasure) ->
        try { deserializers.deserialize(it.type.jvmErasure, json) }
        catch(e:Exception) {
          throw ClassCastException("Cannot cast value of JSON parameter $name to ${it.type}") }

      it.type.withNullability(false).isSubtypeOf(listType) && json is JsonArray -> {
        val innerTypeRaw = it.type.arguments.first().type!!
        val innerType = innerTypeRaw.withNullability(false)

        val list = when {
          innerType.isSupertypeOf(intType) -> json.value.map { it.asInt }
          innerType.isSupertypeOf(floatType) -> json.value.map { it.asFloat }
          innerType.isSupertypeOf(stringType) -> json.value.map { it.asString }
          innerType.isSupertypeOf(booleanType) -> json.value.map { it.asBoolean }

          innerType.isSupertypeOf(jsonType) -> json.value
          innerType.isSupertypeOf(jsonInt) -> json.value.map { it as JsonInt }
          innerType.isSupertypeOf(jsonFloat) -> json.value.map { it as JsonFloat }
          innerType.isSupertypeOf(jsonString) -> json.value.map { it as JsonString }
          innerType.isSupertypeOf(jsonBoolean) -> json.value.map { it as JsonBoolean }
          innerType.isSupertypeOf(jsonArray) -> json.value.map { it as JsonArray }
          innerType.isSupertypeOf(jsonObject) -> json.value.map { it as JsonObject }
          innerType.isSupertypeOf(jsonNull) -> json.value.map { it as JsonNull }

          innerType.isSubtypeOf(jdeserType) -> deserializeList(json, innerTypeRaw) { i, j ->
            if(!j::class.createType().isSubtypeOf(jsonObject))
              throw ClassCastException("Cannot cast value of JSON parameter $name[$i] to $innerType")
            deserialize(innerType.jvmErasure as KClass<JsonSchema>, j, deserializers)
          }

          deserializers.has(innerType.jvmErasure) -> deserializeList(json, innerTypeRaw) { i, j ->
            try { deserializers.deserialize(innerType.jvmErasure, j) }
            catch(e:Exception) {
              throw ClassCastException("Cannot cast value of JSON parameter $name[$i] to $innerType") }
          }

          else ->
            throw ClassCastException("Cannot cast value of JSON parameter $name to ${it.type}")
        }

        if(innerTypeRaw.isMarkedNullable) list else list.mapIndexed { i, e ->
          e ?: throw NullPointerException("Cannot cast null JSON parameter $name[$i] to non-nullable type $innerType") }
      }

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

private inline fun <T> deserializeList(json:JsonArray, listTypeRaw:KType, f:(Int,Json) -> T) =
  json.value.mapIndexed { i, j -> if(listTypeRaw.isMarkedNullable && j.value == null) null else f(i,j) }

/**
 * Marks a constructor parameter to be deserialized from a specific property name.
 * Overrides [JsonNamePolicy] if set.
 * @property name The name of the JSON property this parameter should take it's value from when deserialized.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
annotation class JsonProperty(val name:String)

/**
 * Sets the desired [NamePolicy] for a given [JsonSchema] subclass.
 * If this annotation is not present, [NamePolicy.AsWritten] will be assumed.
 * If multiple [NamePolicy]s are passed, they will be applied from left to right.
 * If [NamePolicy.Custom] is passed, the [JsonSchema] subclass must define a companion object
 *  inheriting from [CustomNamePolicy].
 *
 * @property nc The [NamePolicy] (or multiple) to apply when deserializing.
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class JsonNamePolicy(vararg val nc:NamePolicy)

/**
 * Base interface for use with [NamePolicy.Custom].
 * Must be implemented by a companion object on any [JsonSchema]s declaring a custom [NamePolicy].
 */
interface CustomNamePolicy {
  /**
   * Converts the name of a JSON field to it's corresponding constructor parameter name.
   * @param name The field name in the JSON source.
   * @return The name of the corresponding constructor parameter for this [JsonSchema].
   */
  fun convertFieldName(name:String):String
}

/**
 * Used with [JsonNamePolicy] to define the desired name conversion strategy for a given [JsonSchema].
 */
enum class NamePolicy(internal val nc:(String) -> String) {
  /** Leaves the field name as-is (default). */
  AsWritten({it}),
  /** Converts the field name to uppercase. */
  Uppercase(String::toUpperCase),
  /** Converts the field name to lowercase. */
  Lowercase(String::toLowerCase),
  /** Converts the field name from camel case to snake case. */
  CamelToSnake({ it.decamelize('_') }),
  /** Converts the field name from camel case to kebab case. */
  CamelToKebab({ it.decamelize('-') }),
  /** Converts the field name from camel case to Pascal case. */
  CamelToPascal({ it.capitalize() }),
  /** Converts the field name from snake case to camel case. */
  SnakeToCamel({ it.camelize('_') }),
  /** Converts the field name from snake case to kebab case. */
  SnakeToKebab({ it.replace("_", "-") }),
  /** Converts the field name from snake case to Pascal case. */
  SnakeToPascal({ it.camelize('_').capitalize() }),
  /** Converts the field name from kebab case to camel case. */
  KebabToCamel({ it.camelize('-') }),
  /** Converts the field name from kebab case to snake case. */
  KebabToSnake({ it.replace("-", "_") }),
  /** Converts the field name from kebab case to Pascal case. */
  KebabToPascal({ it.camelize('-').capitalize() }),
  /** Converts the field name from Pascal case to snake case. */
  PascalToSnake({ it.decamelize('_') }),
  /** Converts the field name from Pascal case to kebab case. */
  PascalToKebab({ it.decamelize('-') }),
  /** Converts the field name from Pascal case to camel case. */
  PascalToCamel({ it.decapitalize() }),

  /**
   * Allows for definition of a custom name conversion method.
   * When used, the host [JsonSchema] must define a companion object implementing [CustomNamePolicy],
   *  otherwise the name will not be changed.
   */
  Custom({it});

  /**
   * Applies this [NamePolicy] for the given name.
   * If this is [NamePolicy.Custom], the given class will be used to determine the method to use,
   *  via a companion object implementing [CustomNamePolicy]. If that companion object does not
   *  exist, the name will not be changed.
   *
   * @param name The name to convert.
   * @param clazz The host [JsonSchema] class to pull custom policies from.
   * @return The converted name.
   */
  operator fun invoke(name:String, clazz:KClass<JsonSchema>):String = when(this) {
    Custom -> (clazz.companionObjectInstance as? CustomNamePolicy)?.convertFieldName(name) ?: nc(name)
    else -> nc(name)
  }

  companion object {
    /**
     * Builds a name converter function for the given class.
     * @param clazz The [JsonSchema] subclass to get the name converter for.
     * @return A (String) -> String combining all declared [NamePolicy]s for [clazz].
     */
    fun getNameConverter(clazz: KClass<JsonSchema>) =
      getDeclaredNamePolicies(clazz).fold(AsWritten.nc) { a, b -> { name -> b(a(name), clazz) }}

    /**
     * Returns all declared [NamePolicy]s for the given class.
     * @param clazz The [JsonSchema] subclass to get the [NamePolicy]s for.
     * @return A List<NamePolicy> containing all declared [NamePolicy]s for [clazz].
     */
    fun getDeclaredNamePolicies(clazz:KClass<JsonSchema>) =
      clazz.findAnnotation<JsonNamePolicy>()?.nc?.toList() ?: emptyList()
  }
}

private inline fun <reified A: Annotation> KParameter.isAnnotated(pred:(A) -> Boolean = {true}) =
  this.annotations.any { it.annotationClass == A::class && pred(it as A) }

private val intType = Int::class.createType()
private val floatType = Float::class.createType()
private val stringType = String::class.createType()
private val booleanType = Boolean::class.createType()
private val jdeserType = JsonSchema::class.createType()
private val listType = List::class.createType(listOf(KTypeProjection.STAR))
private val jsonType = JsonValue::class.createType(listOf(KTypeProjection.STAR))
private val jsonInt = JsonInt::class.createType()
private val jsonFloat = JsonFloat::class.createType()
private val jsonString = JsonString::class.createType()
private val jsonBoolean = JsonBoolean::class.createType()
private val jsonArray = JsonArray::class.createType()
private val jsonObject = JsonObject::class.createType()
private val jsonNull = JsonNull::class.createType()
