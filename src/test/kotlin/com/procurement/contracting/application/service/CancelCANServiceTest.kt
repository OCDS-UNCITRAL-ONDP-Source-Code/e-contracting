package com.procurement.contracting.application.service

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.procurement.contracting.AbstractArgumentConverter
import com.procurement.contracting.application.repository.ACRepository
import com.procurement.contracting.application.repository.CANRepository
import com.procurement.contracting.domain.entity.ACEntity
import com.procurement.contracting.domain.entity.CANEntity
import com.procurement.contracting.domain.model.CAN
import com.procurement.contracting.exception.ErrorException
import com.procurement.contracting.exception.ErrorType
import com.procurement.contracting.json.loadJson
import com.procurement.contracting.model.dto.ContractProcess
import com.procurement.contracting.model.dto.ocds.ContractStatus
import com.procurement.contracting.model.dto.ocds.ContractStatusDetails
import com.procurement.contracting.model.dto.ocds.DocumentTypeAmendment
import com.procurement.contracting.utils.toJson
import com.procurement.contracting.utils.toObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.provider.CsvSource
import java.util.*

class CancelCANServiceTest {
    companion object {
        private const val CPID = "cpid-1"
        private val CAN_TOKEN: UUID = UUID.fromString("2909bc16-82c7-4281-8f35-3f0bb13476b8")
        private const val OWNER = "owner-1"
        private val CAN_ID: UUID = UUID.fromString("0dc181db-f5ae-4039-97c7-defcceef89a4")
        private const val LOT_ID: String = "lot-id-0"
        private const val CONTRACT_ID: String = "contract-id-1"

        private val cancellationCAN =
            toObject(CAN::class.java, loadJson("json/application/service/cancel/cancellation-can.json"))
        private val firstOtherCAN =
            toObject(CAN::class.java, loadJson("json/application/service/cancel/first-related-can.json"))
        private val secondOtherCAN =
            toObject(CAN::class.java, loadJson("json/application/service/cancel/second-related-can.json"))
        private val contractProcess =
            toObject(ContractProcess::class.java, loadJson("json/application/service/cancel/contract-process.json"))
    }

    private lateinit var canRepository: CANRepository
    private lateinit var acRepository: ACRepository

    private lateinit var service: CancelCANService

    @BeforeEach
    fun init() {
        canRepository = mock()
        acRepository = mock()

        service = CancelCANServiceImpl(canRepository = canRepository, acRepository = acRepository)
    }

    @ParameterizedTest(name = "CAN - status: ''{0}'' & status details: ''{1}''")
    @CsvSource(
        "pending, contractProject",
        "pending, active",
        "pending, unsuccessful"
    )
    fun cancelCANWithoutContract(
        @ConvertWith(StatusConverter::class) canStatus: ContractStatus,
        @ConvertWith(StatusDetailsConverter::class) canStatusDetails: ContractStatusDetails
    ) {
        val cancellationCANEntity = canEntity(
            can = cancellationCAN.copy(
                status = canStatus,
                statusDetails = canStatusDetails
            ),
            contractID = null
        )

        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(cancellationCANEntity)
        whenever(acRepository.findBy(eq(CPID), eq(CONTRACT_ID)))
            .thenReturn(null)
        whenever(canRepository.findBy(eq(CPID)))
            .thenReturn(listOf(cancellationCANEntity))

        val data = data()
        val response = service.cancel(context = context(), data = data)

        val canceledCAN = response.cancelledCAN
        assertEquals(cancellationCANEntity.id, canceledCAN.id)
        assertEquals(ContractStatus.CANCELLED, canceledCAN.status)
        assertEquals(ContractStatusDetails.EMPTY, canceledCAN.statusDetails)

        assertNotNull(canceledCAN.amendment)
        val amendment = canceledCAN.amendment
        assertEquals(data.amendment.rationale, amendment.rationale)
        assertEquals(data.amendment.description, amendment.description)
        assertEquals(data.amendment.documents!!.size, amendment.documents!!.size)

        val documents = amendment.documents!!
        assertEquals(data.amendment.documents!![0].id, documents[0].id)
        assertEquals(data.amendment.documents!![0].documentType, documents[0].documentType)
        assertEquals(data.amendment.documents!![0].title, documents[0].title)
        assertEquals(data.amendment.documents!![0].description, documents[0].description)

        assertEquals(LOT_ID, response.lotId)
        assertEquals(false, response.isCancelledAC)

        assertNull(response.contract)

        verify(acRepository, times(0)).saveCancelledAC(any())
        verify(canRepository, times(1)).saveCancelledCANs(any(), any(), any())
    }

