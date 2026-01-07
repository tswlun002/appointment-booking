package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.day.domain.DayType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;


@ConfigurationProperties(prefix = "branch-slot")
public record BranchSlotConfigs(
        List<String> branchUseDefaultConfigs,
        Map<String, Map<DayType, SlotProperties>> branchConfigs
) {
    public static final String DEFAULT_CONFIG_KEY = "default";


  public   void copyDefaultConfigs(){

        if(branchUseDefaultConfigs !=null && !branchUseDefaultConfigs.isEmpty()){
            for(String branchId : branchUseDefaultConfigs){
                Map<DayType, SlotProperties> dayTypeSlotPropertiesMap = branchConfigs.get(DEFAULT_CONFIG_KEY);
                branchConfigs.put(branchId, dayTypeSlotPropertiesMap);
            }
        }
  }

  public BranchSlotConfigs(List<String> branchUseDefaultConfigs, Map<String, Map<DayType, SlotProperties>> branchConfigs){
      this.branchUseDefaultConfigs = branchUseDefaultConfigs;
      this.branchConfigs = branchConfigs;
      copyDefaultConfigs();
  }

}
