[![Build Status](https://www.travis-ci.com/tripl3dogdare/haven-json.svg?branch=master)](https://www.travis-ci.com/tripl3dogdare/haven-json)
[![Issue Count](https://img.shields.io/github/issues/tripl3dogdare/haven-json.svg)](https://github.com/tripl3dogdare/haven-json/issues)
[![Contributions Welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/dwyl/esta/issues)

# Haven
Haven is a  simple JSON library written in pure Kotlin with no external dependencies.
It was designed with simplicity of use in mind, with the goal of letting the user manipulate
JSON with an almost Javascript-like syntax.

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

Haven may eventually support a data-binding style of JSON manipulation, but for now, it's at
the bottom of the priority list. Haven's philosophy revolves around simplicity and flexibility,
and data-binding is by necessity rather complex, verbose, and tedious - it has it's uses,
certainly, but when it comes to this project I find it more important to focus on making Haven 
as awesome as possible within it's existing philosophy. That said, if anyone would like to create 
data-binding capabilities for Haven as an addon library, feel free to let me know and I'll be 
sure to link you here.

## Installation
You can install Haven with [JitPack](https://jitpack.io/#tripl3dogdare/haven-json)
(Gradle example shown below) or download the source code and build it yourself.

```groovy
allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.tripl3dogdare:haven-json:master-SNAPSHOT'
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
- [ ] Manual data class destructuring conventions
- [ ] Automatic data class destructuring

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
val jwickString2 = jwick.mkString()) // Default is 2-space indentation
val jwickString0 = jwick.mkString(0)) // Minified
val jwickString4 = jwick.mkString(4)) // Custom indentation depth in spaces
```
