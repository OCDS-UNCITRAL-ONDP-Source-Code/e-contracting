package com.procurement.contracting.application.repository.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.procurement.contracting.infrastructure.handler.v1.model.request.TreasuryBudgetSource
import com.procurement.contracting.model.dto.ocds.AwardContract
import com.procurement.contracting.model.dto.ocds.ContractedAward
import com.procurement.contracting.model.dto.ocds.OrganizationReference
import com.procurement.contracting.model.dto.ocds.OrganizationReferenceBuyer
import com.procurement.contracting.model.dto.ocds.Planning
import com.procurement.contracting.model.dto.ocds.TreasuryData

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContractProcess @JsonCreator constructor(

        var planning: Planning? = null,

        val contract: AwardContract,

        val award: ContractedAward,

        var buyer: OrganizationReferenceBuyer? = null,

        var funders: List<OrganizationReference>? = null,

        var payers: List<OrganizationReference>? = null,

        var treasuryBudgetSources: List<TreasuryBudgetSource>? = null,

        var treasuryData: TreasuryData? = null

)