package capitec.branch.appointment.location.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Asserts;

import java.util.Objects;

public record BranchAddress(
        String addressLine1,
        String addressLine2,
        String city,
        String province
) {
    public BranchAddress {
        Asserts.notBlank(addressLine1, "addressLine1");
        Asserts.notBlank(city, "city");
        Asserts.notBlank(province, "province");
    }

    public String getFullAddress() {
        if (StringUtils.isNotBlank(addressLine2)) {
            return String.format("%s, %s, %s, %s", addressLine1, addressLine2, city, province);
        }
        else{
            return String.format("%s, %s, %s", addressLine1, city, province);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BranchAddress that)) return false;
        return Objects.equals(addressLine1, that.addressLine1) && Objects.equals(city, that.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressLine1, city);
    }
}

