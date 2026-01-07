package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.day.domain.DayType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;


@ConfigurationProperties(prefix = "slot-properties")
public record MapSlotProperties(
        List<String> branchIds,
        Map<String, Map<DayType, SlotProperties>> branchSlotProperties
) {
    public static final String DEFAULT_CONFIG_KEY = "default";


  public   void copyDefaultConfigs(){

        if(branchIds !=null && !branchIds.isEmpty()){
            for(String branchId : branchIds){
                Map<DayType, SlotProperties> dayTypeSlotPropertiesMap = branchSlotProperties.get(DEFAULT_CONFIG_KEY);
                branchSlotProperties.put(branchId, dayTypeSlotPropertiesMap);
            }
        }
  }

  public  MapSlotProperties( List<String> branchIds, Map<String, Map<DayType, SlotProperties>> branchSlotProperties){
      this.branchIds = branchIds;
      this.branchSlotProperties = branchSlotProperties;
      copyDefaultConfigs();
  }

}
