package capitec.branch.appointment.branch.infrastructure;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("address")
record AddressEntity(
        @NotBlank
        @Column("branch_id")
        String branchId,
        @NotBlank
        @Column("street_number")
        String streetNumber,
        @NotBlank
        @Column("street_name")
        String streetName,
        @NotBlank
        String suburb,
        @NotBlank
        String city,
        @NotBlank
        String province,
        @NotBlank
        @Column("postal_code")
        Integer postalCode,
        String country
) {

}
