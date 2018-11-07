package com.procurement.contracting.model.dto.ocds

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.procurement.contracting.model.dto.databinding.MoneyDeserializer
import java.math.BigDecimal

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Value @JsonCreator constructor(

        @JsonDeserialize(using = MoneyDeserializer::class)
        val amount: BigDecimal?,

        @JsonDeserialize(using = MoneyDeserializer::class)
        val amountNet: BigDecimal?,

        val currency: String?,

        val valueAddedTaxIncluded: Boolean?
)