    @ParameterizedTest(name = "Contract - status: ''{0}'' & statusDetails: ''{1}''")
    @CsvSource(
        "pending, contractProject",
        "pending, contractPreparation",
        "pending, active",
        "pending, approved",
        "pending, signed",
        "pending, cancelled",
        "pending, complete",
        "pending, unsuccessful",
        "pending, issued",
        "pending, approvement",
        "pending, execution",
        "pending, empty",
        "cancelled, empty"
    )
    fun cancelCANWithContractWithoutRelatedCANs(
        @ConvertWith(StatusConverter::class) contractStatus: ContractStatus,
        @ConvertWith(StatusDetailsConverter::class) contractStatusDetails: ContractStatusDetails
    ) {
        val cancellationCANEntity = canEntity(can = cancellationCAN)
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(cancellationCANEntity)

        val acEntity = acEntity(
            contractProcess = contractProcess.copy(
                contract = contractProcess.contract.copy(
                    status = contractStatus,
                    statusDetails = contractStatusDetails
                )
            )
        )
        whenever(acRepository.findBy(eq(CPID), eq(CONTRACT_ID)))
            .thenReturn(acEntity)

        whenever(canRepository.findBy(eq(CPID)))
            .thenReturn(listOf(cancellationCANEntity))

        val data = data()
        val response = service.cancel(context = context(), data = data)

        val canceledCAN = response.cancelledCAN
        assertEquals(cancellationCANEntity.id, canceledCAN.id)
        assertEquals(ContractStatus.CANCELLED, canceledCAN.status)
        assertEquals(ContractStatusDetails.EMPTY, canceledCAN.statusDetails)

        assertNotNull(canceledCAN.amendment)
        val amendment = canceledCAN.amendment
        assertEquals(data.amendment.rationale, amendment.rationale)
        assertEquals(data.amendment.description, amendment.description)
        assertEquals(data.amendment.documents!!.size, amendment.documents!!.size)

        val documents = amendment.documents!!
        assertEquals(data.amendment.documents!![0].id, documents[0].id)
        assertEquals(data.amendment.documents!![0].documentType, documents[0].documentType)
        assertEquals(data.amendment.documents!![0].title, documents[0].title)
        assertEquals(data.amendment.documents!![0].description, documents[0].description)

        assertEquals(LOT_ID, response.lotId)
        assertEquals(true, response.isCancelledAC)

        assertNotNull(response.contract)
        val contract = response.contract!!
        assertEquals(CONTRACT_ID, contract.id)
        assertEquals(ContractStatus.CANCELLED, contract.status)
        assertEquals(ContractStatusDetails.EMPTY, contract.statusDetails)

        verify(acRepository, times(1)).saveCancelledAC(any())
        verify(canRepository, times(1)).saveCancelledCANs(any(), any(), any())
    }

    @Test
    fun cancelCANWithContractWithRelatedCAN() {
        val cancellationCANEntity = canEntity(can = cancellationCAN)
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(cancellationCANEntity)

        val acEntity = acEntity(contractProcess = contractProcess)
        whenever(acRepository.findBy(eq(CPID), eq(CONTRACT_ID)))
            .thenReturn(acEntity)

        val firstCANEntity = canEntity(can = firstOtherCAN)
        val secondCANEntity = canEntity(can = secondOtherCAN, contractID = "UNKNOWN")
        whenever(canRepository.findBy(eq(CPID)))
            .thenReturn(listOf(cancellationCANEntity, firstCANEntity, secondCANEntity))

        val data = data()
        val response = service.cancel(context = context(), data = data)

        assertEquals(LOT_ID, response.lotId)
        assertEquals(true, response.isCancelledAC)

        assertNotNull(response.contract)
        val contract = response.contract!!
        assertEquals(CONTRACT_ID, contract.id)
        assertEquals(ContractStatus.CANCELLED, contract.status)
        assertEquals(ContractStatusDetails.EMPTY, contract.statusDetails)

        val cancelledCAN = response.cancelledCAN
        assertEquals(cancellationCAN.id, cancelledCAN.id)
        assertEquals(ContractStatus.CANCELLED, cancelledCAN.status)
        assertEquals(ContractStatusDetails.EMPTY, cancelledCAN.statusDetails)

        assertNotNull(cancelledCAN.amendment)
        val amendment = cancelledCAN.amendment
        assertEquals(data.amendment.rationale, amendment.rationale)
        assertEquals(data.amendment.description, amendment.description)
        assertEquals(data.amendment.documents!!.size, amendment.documents!!.size)

        val documents = amendment.documents!!
        assertEquals(data.amendment.documents!![0].id, documents[0].id)
        assertEquals(data.amendment.documents!![0].documentType, documents[0].documentType)
        assertEquals(data.amendment.documents!![0].title, documents[0].title)
        assertEquals(data.amendment.documents!![0].description, documents[0].description)

        assertEquals(1, response.relatedCANs.size)
        val firstRelatedCAN = response.relatedCANs[0]
        assertEquals(firstOtherCAN.id, firstRelatedCAN.id)
        assertEquals(ContractStatus.PENDING, firstRelatedCAN.status)
        assertEquals(ContractStatusDetails.CONTRACT_PROJECT, firstRelatedCAN.statusDetails)

        verify(acRepository, times(1)).saveCancelledAC(any())
        verify(canRepository, times(1)).saveCancelledCANs(any(), any(), any())
    }

