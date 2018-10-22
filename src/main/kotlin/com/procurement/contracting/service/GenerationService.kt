package com.procurement.contracting.service

import com.datastax.driver.core.utils.UUIDs
import com.procurement.contracting.utils.milliNowUTC
import org.springframework.stereotype.Service
import java.util.*

@Service
class GenerationService {

    fun generateRandomUUID(): UUID {
        return UUIDs.random()
    }

    fun generateTimeBasedUUID(): UUID {
        return UUIDs.timeBased()
    }

    fun newOcId(cpId: String, stage: String): String {
        return cpId + "-" + stage.toUpperCase() + "-" + (milliNowUTC() + Random().nextInt())
    }
}