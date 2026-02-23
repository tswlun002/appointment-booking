package capitec.branch.appointment.appointment.infrastructure.dao;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.AppointmentTerminationReason;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    /**
     * Converts a Domain Appointment to a persistence Entity.
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "appointmentStatusToString")
    @Mapping(target = "terminationReason", source = "terminationReason", qualifiedByName = "terminationReasonToString")
    AppointmentEntity toEntity(Appointment domain);

    /**
     * Converts a persistence Entity back to a Domain Appointment.
     * Maps String fields in the Entity back to Enum fields in the Domain.
     */

     default Appointment toDomain(AppointmentEntity entity){

         String terminationReason = entity.terminationReason();
         AppointmentTerminationReason terminationReasonEnum = terminationReason == null ? null : AppointmentTerminationReason.valueOf(terminationReason);
         return  Appointment.restitutionFromPersistence(
                 entity.id(),
                 entity.slotId(),
                 entity.branchId(),
                 entity.customerUsername(),
                 entity.serviceType(),
                 AppointmentStatus.valueOf(entity.status()),
                 entity.reference(),
                 entity.dateTime(),
                 entity.version(),
                 entity.createdAt(),
                 entity.updatedAt(),
                 entity.checkedInAt(),
                 entity.inProgressAt(),
                 entity.completedAt(),
                 entity.terminatedAt(),
                 entity.terminatedBy(),
                 terminationReasonEnum,
                 entity.terminationNotes(),
                 entity.assignedConsultantId(),
                 entity.serviceNotes(),
                 entity.previousSlotId(),
                 entity.rescheduleCount()
         );
     }

    /** Converts Entity String to Domain Enum */
    @Named("stringToAppointmentStatus")
    default AppointmentStatus stringToAppointmentStatus(String status) {
        if (status == null) {
            return null;
        }
        return AppointmentStatus.valueOf(status);
    }

    /** Converts Domain Enum to Entity String */
    @Named("appointmentStatusToString")
    default String appointmentStatusToString(AppointmentStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    // --- Termination Reason Conversions ---

    /** Converts Entity String to Domain Enum */
    @Named("stringToTerminationReason")
    default AppointmentTerminationReason stringToTerminationReason(String reason) {
        if (reason == null) {
            return null;
        }
        return AppointmentTerminationReason.valueOf(reason);
    }

    /** Converts Domain Enum to Entity String */
    @Named("terminationReasonToString")
    default String terminationReasonToString(AppointmentTerminationReason reason) {
        if (reason == null) {
            return null;
        }
        return reason.name();
    }





}