    @Test
    fun canNotFound() {
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(null)

        val exception = assertThrows<ErrorException> {
            service.cancel(context = context(), data = data())
        }

        assertEquals(ErrorType.CAN_NOT_FOUND, exception.error)
    }

    @Test
    fun invalidOwner() {
        val canEntity = canEntity(can = cancellationCAN, owner = "UNKNOWN")
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(canEntity)

        val exception = assertThrows<ErrorException> {
            service.cancel(context = context(), data = data())
        }

        assertEquals(ErrorType.INVALID_OWNER, exception.error)
    }

    @Test
    fun invalidToken() {
        val canEntity = canEntity(can = cancellationCAN, token = UUID.randomUUID())
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(canEntity)

        val exception = assertThrows<ErrorException> {
            service.cancel(context = context(), data = data())
        }

        assertEquals(ErrorType.INVALID_TOKEN, exception.error)
    }

    @Test
    fun contractNotFound() {
        val canEntity = canEntity(cancellationCAN)
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(canEntity)

        val exception = assertThrows<ErrorException> {
            service.cancel(context = context(), data = data())
        }

        assertEquals(ErrorType.CONTRACT_NOT_FOUND, exception.error)
    }

    @ParameterizedTest(name = "CAN status: ''{0}''")
    @CsvSource(
        "active",
        "cancelled",
        "complete",
        "terminated",
        "unsuccessful"
    )
    fun invalidCANStatus(@ConvertWith(StatusConverter::class) canStatus: ContractStatus) {
        val cancellationCANEntity = canEntity(
            can = cancellationCAN.copy(
                status = canStatus
            ),
            contractID = null
        )
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(cancellationCANEntity)

        whenever(acRepository.findBy(eq(CPID), eq(CONTRACT_ID)))
            .thenReturn(null)

        whenever(canRepository.findBy(eq(CPID)))
            .thenReturn(listOf(cancellationCANEntity))

        val exception = assertThrows<ErrorException> {
            service.cancel(context = context(), data = data())
        }

        assertEquals(ErrorType.INVALID_CAN_STATUS, exception.error)
    }

    @ParameterizedTest(name = "CAN - status: ''{0}'' & status details: ''{1}''")
    @CsvSource(
        "pending, contractPreparation",
        "pending, approved",
        "pending, signed",
        "pending, verification",
        "pending, verified",
        "pending, cancelled",
        "pending, complete",
        "pending, issued",
        "pending, approvement",
        "pending, execution",
        "pending, empty"
    )
    fun invalidCANStatusDetails(
        @ConvertWith(StatusConverter::class) canStatus: ContractStatus,
        @ConvertWith(StatusDetailsConverter::class) canStatusDetails: ContractStatusDetails
    ) {
        val cancellationCANEntity = canEntity(
            can = cancellationCAN.copy(
                status = canStatus,
                statusDetails = canStatusDetails
            ),
            contractID = null
        )
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(cancellationCANEntity)

        whenever(acRepository.findBy(eq(CPID), eq(CONTRACT_ID)))
            .thenReturn(null)

        whenever(canRepository.findBy(eq(CPID)))
            .thenReturn(listOf(cancellationCANEntity))

        val exception = assertThrows<ErrorException> {
            service.cancel(context = context(), data = data())
        }

        assertEquals(ErrorType.INVALID_CAN_STATUS_DETAILS, exception.error)
    }

