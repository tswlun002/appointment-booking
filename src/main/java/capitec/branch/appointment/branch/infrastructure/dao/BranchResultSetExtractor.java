package capitec.branch.appointment.branch.infrastructure.dao;

import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

class BranchResultSetExtractor implements ResultSetExtractor<Collection<BranchEntity>> {

    @Override
    public Collection<BranchEntity> extractData(ResultSet rs) throws SQLException {
        // Map to keep track of unique branches by their ID
        Map<Long, BranchEntity> branchMap = new LinkedHashMap<>();

        while (rs.next()) {
            Long id = rs.getLong("id");

            // 1. Get existing branch or create a new one
            BranchEntity branch = branchMap.computeIfAbsent(id, k -> {
                try {
                    return new BranchEntity(
                            id,
                            rs.getString("branch_id"),
                            rs.getString("branch_name"),
                            new HashMap<>(), // branchAppointmentInfo map
                            new HashMap<>(), // operationHoursOverride map
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("last_modified_date").toLocalDateTime(),
                            rs.getLong("total_count")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            // 2. Map Appointment Info (if exists)
            String dayType = rs.getString("day");
            if (dayType != null) {
                var info = new BranchAppointmentInfoEntity(
                        rs.getString("branch_id"),
                        rs.getInt("slot_duration"),
                        rs.getDouble("utilization_factor"),
                        rs.getInt("staff_count"),
                        dayType,
                        rs.getInt("max_booking_capacity")
                );
                branch.branchAppointmentInfo().put(dayType, info);
            }

            // 3. Map Overrides (if exists)
            LocalDate effectiveDate = rs.getObject("effective_date", LocalDate.class);
            if (effectiveDate != null) {
                var override = new OperationHoursOverrideEntity(
                        rs.getString("branch_id"),
                        effectiveDate,
                        rs.getObject("open_at", LocalTime.class),
                        rs.getObject("close_at", LocalTime.class),
                        rs.getBoolean("closed"),
                        rs.getString("reason"),
                        rs.getTimestamp("oh_created_at").toLocalDateTime(),
                        rs.getTimestamp("oh_modified_at").toLocalDateTime()
                );
                branch.operationHoursOverride().put(effectiveDate, override);
            }
        }
        return branchMap.values();
    }
}