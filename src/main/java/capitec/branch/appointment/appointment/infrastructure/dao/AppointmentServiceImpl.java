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
import java.util.*;
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

    @Override
    public Collection<Appointment> update(Collection<Appointment> appointment) {
        try {

            var list = appointment.stream().map(appointmentMapper::toEntity).toList();
            Iterable<AppointmentEntity> appointmentEntities = appointmentRepository.saveAll(list);
            return ((Collection<AppointmentEntity>) appointmentEntities).stream()
                    .map(appointmentMapper::toDomain)
                    .collect(Collectors.toSet());

        } catch (OptimisticLockingFailureException e) {
            log.error("Failed to update or save appointment to DB.\n", e);
            throw new OptimisticLockConflictException(e.getMessage(),e);
        }
        catch (Exception e) {

            log.error("Failed to update or save appointment to DB.\n", e);
            throw e;
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
    public Optional<Appointment> findById(UUID appointmentId) {

        if(appointmentId == null) {
            return Optional.empty();
        }
        return appointmentRepository.findById(appointmentId)
                .map(appointmentMapper::toDomain);

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
    @Override
    public Collection<Appointment> getUnAttendedAppointments( LocalDate appointmentDate,  UUID lastProcessedId, int limit){
        try {
            Collection<AppointmentEntity> appointments = appointmentRepository.getUnAttendedAppointments(appointmentDate,lastProcessedId,limit);
            return appointments.stream().map(appointmentMapper::toDomain).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get appointment from DB.\n", e);
            throw e;
        }
    }

    @Override
    public Collection<Appointment> findByCustomerUsername(String customerUsername, AppointmentStatus status, int offset, int limit) {
        try {
            String statusValue = status != null ? status.name() : null;
            Collection<AppointmentEntity> appointments = appointmentRepository.findByCustomerUsername(customerUsername, statusValue, offset, limit);
            return appointments.stream().map(appointmentMapper::toDomain).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get customer appointments from DB. Customer: {}, Status: {}", customerUsername, status, e);
            throw e;
        }
    }

}
