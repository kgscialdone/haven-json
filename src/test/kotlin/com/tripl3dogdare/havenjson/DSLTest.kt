package com.tripl3dogdare.havenjson

import io.kotlintest.*
import io.kotlintest.matchers.types.*
import io.kotlintest.specs.WordSpec

class DSLTest : WordSpec({
  "JsonValue#invoke" should {
    "construct correct instances for basic atoms" {
      Json(100) shouldBe JsonInt(100)
      Json(100f) shouldBe JsonFloat(100f)
      Json("test") shouldBe JsonString("test")
      Json(false) shouldBe JsonBoolean(false)
      Json(null) shouldBe null
    }

    "construct JsonArray instances for Lists" {
      val list1 = listOf(100, "a string!")
      val list2 = listOf(JsonInt(100), JsonString("a string!"))

      Json(list1) shouldBe JsonArray(list2)
      Json(100, "a string!") shouldBe JsonArray(list2)
    }

    "construct JsonObject instances for Maps"  {
      val map1 = mapOf("test" to 100, "test2" to "a string!")
      val map2 = mapOf("test" to JsonInt(100), "test2" to JsonString("a string!"))

      Json(map1) shouldBe JsonObject(map2)
      Json("test" to 100, "test2" to "a string!") shouldBe JsonObject(map2)
    }
  }

  "JsonValue#get" should {
    "retrieve values from nested JSON" {
      val json = Json(
        "contacts" to listOf(
          mapOf("name" to "David Dunn", "number" to "123-456-7890"),
          mapOf("name" to "Danny Goki", "number" to "098-765-4321")
        )
      )

      json["contacts"][0]["name"].value shouldBe "David Dunn"
      json["contacts"][0]["number"].value shouldBe "123-456-7890"
      json["contacts"][1]["name"].value shouldBe "Danny Goki"
      json["contacts"][1]["number"].value shouldBe "098-765-4321"
      json["best_friend"].value shouldBe null
    }
  }

  "JsonValue#asType methods" should {
    "return the correct types" {
      val json = Json(
        "int" to 100,
        "float" to 100f,
        "string" to "string",
        "boolean" to true,
        "list" to listOf(100, 100f, "string"),
        "map" to mapOf("test1" to 100, "test2" to "string"),
        "list2" to Json(100, 100f, "string"),
        "map2" to Json("test1" to 100, "test2" to "string")
      )

      json["int"].asInt.shouldBeInstanceOf<Int>()
      json["float"].asFloat.shouldBeInstanceOf<Float>()
      json["string"].asString.shouldBeInstanceOf<String>()
      json["boolean"].asBoolean.shouldBeInstanceOf<Boolean>()

      json["int"].asString shouldBe null

      json["list"].asList.shouldBeInstanceOf<List<Json>>()
      json["map"].asMap.shouldBeInstanceOf<Map<String, Json>>()
      json["list2"].asList.shouldBeInstanceOf<List<Json>>()
      json["map2"].asMap.shouldBeInstanceOf<Map<String, Json>>()
    }
  }

  "JsonValue#mkString" should {
    for(i in 0..5) "output valid JSON at indent $i" {
      val json = Json(
        "int" to 100,
        "float" to 100f,
        "string" to "string \\\"with quotes\"",
        "escapes" to "\u2602\n\b\r\t\u000c\\\"",
        "boolean" to true,
        "null" to null,
        "list" to listOf(100, 100f, "string"),
        "map" to mapOf("test1" to 100, "test2" to "string"),
        "list2" to Json(100, 100f, "string"),
        "map2" to Json("test1" to 100, "test2" to "string")
      )

      Json.parse(json.mkString(i)) shouldBe json
    }
  }
})
