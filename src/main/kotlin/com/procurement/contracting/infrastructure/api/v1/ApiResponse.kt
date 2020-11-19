package com.procurement.contracting.infrastructure.api.v1

import com.fasterxml.jackson.annotation.JsonProperty
import com.procurement.contracting.infrastructure.api.ApiVersion
import com.procurement.contracting.infrastructure.api.command.id.CommandId

sealed class ApiResponse {
    abstract val id: CommandId
    abstract val version: ApiVersion
}

class ApiErrorResponse(
    @field:JsonProperty("id") @param:JsonProperty("id") override val id: CommandId,
    @field:JsonProperty("version") @param:JsonProperty("version") override val version: ApiVersion,
    @field:JsonProperty("errors") @param:JsonProperty("errors") val errors: List<Error>
) : ApiResponse() {
    data class Error(
        @field:JsonProperty("code") @param:JsonProperty("code") val code: String,
        @field:JsonProperty("description") @param:JsonProperty("description") val description: String
    )
}

class ApiSuccessResponse(
    @field:JsonProperty("id") @param:JsonProperty("id") override val id: CommandId,
    @field:JsonProperty("version") @param:JsonProperty("version") override val version: ApiVersion,

    @field:JsonProperty("data") @param:JsonProperty("data") val data: Any
) : ApiResponse()
