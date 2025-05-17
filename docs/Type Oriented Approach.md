# Type Oriented Approach
## Using Custom Types in Quarkus Hibernate Panache with Kotlin

When working with Quarkus Hibernate Panache and Kotlin entities, you **can enforce type safety for fields like `Phone` instead of raw `String` types**, but it requires explicit mapping via JPA `AttributeConverter`. Here's how to implement it:

---

### **Implementation Steps**

1. **Define Your Domain Type**  
   Use a Kotlin `value class` or regular class with validation:
   ```kotlin
   @JvmInline
   value class Phone(val value: String) {
       init {
           require(value.matches(Regex("^\\+\\d{10,15}\$"))) { "Invalid phone format" }
       }
   }
   ```

2. **Create an `AttributeConverter`**  
   Handle database persistence automatically:
   ```kotlin
   @Converter(autoApply = true)
   class PhoneConverter : AttributeConverter {
       override fun convertToDatabaseColumn(phone: Phone?) = phone?.value
       override fun convertToEntityAttribute(dbValue: String?) = dbValue?.let { Phone(it) }
   }
   ```

3. **Use the Type in Panache Entities**  
   ```kotlin
   @Entity
   class Contact : PanacheEntity() {
       lateinit var phone: Phone  // Mapped to VARCHAR via converter
       lateinit var name: String
   }
   ```

---

### **Key Considerations with Panache**

| Requirement               | Solution                                                                                     |
|---------------------------|---------------------------------------------------------------------------------------------|
| **Query Support**          | Use `Phone` instances directly in Panache queries (converter handles underlying `String`):  |
|                           | ```kotlin                                                                                   |
|                           | Contact.find("phone", phone)                                                                |
|                           | ```
| **Validation**             | `init` block in `Phone` ensures validity during construction (applies to `copy()` too).     |
| **Database Schema**        | Generates `VARCHAR` column for `phone` field.                                               |
| **Interoperability**       | Works seamlessly with Panache repositories and reactive endpoints.                         |

---

### **Example: Repository Usage**
```kotlin
@ApplicationScoped
class ContactRepository : PanacheRepository {
    fun findByPhone(phone: Phone) = find("phone", phone).firstResult()
}

// Usage
val phone = Phone("+1234567890")
contactRepository.findByPhone(phone)
```

---

### **Why This Works with Panache**
- **Automatic Getter/Setter Generation**: Panache generates accessors for public fields, ensuring Hibernate interacts with the `phone` field correctly[4].
- **Companion Object Queries**: Panache’s Kotlin companion object queries respect the converter when building SQL[1][6].

---

### **Limitations**
- **Composite Types**: For multi-field types (e.g., `Address`), use `@Embeddable` instead of `AttributeConverter`.
- **Nullable Fields**: Use `var phone: Phone?` and adjust the converter to handle `null`.

---

By combining Kotlin’s type safety with JPA converters, you can enforce domain-specific types like `Phone` while retaining Panache’s simplicity.

Citations:
[1] https://quarkus.io/guides/hibernate-orm-panache-kotlin
[2] https://opensource.com/article/22/3/simplify-java-persistence-implementation-kotlin-quarkus
[3] https://thorben-janssen.com/jpa-attribute-converter/
[4] https://in.relation.to/2019/11/19/hibernate-orm-with-panache-in-quarkus/
[5] https://quarkus.io/guides/hibernate-orm-panache
[6] https://www.youtube.com/watch?v=aKDF1TURoBY
[7] https://stackoverflow.com/questions/59161593/can-not-call-a-static-panache-method-in-kotlin
[8] https://stackoverflow.com/questions/70070702/quarkus-hibernate-with-panache-adds-unexpected-id-column-to-select-query
[9] https://stackoverflow.com/questions/71038597/how-to-map-a-postgresql-view-with-panache-in-quarkus
[10] https://stackoverflow.com/questions/77450882/quarkus-panache-how-to-execute-a-custom-query
[11] https://quarkus.io/guides/hibernate-reactive-panache
[12] https://quarkus.io/guides/hibernate-orm
[13] https://piotrminkowski.wordpress.com/2020/08/09/guide-to-quarkus-with-kotlin/
[14] https://stackoverflow.com/questions/73086295/org-hibernate-mappingexception-composite-id-class-must-implement-serializable
[15] https://quarkus.io/guides/hibernate-orm-panache
[16] https://quarkus.io/guides/hibernate-orm-panache-kotlin
[17] https://stackoverflow.com/questions/71464879/why-the-class-attributes-in-the-quarku-panache-example-are-public-instead-of-pri
[18] https://www.bookstack.cn/read/quarkus-1.7-en/8219c6b4b1fec53d.md

