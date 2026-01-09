package capitec.branch.appointment.appointment.infrastructure.dao;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.exeption.SlotFullyBookedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Validated
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    @Transactional
    public Appointment book(Appointment appointment) {


        try {

            AppointmentEntity entity = appointmentMapper.toEntity(appointment);
            AppointmentEntity save = appointmentRepository.save(entity);

            return appointmentMapper.toDomain(save);

        } catch (OptimisticLockingFailureException e) {

            log.error("Failed to save appointment db.\n", e);
            throw new SlotFullyBookedException(e.getMessage(),e);
        } catch (Exception e) {

            log.error("Failed to save appointment to DB.\n", e);

            if (e instanceof DuplicateKeyException || (e.getCause() != null && e.getCause() instanceof DuplicateKeyException)) {
                throw new EntityAlreadyExistException(e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public boolean checkIn(UUID appointmentId) {
        return false;
    }

    @Override
    public boolean startService(String staffRef, UUID appointmentId) {
        return false;
    }

    @Override
    public boolean complete(UUID appointmentId) {
        return false;
    }

    @Override
    public boolean cancelByCustomer(UUID appointmentId) {
        return false;
    }

    @Override
    public boolean cancelByStaff(String staffRef, String reason, UUID appointmentId) {
        return false;
    }

    @Override
    public boolean reschedule(UUID appointmentId, UUID newSlotId) {
        return false;
    }

    @Override
    public boolean markAsNoShow(UUID appointmentId) {
        return false;
    }

    @Override
    public Optional<Appointment> findById(UUID appointmentId) {

        if(appointmentId == null) {
            return Optional.empty();
        }
        return appointmentRepository.findById(appointmentId.toString())
                .map(appointmentMapper::toDomain);

    }

    @Override
    public Collection<Appointment> customerAppointments(String username, AppointmentStatus status) {
        return List.of();
    }

    @Override
    public boolean deleteAppointment(UUID appointmentId) {
        if(appointmentId == null) {
            return false;
        }
        var isDeleted = false;
        try {
            appointmentRepository.deleteById(appointmentId.toString());
            isDeleted = true;

        } catch (Exception e) {

            log.error("Failed to delete appointment from DB.\n", e);
            throw e;
        }
        return isDeleted;
    }

    @Override
    public Collection<Appointment> branchAppointments(String branchId) {
        Collection<AppointmentEntity> appointments = appointmentRepository.getBranchAppointments(branchId);
        return appointments.stream().map(appointmentMapper::toDomain).collect(Collectors.toSet());
    }
}
