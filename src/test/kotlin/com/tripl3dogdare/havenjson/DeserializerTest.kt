package com.tripl3dogdare.havenjson

import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import java.time.ZonedDateTime

class DeserializerTest : WordSpec({
  "JsonValue#deserialize" should {
    "deserialize basic types" {
      val deser = Json.deserialize(::BasicTypes, """{
        "int": 100,
        "float": 100.0,
        "string": "Hello, world!",
        "boolean": true
      }""")

      deser shouldBe BasicTypes(100, 100f, "Hello, world!", true)
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

    "apply custom deserializers" {
      JsonSchema.registerDeserializer(ZonedDateTime::parse)
      Json.deserialize(::CustomDeser, """{"date":"2018-07-05T18:13:59+00:00"}""") shouldBe
        CustomDeser(ZonedDateTime.parse("2018-07-05T18:13:59+00:00"))

      val reg = JsonSchema.Registry().registerDeserializer(ZonedDateTime::parse)
      Json.deserialize(::CustomDeser, """{"date":"2018-07-05T18:13:59+00:00"}""", registry=reg) shouldBe
        CustomDeser(ZonedDateTime.parse("2018-07-05T18:13:59+00:00"))
    }
  }
}) {
  data class BasicTypes(
    val int:Int,
    val float:Float,
    val string:String,
    val boolean:Boolean
  ) : JsonSchema

  data class NullableTypes(
    val nullInt:Int?,
    val nullFloat:Float?,
    val nullString:String?,
    val nullBoolean:Boolean?
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

  data class CustomDeser(val date:ZonedDateTime) : JsonSchema
}
