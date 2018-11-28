package com.procurement.contracting.model.dto.ocds

import com.fasterxml.jackson.annotation.JsonValue
import com.procurement.contracting.exception.EnumException


enum class MainProcurementCategory(@JsonValue val value: String) {
    GOODS("goods"),
    SERVICES("services"),
    WORKS("works");

    override fun toString(): String {
        return this.value
    }

    companion object {
        private val CONSTANTS = HashMap<String, MainProcurementCategory>()

        init {
            MainProcurementCategory.values().forEach { CONSTANTS[it.value] = it }
        }

        fun fromValue(v: String): MainProcurementCategory {
            return CONSTANTS[v] ?: throw EnumException(MainProcurementCategory::class.java.name, v, values().toString())
        }
    }
}

enum class TransactionType(@JsonValue val value: String) {
    ADVANCE("advance"),
    PAYMENT("payment");


    override fun toString(): String {
        return this.value
    }
}

enum class MilestoneStatus(@JsonValue val value: String) {
    SCHEDULED("scheduled");

    override fun toString(): String {
        return this.value
    }
}

enum class ContractStatus(@JsonValue val value: String) {
    PENDING("pending"),
    ACTIVE("active"),
    CANCELLED("cancelled"),
    COMPLETE("complete"),
    TERMINATED("terminated"),
    UNSUCCESSFUL("unsuccessful");

    override fun toString(): String {
        return this.value
    }

    companion object {
        private val CONSTANTS = HashMap<String, ContractStatus>()

        init {
            ContractStatus.values().forEach { CONSTANTS[it.value] = it }
        }

        fun fromValue(v: String): ContractStatus {
            return CONSTANTS[v] ?: throw EnumException(ContractStatus::class.java.name, v, values().toString())
        }
    }
}

enum class ContractStatusDetails(@JsonValue val value: String) {
    CONTRACT_PROJECT("contractProject"),
    CONTRACT_PREPARATION("contractPreparation"),
    ACTIVE("active"),
    SIGNED("signed"),
    VERIFICATION("verification"),
    VERIFIED("verified"),
    CANCELLED("cancelled"),
    COMPLETE("complete"),
    UNSUCCESSFUL("unsuccessful"),
    ISSUED("issued"),
    APPROVEMENT("approvement"),
    EMPTY("empty");

    override fun toString(): String {
        return this.value
    }

    companion object {
        private val CONSTANTS = HashMap<String, ContractStatusDetails>()

        init {
            ContractStatusDetails.values().forEach { CONSTANTS[it.value] = it }
        }

        fun fromValue(v: String): ContractStatusDetails {
            return CONSTANTS[v] ?: throw EnumException(ContractStatusDetails::class.java.name, v, values().toString())
        }
    }
}

enum class TenderStatus(@JsonValue val value: String) {
    PLANNING("planning"),
    PLANNED("planned"),
    ACTIVE("active"),
    CANCELLED("cancelled"),
    UNSUCCESSFUL("unsuccessful"),
    COMPLETE("complete"),
    WITHDRAWN("withdrawn");

    override fun toString(): String {
        return this.value
    }
}

enum class TenderStatusDetails(@JsonValue val value: String) {
    PRESELECTION("preselection"),
    PRESELECTED("preselected"),
    PREQUALIFICATION("prequalification"),
    PREQUALIFIED("prequalified"),
    EVALUATION("evaluation"),
    EVALUATED("evaluated"),
    EXECUTION("execution"),
    AWARDED("awarded"),
    //**//
    PLANNING("planning"),
    PLANNED("planned"),
    ACTIVE("active"),
    BLOCKED("blocked"),
    CANCELLED("cancelled"),
    UNSUCCESSFUL("unsuccessful"),
    COMPLETE("complete"),
    WITHDRAWN("withdrawn"),
    SUSPENDED("suspended"),
    EMPTY("empty");

    override fun toString(): String {
        return this.value
    }
}

enum class DocumentTypeAward(@JsonValue val value: String) {
    AWARD_NOTICE("awardNotice"),
    EVALUATION_REPORTS("evaluationReports"),
    SHORTLISTED_FIRMS("shortlistedFirms"),
    WINNING_BID("winningBid"),
    COMPLAINTS("complaints"),
    BIDDERS("bidders"),
    CONFLICT_OF_INTEREST("conflictOfInterest"),
    CANCELLATION_DETAILS("cancellationDetails"),
    CONTRACT_DRAFT("contractDraft"),
    CONTRACT_ARRANGEMENTS("contractArrangements"),
    CONTRACT_SCHEDULE("contractSchedule");

    override fun toString(): String {
        return this.value
    }
}

enum class DocumentTypeContract(@JsonValue val value: String) {

    CONTRACT_NOTICE("contractNotice"),
    COMPLETION_CERTIFICATE("completionCertificate"),
    CONTRACT_DRAFT("contractDraft"),
    CONTRACT_ARRANGEMENTS("contractArrangements"),
    CONTRACT_SCHEDULE("contractSchedule"),
    ENVIRONMENTAL_IMPACT("environmentalImpact"),
    CONTRACT_ANNEXE("contractAnnexe"),
    CONTRACT_GUARANTEES("contractGuarantees"),
    SUB_CONTRACT("subContract"),
    ILLUSTRATION("illustration"),
    CONTRACT_SIGNED("contractSigned"),
    CONTRACT_SUMMARY("contractSummary");

    override fun toString(): String {
        return this.value
    }
}

enum class DocumentTypeBF(@JsonValue val value: String) {

    REGULATORY_DOCUMENT("regulatoryDocument");

    override fun toString(): String {
        return this.value
    }
}


enum class MilestoneType(@JsonValue val value: String) {

    DELIVERY("delivery"),
    X_WARRANTY("x_warranty"),
    X_REPORTING("x_reporting"),
    APPROVAL("approval");


    override fun toString(): String {
        return this.value
    }
}


enum class RelatedProcessType(@JsonValue val value: String) {
    FRAMEWORK("framework"),
    PLANNING("planning"),
    PARENT("parent"),
    PRIOR("prior"),
    UNSUCCESSFUL_PROCESS("unsuccessfulProcess"),
    SUB_CONTRACT("subContract"),
    REPLACEMENT_PROCESS("replacementProcess"),
    RENEWAL_PROCESS("renewalProcess");

    override fun toString(): String {
        return this.value
    }
}

enum class RelatedProcessScheme(@JsonValue val value: String) {
    OCID("ocid");

    override fun toString(): String {
        return this.value
    }
}