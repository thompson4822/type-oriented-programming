package com.sthompson.resource

import com.sthompson.domain.Email
import com.sthompson.domain.Phone
import com.sthompson.dto.PersonDto
import com.sthompson.entity.Person
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder

@Path("/persons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class PersonResource {

    @GET
    fun getAll(): List<PersonDto> =
        Person.listAll().map {
            PersonDto(it.id, it.name, it.email, it.phone)
        }

    @GET
    @Path("/{id}")
    fun getById(@PathParam("id") id: Long): Response {
        val person = Person.findById(id) ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(PersonDto(person.id, person.name, person.email, person.phone)).build()
    }

    @POST
    @Transactional
    fun create(dto: PersonDto): Response {
        val person = Person().apply {
            name = dto.name
            email = dto.email
            phone = dto.phone
        }
        person.persist()

        return Response.created(
            UriBuilder.fromResource(PersonResource::class.java)
                .path(person.id.toString())
                .build()
        ).entity(PersonDto(person.id, person.name, person.email, person.phone)).build()
    }

    @GET
    @Path("/byEmail/{email}")
    fun findByEmail(@PathParam("email") email: Email): Response {
        val person = Person.findByEmail(email) ?:
            return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("message" to "No person found with email ${email.value}"))
                .build()

        return Response.ok(PersonDto(person.id, person.name, person.email, person.phone)).build()
    }

    @GET
    @Path("/byPhone/{phone}")
    fun findByPhone(@PathParam("phone") phone: Phone): Response {
        val person = Person.findByPhone(phone) ?:
            return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("message" to "No person found with phone ${phone.value}"))
                .build()

        return Response.ok(PersonDto(person.id, person.name, person.email, person.phone)).build()
    }
}
