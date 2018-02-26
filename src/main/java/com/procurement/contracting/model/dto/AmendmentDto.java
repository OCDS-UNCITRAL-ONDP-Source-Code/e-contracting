package com.procurement.contracting.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Getter
@JsonPropertyOrder({
    "rationale",
    "description"
})
public class AmendmentDto {

    @JsonProperty("rationale")
    @NotNull
    private final String rationale;

    @JsonProperty("description")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String description;

    public AmendmentDto(@JsonProperty("rationale")
                        @NotNull final String rationale,
                        @JsonProperty("description")
                        @JsonInclude(JsonInclude.Include.NON_NULL) final String description) {
        this.rationale = rationale;
        this.description = description;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(description)
                                    .append(rationale)
                                    .toHashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof AmendmentDto)) {
            return false;
        }
        final AmendmentDto rhs = (AmendmentDto) other;
        return new EqualsBuilder().append(description, rhs.description)
                                  .append(rationale, rhs.rationale)
                                  .isEquals();
    }
}
