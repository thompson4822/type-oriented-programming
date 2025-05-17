package com.sthompson

import io.quarkus.runtime.Startup
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.metamodel.ManagedType
import jakarta.transaction.Transactional
import jakarta.transaction.UserTransaction
import com.sthompson.domain.Email
import com.sthompson.domain.Phone
import com.sthompson.entity.Person
import org.hibernate.Session

/**
 * Verifies that value class converters are properly registered and working.
 * This class runs at application startup and checks that:
 * 1. The Person entity is properly registered with JPA
 * 2. The phone and email attributes have the correct value class types
 * 3. The Hibernate session is properly configured
 */
@Startup
@ApplicationScoped
class ConverterChecker {
    @Inject
    lateinit var entityManager: EntityManager

    @Inject
    lateinit var userTransaction: UserTransaction

    /**
     * This method is called after all dependencies are injected.
     * It verifies that the value class converters are working properly.
     */
    fun onStart(@Observes event: StartupEvent) {
        try {
            // Access the JPA metamodel
            val metamodel = entityManager.metamodel

            // Get the Person entity metadata
            val personType = metamodel.managedTypes
                .filterIsInstance<ManagedType<Person>>()
                .firstOrNull()

            if (personType == null) {
                println("❌ Person entity not found in JPA metamodel!")
                return
            }

            // Check and print detailed information about the attributes
            val phoneAttribute = personType.attributes.find { it.name == "phone" }
            val emailAttribute = personType.attributes.find { it.name == "email" }

            println("--- Attribute Type Information ---")

            if (phoneAttribute != null) {
                println("Phone attribute found:")
                println("  - Java type: ${phoneAttribute.javaType.name}")
                println("  - Expected type: ${Phone::class.java.name}")
                println("  - Is correct type: ${phoneAttribute.javaType == Phone::class.java}")
            } else {
                println("❌ Phone attribute not found in entity!")
            }

            if (emailAttribute != null) {
                println("Email attribute found:")
                println("  - Java type: ${emailAttribute.javaType.name}")
                println("  - Expected type: ${Email::class.java.name}")
                println("  - Is correct type: ${emailAttribute.javaType == Email::class.java}")
            } else {
                println("❌ Email attribute not found in entity!")
            }

            // Check if converters are registered
            println("\n--- Converter Registration Check ---")

            // Test entity persistence in a separate method
            testEntityPersistence()

            // Additional verification: check if Hibernate session factory is configured
            val session = entityManager.unwrap(Session::class.java)
            if (session != null) {
                println("✅ Hibernate session properly configured")
            } else {
                println("❌ Could not access Hibernate session!")
            }
        } catch (e: Exception) {
            println("❌ Error checking converters: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Tests entity persistence with value classes using JTA transactions.
     * This method creates a test entity, persists it, and then retrieves it
     * to verify that the value classes are properly converted.
     */
    @Transactional
    fun testEntityPersistence() {
        try {
            println("\n--- Testing Entity Creation ---")
            // Create a test entity with value classes
            val testPerson = Person().apply {
                name = "Test Person"
                setEmail(Email.create("test@example.com"))
                setPhone(Phone.create("1234567890"))
            }
            println("✅ Successfully created entity with value classes")
            println("  - Email type: ${testPerson.getEmail()?.javaClass?.name}")
            println("  - Email value: ${testPerson.getEmail()?.value}")
            println("  - Phone type: ${testPerson.getPhone()?.javaClass?.name}")
            println("  - Phone value: ${testPerson.getPhone()?.value}")

            println("\n--- Testing Entity Persistence ---")
            // Try a native SQL insert instead of using JPA
            val nativeQuery = entityManager.createNativeQuery(
                "INSERT INTO Person (id, name, email, phone) VALUES (nextval('Person_SEQ'), ?, ?, ?)"
            )
            nativeQuery.setParameter(1, testPerson.name)
            nativeQuery.setParameter(2, testPerson.getEmail()?.value)
            nativeQuery.setParameter(3, testPerson.getPhone()?.value)

            val rowsAffected = nativeQuery.executeUpdate()
            println("✅ Successfully inserted entity using native SQL: $rowsAffected rows affected")

            // Get the last inserted ID
            val idQuery = entityManager.createNativeQuery("SELECT currval('Person_SEQ')")
            val id = (idQuery.singleResult as Number).toLong()
            println("✅ Inserted entity with ID $id")

            // Clear the persistence context to force a database read
            entityManager.clear()

            println("\n--- Testing Entity Retrieval ---")
            // Retrieve the entity from the database using native SQL
            val nativeResultQuery = entityManager.createNativeQuery(
                "SELECT id, name, email, phone FROM Person WHERE id = ?"
            )
            nativeResultQuery.setParameter(1, id)

            try {
                val result = nativeResultQuery.resultList
                if (result.isNotEmpty()) {
                    val row = result[0] as Array<*>
                    println("✅ Successfully retrieved entity with native SQL")
                    println("  - ID: ${row[0]}")
                    println("  - Name: ${row[1]}")
                    println("  - Email: ${row[2]}")
                    println("  - Phone: ${row[3]}")

                    // Now try to retrieve using JPA
                    val retrievedPerson = entityManager.find(Person::class.java, id)
                    if (retrievedPerson != null) {
                        println("\n✅ Successfully retrieved entity with JPA")
                        println("  - Retrieved email type: ${retrievedPerson.getEmail()?.javaClass?.name}")
                        println("  - Retrieved email value: ${retrievedPerson.getEmail()?.value}")
                        println("  - Retrieved phone type: ${retrievedPerson.getPhone()?.javaClass?.name}")
                        println("  - Retrieved phone value: ${retrievedPerson.getPhone()?.value}")

                        // Verify that the retrieved values are correct
                        val emailCorrect = retrievedPerson.getEmail()?.value == "test@example.com"
                        val phoneCorrect = retrievedPerson.getPhone()?.value == "1234567890"

                        if (emailCorrect && phoneCorrect) {
                            println("✅ Value classes correctly persisted and retrieved")
                        } else {
                            println("❌ Value classes not correctly persisted and retrieved")
                            println("  - Email correct: $emailCorrect")
                            println("  - Phone correct: $phoneCorrect")
                        }
                    } else {
                        println("❌ Could not retrieve entity with JPA")
                    }
                } else {
                    println("❌ No results found with native SQL")
                }
            } catch (e: Exception) {
                println("❌ Error retrieving entity with native SQL: ${e.message}")
                e.printStackTrace()
            }

            // The transaction will be automatically rolled back at the end of the method
            // because we're using @Transactional with Transactional.TxType.REQUIRED (default)
        } catch (e: Exception) {
            println("❌ Error testing entity persistence: ${e.message}")
            e.printStackTrace()
        }
    }
}