package capitec.branch.appointment.location.infrastructure.api;

import capitec.branch.appointment.sharekernel.day.domain.Day;
import capitec.branch.appointment.location.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


interface ApiToDomainMapper {
    Logger log = LoggerFactory.getLogger(ApiToDomainMapper.class);


    static BranchLocation mapToDomain(CapitecBranchApiResponse.CapitecBranchDto dto, Set<Day> dateOfTheWeek) {
        try {

            if (dto.latitude() == null || dto.longitude() == null) {
                log.warn("Skipping branch {} - missing coordinates", dto.code());
                return null;
            }

            Coordinates coordinates = new Coordinates(dto.latitude(), dto.longitude());
            BranchAddress address = new BranchAddress(
                    dto.addressLine1(),
                    dto.addressLine2(),
                    dto.city(),
                    dto.province()
            );

            var dayTypeResponseOperationTimeResponseMap = dto.operationHours();

            HashMap<LocalDate, OperationTime> operationHours = new HashMap<>();
            mapToOperatingHours(dayTypeResponseOperationTimeResponseMap, dateOfTheWeek, operationHours);


            return BranchLocation.create(dto.code(), dto.id(), dto.name(), coordinates, address, operationHours,
                    Boolean.TRUE.equals(dto.businessBankCenter()), Boolean.TRUE.equals(dto.isClosed()));

        } catch (Exception e) {
            log.error("Failed to map branch {}: {}", dto.code(), e.getMessage());
            return null;
        }
    }

    private static void mapToOperatingHours(Map<DayTypeResponse, OperationTimeResponse> operationHourApiResponseMap,
                                            Set<Day> dateOfTheWeek,
                                            Map<LocalDate,OperationTime> operationHours
                                            ) {
        if (operationHourApiResponseMap == null ) {
          return;
        }


        for (DayTypeResponse dayTypeResponse : operationHourApiResponseMap.keySet()) {

            switch (dayTypeResponse) {
                case PUBLIC_HOLIDAY ->{
                    var dates = dateOfTheWeek.stream().filter(Day::isHoliday).collect(Collectors.toSet());
                    OperationTimeResponse operationTimeResponse = operationHourApiResponseMap.get(DayTypeResponse.PUBLIC_HOLIDAY);
                    for(var day : dates) {
                        OperationTime operationTime = mapToOperationTime(operationTimeResponse, true,day.getDate());
                        operationHours.put(day.getDate(), operationTime);
                    }
                }
                case MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY->{
                    var day = dateOfTheWeek.stream().filter(d->d.getDate().getDayOfWeek().name().equals(dayTypeResponse.name())).findFirst();
                    OperationTimeResponse operationTimeResponse = operationHourApiResponseMap.get(dayTypeResponse);
                    if(day.isPresent()) {
                        OperationTime operationTime = mapToOperationTime(operationTimeResponse, false,day.get().getDate());
                        operationHours.put(day.get().getDate(), operationTime);
                    }

                }
            }
        }
    }

    private  static OperationTime mapToOperationTime(OperationTimeResponse resp,boolean isHoliday,
                                              LocalDate fromDate) {
        if(resp.closed()){

            return new OperationTime(null,null, true,isHoliday, fromDate);
        }
        return  new OperationTime(resp.openAt(),resp.closeAt(), false,isHoliday, fromDate);
    }
}
