package com.procurement.contracting.infrastructure.handler.v1

import com.procurement.contracting.application.service.ActivationAwardContractService
import com.procurement.contracting.application.service.AwardContractService
import com.procurement.contracting.application.service.CANService
import com.procurement.contracting.application.service.CancelCANContext
import com.procurement.contracting.application.service.CancelCANData
import com.procurement.contracting.application.service.CancelCANService
import com.procurement.contracting.application.service.CreateCanService
import com.procurement.contracting.application.service.FinalUpdateService
import com.procurement.contracting.application.service.IssuingAwardContractService
import com.procurement.contracting.application.service.SigningAwardContractService
import com.procurement.contracting.application.service.StatusService
import com.procurement.contracting.application.service.TreasuryProcessing
import com.procurement.contracting.application.service.UpdateAwardContractService
import com.procurement.contracting.application.service.UpdateDocumentsService
import com.procurement.contracting.application.service.VerificationAwardContractService
import com.procurement.contracting.application.service.model.CreateAwardContractContext
import com.procurement.contracting.application.service.model.CreateAwardContractData
import com.procurement.contracting.application.service.model.CreateCANContext
import com.procurement.contracting.application.service.model.CreateCANData
import com.procurement.contracting.application.service.model.TreasuryProcessingContext
import com.procurement.contracting.application.service.model.TreasuryProcessingData
import com.procurement.contracting.infrastructure.api.v1.ApiResponseV1
import com.procurement.contracting.infrastructure.api.v1.CommandTypeV1
import com.procurement.contracting.infrastructure.handler.HistoryRepository
import com.procurement.contracting.infrastructure.handler.v1.model.request.CancelCANRequest
import com.procurement.contracting.infrastructure.handler.v1.model.request.CreateCANRequest
import com.procurement.contracting.infrastructure.handler.v1.model.request.TreasuryProcessingRequest
import com.procurement.contracting.infrastructure.handler.v1.model.response.CancelCANResponse
import com.procurement.contracting.infrastructure.handler.v1.model.response.CreateCANResponse
import com.procurement.contracting.infrastructure.handler.v1.model.response.TreasuryProcessingResponse
import com.procurement.contracting.infrastructure.handler.v2.model.request.CreateACRequest
import com.procurement.contracting.infrastructure.handler.v2.model.response.CreateAwardContractResponse
import com.procurement.contracting.utils.toJson
import com.procurement.contracting.utils.toObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommandService(
    private val historyRepository: HistoryRepository,
    private val createCanService: CreateCanService,
    private val updateAcService: UpdateAwardContractService,
    private val issuingAcService: IssuingAwardContractService,
    private val statusService: StatusService,
    private val finalUpdateService: FinalUpdateService,
    private val verificationAcService: VerificationAwardContractService,
    private val signingAcService: SigningAwardContractService,
    private val activationAcService: ActivationAwardContractService,
    private val updateDocumentsService: UpdateDocumentsService,
    private val cancelService: CancelCANService,
    private val treasuryProcessing: TreasuryProcessing,
    private val canService: CANService,
    private val awardContractService: AwardContractService
) {

    companion object {
        private val log = LoggerFactory.getLogger(CommandService::class.java)
    }

    fun execute(cm: CommandMessage): ApiResponseV1.Success {
        val history = historyRepository.getHistory(cm.id, cm.command)
            .orThrow { it.exception }
        if (history != null) {
            return toObject(ApiResponseV1.Success::class.java, history)
        }
        val dataOfResponse: Any = when (cm.command) {
            CommandTypeV1.CHECK_CAN -> createCanService.checkCan(cm)
            CommandTypeV1.CHECK_CAN_BY_AWARD -> createCanService.checkCanByAwardId(cm)
            CommandTypeV1.CREATE_CAN -> {
                val context = CreateCANContext(
                    cpid = cm.cpid,
                    ocid = cm.ocid,
                    owner = cm.owner,
                    startDate = cm.startDate,
                    lotId = cm.lotId
                )
                val request = toObject(CreateCANRequest::class.java, cm.data)
                val createCANData = CreateCANData(
                    award = request.award?.let { award ->
                        CreateCANData.Award(
                            id = award.id
                        )
                    }
                )
                val result = canService.create(context = context, data = createCANData)
                if (log.isDebugEnabled)
                    log.debug("CAN was create. Result: ${toJson(result)}")

                val dataResponse = CreateCANResponse(
                    token = result.token,
                    can = result.can.let { can ->
                        CreateCANResponse.CAN(
                            id = can.id,
                            awardId = can.awardId,
                            lotId = can.lotId,
                            date = can.date,
                            status = can.status,
                            statusDetails = can.statusDetails
                        )
                    }
                )
                if (log.isDebugEnabled)
                    log.debug("CAN was create. Response: ${toJson(dataResponse)}")
                dataResponse
            }
            CommandTypeV1.GET_CANS -> createCanService.getCans(cm)
            CommandTypeV1.UPDATE_CAN_DOCS -> updateDocumentsService.updateCanDocs(cm)
            CommandTypeV1.CANCEL_CAN -> {
                val context = CancelCANContext(
                    cpid = cm.cpid,
                    token = cm.token,
                    owner = cm.owner,
                    canId = cm.canId
                )
                val request = toObject(CancelCANRequest::class.java, cm.data)
                val data = CancelCANData(
                    amendment = request.contract.amendment.let { amendment ->
                        CancelCANData.Amendment(
                            rationale = amendment.rationale,
                            description = amendment.description,
                            documents = amendment.documents?.map { document ->
                                CancelCANData.Amendment.Document(
                                    id = document.id,
                                    documentType = document.documentType,
                                    title = document.title,
                                    description = document.description
                                )
                            }
                        )
                    }
                )
                val result = cancelService.cancel(context = context, data = data)
                if (log.isDebugEnabled)
                    log.debug("CANs were cancelled. Result: ${toJson(result)}")
                val dataResponse = CancelCANResponse(
                    cancelledCAN = result.cancelledCAN.let { can ->
                        CancelCANResponse.CancelledCAN(
                            id = can.id,
                            status = can.status,
                            statusDetails = can.statusDetails,
                            amendment = can.amendment.let { amendment ->
                                CancelCANResponse.CancelledCAN.Amendment(
                                    rationale = amendment.rationale,
                                    description = amendment.description,
                                    documents = amendment.documents?.map { document ->
                                        CancelCANResponse.CancelledCAN.Amendment.Document(
                                            id = document.id,
                                            documentType = document.documentType,
                                            title = document.title,
                                            description = document.description
                                        )
                                    }
                                )
                            }
                        )
                    },
                    relatedCANs = result.relatedCANs.map { relatedCAN ->
                        CancelCANResponse.RelatedCAN(
                            id = relatedCAN.id,
                            status = relatedCAN.status,
                            statusDetails = relatedCAN.statusDetails
                        )
                    },
                    contract = result.contract?.let { contract ->
                        CancelCANResponse.Contract(
                            id = contract.id,
                            status = contract.status,
                            statusDetails = contract.statusDetails
                        )
                    },
                    lotId = result.lotId
                )
                if (log.isDebugEnabled)
                    log.debug("CANs were cancelled. Response: ${toJson(dataResponse)}")
                dataResponse
            }
            CommandTypeV1.CONFIRMATION_CAN -> createCanService.confirmationCan(cm)
            CommandTypeV1.CREATE_AC -> {
                val context = CreateAwardContractContext(
                    cpid = cm.cpid,
                    owner = cm.owner,
                    pmd = cm.pmd,
                    startDate = cm.startDate,
                    language = cm.language
                )
                val request = toObject(CreateACRequest::class.java, cm.data)
                val data = CreateAwardContractData(
                    cans = request.cans.map { can ->
                        CreateAwardContractData.CAN(
                            id = can.id
                        )
                    },
                    awards = request.awards.map { award ->
                        CreateAwardContractData.Award(
                            id = award.id,
                            value = award.value.let { value ->
                                CreateAwardContractData.Award.Value(
                                    amount = value.amount,
                                    currency = value.currency
                                )
                            },
                            relatedLots = award.relatedLots.toList(),
                            relatedBid = award.relatedBid,
                            suppliers = award.suppliers.map { supplier ->
                                CreateAwardContractData.Award.Supplier(
                                    id = supplier.id,
                                    name = supplier.name,
                                    identifier = supplier.identifier.let { identifier ->
                                        CreateAwardContractData.Award.Supplier.Identifier(
                                            scheme = identifier.scheme,
                                            id = identifier.id,
                                            legalName = identifier.legalName,
                                            uri = identifier.uri
                                        )
                                    },
                                    additionalIdentifiers = supplier.additionalIdentifiers?.map { additionalIdentifier ->
                                        CreateAwardContractData.Award.Supplier.AdditionalIdentifier(
                                            scheme = additionalIdentifier.scheme,
                                            id = additionalIdentifier.id,
                                            legalName = additionalIdentifier.legalName,
                                            uri = additionalIdentifier.uri
                                        )
                                    },
                                    address = supplier.address.let { address ->
                                        CreateAwardContractData.Award.Supplier.Address(
                                            streetAddress = address.streetAddress,
                                            postalCode = address.postalCode,
                                            addressDetails = address.addressDetails.let { addressDetails ->
                                                CreateAwardContractData.Award.Supplier.Address.AddressDetails(
                                                    country = addressDetails.country.let { country ->
                                                        CreateAwardContractData.Award.Supplier.Address.AddressDetails.Country(
                                                            scheme = country.scheme,
                                                            id = country.id,
                                                            description = country.description,
                                                            uri = country.uri
                                                        )
                                                    },
                                                    region = addressDetails.region.let { region ->
                                                        CreateAwardContractData.Award.Supplier.Address.AddressDetails.Region(
                                                            scheme = region.scheme,
                                                            id = region.id,
                                                            description = region.description,
                                                            uri = region.uri
                                                        )
                                                    },
                                                    locality = addressDetails.locality.let { locality ->
                                                        CreateAwardContractData.Award.Supplier.Address.AddressDetails.Locality(
                                                            scheme = locality.scheme,
                                                            id = locality.id,
                                                            description = locality.description,
                                                            uri = locality.uri
                                                        )
                                                    }
                                                )
                                            }
                                        )
                                    },
                                    contactPoint = supplier.contactPoint.let { contactPoint ->
                                        CreateAwardContractData.Award.Supplier.ContactPoint(
                                            name = contactPoint.name,
                                            email = contactPoint.email,
                                            telephone = contactPoint.telephone,
                                            faxNumber = contactPoint.faxNumber,
                                            url = contactPoint.url
                                        )
                                    }
                                )
                            },
                            documents = award.documents?.map { document ->
                                CreateAwardContractData.Award.Document(
                                    documentType = document.documentType,
                                    id = document.id,
                                    title = document.title,
                                    description = document.description,
                                    relatedLots = document.relatedLots?.toList()
                                )
                            }
                        )
                    },
                    contractedTender = request.contractedTender.let { contractedTender ->
                        CreateAwardContractData.ContractedTender(
                            mainProcurementCategory = contractedTender.mainProcurementCategory,
                            items = contractedTender.items.map { item ->
                                CreateAwardContractData.ContractedTender.Item(
                                    id = item.id,
                                    internalId = item.internalId,
                                    classification = item.classification.let { classification ->
                                        CreateAwardContractData.ContractedTender.Item.Classification(
                                            scheme = classification.scheme,
                                            id = classification.id,
                                            description = classification.description
                                        )
                                    },
                                    additionalClassifications = item.additionalClassifications?.map { additionalClassification ->
                                        CreateAwardContractData.ContractedTender.Item.AdditionalClassification(
                                            scheme = additionalClassification.scheme,
                                            id = additionalClassification.id,
                                            description = additionalClassification.description
                                        )
                                    },
                                    quantity = item.quantity,
                                    unit = item.unit.let { unit ->
                                        CreateAwardContractData.ContractedTender.Item.Unit(
                                            id = unit.id,
                                            name = unit.name
                                        )
                                    },
                                    description = item.description,
                                    relatedLot = item.relatedLot
                                )
                            }
                        )
                    }
                )
                val result = awardContractService.create(context = context, data = data)
                if (log.isDebugEnabled)
                    log.debug("AC was created. Result: ${toJson(result)}")

                val dataResponse = CreateAwardContractResponse(
                    token = result.token,
                    cans = result.cans.map { can ->
                        CreateAwardContractResponse.CAN(
                            id = can.id,
                            status = can.status,
                            statusDetails = can.statusDetails
                        )
                    },
                    contract = result.contract.let { contract ->
                        CreateAwardContractResponse.AwardContract(
                            id = contract.id,
                            awardId = contract.awardId,
                            status = contract.status,
                            statusDetails = contract.statusDetails
                        )
                    },
                    contractedAward = result.contractedAward.let { contractedAward ->
                        CreateAwardContractResponse.ContractedAward(
                            id = contractedAward.id,
                            date = contractedAward.date,
                            value = contractedAward.value.let { value ->
                                CreateAwardContractResponse.ContractedAward.Value(
                                    amount = value.amount,
                                    currency = value.currency
                                )
                            },
                            relatedLots = contractedAward.relatedLots.toList(),
                            suppliers = contractedAward.suppliers.map { supplier ->
                                CreateAwardContractResponse.ContractedAward.Supplier(
                                    id = supplier.id,
                                    name = supplier.name,
                                    identifier = supplier.identifier.let { identifier ->
                                        CreateAwardContractResponse.ContractedAward.Supplier.Identifier(
                                            scheme = identifier.scheme,
                                            id = identifier.id,
                                            legalName = identifier.legalName,
                                            uri = identifier.uri
                                        )
                                    },
                                    additionalIdentifiers = supplier.additionalIdentifiers?.map { additionalIdentifier ->
                                        CreateAwardContractResponse.ContractedAward.Supplier.AdditionalIdentifier(
                                            scheme = additionalIdentifier.scheme,
                                            id = additionalIdentifier.id,
                                            legalName = additionalIdentifier.legalName,
                                            uri = additionalIdentifier.uri
                                        )
                                    },
                                    address = supplier.address.let { address ->
                                        CreateAwardContractResponse.ContractedAward.Supplier.Address(
                                            streetAddress = address.streetAddress,
                                            postalCode = address.postalCode,
                                            addressDetails = address.addressDetails.let { addressDetails ->
                                                CreateAwardContractResponse.ContractedAward.Supplier.Address.AddressDetails(
                                                    country = addressDetails.country.let { country ->
                                                        CreateAwardContractResponse.ContractedAward.Supplier.Address.AddressDetails.Country(
                                                            scheme = country.scheme,
                                                            id = country.id,
                                                            description = country.description,
                                                            uri = country.uri
                                                        )
                                                    },
                                                    region = addressDetails.region.let { region ->
                                                        CreateAwardContractResponse.ContractedAward.Supplier.Address.AddressDetails.Region(
                                                            scheme = region.scheme,
                                                            id = region.id,
                                                            description = region.description,
                                                            uri = region.uri
                                                        )
                                                    },
                                                    locality = addressDetails.locality.let { locality ->
                                                        CreateAwardContractResponse.ContractedAward.Supplier.Address.AddressDetails.Locality(
                                                            scheme = locality.scheme,
                                                            id = locality.id,
                                                            description = locality.description,
                                                            uri = locality.uri
                                                        )
                                                    }
                                                )
                                            }
                                        )
                                    },
                                    contactPoint = supplier.contactPoint.let { contactPoint ->
                                        CreateAwardContractResponse.ContractedAward.Supplier.ContactPoint(
                                            name = contactPoint.name,
                                            email = contactPoint.email,
                                            telephone = contactPoint.telephone,
                                            faxNumber = contactPoint.faxNumber,
                                            url = contactPoint.url
                                        )
                                    }
                                )
                            },
                            documents = contractedAward.documents.map { document ->
                                CreateAwardContractResponse.ContractedAward.Document(
                                    documentType = document.documentType,
                                    id = document.id,
                                    title = document.title,
                                    description = document.description,
                                    relatedLots = document.relatedLots?.toList()
                                )
                            },
                            items = contractedAward.items.map { item ->
                                CreateAwardContractResponse.ContractedAward.Item(
                                    id = item.id,
                                    internalId = item.internalId,
                                    classification = item.classification.let { classification ->
                                        CreateAwardContractResponse.ContractedAward.Item.Classification(
                                            scheme = classification.scheme,
                                            id = classification.id,
                                            description = classification.description
                                        )
                                    },
                                    additionalClassifications = item.additionalClassifications?.map { additionalClassification ->
                                        CreateAwardContractResponse.ContractedAward.Item.AdditionalClassification(
                                            scheme = additionalClassification.scheme,
                                            id = additionalClassification.id,
                                            description = additionalClassification.description
                                        )
                                    },
                                    quantity = item.quantity,
                                    unit = item.unit.let { unit ->
                                        CreateAwardContractResponse.ContractedAward.Item.Unit(
                                            id = unit.id,
                                            name = unit.name
                                        )
                                    },
                                    description = item.description,
                                    relatedLot = item.relatedLot
                                )
                            }
                        )
                    }
                )
                if (log.isDebugEnabled)
                    log.debug("AC was created. Response: ${toJson(dataResponse)}")
                dataResponse
            }
            CommandTypeV1.UPDATE_AC -> updateAcService.updateAC(cm)
            CommandTypeV1.CHECK_STATUS_DETAILS -> TODO()
            CommandTypeV1.GET_BUDGET_SOURCES -> statusService.getActualBudgetSources(cm)
            CommandTypeV1.GET_RELATED_BID_ID -> statusService.getRelatedBidId(cm)
            CommandTypeV1.ISSUING_AC -> issuingAcService.issuingAc(cm)
            CommandTypeV1.FINAL_UPDATE -> finalUpdateService.finalUpdate(cm)
            CommandTypeV1.BUYER_SIGNING_AC -> signingAcService.buyerSigningAC(cm)
            CommandTypeV1.SUPPLIER_SIGNING_AC -> signingAcService.supplierSigningAC(cm)
            CommandTypeV1.VERIFICATION_AC -> verificationAcService.verificationAc(cm)
            CommandTypeV1.TREASURY_RESPONSE_PROCESSING -> {
                val context = TreasuryProcessingContext(
                    cpid = cm.cpid,
                    ocid = cm.ocid,
                    startDate = cm.startDate
                )
                val request = toObject(TreasuryProcessingRequest::class.java, cm.data)
                val data = TreasuryProcessingData(
                    verification = request.verification.let { verification ->
                        TreasuryProcessingData.Verification(
                            status = verification.status,
                            rationale = verification.rationale
                        )
                    },
                    dateMet = request.dateMet,
                    regData = request.regData?.let { regData ->
                        TreasuryProcessingData.RegData(
                            externalRegId = regData.externalRegId,
                            regDate = regData.regDate
                        )
                    }
                )

                val result = treasuryProcessing.processing(context = context, data = data)
                if (log.isDebugEnabled)
                    log.debug("CANs were cancelled. Result: ${toJson(result)}")

                val dataResponse = TreasuryProcessingResponse(
                    contract = result.contract.let { contract ->
                        TreasuryProcessingResponse.Contract(
                            id = contract.id,
                            date = contract.date,
                            awardId = contract.awardId,
                            status = contract.status,
                            statusDetails = contract.statusDetails,
                            title = contract.title,
                            description = contract.description,
                            period = contract.period.let { period ->
                                TreasuryProcessingResponse.Contract.Period(
                                    startDate = period.startDate,
                                    endDate = period.endDate
                                )
                            },
                            documents = contract.documents.map { document ->
                                TreasuryProcessingResponse.Contract.Document(
                                    id = document.id,
                                    documentType = document.documentType,
                                    title = document.title,
                                    description = document.description,
                                    relatedLots = document.relatedLots?.toList(),
                                    relatedConfirmations = document.relatedConfirmations?.toList()
                                )
                            },
                            milestones = contract.milestones.map { milestone ->
                                TreasuryProcessingResponse.Contract.Milestone(
                                    id = milestone.id,
                                    relatedItems = milestone.relatedItems?.toList(),
                                    status = milestone.status,
                                    additionalInformation = milestone.additionalInformation,
                                    dueDate = milestone.dueDate,
                                    title = milestone.title,
                                    type = milestone.type,
                                    description = milestone.description,
                                    dateModified = milestone.dateModified,
                                    dateMet = milestone.dateMet,
                                    relatedParties = milestone.relatedParties.map { relatedParty ->
                                        TreasuryProcessingResponse.Contract.Milestone.RelatedParty(
                                            id = relatedParty.id,
                                            name = relatedParty.name
                                        )
                                    }
                                )
                            },
                            confirmationRequests = contract.confirmationRequests.map { confirmationRequest ->
                                TreasuryProcessingResponse.Contract.ConfirmationRequest(
                                    id = confirmationRequest.id,
                                    type = confirmationRequest.type,
                                    title = confirmationRequest.title,
                                    description = confirmationRequest.description,
                                    relatesTo = confirmationRequest.relatesTo,
                                    relatedItem = confirmationRequest.relatedItem,
                                    source = confirmationRequest.source,
                                    requestGroups = confirmationRequest.requestGroups.map { requestGroup ->
                                        TreasuryProcessingResponse.Contract.ConfirmationRequest.RequestGroup(
                                            id = requestGroup.id,
                                            requests = requestGroup.requests.map { request ->
                                                TreasuryProcessingResponse.Contract.ConfirmationRequest.RequestGroup.Request(
                                                    id = request.id,
                                                    title = request.title,
                                                    description = request.description,
                                                    relatedPerson = request.relatedPerson?.let { relatedPerson ->
                                                        TreasuryProcessingResponse.Contract.ConfirmationRequest.RequestGroup.Request.RelatedPerson(
                                                            id = relatedPerson.id,
                                                            name = relatedPerson.name
                                                        )
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                            },
                            confirmationResponses = contract.confirmationResponses.map { confirmationResponse ->
                                TreasuryProcessingResponse.Contract.ConfirmationResponse(
                                    id = confirmationResponse.id,
                                    value = confirmationResponse.value.let { value ->
                                        TreasuryProcessingResponse.Contract.ConfirmationResponse.Value(
                                            id = value.id,
                                            name = value.name,
                                            date = value.date,
                                            relatedPerson = value.relatedPerson?.let { relatedPerson ->
                                                TreasuryProcessingResponse.Contract.ConfirmationResponse.Value.RelatedPerson(
                                                    id = relatedPerson.id,
                                                    name = relatedPerson.name
                                                )
                                            },
                                            verifications = value.verifications.map { verification ->
                                                TreasuryProcessingResponse.Contract.ConfirmationResponse.Value.Verification(
                                                    type = verification.type,
                                                    value = verification.value,
                                                    rationale = verification.rationale
                                                )
                                            }
                                        )
                                    },
                                    request = confirmationResponse.request
                                )
                            },
                            value = contract.value.let { value ->
                                TreasuryProcessingResponse.Contract.Value(
                                    amount = value.amount,
                                    currency = value.currency,
                                    amountNet = value.amountNet,
                                    valueAddedTaxIncluded = value.valueAddedTaxIncluded
                                )
                            }
                        )
                    },
                    cans = result.cans?.map { can ->
                        TreasuryProcessingResponse.Can(
                            id = can.id,
                            status = can.status,
                            statusDetails = can.statusDetails
                        )
                    }
                )
                if (log.isDebugEnabled)
                    log.debug("CANs were cancelled. Response: ${toJson(dataResponse)}")
                dataResponse
            }
            CommandTypeV1.ACTIVATION_AC -> activationAcService.activateAc(cm)
        }
        return ApiResponseV1.Success(id = cm.id, version = cm.version, data = dataOfResponse)
            .also {
                val data = toJson(it)
                if (log.isDebugEnabled)
                    log.debug("Response: $data")
                historyRepository.saveHistory(cm.id, cm.command, data)
            }
    }
}
