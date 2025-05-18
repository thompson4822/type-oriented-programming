package com.sthompson.resource

import com.sthompson.domain.result.FailureReason
import com.sthompson.dto.OrganizationDto
import com.sthompson.entity.Organization
import com.sthompson.service.OrganizationService
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder

/**
 * REST resource for Organization entities
 */
@Path("/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class OrganizationResource {
    @Inject
    lateinit var organizationService: OrganizationService

    @GET
    fun getAll(): List<OrganizationDto> =
        Organization.listAll().map { OrganizationDto.fromEntity(it) }

    @GET
    @Path("/{id}")
    fun getById(@PathParam("id") id: Long): Response {
        val result = organizationService.findById(id)

        return result.fold(
            onSuccess = { organization ->
                Response.ok(OrganizationDto.fromEntity(organization)).build()
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
    fun create(dto: OrganizationDto, @QueryParam("creatorId") creatorId: Long): Response {
        val result = organizationService.createOrganization(dto, creatorId)

        return result.fold(
            onSuccess = { organization ->
                Response.created(
                    UriBuilder.fromResource(OrganizationResource::class.java)
                        .path(organization.id.toString())
                        .build()
                ).entity(OrganizationDto.fromEntity(organization)).build()
            },
            onFailure = { reason ->
                when (reason) {
                    is FailureReason.NotFound -> Response.status(Response.Status.NOT_FOUND)
                        .entity(mapOf("message" to reason.message))
                        .build()
                    is FailureReason.OrganizationFailure.NameAlreadyExists,
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

    @GET
    @Path("/{id}/members")
    fun getMembers(@PathParam("id") id: Long): Response {
        val result = organizationService.getMembers(id)

        return result.fold(
            onSuccess = { memberships ->
                Response.ok(memberships.map { com.sthompson.dto.MembershipDto.fromEntity(it) }).build()
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
    @Path("/byName/{name}")
    fun findByName(@PathParam("name") name: String): Response {
        val result = organizationService.findByName(name)

        return result.fold(
            onSuccess = { organization ->
                Response.ok(OrganizationDto.fromEntity(organization)).build()
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
