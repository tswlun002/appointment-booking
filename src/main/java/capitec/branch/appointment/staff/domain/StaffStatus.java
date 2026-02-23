package capitec.branch.appointment.staff.domain;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
public enum StaffStatus{
    TRAINING,WORKING, LEAVE;

    public static boolean isValid(String value){
        for(StaffStatus staffStatus : StaffStatus.values()){
            if(staffStatus.name().equals(value)){
                return true;
            }
        }
        return false;
    }

    record IsStaffStatusValidator() implements ConstraintValidator<IsStaffStatus, String> {
        @Override
        public boolean isValid(String status, ConstraintValidatorContext constraintValidatorContext) {

                return StringUtils.isNotBlank(status) && StaffStatus.isValid(status) ;

        }
    }
}
