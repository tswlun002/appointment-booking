package capitec.branch.appointment.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Setter
@Getter
public class IdStore {

    private  String id;
    private List<String> idList;


}
