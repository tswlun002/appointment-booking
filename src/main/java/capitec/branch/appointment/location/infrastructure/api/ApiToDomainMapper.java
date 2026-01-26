package capitec.branch.appointment.location.infrastructure.api;

import capitec.branch.appointment.day.domain.Day;
import capitec.branch.appointment.location.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.DayOfWeek;
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
                    dto.addressLine1() != null ? dto.addressLine1() : "Unknown",
                    dto.addressLine2(),
                    dto.city() != null ? dto.city() : "Unknown",
                    dto.province() != null ? dto.province() : "Unknown"
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
                case SATURDAY ->{
                    var dateOption = dateOfTheWeek.stream().filter(day->DayOfWeek.SATURDAY.equals(day.getValue())).findFirst();
                    OperationTimeResponse operationTimeResponse = operationHourApiResponseMap.get(DayTypeResponse.SATURDAY);
                    if(dateOption.isPresent()) {
                        LocalDate date = dateOption.get().getDate();
                        OperationTime operationTime = mapToOperationTime(operationTimeResponse, false, date, date);
                        operationHours.put(date, operationTime);
                    }
                }
                case SUNDAY ->{
                    var dateOption = dateOfTheWeek.stream().filter(day->DayOfWeek.SUNDAY.equals(day.getValue())).findFirst();
                    OperationTimeResponse operationTimeResponse = operationHourApiResponseMap.get(DayTypeResponse.SUNDAY);
                    if(dateOption.isPresent()) {
                        LocalDate date = dateOption.get().getDate();
                        OperationTime operationTime = mapToOperationTime(operationTimeResponse, false, date, date);
                        operationHours.put(date, operationTime);
                    }
                }
                case PUBLIC_HOLIDAY ->{
                    var dates = dateOfTheWeek.stream().filter(Day::isHoliday).collect(Collectors.toSet());
                    OperationTimeResponse operationTimeResponse = operationHourApiResponseMap.get(DayTypeResponse.PUBLIC_HOLIDAY);
                    for(var day : dates) {
                        OperationTime operationTime = mapToOperationTime(operationTimeResponse, true, day.getDate(), day.getDate());
                        operationHours.put(day.getDate(), operationTime);
                    }
                }
                case WEEK_DAYS->{
                    var dates = dateOfTheWeek.stream().filter(Day::isWeekday).collect(Collectors.toSet());
                    OperationTimeResponse operationTimeResponse = operationHourApiResponseMap.get(DayTypeResponse.WEEK_DAYS);
                    for(var day: dates) {
                        OperationTime operationTime = mapToOperationTime(operationTimeResponse, false, day.getDate(), day.getDate());
                        operationHours.put(day.getDate(), operationTime);
                    }

                }
            }
        }




    }

    private  static OperationTime mapToOperationTime(OperationTimeResponse resp,boolean isHoliday,
                                              LocalDate fromDate, LocalDate toDate) {
        if(resp.closed()){

            return new OperationTime(null,null, true,isHoliday, fromDate,toDate);
        }
        return  new OperationTime(resp.openAt(),resp.closeAt(), false,isHoliday, fromDate,toDate);
    }
}
