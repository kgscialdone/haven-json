package com.tripl3dogdare.havenjson

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class NamePolicyTest : StringSpec({
  "NamePolicy.AsWritten should not change field names" {
    Json.deserialize(::AsWrittenTest, """{ "targetProperty": true }""") shouldBe AsWrittenTest(true)
    shouldThrow<NoSuchFieldException> {
      Json.deserialize(::AsWrittenTest, """{ "target_property": true }""")
    }
  }

  "NamePolicy.Uppercase should convert field names to uppercase" {
    Json.deserialize(::UppercaseTest, """{ "target_property": true }""") shouldBe UppercaseTest(true)
  }

  "NamePolicy.Lowercase should convert field names to lowercase" {
    Json.deserialize(::LowercaseTest, """{ "TARGET_PROPERTY": true }""") shouldBe LowercaseTest(true)
  }

  "NamePolicy.XToCamel should convert field names from X case to camel case" {
    CamelCaseTest.currentNamePolicy = NamePolicy.SnakeToCamel
    Json.deserialize(::CamelCaseTest, """{ "target_property": true }""") shouldBe CamelCaseTest(true)

    CamelCaseTest.currentNamePolicy = NamePolicy.KebabToCamel
    Json.deserialize(::CamelCaseTest, """{ "target-property": true }""") shouldBe CamelCaseTest(true)

    CamelCaseTest.currentNamePolicy = NamePolicy.PascalToCamel
    Json.deserialize(::CamelCaseTest, """{ "TargetProperty": true }""") shouldBe CamelCaseTest(true)
  }

  "NamePolicy.XToSnake should convert field names from X case to snake case" {
    SnakeCaseTest.currentNamePolicy = NamePolicy.CamelToSnake
    Json.deserialize(::SnakeCaseTest, """{ "targetProperty": true }""") shouldBe SnakeCaseTest(true)

    SnakeCaseTest.currentNamePolicy = NamePolicy.KebabToSnake
    Json.deserialize(::SnakeCaseTest, """{ "target-property": true }""") shouldBe SnakeCaseTest(true)

    SnakeCaseTest.currentNamePolicy = NamePolicy.PascalToSnake
    Json.deserialize(::SnakeCaseTest, """{ "TargetProperty": true }""") shouldBe SnakeCaseTest(true)
  }

  "NamePolicy.XToKebab should convert field names from X case to kebab case" {
    KebabCaseTest.currentNamePolicy = NamePolicy.CamelToKebab
    Json.deserialize(::KebabCaseTest, """{ "targetProperty": true }""") shouldBe KebabCaseTest(true)

    KebabCaseTest.currentNamePolicy = NamePolicy.SnakeToKebab
    Json.deserialize(::KebabCaseTest, """{ "target_property": true }""") shouldBe KebabCaseTest(true)

    KebabCaseTest.currentNamePolicy = NamePolicy.PascalToKebab
    Json.deserialize(::KebabCaseTest, """{ "TargetProperty": true }""") shouldBe KebabCaseTest(true)
  }

  "NamePolicy.XToPascal should convert field names from X case to Pascal case" {
    PascalCaseTest.currentNamePolicy = NamePolicy.CamelToPascal
    Json.deserialize(::PascalCaseTest, """{ "targetProperty": true }""") shouldBe PascalCaseTest(true)

    PascalCaseTest.currentNamePolicy = NamePolicy.SnakeToPascal
    Json.deserialize(::PascalCaseTest, """{ "target_property": true }""") shouldBe PascalCaseTest(true)

    PascalCaseTest.currentNamePolicy = NamePolicy.KebabToPascal
    Json.deserialize(::PascalCaseTest, """{ "target-property": true }""") shouldBe PascalCaseTest(true)
  }
}) {
  @JsonNamePolicy(NamePolicy.AsWritten)
  data class AsWrittenTest(val targetProperty:Boolean) : JsonSchema

  @JsonNamePolicy(NamePolicy.Uppercase)
  data class UppercaseTest(val TARGET_PROPERTY:Boolean) : JsonSchema

  @JsonNamePolicy(NamePolicy.Lowercase)
  data class LowercaseTest(val target_property:Boolean) : JsonSchema

  @JsonNamePolicy(NamePolicy.Custom)
  data class CamelCaseTest(val targetProperty:Boolean) : JsonSchema { companion object : VariableNamePolicy() }

  @JsonNamePolicy(NamePolicy.Custom)
  data class SnakeCaseTest(val target_property:Boolean) : JsonSchema { companion object : VariableNamePolicy() }

  @JsonNamePolicy(NamePolicy.Custom)
  data class KebabCaseTest(val `target-property`:Boolean) : JsonSchema { companion object : VariableNamePolicy() }

  @JsonNamePolicy(NamePolicy.Custom)
  data class PascalCaseTest(val TargetProperty:Boolean) : JsonSchema { companion object : VariableNamePolicy() }

  abstract class VariableNamePolicy : CustomNamePolicy {
    var currentNamePolicy = NamePolicy.AsWritten
    override fun convertFieldName(name:String) = currentNamePolicy.nc(name)
  }
}