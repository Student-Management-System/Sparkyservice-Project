package net.ssehub.sparkyservice.api.storeduser;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.auth.exceptions.AccessViolationException;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.storeduser.dto.EditUserDto;
import net.ssehub.sparkyservice.api.storeduser.dto.NewUserDto;
import net.ssehub.sparkyservice.api.storeduser.exceptions.UserEditException;
import net.ssehub.sparkyservice.db.user.StoredUser;

/**
 * @author Marcel
 */
@RestController
public class StoredUserController {

    private Logger log = LoggerFactory.getLogger(StoredUserController.class);

    @Autowired
    private IStoredUserService userService;
    
    @Autowired
    private StoredUserTransformer transformer;

    @PutMapping(ControllerPath.MANAGEMENT_ADD_USER)
    public void addLocalUser(@RequestBody @NotNull @Valid NewUserDto newUserDto) throws UserEditException {
        final var newUser = StoredUserDetails.createStoredLocalUser(newUserDto.username, newUserDto.password, true);
        if (!userService.isUserInDatabase(newUser)) {
            userService.storeUser(newUser);
            // return newUser.toString();
        } else {
            log.info("No user added: Duplicate entry");
            throw new UserEditException("Can't add user: Already existing");
        }
    }

    @PutMapping(ControllerPath.MANAGEMENT_EDIT_USER)
    public void editLocalUser(@RequestBody @NotNull @Nonnull @Valid EditUserDto userDto,
                              @AuthenticationPrincipal @Nullable UserDetails users, Authentication auth)
                              throws MissingDataException, UserNotFoundException, AccessViolationException {
        if (auth == null) {
            throw new InternalError("Could not get authentication");
        }
        @Nonnull Object principal = notNull(Optional.ofNullable(auth.getPrincipal()).orElse(""));
        @Nullable StoredUser authenticatedUser = null;
        if (auth.getPrincipal() instanceof SparkysAuthPrincipal) {
            var userPrincipal = (SparkysAuthPrincipal) principal;
            try {
                authenticatedUser = transformer.extendFromSparkyPrincipal(userPrincipal);
            } catch (UserNotFoundException e) {
                log.info("User is logged  in but no data is in the database. Maybe database is down?");
                throw new UserNotFoundException("Could not edit user, reason: " + e.getMessage() + ". Maybe our databse"
                        + " is offline or the logged in user was deleted.");
            }
        } else if (auth.getPrincipal() instanceof UserDetails) {
            authenticatedUser = transformer.castFromUserDetails((UserDetails) principal);
        }
        if (authenticatedUser == null) {
            throw new AccessViolationException("Unkown user type.");
        } else {
            boolean selfEdit = authenticatedUser.getUserName().equals(userDto.username) 
                    && authenticatedUser.getRealm().equals(userDto.realm);
            if (!selfEdit) {
                log.warn("User " + authenticatedUser.getUserName() + " tries to modify data of " + userDto.username);
                throw new AccessViolationException("Could not edit other users data");
            } 
            authenticatedUser = EditUserDto.editUserFromDtoValues(authenticatedUser, userDto);
            userService.storeUser(authenticatedUser);
        }
    }

    // @PutMapping(ControllerPath.MANAGEMENT_EDIT_ADMIN)
    public void editAdminUser(@RequestBody @NotNull @Nonnull @Valid Object user) {
        // TODO
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
