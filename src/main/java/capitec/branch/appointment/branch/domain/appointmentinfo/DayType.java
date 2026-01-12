package capitec.branch.appointment.branch.domain.appointmentinfo;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum DayType {

    WEEK_DAYS,
    WEEKEND,
    HOLIDAY
}
record IsDayTypeValidator() implements ConstraintValidator<IsDayType, String> {
    @Override
    public boolean isValid(String status, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isNotBlank(status) && Arrays.asList(DayType.values()).contains(status);
    }
}