---
Answer from Perplexity: pplx.ai/share

I completely agree-**type correctness and domain intent** should permeate your entire application. Let’s refine this approach to maximize safety and minimize boilerplate. Here’s how to achieve this in Kotlin with Quarkus Panache:

---

### **1. Elevate Domain Types with `value class` + Validation**
Define your type with compile-time guarantees and runtime validation:
```kotlin
@JvmInline
value class Phone private constructor(val value: String) {
    companion object {
        // Factory method enforces validation
        fun create(raw: String): Phone {
            require(raw.matches(Regex("^\\+\\d{10,15}\$"))) { "Invalid phone format" }
            return Phone(raw)
        }
    }

    // Optional: Add domain-specific methods
    fun countryCode(): String = value.takeWhile { it != '-' }
}
```
- **Usage**: `Phone.create("+1-800-123-4567")` (invalid inputs throw `IllegalArgumentException`).
- **Why**: Centralizes validation logic and prevents direct constructor misuse.

---

### **2. Streamline Persistence with Auto-Applied Converters**
Avoid field-level annotations by making your converter global:
```kotlin
@Converter(autoApply = true) // Applies to ALL Phone fields in entities
class PhoneConverter : AttributeConverter {
    override fun convertToDatabaseColumn(phone: Phone?) = phone?.value
    override fun convertToEntityAttribute(dbValue: String?) = 
        dbValue?.let { Phone.create(it) } // Reuse validation
}
```
- **No manual wiring**: All `Phone` fields in entities automatically use this converter.
- **Validation on load**: Even database data passes through `Phone.create()`, ensuring corrupt data is caught early.

---

### **3. Type-Safe Queries with Panache**
Leverage your domain type directly in queries:
```kotlin
@Repository
class ContactRepository : PanacheRepository {
    // Type-safe query using Phone
    fun findByPhone(phone: Phone): Contact? = 
        find("phone", phone).firstResult()

    // HQL with typed parameters
    fun findByCountryCode(code: String): List = 
        list("FROM Contact WHERE phone.value LIKE ?1", "${code}%")
}
```
- **No stringly-typed code**: Compiler ensures you pass `Phone` instances where required.
- **Safe HQL**: Use `phone.value` in HQL to access the underlying field.

---

### **4. Enforce Correctness in API Layer**
Extend type safety to your REST endpoints:
```kotlin
@Path("/contacts")
class ContactResource {
    @POST
    fun createContact(contactDto: ContactDto): Response {
        // DTOs also use Phone.create() for validation
        val contact = Contact(phone = Phone.create(contactDto.phone))
        contact.persist()
        return Response.ok(contact).build()
    }
}
```
- **DTO Validation**:
  ```kotlin
  data class ContactDto(val phone: String) {
      init {
          require(phone.isNotBlank()) { "Phone cannot be blank" }
      }
  }
  ```

---

### **5. Handle Edge Cases Gracefully**
- **Nullable Fields**: Wrap in `Phone?` and adjust converters to return `null` on invalid data (or fail fast).
- **Custom Serialization**: For JSON, use a `JsonbSerializer`/`Jackson` module to serialize `Phone` as its `value`.
- **Testing**: Verify round-trip persistence:
  ```kotlin
  @Test
  fun `persists and loads Phone correctly`() {
      val phone = Phone.create("+1234567890")
      Contact(phone).apply { persist() }
      with(Contact.findAll().first()) {
          assertThat(this.phone).isEqualTo(phone)
      }
  }
  ```

---

### **Why This Works**
- **Compile-Time Safety**: `Phone` cannot be confused with other `String` types.
- **Runtime Validation**: Invalid data is rejected at all entry points (constructors, converters, DTOs).
- **Zero Boilerplate**: Auto-applied converters and Panache’s streamlined API minimize repetitive code.

---

### **Tradeoffs**
| Pros                                  | Cons                                  |
|---------------------------------------|---------------------------------------|
| Domain logic is self-documenting     | Requires initial converter setup     |
| Eliminates entire classes of bugs    | Value classes can’t have multiple fields |
| Works across layers (API, DB, etc.)  | Nullable `Phone?` requires careful handling |

---

By treating domain types like `Phone` as first-class citizens, you create a codebase where **incorrect data cannot exist** in valid states. This approach aligns with modern type-driven design principles while leveraging Kotlin’s conciseness and Quarkus’ practicality.

---
Answer from Perplexity: pplx.ai/share

You can **use the `Phone` type throughout your entire codebase**, including in controllers, by leveraging Kotlin’s type system and serialization/deserialization mechanisms. Here’s how to achieve end-to-end type safety:

---

