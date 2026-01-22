package capitec.branch.appointment.location.infrastructure.api;

import capitec.branch.appointment.location.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

interface ApiToDomainMapper {
    Logger log = LoggerFactory.getLogger(ApiToDomainMapper.class);
    static Optional<BranchLocation> mapToDomain(CapitecBranchApiResponse.CapitecBranchDto dto) {
        try {

            if (dto.latitude() == null || dto.longitude() == null) {
                log.warn("Skipping branch {} - missing coordinates", dto.code());
                return Optional.empty();
            }

            Coordinates coordinates = new Coordinates(dto.latitude(), dto.longitude());

            BranchAddress address = new BranchAddress(
                    dto.addressLine1() != null ? dto.addressLine1() : "Unknown",
                    dto.addressLine2(),
                    dto.city() != null ? dto.city() : "Unknown",
                    dto.province() != null ? dto.province() : "Unknown"
            );

            String weeklyHours = dto.openingHours();

            OperationTime weekelyTime = null;

            if(StringUtils.isNoneBlank(weeklyHours)) {

                var  closed = weeklyHours.toLowerCase().contains("closed");
                String[] weekdaysOperationHours = weeklyHours.replaceAll("_", ",")
                        .replace("am", "").replace("pm", "").split(",");

                weekelyTime = closed ? null : new OperationTime(
                        LocalTime.parse(weekdaysOperationHours[2]),
                        LocalTime.parse(weekdaysOperationHours[3]),
                        false,
                        DayOfWeek.valueOf(weekdaysOperationHours[0].trim()),
                        DayOfWeek.valueOf(weekdaysOperationHours[1].trim())
                );

            }

            OperationTime[] operatingDaysArray = new OperationTime[3];
            int index = 0;
            for(var day : new String[]{dto.saturdayHours(),dto.sundayHours(),dto.publicHolidayHours()}) {

                if (StringUtils.isBlank(day)) {
                    var closed = day.toLowerCase().contains("closed");
                    String[] hoursDetails = day.replace("_", ",")
                            .replace("am", "").replace("pm", "")
                            .split(",");
                    var  dayOperation = closed ? null : new OperationTime(
                            LocalTime.parse(hoursDetails[1]),
                            LocalTime.parse(hoursDetails[2]),
                            false,
                            DayOfWeek.valueOf(hoursDetails[0].trim()),
                            null
                    );
                    operatingDaysArray[index]=dayOperation;
                }

            }

            OperatingHours operatingHours = new OperatingHours(
                    weekelyTime,
                    operatingDaysArray[0],
                    operatingDaysArray[1],
                    operatingDaysArray[2]
            );



            return Optional.of(BranchLocation.create(
                    dto.code(),
                    dto.id(),
                    dto.name(),
                    coordinates,
                    address,
                    operatingHours,
                    Boolean.TRUE.equals(dto.businessBankCenter()),
                    Boolean.TRUE.equals(dto.isClosed())
            ));

        } catch (Exception e) {
            log.error("Failed to map branch {}: {}", dto.code(), e.getMessage());
            return Optional.empty();
        }
    }
}
