package capitec.branch.appointment.branch.domain.address;

import jakarta.validation.constraints.NotBlank;

public record Address(
        @NotBlank
        String streetNumber,
        @NotBlank
        String streetName,
        @NotBlank
        String suburbs,
        @NotBlank
        String city,
        @NotBlank
        String province,
        @NotBlank
        Integer postalCode,
        String country
){
   public  Address (
                         String streetNumber,
                         String streetName,
                         String suburbs,
                         String city,
                         String province,
                         Integer postalCode,
                         String country) {
       this.streetNumber = streetNumber;
        this.streetName = streetName;
        this.suburbs = suburbs;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.country = country;

        assert  streetNumber != null;
        assert streetName != null;
        assert suburbs != null;
        assert province != null;
        assert postalCode != null;


   }

}
