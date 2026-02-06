package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

/**
 * Use case to retrieve appointments for a specific customer.
 * Supports optional filtering by appointment status.
 */
@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class GetCustomerAppointmentsUseCase {

    private final AppointmentService appointmentService;

    public Collection<Appointment> execute(@Valid GetCustomerAppointmentsQuery query) {
        log.info("Fetching appointments for customer: {}, status filter: {}, offset: {}, limit: {}",
                query.customerUsername(), query.status(), query.offset(), query.limit());

        try {
            Collection<Appointment> appointments = appointmentService.findByCustomerUsername(
                    query.customerUsername(),
                    query.status(),
                    query.offset(),
                    query.limit()
            );

            log.info("Found {} appointments for customer: {}", appointments.size(), query.customerUsername());
            return appointments;

        } catch (Exception e) {
            log.error("Failed to fetch appointments for customer: {}", query.customerUsername(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve customer appointments", e);
        }
    }
}
