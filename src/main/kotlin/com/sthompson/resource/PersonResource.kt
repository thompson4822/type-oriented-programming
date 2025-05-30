package com.sthompson.resource

import com.sthompson.domain.Email
import com.sthompson.domain.Phone
import com.sthompson.domain.events.PersonEvent
import com.sthompson.domain.result.FailureReason
import com.sthompson.dto.PersonDto
import com.sthompson.entity.Person
import com.sthompson.service.EventPublisher
import com.sthompson.service.PersonService
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder

@Path("/persons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class PersonResource {
    @Inject
    lateinit var personService: PersonService

    @Inject
    lateinit var eventPublisher: EventPublisher

    @GET
    fun getAll(): List<PersonDto> =
        Person.listAll().map {
            PersonDto(it.id, it.name, it.email, it.phone)
        }

    @GET
    @Path("/{id}")
    fun getById(@PathParam("id") id: Long): Response {
        val result = personService.findById(id)

        return result.fold(
            onSuccess = { person ->
                Response.ok(PersonDto(person.id, person.name, person.email, person.phone)).build()
            },
            onFailure = { reason ->
                when (reason) {
                    is FailureReason.NotFound -> Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to reason.message))
                        .build()
                    else -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(mapOf("message" to "An error occurred"))
                        .build()
                }
            }
        )
    }

    @POST
    @Transactional
    fun create(dto: PersonDto): Response {
        val result = personService.createPerson(dto)

        return result.fold(
            onSuccess = { person ->
                Response.created(
                    UriBuilder.fromResource(PersonResource::class.java)
                        .path(person.id.toString())
                        .build()
                ).entity(PersonDto(person.id, person.name, person.email, person.phone)).build()
            },
            onFailure = { reason ->
                when (reason) {
                    is FailureReason.PersonFailure.EmailAlreadyExists,
                    is FailureReason.PersonFailure.PhoneAlreadyExists,
                    is FailureReason.Conflict -> Response.status(Response.Status.CONFLICT)
                        .entity(mapOf("message" to reason.toString()))
                        .build()
                    else -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(mapOf("message" to "An error occurred"))
                        .build()
                }
            }
        )
    }

    @PUT
    @Path("/{id}")
    @Transactional
    fun update(@PathParam("id") id: Long, dto: PersonDto): Response {
        val result = personService.updatePerson(id, dto)

        return result.fold(
            onSuccess = { person ->
                Response.ok(PersonDto(person.id, person.name, person.email, person.phone)).build()
            },
            onFailure = { reason ->
                when (reason) {
                    is FailureReason.NotFound -> Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to reason.message))
                        .build()
                    is FailureReason.Conflict -> Response.status(Response.Status.CONFLICT)
                        .entity(mapOf("message" to reason.message))
                        .build()
                    else -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(mapOf("message" to "An error occurred"))
                        .build()
                }
            }
        )
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    fun delete(@PathParam("id") id: Long): Response {
        val result = personService.deletePerson(id)

        return result.fold(
            onSuccess = {
                Response.noContent().build()
            },
            onFailure = { reason ->
                when (reason) {
                    is FailureReason.NotFound -> Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to reason.message))
                        .build()
                    else -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(mapOf("message" to "An error occurred"))
                        .build()
                }
            }
        )
    }

    @GET
    @Path("/byEmail/{email}")
    fun findByEmail(@PathParam("email") email: Email): Response {
        val result = personService.findByEmail(email)

        return result.fold(
            onSuccess = { person ->
                Response.ok(PersonDto(person.id, person.name, person.email, person.phone)).build()
            },
            onFailure = { reason ->
                when (reason) {
                    is FailureReason.NotFound -> Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to reason.message))
                        .build()
                    else -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(mapOf("message" to "An error occurred"))
                        .build()
                }
            }
        )
    }

    @GET
    @Path("/byPhone/{phone}")
    fun findByPhone(@PathParam("phone") phone: Phone): Response {
        val result = personService.findByPhone(phone)

        return result.fold(
            onSuccess = { person ->
                Response.ok(PersonDto(person.id, person.name, person.email, person.phone)).build()
            },
            onFailure = { reason ->
                when (reason) {
                    is FailureReason.NotFound -> Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to reason.message))
                        .build()
                    else -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(mapOf("message" to "An error occurred"))
                        .build()
                }
            }
        )
    }

    @POST
    @Path("/{id}/verify-email")
    @Transactional
    fun verifyEmail(@PathParam("id") id: Long, @QueryParam("email") email: Email): Response {
        val result = personService.verifyEmail(id, email)

        return result.fold(
            onSuccess = { person ->
                // Publish a verification event directly from the resource
                // This demonstrates that events can be published from anywhere
                val event = PersonEvent.ContactVerified.EmailVerified(
                    personId = id,
                    email = email
                )
                eventPublisher.publish(event)

                Response.ok(PersonDto(person.id, person.name, person.email, person.phone)).build()
            },
            onFailure = { reason ->
                when (reason) {
                    is FailureReason.NotFound -> Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to reason.message))
                        .build()
                    is FailureReason.PersonFailure.EmailMismatch,
                    is FailureReason.ValidationFailed -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(mapOf("message" to reason.toString()))
                        .build()
                    else -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(mapOf("message" to "An error occurred"))
                        .build()
                }
            }
        )
    }

    /**
     * Endpoint to verify a phone number
     */
    @POST
    @Path("/{id}/verify-phone")
    @Transactional
    fun verifyPhone(@PathParam("id") id: Long, @QueryParam("phone") phone: Phone): Response {
        val personResult = personService.findById(id)

        return personResult.fold(
            onSuccess = { person ->
                if (person.phone != phone) {
                    val mismatchReason = FailureReason.PersonFailure.PhoneMismatch(
                        expected = person.phone,
                        actual = phone
                    )
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(mapOf("message" to mismatchReason.toString()))
                        .build()
                } else {

                    // Publish a verification event
                    val event = PersonEvent.ContactVerified.PhoneVerified(
                        personId = id,
                        phone = phone
                    )
                    eventPublisher.publish(event)

                    Response.ok(PersonDto(person.id, person.name, person.email, person.phone)).build()
                }
            },
            onFailure = { reason ->
                when (reason) {
                    is FailureReason.NotFound -> Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to reason.message))
                        .build()
                    else -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(mapOf("message" to "An error occurred"))
                        .build()
                }
            }
        )
    }
}
