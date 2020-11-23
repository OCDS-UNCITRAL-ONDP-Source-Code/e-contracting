package com.procurement.contracting.application.service.model

import com.procurement.contracting.domain.model.Owner
import com.procurement.contracting.domain.model.lot.LotId
import com.procurement.contracting.domain.model.process.Cpid
import com.procurement.contracting.domain.model.process.Ocid
import java.time.LocalDateTime

data class CreateCANContext(
    val cpid: Cpid,
    val ocid: Ocid,
    val owner: Owner,
    val startDate: LocalDateTime,
    val lotId: LotId
)