    @ParameterizedTest(name = "Contract - status: ''{0}''")
    @CsvSource(
        "active",
        "complete",
        "terminated",
        "unsuccessful"
    )
    fun invalidContractStatus(@ConvertWith(StatusConverter::class) contractStatus: ContractStatus) {
        val cancellationCANEntity = canEntity(can = cancellationCAN)
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(cancellationCANEntity)

        val acEntity = acEntity(
            contractProcess.copy(
                contract = contractProcess.contract.copy(
                    status = contractStatus
                )
            )
        )
        whenever(acRepository.findBy(eq(CPID), eq(CONTRACT_ID)))
            .thenReturn(acEntity)

        whenever(canRepository.findBy(eq(CPID)))
            .thenReturn(listOf(cancellationCANEntity))

        val exception = assertThrows<ErrorException> {
            service.cancel(context = context(), data = data())
        }

        assertEquals(ErrorType.CONTRACT_STATUS, exception.error)
    }

    @ParameterizedTest(name = "Contract - status: ''{0}'' & status details: ''{1}''")
    @CsvSource(
        "pending, verification",
        "pending, verified",
        "cancelled, contractProject",
        "cancelled, contractPreparation",
        "cancelled, active",
        "cancelled, approved",
        "cancelled, signed",
        "cancelled, verification",
        "cancelled, verified",
        "cancelled, cancelled",
        "cancelled, complete",
        "cancelled, unsuccessful",
        "cancelled, issued",
        "cancelled, approvement",
        "cancelled, execution"
    )
    fun invalidContractStatusDetails(
        @ConvertWith(StatusConverter::class) contractStatus: ContractStatus,
        @ConvertWith(StatusDetailsConverter::class) contractStatusDetails: ContractStatusDetails
    ) {
        val cancellationCANEntity = canEntity(can = cancellationCAN)
        whenever(canRepository.findBy(eq(CPID), eq(CAN_ID)))
            .thenReturn(cancellationCANEntity)

        val acEntity = acEntity(
            contractProcess.copy(
                contract = contractProcess.contract.copy(
                    status = contractStatus,
                    statusDetails = contractStatusDetails
                )
            )
        )
        whenever(acRepository.findBy(eq(CPID), eq(CONTRACT_ID)))
            .thenReturn(acEntity)

        whenever(canRepository.findBy(eq(CPID)))
            .thenReturn(listOf(cancellationCANEntity))

        val exception = assertThrows<ErrorException> {
            service.cancel(context = context(), data = data())
        }

        assertEquals(ErrorType.CONTRACT_STATUS_DETAILS, exception.error)
    }

    private fun context(
        cpid: String = CPID,
        token: UUID = CAN_TOKEN,
        owner: String = OWNER,
        canId: UUID = CAN_ID
    ): CancelCANContext {
        return CancelCANContext(
            cpid = cpid,
            token = token,
            owner = owner,
            canId = canId
        )
    }

    private fun data(): CancelCANData {
        return CancelCANData(
            amendment = CancelCANData.Amendment(
                rationale = "amendment.rationale",
                description = "amendment.description",
                documents = listOf(
                    CancelCANData.Amendment.Document(
                        id = "amendment.documents[0].id",
                        documentType = DocumentTypeAmendment.CONTRACT_NOTICE,
                        title = "amendment.documents[0].title",
                        description = "amendment.documents[0].description"
                    )
                )
            )
        )
    }

    private fun canEntity(can: CAN, owner: String = OWNER, token: UUID? = null, contractID: String? = CONTRACT_ID) =
        CANEntity(
            cpid = CPID,
            id = can.id,
            token = token ?: can.token,
            owner = owner,
            createdDate = can.date,
            awardId = can.awardId,
            lotId = can.lotId,
            contractId = contractID,
            status = can.status.toString(),
            statusDetails = can.statusDetails.toString(),
            jsonData = toJson(can)
        )

    private fun acEntity(contractProcess: ContractProcess) = ACEntity(
        cpid = CPID,
        id = contractProcess.contract.id,
        token = UUID.fromString(contractProcess.contract.token),
        owner = OWNER,
        createdDate = contractProcess.contract.date!!,
        status = contractProcess.contract.status.toString(),
        statusDetails = contractProcess.contract.statusDetails.toString(),
        mainProcurementCategory = "",
        language = "RO",
        jsonData = toJson(contractProcess)
    )
}

class StatusConverter : AbstractArgumentConverter<ContractStatus>() {
    override fun converting(source: String): ContractStatus = ContractStatus.fromValue(source)
}

class StatusDetailsConverter : AbstractArgumentConverter<ContractStatusDetails>() {
    override fun converting(source: String): ContractStatusDetails = ContractStatusDetails.fromValue(source)
}