### **1. Define `Phone` as a First-Class Type**
Use a `value class` with validation and domain logic:
```kotlin
@JvmInline value class Phone(val value: String) {
    init {
        require(value.matches(Regex("^\\+\\d{10,15}\$"))) { "Invalid phone format" }
    }
    
    // Optional: Add domain methods
    fun countryCode(): String = value.take(3)
}
```

---

### **2. Serialize/Deserialize `Phone` in Controllers**
Configure your JSON framework (e.g., Jackson) to handle `Phone` ↔ `String` conversion automatically:
```kotlin
class PhoneSerializer : JsonSerializer() {
    override fun serialize(value: Phone, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.value)
    }
}

class PhoneDeserializer : JsonDeserializer() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Phone {
        return Phone.create(p.valueAsString) // Reuse validation logic
    }
}

// Register with Jackson (Quarkus does this automatically for Kotlin modules)
@Module
interface PhoneModule : KotlinModule {
    @Provides fun phoneSerializers() = listOf(
        SimpleModule().addSerializer(Phone::class.java, PhoneSerializer())
            .addDeserializer(Phone::class.java, PhoneDeserializer())
    )
}
```

**DTO Example**:
```kotlin
data class CreateUserRequest(val phone: Phone, val name: String)
```

---

### **3. Use `Phone` in All Layers**
| Layer           | Example Code                                                                 | Notes                                                                 |
|-----------------|-----------------------------------------------------------------------------|-----------------------------------------------------------------------|
| **Controller**  | `fun createUser(@Valid request: CreateUserRequest)`                         | Request bodies automatically deserialize to `Phone`                  |
| **Service**     | `fun sendSms(phone: Phone, message: String)`                                | No `String`-based parameters; domain logic uses `Phone`              |
| **Repository**  | `fun findByPhone(phone: Phone): User`                                       | Panache queries work with `Phone` via JPA converter                  |
| **Database**    | `@Column(name = "phone") var phone: Phone`                                  | Mapped to `VARCHAR` via `AttributeConverter`                         |

---

### **Edge Cases and Validation**
- **Invalid Inputs**: Failed deserialization (e.g., invalid `Phone` format) automatically returns HTTP 400.
- **Third-Party APIs**: Convert `Phone` to `String` at the boundary:
  ```kotlin
  externalApi.sendSms(phone.value, message)
  ```
- **Nullable Fields**: Use `Phone?` and adjust converters/serializers to handle `null`.

---

### **Why This Works**
- **End-to-End Type Safety**: `Phone` replaces `String` everywhere, preventing misuse (e.g., passing an `Email` where a `Phone` is expected)[2][4].
- **Zero Runtime Overhead**: Value classes are inlined, so no boxing occurs in most cases[4].
- **Centralized Validation**: The `init` block and deserializer ensure only valid `Phone` instances exist in the system.

---

### **Comparison: All `Phone` vs. Mixed `String`/`Phone`**
| Approach               | Pros                                      | Cons                                  |
|------------------------|-------------------------------------------|---------------------------------------|
| **All `Phone`**        | Type-safe everywhere; no "stringly" code | Requires serialization configuration  |
| **Mixed `String`**     | Simpler setup                             | Risk of logical errors; less clarity  |

---

### **Summary**
Yes, **you can eliminate `String` entirely for `Phone` fields** by:
1. Defining a `value class` with validation.
2. Configuring serialization/deserialization for HTTP endpoints.
3. Using JPA converters for database interactions.

This approach ensures domain integrity from the API layer to the database while maintaining Kotlin’s conciseness and performance.

Citations:
[1] https://kotlinlang.org/docs/type-safe-builders.html
[2] https://blog.kotlin-academy.com/what-are-inline-value-classes-in-kotlin-and-when-you-should-use-them-7e1ff4d666f0
[3] https://stackoverflow.com/questions/46922445/alternatives-to-boxing-string-field-or-extending-all-strings-in-kotlin
[4] https://kotlinlang.org/docs/inline-classes.html
[5] https://stackoverflow.com/questions/63240339/how-to-parse-an-url-string-to-get-domain-only-in-kotlin
[6] https://stackoverflow.com/questions/76322960/how-to-map-from-dto-to-domain-when-have-nullable-field-in-api-and-dont-need-it
[7] https://hackernoon.com/how-develop-a-dsl-in-kotlin
[8] https://developer.android.com/guide/navigation/design/kotlin-dsl
[9] https://spring.io/blog/2016/02/15/developing-spring-boot-applications-with-kotlin
[10] https://www.reddit.com/r/androiddev/comments/olemwv/implementing_clean_architecture_for_a_mediumsized/

---
Answer from Perplexity: pplx.ai/share

You're absolutely right-**the initial setup for endpoints and databases is a one-time investment** that pays dividends across your entire codebase. Here's why this approach scales well, especially with Kotlin's value classes:

