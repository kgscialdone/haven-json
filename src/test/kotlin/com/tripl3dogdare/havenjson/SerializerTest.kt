package com.tripl3dogdare.havenjson

import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec

class SerializerTest : WordSpec({
  "JsonSchema#toJson" should {
    "properly serialize basic types" {
      BasicTypes(100, 100f, "test", false, null, "test", Schema("test")).toJson() shouldBe
        Json("int" to 100,
             "float" to 100f,
             "string" to "test",
             "bool" to false,
             "nil" to null,
             "annotated_name" to "test",
             "schema" to mapOf("test" to "test"))
    }

    "properly serialize lists" {
      Lists(listOf(100),
            listOf(100f),
            listOf("test"),
            listOf(true, false),
            listOf(null))
      .toJson() shouldBe
        Json("int" to listOf(100),
             "float" to listOf(100f),
             "string" to listOf("test"),
             "bool" to listOf(true, false),
             "nil" to listOf(null))
    }

    "properly serialize JSON types" {
      JsonTypes(JsonInt(100),
                JsonFloat(100f),
                JsonString("test"),
                JsonBoolean(false),
                JsonNull,
                JsonArray(listOf(JsonBoolean(true), JsonBoolean(false))),
                JsonObject(mapOf("greeting" to JsonString("Hello"),
                                 "target" to JsonString("world"))))
      .toJson() shouldBe
        Json("int" to 100,
             "float" to 100f,
             "string" to "test",
             "bool" to false,
             "nil" to null,
             "arr" to listOf(true, false),
             "obj" to mapOf("greeting" to "Hello",
                            "target" to "world"))
    }
  }
}) {
  data class BasicTypes(
    val int:Int,
    val float:Float,
    val string:String,
    val bool:Boolean,
    val nil:Nothing?,
    @JsonProperty("annotated_name") val annotatedName:String,
    val schema:Schema
  ) : JsonSchema

  data class Lists(
    val int:List<Int>,
    val float:List<Float>,
    val string:List<String>,
    val bool:List<Boolean>,
    val nil:List<Nothing?>
  ) : JsonSchema

  data class JsonTypes(
    val int:JsonInt,
    val float:JsonFloat,
    val string:JsonString,
    val bool:JsonBoolean,
    val nil:JsonNull,
    val arr:JsonArray,
    val obj:JsonObject
  ) : JsonSchema

  class Schema(val test:String) : JsonSchema
}