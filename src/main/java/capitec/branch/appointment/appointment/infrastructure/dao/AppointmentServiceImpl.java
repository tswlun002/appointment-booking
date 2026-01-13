package capitec.branch.appointment.appointment.infrastructure.dao;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
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
    public Appointment book(@Valid Appointment appointment) {

        try {

            return save(appointment);

        } catch (Exception e) {

            if (e instanceof DuplicateKeyException || (e.getCause() != null && e.getCause() instanceof DuplicateKeyException)) {
                throw new EntityAlreadyExistException(e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public Appointment update(@Valid Appointment appointment) {
        try {

           return save(appointment);

        } catch (OptimisticLockingFailureException e) {

            throw new OptimisticLockConflictException(e.getMessage(),e);
        }
    }

    private Appointment save(Appointment appointment) {
        try {

            AppointmentEntity entity = appointmentMapper.toEntity(appointment);
            AppointmentEntity save = appointmentRepository.save(entity);

            return appointmentMapper.toDomain(save);

        }
        catch (Exception e) {

            log.error("Failed to update or save appointment to DB.\n", e);
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
    public Appointment cancelByCustomer(@Valid Appointment appointment) {
        try {

            var entity = appointmentMapper.toEntity(appointment);
            entity = appointmentRepository.save(entity);

            return appointmentMapper.toDomain(entity);
        }
        catch (OptimisticLockingFailureException e) {
            log.error("Failed to update or save appointment to DB.\n", e);
            throw new OptimisticLockConflictException(e.getMessage(),e);
        }
        catch (Exception e) {

            log.error("Failed to update appointment status to cancel on DB.",e);
            throw e;
        }
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
        return appointmentRepository.findById(appointmentId)
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
            appointmentRepository.deleteById(appointmentId);
            isDeleted = true;

        } catch (Exception e) {

            log.error("Failed to delete appointment from DB.\n", e);
            throw e;
        }
        return isDeleted;
    }

    @Override
    public Collection<Appointment> branchAppointments(String branchId, int pageNumber, int pageSize) {

        try {
            Collection<AppointmentEntity> appointments = appointmentRepository.getBranchAppointments(branchId,pageNumber,pageSize);
            return appointments.stream().map(appointmentMapper::toDomain).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get appointment from DB.\n", e);
            throw e;
        }
    }

    @Override
    public Optional<Appointment> getUserActiveAppointment(String branchId, LocalDate day, String customerUsername) {
        try {
            Optional<AppointmentEntity> entity = appointmentRepository.getUserActiveAppointment(branchId, day, customerUsername);
            return entity.map(appointmentMapper::toDomain);
        }catch (Exception e) {
            log.error("Failed to get user active appointment from DB.\n", e);
            throw e;
        }
    }
}
