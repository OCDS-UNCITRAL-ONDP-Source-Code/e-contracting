package com.procurement.contracting.model.dto.ocds

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.procurement.contracting.domain.model.award.AwardId
import com.procurement.contracting.domain.model.bid.BidId
import com.procurement.contracting.domain.model.lot.LotId
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Award @JsonCreator constructor(

    val id: AwardId,

    var date: LocalDateTime,

    var description: String? = null,

    var title: String? = null,

    var value: Value,

    val relatedLots: List<LotId>,

    val relatedBid: BidId,

    var documents: List<DocumentAward>?,

    var suppliers: List<OrganizationReferenceSupplier>
)
