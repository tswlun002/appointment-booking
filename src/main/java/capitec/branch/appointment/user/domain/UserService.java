package capitec.branch.appointment.user.domain;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;


public interface UserService extends   FetchUser {
    User registerUser(User user);
    boolean verifyUser(String username);
    boolean verifyUserCurrentPassword(String username, String password);
    Optional<User> getUserByUsername(String username);
    default Optional<UserProfile> fetchUser(String username){
       return getUserByUsername(username).map(user -> new UserProfile(user.getUsername(), user.getEmail(), user.getFirstname()+ " " + user.getLastname()));
    }
     boolean deleteUser( String username);
     boolean checkIfUserExists(String username);

    Optional<User> getUserByEmail(@NotBlank @Email String email);

    void updateUseStatus(String username, Boolean useStatus);
}
