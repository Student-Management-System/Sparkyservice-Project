package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.ssehub.sparkyservice.api.auth.exceptions.AccessViolationException;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
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
    @PutMapping(ControllerPath.USERS_PUT)
    @ResponseStatus(HttpStatus.CREATED)
    @Secured(UserRole.FullName.ADMIN)
    public UserDto addLocalUser(@RequestBody @NotNull @Valid NewUserDto newUserDto) throws UserEditException {
        final @Nonnull String username = notNull(newUserDto.username); // spring validation
        final @Nonnull String password = notNull(newUserDto.password); // spring validation
        final @Nonnull var role = notNull(Optional.ofNullable(newUserDto.role).orElse(UserRole.DEFAULT));
        final var newUser = LocalUserDetails.newLocalUser(username, password, role);
        if (!userService.isUserInDatabase(newUser)) {
            userService.storeUser(newUser);
            log.info("Created new user: {}@{}", newUser.getUsername(), newUser.getRealm());
            return newUser.asDto();
        } else {
            log.info("No user added: Duplicate entry");
            throw new UserEditException("Can't add user: Already existing");
        }
    }

    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @PatchMapping(ControllerPath.USERS_PATCH)
    @ResponseStatus(HttpStatus.CREATED)
    @Secured({UserRole.FullName.DEFAULT, UserRole.FullName.ADMIN})
    public UserDto editLocalUser(@RequestBody @NotNull @Nonnull @Valid UserDto userDto, @Nonnull Authentication auth) 
            throws AccessViolationException, UserNotFoundException, MissingDataException {
        var authenticatedUser = notNull( 
                Optional.ofNullable(transformer.extendFromAuthentication(auth))
                .orElseThrow(() -> new UserNotFoundException("The authenticated user can't be edited or the database is down")));
        boolean selfEdit = authenticatedUser.getUserName().equals(userDto.username)
              && authenticatedUser.getRealm().equals(userDto.realm);
        var authority = (GrantedAuthority) auth.getAuthorities().toArray()[0];
        User editTargetUser = null;
        if (UserRole.ADMIN.getEnum(authority.getAuthority()) == UserRole.ADMIN) {
            editTargetUser = userService.findUserByNameAndRealm(userDto.username, userDto.realm);
            User.adminUserDtoEdit(editTargetUser, userDto);
        } else if (selfEdit) {
            editTargetUser = userService.findUserByNameAndRealm(userDto.username, userDto.realm);
            User.defaultUserDtoEdit(editTargetUser, userDto);
        } else {
            log.info("User {}@{} tries to modify the data of other user without admin privileges", 
                    authenticatedUser.getUserName(), authenticatedUser.getRealm());
            log.debug("Edit target was: {}@{}", userDto.username, userDto.realm);
            throw new AccessViolationException("Not allowed to modify other users data");
        }
        userService.storeUser(editTargetUser);
        return editTargetUser.asDto();
    }

    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(ControllerPath.USERS_GET_ALL)
    @Secured(UserRole.FullName.ADMIN)
    public UserDto[] getAllUsers() {
        var list = userService.findAllUsers();
        var dtoArray = new UserDto[list.size()];
        for (int i = 0; i < list.size(); i++) {
            dtoArray[i] = list.get(i).asDto();
        }
        return dtoArray;
    }

    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(ControllerPath.USERS_PREFIX + "/{realm}")
    @Secured(UserRole.FullName.ADMIN)
    public UserDto[] getAllUsersFromRealm(@PathVariable("realm") UserRealm realm) {
        var list = userService.findAllUsersInRealm(realm);
        var dtoArray = new UserDto[list.size()];
        for (int i = 0; i < list.size(); i++) {
            dtoArray[i] = list.get(i).asDto();
        }
        return dtoArray;
    }

    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(ControllerPath.USERS_PREFIX + "/{realm}/{username}")
    public UserDto getSingleUser(@PathVariable("realm") UserRealm realm, @PathVariable("username") String username,
            Authentication auth) throws AccessViolationException, MissingDataException {
        var singleAuthy = (GrantedAuthority) auth.getAuthorities().toArray()[0];
        var role =  UserRole.DEFAULT.getEnum(singleAuthy.getAuthority());
        User authenticatedUser = userService.getDefaultTransformer().extendFromAuthentication(auth);
        if (authenticatedUser != null && username.equals(authenticatedUser.getUserName()) || role == UserRole.ADMIN) {
            var user = userService.findUserByNameAndRealm(username, realm);
            return user.asDto();
        } else {
            throw new AccessViolationException("User is not correctly authenticated or tries to modify others  data.");
        }
    }

    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @DeleteMapping(ControllerPath.USERS_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Secured(UserRole.FullName.ADMIN)
    public void deleteUser(@PathVariable("realm") UserRealm realm, @PathVariable("username") String username) { 
        userService.deleteUser(username, realm);
    }

    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessViolationException.class)
    public String handleUserEditException(AccessViolationException ex) {
        return handleException(ex);
    }

    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ExceptionHandler(UserEditException.class)
    public String handleUserNotFound(MissingDataException ex) {
        return handleException(ex);
    }

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ UserNotFoundException.class, MissingDataException.class })
    public String handleException(Exception ex) {
        log.debug("Exception in UserController", ex);
        if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
            return "There was a problem with the user data.";
        } else {
            return ex.getMessage();
        }
    }
}
