package com.tripl3dogdare.havenjson

import io.kotlintest.matchers.maps.shouldContainKey
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import java.io.File

class ParserTest : WordSpec() {
  init {
    "JsonValue#parse" should {
      for(file in File("./test_files/").walkTopDown().filter { it.isFile })
        "correctly parse ${file.name}" {
          results.shouldContainKey(file.name)
          Json.parse(file.readText()) shouldBe results[file.name]
        }
    }
  }

  val results = mapOf()
}
