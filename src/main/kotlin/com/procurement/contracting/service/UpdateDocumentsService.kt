package com.procurement.contracting.service

import com.procurement.contracting.dao.AcDao
import com.procurement.contracting.dao.CanDao
import com.procurement.contracting.exception.ErrorException
import com.procurement.contracting.exception.ErrorType
import com.procurement.contracting.exception.ErrorType.*
import com.procurement.contracting.model.dto.*
import com.procurement.contracting.model.dto.bpe.CommandMessage
import com.procurement.contracting.model.dto.bpe.ResponseDto
import com.procurement.contracting.model.dto.ocds.Can
import com.procurement.contracting.model.dto.ocds.ContractStatus
import com.procurement.contracting.model.dto.ocds.ContractStatusDetails
import com.procurement.contracting.model.dto.ocds.DocumentContract
import com.procurement.contracting.utils.toJson
import com.procurement.contracting.utils.toObject
import org.springframework.stereotype.Service

@Service
class UpdateDocumentsService(private val canDao: CanDao,
                             private val acDao: AcDao) {

    fun updateCanDocs(cm: CommandMessage): ResponseDto {
        val cpId = cm.context.cpid ?: throw ErrorException(CONTEXT)
        val canId = cm.context.id ?: throw ErrorException(CONTEXT)

        val dto = toObject(UpdateDocumentsRq::class.java, cm.data)

        val canEntity = canDao.getByCpIdAndAcId(cpId, canId)
        val canAcOcId = canEntity.acId ?: throw ErrorException(CAN_AC_ID_NOT_FOUND)
        val acEntity = acDao.getByCpIdAndAcId(cpId, canAcOcId)
        val contractProcess = toObject(ContractProcess::class.java, acEntity.jsonData)


        if (contractProcess.contract.status != ContractStatus.PENDING) throw ErrorException(ErrorType.CONTRACT_STATUS)
        if (!(contractProcess.contract.statusDetails == ContractStatusDetails.CONTRACT_PREPARATION
                || contractProcess.contract.statusDetails == ContractStatusDetails.CONTRACT_PROJECT)) throw ErrorException(ErrorType.CONTRACT_STATUS_DETAILS)

        val can = toObject(Can::class.java, canEntity.jsonData)
        val canDocuments = can.contract.documents?.toMutableList() ?: mutableListOf()
        if (canDocuments.isEmpty()) {
            validateRelatedLotInRq(dto, contractProcess)
            val newDocuments: List<DocumentContract> = mutableListOf()
            dto.documents.forEach {
                newDocuments.toMutableList().add(DocumentContract(
                    id = it.id,
                    documentType = it.documentType,
                    title = it.title,
                    description = it.description,
                    relatedLots = it.relatedLots
                ))

                canDocuments.addAll(newDocuments)
            }
        } else {
            val newDocs = newDocumentsInRq(dtoDocuments = dto.documents, canDocuments = canDocuments)
            canDocuments.forEach { docDb -> docDb.update(dto.documents.firstOrNull { it.id == docDb.id }) }
            if (isNewDocumentsInRq(newDocs)) {
                validateRelatedLotInRq(dto, contractProcess)
                canDocuments.addAll(newDocs!!)
            }
        }

        can.contract.apply {
            documents = canDocuments
        }

        canEntity.jsonData = toJson(can)
        canDao.save(canEntity)

        return ResponseDto(data = UpdateDocumentsRs(
            contract = UpdateDocumentContract(
                id = canAcOcId,
                documents = canDocuments
            )
        ))
    }

    private fun validateRelatedLotInRq(dto: UpdateDocumentsRq, contractProcess: ContractProcess) {
        val relatedLots: MutableList<String> = mutableListOf()

        dto.documents.forEach {
            val relatedLot = it.relatedLots?.toMutableList() ?: mutableListOf()
            if (relatedLot.isNotEmpty()) {
                relatedLots.addAll(relatedLot)
            }
        }

        if (relatedLots.isNotEmpty() && !relatedLots.contains(contractProcess.award.relatedLots.first()))
            throw ErrorException(DOCS_RELATED_LOTS)


    }

    private fun newDocumentsInRq(dtoDocuments: List<UpdateDocument>, canDocuments: List<DocumentContract>): List<DocumentContract>? {
        val newDocuments: List<DocumentContract> = mutableListOf()
        val canDocumentsIds: MutableList<String> = mutableListOf()

        canDocuments.forEach {
            canDocumentsIds.add(it.id)
        }

        dtoDocuments.forEach {
            if (!canDocumentsIds.contains(it.id)) {
                newDocuments.toMutableList().add(DocumentContract(
                    id = it.id,
                    documentType = it.documentType,
                    title = it.title,
                    description = it.description,
                    relatedLots = it.relatedLots
                ))
            }
        }
        return newDocuments


    }

    private fun isNewDocumentsInRq(documents: List<DocumentContract>?): Boolean {
        if (documents != null && documents.isNotEmpty()) return true
        return false
    }

    private fun DocumentContract.update(documentDto: UpdateDocument?) {
        if (documentDto != null) {
            this.title = documentDto.title
            this.description = documentDto.description
        }
    }
}