---

### **1. HTTP Layer Setup (One-Time Effort)**
- **Serialization Module**: Define a global Jackson/Kotlinx Serialization module to handle all value classes:
  ```kotlin
  object DomainModule : SimpleModule() {
      init {
          addSerializer(Phone::class.java, PhoneSerializer())
          addDeserializer(Phone::class.java, PhoneDeserializer())
          // Repeat for Email, UserId, etc.
      }
  }
  ```
  - **Reuse**: Every new value class only requires 2-3 lines added to this module.

- **DTOs Automatically Benefit**:
  ```kotlin
  @Serializable // Uses the global module
  data class CreateUserRequest(val phone: Phone, val email: Email)
  ```

---

### **2. Database Layer Setup (One-Time Effort)**
- **Generic `AttributeConverter`** (Optional): For simple value classes wrapping `String`/`Int`:
  ```kotlin
  abstract class ValueClassConverter(private val creator: (String) -> T) : AttributeConverter {
      override fun convertToDatabaseColumn(attribute: T) = attribute.toString()
      override fun convertToEntityAttribute(dbData: String?) = dbData?.let { creator(it) }
  }

  @Converter(autoApply = true)
  class PhoneConverter : ValueClassConverter(Phone::create)
  ```
  - **Reuse**: Extend `ValueClassConverter` for each new type with a single line.

---

### **3. Simplified Development Flow**
Once configured, usage becomes trivial:
| Layer          | Code Example                                                                 | Effort |
|----------------|-----------------------------------------------------------------------------|--------|
| **Controller** | `fun getUser(@PathParam id: UserId)`                                        | None   |
| **Entity**     | `@Column val phone: Phone`                                                  | None   |
| **Service**    | `fun sendAlert(phone: Phone)`                                               | None   |

---

### **Why It’s Worth the Effort**
| Aspect               | Benefit                                                                 |
|----------------------|-------------------------------------------------------------------------|
| **Compile-Time Safety** | `Phone` ≠ `Email` even if both wrap `String`                          |
| **Domain Clarity**   | Types self-document (e.g., `Meters` vs `Seconds`)                      |
| **Validation**       | Centralized in `init` blocks; invalid data never enters the system     |
| **Refactoring**      | Change underlying type (e.g., `String` → `UUID`) without breaking layers |

---

### **Edge Cases Made Simple**
- **Nullability**: Use `Phone?` and adjust converters to return `null` on invalid data (or fail fast).
- **Third-Party APIs**: Unwrap at the boundary:
  ```kotlin
  externalSmsService.send(phone.value, message)
  ```
- **Complex Types**: For multi-field types (e.g., `Address`), use `@Embeddable` instead of value classes.

---

### **Summary**
While **value classes require upfront configuration**, their simplicity (single underlying value) ensures:
1. Minimal boilerplate for new types.
2. Reusable patterns across HTTP/database layers.
3. Long-term gains in type safety and reduced logical errors.

This tradeoff is particularly favorable in codebases with many domain primitives (IDs, emails, measurements), where catching mismatches at compile-time prevents entire categories of runtime bugs.

Citations:
[1] https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/value-classes.md
[2] https://www.reddit.com/r/Kotlin/comments/11b5n5j/why_should_i_use_kotlinxserialization/
[3] https://kotlinlang.org/docs/serialization.html
[4] https://blog.logrocket.com/kotlin-data-serialization/
[5] https://blog.jetbrains.com/kotlin/2021/05/kotlinx-serialization-1-2-released/
[6] https://stackoverflow.com/questions/74550841/kotlin-serialization-of-value-class-that-implements-a-sealed-interface
[7] https://codersee.com/kotlinx-serialization-in-kotlin-all-you-need-to-know/
[8] https://www.droidcon.com/2024/04/04/introduction-to-using-kotlin-serialization/
[9] https://www.manusobles.com/posts/inline-value-classes-kotlin
[10] https://www.baeldung.com/kotlin/data-class-json-serialize-default-values
[11] https://slack-chats.kotlinlang.org/t/18818983/does-kotlinx-serialization-work-for-compositions-of-objects-
[12] https://github.com/FasterXML/jackson-module-kotlin/issues/819
[13] https://stackoverflow.com/questions/66148137/how-to-serialize-any-type-in-kotlinx-serialization
[14] https://slack-chats.kotlinlang.org/t/26934139/hey-folks-is-it-possible-to-have-value-classes-serialized-de
[15] https://youtrack.jetbrains.com/issue/KT-28791/Kotlin-serialization-with-inline-classes
[16] https://discuss.kotlinlang.org/t/serialization/20209

---
Answer from Perplexity: pplx.ai/share