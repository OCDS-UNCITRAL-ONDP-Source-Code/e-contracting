package com.procurement.contracting.application.service.can

import com.procurement.contracting.domain.model.can.status.CANStatus
import com.procurement.contracting.domain.model.can.status.CANStatusDetails
import java.time.LocalDateTime
import java.util.*

data class CreatedCANData(
    val token: UUID,
    val can: CAN
) {
    data class CAN(
        val id: UUID,
        val awardId: UUID?,
        val lotId: UUID,
        val date: LocalDateTime,
        val status: CANStatus,
        val statusDetails: CANStatusDetails
    )
}