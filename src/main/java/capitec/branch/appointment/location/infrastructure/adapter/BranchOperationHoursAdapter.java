package capitec.branch.appointment.location.infrastructure.adapter;

import capitec.branch.appointment.branch.app.port.BranchOperationHoursPort;
import capitec.branch.appointment.branch.app.port.OperationHourDetails;
import capitec.branch.appointment.exeption.BranchIsClosedException;
import capitec.branch.appointment.location.app.BranchLocationFetcher;
import capitec.branch.appointment.location.domain.OperationTime;
import capitec.branch.appointment.slots.app.CheckHolidayQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchOperationHoursAdapter implements BranchOperationHoursPort {

    private final BranchLocationFetcher branchLocationFetcher;
    private final CheckHolidayQuery checkHolidayQuery;


    @Override
    public Optional<OperationHourDetails> getOperationHours(String country, String branchId, LocalDate day) {

        return branchLocationFetcher.fetchByArea(country)
                .stream()
                .filter(b->{

                    if(b.isClosed()){
                        log.warn("Branch is closed, branch:{}",b);
                        throw new BranchIsClosedException(String.format("Branch(id:%s, code:%s) is closed", b.getBranchCode(),branchId));
                    }
                    return b.getBranchId().equals(branchId);
                })
                .map(b->{
                    boolean isHoliday = checkHolidayQuery.execute(day);

                    DayOfWeek dayOfWeek = day.getDayOfWeek();

                    OperationTime operationTime = isHoliday ?
                            b.getOperatingHours()==null?null:b.getOperatingHours().publicHolidayHours() :
                            dayOfWeek == DayOfWeek.SATURDAY ?
                            b.getOperatingHours()==null?null:b.getOperatingHours().saturdayHours() :
                            dayOfWeek == DayOfWeek.SUNDAY ?
                            b.getOperatingHours()==null?null: b.getOperatingHours().sundayHours() :
                            b.getOperatingHours()==null?null:b.getOperatingHours().weekdayHours();

                    return operationTime==null?null: new OperationHourDetails(
                            operationTime.openTime(), operationTime.closingTime(),
                            operationTime.closed());

                })
                .findFirst();
    }

    @Override
    public boolean checkExist(String country, String branchId) {
       return branchLocationFetcher.fetchByArea(country)
                .stream()
                .anyMatch(b->{

                    if(b.isClosed()){
                        log.warn("Branch is closed, branch:{}",b);
                        throw new BranchIsClosedException(String.format("Branch(id:%s, code:%s) is closed", b.getBranchCode(),branchId));
                    }
                    return b.getBranchId().equals(branchId);
                });
    }


}
