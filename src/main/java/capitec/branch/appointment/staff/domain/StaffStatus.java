package capitec.branch.appointment.staff.domain;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum StaffStatus{
    TRAINING,WORKING, LEAVE;

    record IsStaffStatusValidator() implements ConstraintValidator<IsStaffStatus, String> {
        @Override
        public boolean isValid(String status, ConstraintValidatorContext constraintValidatorContext) {
            return StringUtils.isNotBlank(status) && Arrays.asList(StaffStatus.values()).contains(status);
        }
    }
}
