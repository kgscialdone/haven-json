[![Build Status](https://www.travis-ci.com/tripl3dogdare/haven-json.svg?branch=master)](https://www.travis-ci.com/tripl3dogdare/haven-json)
[![Issue Count](https://img.shields.io/github/issues/tripl3dogdare/haven-json.svg)](https://github.com/tripl3dogdare/haven-json/issues)
![Contributions Welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)
[![Documentation: Read Now](https://img.shields.io/badge/documentation-read%20now-blue.svg)](http://docs.tripl3dogdare.com/haven-json/1.1.0/)

# Haven
Haven is a  simple JSON library written in pure Kotlin with no external dependencies.
It was designed with simplicity of use in mind, with the goal of letting the user manipulate
JSON with an almost Javascript-like syntax.

**Quick Links:**
- [API Documentation](http://docs.tripl3dogdare.com/haven-json/1.1.0/)
- [Installation](#installation)
- [Examples](#examples)

## The Haven Philosophy

- Simple is better than complex
- Concise and intuitive is better than verbose and idiomatic
- Dependencies are a liability
- Correctness is not the gold standard, it's the baseline

In order to achieve this, Haven:

- Makes accessing JSON on the fly as simple as possible, providing type
  safety without forcing the user to think about the static types of
  their data at every step
- Provides a DSL syntax as Javascript-like as possible within the
  restraints of the Kotlin language
- Uses no external dependencies other than the 
  [kotlintest](https://github.com/kotlintest/kotlintest) testing framework
- Provides extensive test cases to ensure there are as few bugs and
  inconsistencies as possible

## What makes Haven different?

Haven primarily takes a tree-model approach to dealing with JSON data - parsed JSON data
is an in-memory tree of objects that can be deeply accessed on demand. Most JSON libraries
available for the JVM take a data-binding approach - you define an object that serves as
the structure, which the library then maps the JSON values onto. This difference in approach
makes Haven much quicker and simpler to get down and dirty with your data, just like if it
was stored in native Map and List objects (because it is!), but it has it's drawbacks as well,
namely in type-safety and a hard hit to syntax conciseness. Haven aims to minimize these
issues as much as possible, giving you a simple, concise, easy to understand way to interface
with your data, while still being type-safe (and without all the type-safety syntax clutter).

Haven also supports a data-binding style of JSON manipulation, similar to most existing JVM
libraries; however, this functionality is built on top of the core tree-model system and is
really somewhat of a second class citizen. It's recommended to use the tree-model style for
simple interactions with your data, and apply the data binding approach primarily when your
code needs to interface with other outside code (for example, returning an object instead
of raw JSON from an API call convenience method).

## Installation
You can install Haven with [JitPack](https://jitpack.io/#tripl3dogdare/haven-json) (Gradle example 
shown below), check out the [Releases](https://github.com/tripl3dogdare/haven-json/releases) 
page, or download the source code and build it yourself.

```groovy
allprojects {
  repositories {
    // ...
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
  implementation 'com.github.tripl3dogdare:haven-json:1.1.0'
}
```

## Development To-Do List

- [x] Basic JSON AST structure
- [x] DSL for creating JSON objects
- [x] DSL for accessing JSON objects
- [ ] DSL for modifying JSON objects
- [x] Methods to convert JSON objects to valid JSON strings
  - [x] Minified
  - [x] Pretty-printed at any indentation size
- [x] [JSON.org](https://json.org) spec compliant JSON parser
- [x] Data-binding style destructuring

## Examples

**Importing**
```kotlin
import com.tripl3dogdare.havenjson.*
```

**Creating JSON via the DSL** 
```kotlin
val jwick:Json = Json(
  "name" to "John Wick",
  "age" to 37,
  "occupation" to "Hitman",
  "contacts" to listOf(
    mapOf("name" to "Ethan Hunt", "number" to "[REDACTED]"),
    mapOf("name" to "Jason Bourne", "number" to "[REDACTED]"),
    mapOf("name" to "Bender", "number" to "2716057")
  )
)
```

**Accessing JSON via the DSL**
```kotlin
// Untyped value access
// Returns null if the value doesn't exist
val jwickName:Any? = jwick["name"].value

// Typed value access
// Returns null if the value doesn't exist or is the wrong type
val jwickName = jwick["name"].value<String>()
val jwickAge= jwick["age"].asInt
val ehuntNumber = jwick["contacts"][0]["number"].asString
```

**Parsing JSON from a string**
```kotlin
val string = """
  {
    "name": "John Wick",
    "age": 37,
    "occupation": "Hitman",
    "contacts": [
      { "name": "Ethan Hunt", "number": "[REDACTED]" },
      { "name": "Jason Bourne", "number": "[REDACTED]" },
      { "name": "Bender", "number": "2716057" }
    ]
  }
"""

val jwick:Json = Json.parse(string)
```

**Converting JSON back to a string**
```kotlin
val jwickString2 = jwick.mkString())  // Default is 2-space indentation
val jwickString0 = jwick.mkString(0)) // Minified
val jwickString4 = jwick.mkString(4)) // Custom indentation depth in spaces
```

**Deserializing a class instance from JSON**
```kotlin
class Field(name:String, value:String) : JsonSchema

// The first parameter can either be a constructor function (::Name) or a class instance (Name::class)
//   The passed function or given class's primary constructor must return a subtype of JsonSchema!
// The second parameter can either be a string (will be parsed as JSON) or an existing Json instance
val field = Json.deserialize(::Field, """{"name":"Test","value":"test"}""")
```

**Deserializing with a name converter function**
```kotlin
class Thing(thingName:String) : JsonSchema

// The optional third parameter can be any (String) -> String function
// JsonSchema provides some common defaults like snake case to camel case (shown here)
val field = Json.deserialize(::Thing, """{"thing_name":"Bob"}""", JsonSchema.SNAKE_TO_CAMEL)
```

**Deserializing with overridden property names**
```kotlin
class Thing( @JsonProperty("thing_name") thingName:String ) : JsonSchema

// This will do the same as the above; useful for when a particular field doesn't follow conventions
//   or you'd like to explicitly rename something
val field = Json.deserialize(::Thing, """{"thing_name":"Bob"}""")
```

**Custom deserializer functions**
```kotlin
class WithDate(date:ZonedDateTime) : JsonSchema

// Register a deserializer function with the default group
// The input and output types of the given function are used to determine what to use
// Only one deserializer function is allowed per output type
Deserializers.register(ZonedDateTime::parse) // (String) -> ZonedDateTime
val withDate = Json.deserialize(::WithDate, """{"date":"2018-07-05T18:13:59+00:00"}""")

// Custom deserializer group
// Useful for having different ways to deserialize a particular output type or not leaking deserializers
//   to other functions
val deser = Deserializers().add(ZonedDateTime::parse)
val withDate = Json.deserialize(::WithDate, """{"date":"2018-07-05T18:13:59+00:00"}""", deserializers = deser)
```