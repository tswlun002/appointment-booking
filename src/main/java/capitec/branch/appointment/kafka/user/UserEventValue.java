package capitec.branch.appointment.kafka.user;


import capitec.branch.appointment.kafka.domain.ExtendedEventValue;

public interface UserEventValue extends ExtendedEventValue<UserMetadata> {
   default String getFullname(){
       return getMetadata().fullname();
   }
   default String getEmail(){
       return getMetadata().email();
   }
    default String getUsername(){
       return getMetadata().username();
    }
}
