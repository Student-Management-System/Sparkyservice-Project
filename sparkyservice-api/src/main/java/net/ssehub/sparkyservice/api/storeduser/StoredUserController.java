package net.ssehub.sparkyservice.api.storeduser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.storeduser.dto.EditUserDto;
import net.ssehub.sparkyservice.api.storeduser.dto.NewUserDto;
import net.ssehub.sparkyservice.api.storeduser.exceptions.UserEditException;

/**
 * @author Marcel
 */
@RestController
public class StoredUserController {

    private Logger log = LoggerFactory.getLogger(StoredUserController.class);

    @Autowired
    private IStoredUserService userService;

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
            Authentication auth)
            throws MissingDataException, UserNotFoundException, UserEditException {
        if (auth != null && auth.getPrincipal() instanceof UserDetails) { 
            var user = (UserDetails) auth.getPrincipal();
            user.isEnabled();
            var authenticatedUser = userService.convertUserDetailsToStoredUser(user);
            boolean selfEdit = authenticatedUser.getRealm().equals(userDto.realm)
                    && authenticatedUser.getUserName().equals(userDto.username);
            if (selfEdit) {
                authenticatedUser = EditUserDto.editUserFromDtoValues(authenticatedUser, userDto);
                userService.storeUser(authenticatedUser);
            } else {
                log.warn("Security Warning: User tried to modify other users data.");
                throw new UserEditException("Permission denied.");
            }
        } else {
            throw new InternalError("Could not get authentication");
        }
    }

    // @PutMapping(ControllerPath.MANAGEMENT_EDIT_ADMIN)
    public void editAdminUser(@RequestBody @NotNull @Nonnull @Valid Object user) {
        // TODO
    }

//    @GetMapping("/user/delete")
//    @GetMapping("/user/changepass")

    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ExceptionHandler(UserEditException.class)
    public String handleUserEditException(UserEditException ex) {
        return handleException(ex);
    }

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ UserNotFoundException.class, MissingDataException.class })
    public String handleException(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
            return "User not found.";
        } else {
            return ex.getMessage();
        }
    }
}
