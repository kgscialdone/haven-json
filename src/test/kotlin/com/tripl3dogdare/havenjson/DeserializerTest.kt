package com.tripl3dogdare.havenjson

import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import java.time.ZonedDateTime
import io.kotlintest.shouldThrow
import java.util.*

class DeserializerTest : WordSpec({
  "JsonValue#deserialize" should {
    "deserialize basic types" {
      val deser = Json.deserialize(::BasicTypes, """{
        "int": 100,
        "float": 100.0,
        "string": "Hello, world!",
        "boolean": true,
        "any": "Anything at all!"
      }""")

      deser shouldBe BasicTypes(100, 100f, "Hello, world!", true, "Anything at all!")
    }

    "deserialize nullable types" {
      val deser = Json.deserialize(::NullableTypes, """{
        "nullInt": null,
        "nullFloat": null,
        "nullString": null,
        "nullBoolean": null
      }""")

      deser shouldBe NullableTypes(null, null, null, null)
    }

    "deserialize optional parameters" {
      val included = Json.deserialize(::OptionalParams, Json(
        "nonoptional" to "test",
        "optional" to "test"
      ))

      val notIncluded = Json.deserialize(::OptionalParams, Json(
        "nonoptional" to "test"
      ))

      included shouldBe OptionalParams("test", "test")
      notIncluded shouldBe OptionalParams("test")
    }

    "deserialize JSON types" {
      val deser = Json.deserialize(::JsonTypes, """{
        "json": null,
        "jint": 100,
        "jfloat": 100.0,
        "jstring": "test",
        "jboolean": false,
        "jarray": [],
        "jobject": {},
        "jnull": null
      }""")

      deser shouldBe JsonTypes(
        Json(null),
        JsonInt(100),
        JsonFloat(100f),
        JsonString("test"),
        JsonBoolean(false),
        JsonArray(emptyList()),
        JsonObject(emptyMap()),
        JsonNull
      )
    }

    "deserialize basic lists" {
      val deser = Json.deserialize(::BasicLists, Json(
        "intList" to listOf(1, 2, 3),
        "floatList" to listOf(1f, 2f, 3f),
        "stringList" to listOf("test", "hi", "derp", "yay"),
        "booleanList" to listOf(true, false, false),
        "nullStringList" to listOf("test", null, "derp"),
        "stringNullList" to null,
        "nullStringNullList1" to null,
        "nullStringNullList2" to listOf("test", null, "derp")
      ))

      deser shouldBe BasicLists(
        listOf(1, 2, 3),
        listOf(1f, 2f, 3f),
        listOf("test", "hi", "derp", "yay"),
        listOf(true, false, false),
        listOf("test", null, "derp"),
        null,
        null,
        listOf("test", null, "derp")
      )
    }

    "deserialize JSON lists" {
      val deser = Json.deserialize(::JsonLists, """{
        "jsonList": [null],
        "jintList": [100],
        "jfloatList": [100.0],
        "jstringList": ["test"],
        "jbooleanList": [false],
        "jarrayList": [[]],
        "jobjectList": [{}],
        "jnullList": [null]
      }""")

      deser shouldBe JsonLists(
        listOf(Json(null)),
        listOf(JsonInt(100)),
        listOf(JsonFloat(100f)),
        listOf(JsonString("test")),
        listOf(JsonBoolean(false)),
        listOf(JsonArray(emptyList())),
        listOf(JsonObject(emptyMap())),
        listOf(JsonNull)
      )
    }

    "deserialize nested JsonDeserializables" {
      val deser = Json.deserialize(::Nested, """{
        "inner": { "name": "inner", "value": "test" },
        "nullInner": null,
        "innerList": [{ "name": "inner", "value": "test" }, { "name": "inner", "value": "test" }],
        "nullInnerList": [null, { "name": "inner", "value": "test" }, null]
      }""")

      deser shouldBe Nested(
        Inner("inner", "test"),
        null,
        listOf(Inner("inner", "test"), Inner("inner", "test")),
        listOf(null, Inner("inner", "test"), null)
      )
    }

    "deserialize to normal functions (not local/anon/lambda)" {
      Json.deserialize(::normalFunction, """{"name":"Santa Claus","age":500}""")
    }

    "apply name converter functions" {
      val camelCase = Json.deserialize(::NamesCamel, """{
        "camel_case": true,
        "property_names": ["camelCase", "propertyNames"]
      }""", JsonSchema.SNAKE_TO_CAMEL)

      val snakeCase = Json.deserialize(::NamesSnake, """{
        "camelCase": false,
        "propertyNames": ["camel_case", "property_names"]
      }""", JsonSchema.CAMEL_TO_SNAKE)

      camelCase shouldBe NamesCamel(true, listOf("camelCase", "propertyNames"))
      snakeCase shouldBe NamesSnake(false, listOf("camel_case", "property_names"))
    }

    "use the JsonProperty annotation to resolve names" {
      val deser = Json.deserialize(::NamesChanged, """{
        "camel_case": true,
        "propNames": ["camelCase", "propertyNames"]
      }""", JsonSchema.SNAKE_TO_CAMEL)

      deser shouldBe NamesChanged(true, listOf("camelCase", "propertyNames"))
    }

    "apply custom deserializers" {
      Deserializers.add(ZonedDateTime::parse)
      Json.deserialize(::CustomDeser, """{"date":"2018-07-05T18:13:59+00:00"}""") shouldBe
        CustomDeser(ZonedDateTime.parse("2018-07-05T18:13:59+00:00"))

      val deser = Deserializers().add { j:JsonString -> ZonedDateTime.parse(j.asString!!) }
      Json.deserialize(::CustomDeser, """{"date":"2018-07-05T18:13:59+00:00"}""", deserializers=deser) shouldBe
        CustomDeser(ZonedDateTime.parse("2018-07-05T18:13:59+00:00"))

      Json.deserialize(::CustomDeserList, """{"dates":["2018-07-05T18:13:59+00:00",null]}""") shouldBe
        CustomDeserList(listOf(ZonedDateTime.parse("2018-07-05T18:13:59+00:00"), null))
    }

    "throw when given anything but a JSON object" {
      shouldThrow<ClassCastException> {
        Json.deserialize(::BasicTypes, "[100, 200, 300]")
      }.also {
        it.message shouldBe "Cannot deserialize JsonSchema from non-object JSON value"
      }
    }

    "throw when given a JSON property with no matching parameter" {
      shouldThrow<NoSuchFieldException> {
        Json.deserialize(::BasicTypes, Json("nonexistant" to "derp"))
      }.also {
        it.message shouldBe "Cannot deserialize JSON parameter nonexistant, no matching constructor parameter found"
      }
    }

    "throw when given null for a non-nullable parameter" {
      shouldThrow<NullPointerException> {
        Json.deserialize(::BasicTypes, Json("string" to null))
      }.also {
        it.message shouldBe "Cannot cast null JSON parameter string to non-nullable type kotlin.String"
      }
    }

    "throw when not given a non-optional parameter" {
      shouldThrow<InstantiationException> {
        Json.deserialize(::OptionalParams, JsonObject(emptyMap()))
      }.also {
        it.message shouldBe "Cannot instantiate com.tripl3dogdare.havenjson.DeserializerTest.OptionalParams from JSON, missing non-optional parameters: nonoptional"
      }
    }

    "throw when given a non-object for a nested schema" {
      shouldThrow<ClassCastException> {
        Json.deserialize(::Nested, Json("inner" to "test"))
      }.also {
        it.message shouldBe "Cannot cast value of JSON parameter inner to com.tripl3dogdare.havenjson.DeserializerTest.Inner"
      }
    }

    "throw when encountering a non-deserializable type" {
      shouldThrow<ClassCastException> {
        Json.deserialize(::Undeserializable, Json("error" to "test")) }.also {
        it.message shouldBe "Cannot cast value of JSON parameter error to com.tripl3dogdare.havenjson.DeserializerTest.Undeserializable" }
      shouldThrow<ClassCastException> {
        Json.deserialize(::UndeserializableList, Json("error" to listOf("test"))) }.also {
        it.message shouldBe "Cannot cast value of JSON parameter error[0] to com.tripl3dogdare.havenjson.DeserializerTest.Undeserializable" }
    }

    "throw when a deserializer expects an invalid input type" {
      shouldThrow<ClassCastException> {
        Deserializers.add<Date, Date> { it }
        Json.deserialize(::InvalidDeser, Json("invalid" to "deserializer"))
      }.also {
        it.message shouldBe "Cannot cast value of JSON parameter invalid to java.util.Date"
      }
    }
  }
}) {
  data class BasicTypes(
    val int:Int,
    val float:Float,
    val string:String,
    val boolean:Boolean,
    val any:Any
  ) : JsonSchema

  data class NullableTypes(
    val nullInt:Int?,
    val nullFloat:Float?,
    val nullString:String?,
    val nullBoolean:Boolean?
  ) : JsonSchema

  data class OptionalParams(
    val nonoptional:String,
    val optional:String = "test"
  ) : JsonSchema

  data class JsonTypes(
    val json:Json,
    val jint:JsonInt,
    val jfloat:JsonFloat,
    val jstring:JsonString,
    val jboolean:JsonBoolean,
    val jarray:JsonArray,
    val jobject:JsonObject,
    val jnull:JsonNull
  ) : JsonSchema

  data class BasicLists(
    val intList:List<Int>,
    val floatList:List<Float>,
    val stringList:List<String>,
    val booleanList:List<Boolean>,
    val nullStringList:List<String?>,
    val stringNullList:List<String>?,
    val nullStringNullList1:List<String?>?,
    val nullStringNullList2:List<String?>?
  ) : JsonSchema

  data class JsonLists(
    val jsonList:List<Json>,
    val jintList:List<JsonInt>,
    val jfloatList:List<JsonFloat>,
    val jstringList:List<JsonString>,
    val jbooleanList:List<JsonBoolean>,
    val jarrayList:List<JsonArray>,
    val jobjectList:List<JsonObject>,
    val jnullList:List<JsonNull>
  ) : JsonSchema

  data class Nested(
    val inner:Inner,
    val nullInner:Inner?,
    val innerList:List<Inner>,
    val nullInnerList:List<Inner?>
  ) : JsonSchema

  data class Inner(
    val name:String,
    val value:String?
  ) : JsonSchema

  data class NamesCamel(
    val camelCase:Boolean,
    val propertyNames:List<String>
  ) : JsonSchema

  data class NamesSnake(
    val camel_case:Boolean,
    val property_names:List<String>
  ) : JsonSchema

  data class NamesChanged(
    val camelCase:Boolean,
    @JsonProperty("propNames") val propertyNames:List<String>
  ) : JsonSchema

  data class CustomDeser(val date:ZonedDateTime) : JsonSchema
  data class CustomDeserList(val dates:List<ZonedDateTime?>) : JsonSchema

  data class Undeserializable(val error:Undeserializable) : JsonSchema
  data class UndeserializableList(val error:List<Undeserializable>) : JsonSchema
  data class InvalidDeser(val invalid:Date) : JsonSchema

  companion object {
    fun normalFunction(name:String, age:Int):JsonSchema {
      name shouldBe "Santa Claus"
      age shouldBe 500
      return object:JsonSchema{}
    }
  }
}
