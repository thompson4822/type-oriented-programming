package com.sthompson.resource

import com.sthompson.domain.result.FailureReason
import com.sthompson.dto.MembershipDto
import com.sthompson.entity.MembershipRole
import com.sthompson.service.OrganizationService
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder

/**
 * REST resource for managing memberships between persons and organizations
 */
@Path("/memberships")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MembershipResource {
    @Inject
    lateinit var organizationService: OrganizationService
    
    @POST
    @Path("/organizations/{organizationId}/members/{personId}")
    @Transactional
    fun addMember(
        @PathParam("organizationId") organizationId: Long,
        @PathParam("personId") personId: Long,
        @QueryParam("role") @DefaultValue("MEMBER") role: MembershipRole
    ): Response {
        val result = organizationService.addMember(organizationId, personId, role)
        
        return result.fold(
            onSuccess = { membership ->
                Response.created(
                    UriBuilder.fromResource(MembershipResource::class.java)
                        .path(membership.id.toString())
                        .build()
                ).entity(MembershipDto.fromEntity(membership)).build()
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
    
    @GET
    @Path("/persons/{personId}/organizations")
    fun getPersonOrganizations(@PathParam("personId") personId: Long): Response {
        val result = organizationService.getPersonOrganizations(personId)
        
        return result.fold(
            onSuccess = { memberships ->
                Response.ok(memberships.map { MembershipDto.fromEntity(it) }).build()
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
