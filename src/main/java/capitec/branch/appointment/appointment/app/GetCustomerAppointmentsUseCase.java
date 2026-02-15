package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.port.AppointmentQueryPort;
import capitec.branch.appointment.appointment.app.port.AppointmentQueryResult;
import capitec.branch.appointment.appointment.app.port.BranchInfoPort;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;

/**
 * Use case to retrieve appointments for a specific customer.
 * Supports optional filtering by appointment status.
 */
@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class GetCustomerAppointmentsUseCase {

    private final AppointmentQueryPort appointmentQueryPort;
    private final BranchInfoPort branchInfoPort;

    public CustomerAppointmentsResult execute(@Valid GetCustomerAppointmentsQuery query) {
        log.info("Fetching appointments for customer: {}, status filter: {}, offset: {}, limit: {}",
                query.customerUsername(), query.status(), query.offset(), query.limit());

        try {
            AppointmentQueryResult queryResult = appointmentQueryPort.findByCustomerUsername(
                    query.customerUsername(),
                    query.status(),
                    query.offset(),
                    query.limit()
            );

            log.info("Found {} appointments for customer: {} (total: {})",
                    queryResult.appointments().size(), query.customerUsername(), queryResult.totalCount());

            List<AppointmentWithBranchDTO> enrichedAppointments = queryResult.appointments().stream()
                    .map(this::enrichWithBranchInfo)
                    .toList();

            return CustomerAppointmentsResult.of(enrichedAppointments, queryResult.totalCount());

        } catch (Exception e) {
            log.error("Failed to fetch appointments for customer: {}", query.customerUsername(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve customer appointments", e);
        }
    }

    private AppointmentWithBranchDTO enrichWithBranchInfo(Appointment appointment) {
        var branchInfo = branchInfoPort.getBranchInfo(appointment.getBranchId()).orElse(null);
        return AppointmentWithBranchDTO.from(
                appointment,
                branchInfo != null ? branchInfo.name() : null,
                branchInfo != null ? branchInfo.address() : null
        );
    }

    public Collection<Appointment> branchAppointments(String branchId, int offset, int limit) {
        try {
            return appointmentQueryPort.findByBranchId(branchId, offset, limit);
        } catch (Exception e) {
            log.error("Failed to fetch appointments for branch: {}", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch branch appointments", e);
        }
    }
}
