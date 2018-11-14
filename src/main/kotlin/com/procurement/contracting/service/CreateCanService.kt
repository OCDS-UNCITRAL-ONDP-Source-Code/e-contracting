package com.procurement.contracting.service

import com.procurement.contracting.dao.CanDao
import com.procurement.contracting.exception.ErrorException
import com.procurement.contracting.exception.ErrorType.CONTEXT
import com.procurement.contracting.model.dto.CanCreate
import com.procurement.contracting.model.dto.CreateCanRs
import com.procurement.contracting.model.dto.bpe.CommandMessage
import com.procurement.contracting.model.dto.bpe.ResponseDto
import com.procurement.contracting.model.dto.ocds.Can
import com.procurement.contracting.model.dto.ocds.Contract
import com.procurement.contracting.model.dto.ocds.ContractStatus
import com.procurement.contracting.model.dto.ocds.ContractStatusDetails
import com.procurement.contracting.model.entity.CanEntity
import com.procurement.contracting.utils.toDate
import com.procurement.contracting.utils.toLocalDateTime
import com.procurement.contracting.utils.toObject
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CreateCanService(private val canDao: CanDao,
                       private val generationService: GenerationService) {

    fun createCAN(cm: CommandMessage): ResponseDto {
        val cpId = cm.context.cpid ?: throw ErrorException(CONTEXT)
        val owner = cm.context.owner ?: throw ErrorException(CONTEXT)
        val dateTime = cm.context.startDate?.toLocalDateTime() ?: throw ErrorException(CONTEXT)
        val dto = toObject(CanCreate::class.java, cm.data)

        val canEntities = dto.awards.asSequence()
                .map { createCanEntity(cpId, it.id, owner, dateTime) }
                .toList()
        val cans = canEntities.asSequence().map { convertEntityToCanDto(it, dateTime) }.toList()
        canDao.saveAll(canEntities)
        return ResponseDto(data = CreateCanRs(cans))
    }

    private fun convertEntityToCanDto(entity: CanEntity, dateTime: LocalDateTime): Can {
        val contract = Contract(
                token = null,
                id = entity.canId.toString(),
                date = dateTime,
                awardId = entity.awardId,
                status = ContractStatus.fromValue(entity.status),
                statusDetails = ContractStatusDetails.fromValue(entity.statusDetails),
                documents = null,
                classification = null,
                relatedProcesses = null,
                amendments = null,
                budgetSource = null,
                dateSigned = null,
                extendsContractID = null,
                period = null,
                items = null,
                value = null,
                description = null,
                title = null,
                milestones = null,
                confirmationRequests = null,
                agreedMetrics = null)
        return Can(contract)
    }

    private fun createCanEntity(cpId: String,
                                awardId: String,
                                owner: String,
                                dateTime: LocalDateTime): CanEntity {
        return CanEntity(
                cpId = cpId,
                canId = generationService.generateRandomUUID(),
                awardId = awardId,
                acId = null,
                owner = owner,
                status = ContractStatus.PENDING.value,
                statusDetails = ContractStatusDetails.CONTRACT_PROJECT.value,
                createdDate = dateTime.toDate()
        )
    }
}