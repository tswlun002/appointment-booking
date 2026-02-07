package capitec.branch.appointment.user.infrastructure.controller;

import capitec.branch.appointment.user.app.*;
import capitec.branch.appointment.user.app.dto.ChangePasswordDTO;
import capitec.branch.appointment.user.app.dto.ChangePasswordRequestDTO;
import capitec.branch.appointment.user.app.dto.DeleteUserDTO;
import capitec.branch.appointment.user.app.dto.DeleteUserRequestDTO;
import capitec.branch.appointment.user.app.dto.EmailCommand;
import capitec.branch.appointment.user.app.dto.UsernameCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final GetUserQuery getUserQuery;
    private final PasswordResetUseCase passwordResetUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    @GetMapping("/{username}")
    @PreAuthorize("@securityUtils.isUsernameMatching(authentication, #username)") // ✅ Check Username from header vs path
    public ResponseEntity<?> getUser(@PathVariable("username") String username, @RequestHeader("Trace-Id") String traceId) {

        log.info("Getting user traceId:{}", traceId);
        var user = getUserQuery.execute(new UsernameCommand(username));
        var isFound = user != null;
        return  new ResponseEntity<>(isFound ? user : "Failed to fetch user", isFound ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @GetMapping("/")
    @PreAuthorize("@securityUtils.isEmailMatching(authentication, #email)")
    public ResponseEntity<?> getUserEmail(@RequestParam("email") String email, @RequestHeader("Trace-Id") String traceId) {

        log.info("Getting user, traceId:{}", traceId);
        var user = getUserQuery.execute(new EmailCommand(email));
        return  new ResponseEntity<>(user, HttpStatus.OK);
    }


    @PutMapping("/credentials/password/update-request")
    @PreAuthorize("@securityUtils.isUsernameMatching(authentication, #changePasswordRequestDTO.username())") // ✅ Check Username from header vs path
    public ResponseEntity<?> RequestToUpdateUserPassword(@RequestBody ChangePasswordRequestDTO changePasswordRequestDTO, @RequestHeader("Trace-Id") String traceId) {

        log.info("Changing password for traceId:{}", traceId);
       var isVerified=  passwordResetUseCase.passwordChangeRequest(changePasswordRequestDTO.username(), changePasswordRequestDTO.password(), traceId);

       return new ResponseEntity<>(isVerified ? "Confirm OTP code sent to your email" : "Password is invalid", isVerified ? HttpStatus.OK : HttpStatus.UNAUTHORIZED);
    }
    @PutMapping("/credentials/password/update")
    @PreAuthorize("@securityUtils.isUsernameMatching(authentication, #changePasswordDTO.username())") // ✅ Check Username from header vs path
    public ResponseEntity<?> updateUserPassword(@RequestBody @Valid ChangePasswordDTO changePasswordDTO, @RequestHeader("Trace-Id") String traceId) {

        log.info("Updating password for user traceId:{}", traceId);
        passwordResetUseCase.passwordChange(changePasswordDTO, traceId);
        return new ResponseEntity<>("Password updated successfully", HttpStatus.OK);
    }
    @PostMapping("/delete/request")
    @PreAuthorize("hasAnyRole('app_user')") // ✅ Check Username from header vs path
    public ResponseEntity<?> deleteUser(@RequestHeader("Trace-Id") String traceId, @RequestBody @Valid DeleteUserRequestDTO deleteUserRequest) {

        log.info("Deleting user request traceId:{}", traceId);
        boolean requestSuccessful = deleteUserUseCase.deleteUserRequest(deleteUserRequest.username(), deleteUserRequest.password(), traceId);
        return new ResponseEntity<>(requestSuccessful ? "User delete request successful. OTP code is sent to your email" : "User deletion request failed",
                requestSuccessful ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('app_user')") // ✅ Check Username from header vs path
    public ResponseEntity<?> deleteUser(@RequestHeader("Trace-Id") String traceId, @RequestBody @Valid DeleteUserDTO deleteUser) {

        log.info("Deleting user traceId:{}", traceId);
        boolean requestSuccessful = deleteUserUseCase.deleteUser(deleteUser.username(), deleteUser.OTP(), traceId);
        return new ResponseEntity<>(requestSuccessful ? "User deleted successfully" : "User deletion failed, double check OTP",
                requestSuccessful ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
