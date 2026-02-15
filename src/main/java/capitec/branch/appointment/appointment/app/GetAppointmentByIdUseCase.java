package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.port.AppointmentQueryPort;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

/**
 * Use case to retrieve an appointment by its ID.
 */
@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class GetAppointmentByIdUseCase {

    private final AppointmentQueryPort appointmentQueryPort;

    public Appointment execute(@Valid GetAppointmentByIdQuery query) {
        log.info("Fetching appointment by ID: {}", query.appointmentId());

        return appointmentQueryPort.findById(query.appointmentId())
                .orElseThrow(() -> {
                    log.warn("Appointment not found: {}", query.appointmentId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
                });
    }
}
