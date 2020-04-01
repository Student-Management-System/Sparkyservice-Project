package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.ssehub.sparkyservice.api.auth.exceptions.AccessViolationException;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.NewUserDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.exceptions.MissingDataException;
import net.ssehub.sparkyservice.api.user.exceptions.UserEditException;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;

/**
 * @author Marcel
 */
@RestController
public class UserController {

    private Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private IUserService userService;
    
    @Autowired
    private UserTransformer transformer;

    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @PutMapping(ControllerPath.MANAGEMENT_ADD_USER)
    public void addLocalUser(@RequestBody @NotNull @Valid NewUserDto newUserDto) throws UserEditException {
        @Nonnull String username = notNull(newUserDto.username); // spring validation
        @Nonnull String password = notNull(newUserDto.password); // spring validation
        final var newUser = LocalUserDetails.newLocalUser(username, password, true);
        if (!userService.isUserInDatabase(newUser)) {
            userService.storeUser(newUser);
            // return newUser.toString();
        } else {
            log.info("No user added: Duplicate entry");
            throw new UserEditException("Can't add user: Already existing");
        }
    }

    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @PutMapping(ControllerPath.MANAGEMENT_EDIT_USER)
    public void editLocalUser(@RequestBody @NotNull @Nonnull @Valid UserDto userDto, @Nullable Authentication auth)
                              throws MissingDataException, UserNotFoundException, AccessViolationException {
        if (auth == null) {
            throw new InternalError("Authentication not received");
        }
        @Nullable User authenticatedUser = null;
        try {
            authenticatedUser = transformer.extendFromAny(auth.getPrincipal());
            if (authenticatedUser == null) {
                log.warn("Unknown user type is logged in: " + auth.getPrincipal().toString());
                throw new AccessViolationException("User not known.");
            }
            boolean selfEdit = authenticatedUser.getUserName().equals(userDto.username) 
                    && authenticatedUser.getRealm().equals(userDto.realm);
            if (authenticatedUser.getRole() == UserRole.ADMIN) {
                User.adminUserDtoEdit(authenticatedUser, userDto);
            } else if (selfEdit) {
                User.defaultUserDtoEdit(authenticatedUser, userDto);
                userService.storeUser(authenticatedUser);
            } else {
                log.warn("User " + authenticatedUser.getUserName() + " tries to modify data of " + userDto.username);
                throw new AccessViolationException("Could not edit other users data");
            }
        } catch (UserNotFoundException e) {
            log.info("User is logged  in but no data is in the database. Maybe database is down?");
            throw new UserNotFoundException("Could not edit user, reason: " + e.getMessage() + ". Maybe our databse"
                    + " is offline or the logged in user was deleted.");
        }
    }

//    @GetMapping("/user/delete")
//    @GetMapping("/user/changepass")

    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessViolationException.class)
    public String handleUserEditException(AccessViolationException ex) {
        return handleException(ex);
    }
    
    @ResponseStatus(code = HttpStatus.NOT_MODIFIED)
    @ExceptionHandler(MissingDataException.class)
    public String handleUserNotFound(MissingDataException ex) {
        return handleException(ex);
    }

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ UserNotFoundException.class, UserEditException.class })
    public String handleException(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
            return "There was a problem with the user data.";
        } else {
            return ex.getMessage();
        }
    }
}
