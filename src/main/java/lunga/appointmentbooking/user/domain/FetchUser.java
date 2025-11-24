package lunga.appointmentbooking.user.domain;

import java.util.Optional;

public interface FetchUser {

    Optional<UserProfile> fetchUser(String username);
}
