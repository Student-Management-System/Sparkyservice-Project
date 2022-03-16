package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.user.dto.ErrorDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.extraction.MissingDataException;
import net.ssehub.sparkyservice.api.user.modification.UserEditException;
import net.ssehub.sparkyservice.api.user.storage.DuplicateEntryException;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;
import net.ssehub.sparkyservice.api.util.ErrorDtoBuilder;

/**
 * Rest controller for add, edit and view user data. 
 * 
 * @author Marcel
 */
@RestController
@Tag(name = "user-controller", description = "Controller for user managment")
public class UserController {

    /**
     * Gives the username of a user especially for new users.
     * 
     * @author marcel
     */
    //checkstyle: stop visibility modifier check
    public static class UsernameDto {
        @NotBlank
        public String username;
    }
    
    private Logger log = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;

    @Autowired
    private UserStorageService storageService;

    @Autowired
    private ServletContext servletContext;

    /**
     * Creates a new user in the LOCAL realm. 
     * 
     * @param dto - DTO with unique username which will be created in {@link UserRealm#LOCAL}
     * @return created User as DTO
     * @throws UserEditException
     */
    @Operation(summary = "Adds a new local user", security = { @SecurityRequirement(name = "bearer-key") })
    @PutMapping(value = ControllerPath.USERS_PUT, consumes = { "application/json" })
    @ResponseStatus(HttpStatus.CREATED)
    @Secured(UserRole.FullName.ADMIN)
    public UserDto createLocalUser(@RequestBody @NotNull @Nonnull UsernameDto dto) throws UserEditException {
        log.trace("Request for creating a local user");
        // notNull because the dto is provided via spring with NotNull
        // as runtime validation.
        String username = notNull(dto.username); 
        return userService.createLocalUser(username);
    }

    /**
     * Edits a user with the given DTO - User must be authorized. 
     * 
     * @param userDto
     * @param auth Holds authentication information - is necessary in order to decide if the target can be modified with
     *             permissions of the authorized user
     * @return Edited user as DTO
     * @throws UserNotFoundException
     * @throws MissingDataException
     */
    @Operation(summary = "Edits users (in any realm)", description = "Edit and return the new user", 
        security = { @SecurityRequirement(name = "bearer-key") })
    @PatchMapping(value = ControllerPath.USERS_PATCH, consumes = { "application/json" })
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "User edit was successful", 
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "403", description = "Current user is not authorized to edit this user", 
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Current user is not authenticated", content = @Content),
        @ApiResponse(responseCode = "404", description = "The edit target was not found", content = @Content),
    })
    @Secured({ UserRole.FullName.DEFAULT, UserRole.FullName.ADMIN })
    public UserDto editUser(@RequestBody @NotNull @Nonnull @Valid UserDto userDto, @Nonnull Authentication auth)
            throws UserNotFoundException, MissingDataException {
        log.trace("Request for creating editing a user");
        return userService.modifyUser(userDto, auth);
    }

    /**
     * Returnes all user from a persistent storage (those which aren't in a persistent storage, can't be returned here
     * e.g. memory realm users).
     * 
     * @return Array of user DTOs from all supported realms
     */
    @Operation(summary = "Gets all users from all supported realms", 
        security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(ControllerPath.USERS_GET_ALL)
    @Secured(UserRole.FullName.ADMIN)
    public UserDto[] getAllUsers() {
        var list = storageService.findAllUsers();
        return UserService.userListToDtoList(list);
    }

    /**
     * Get all user from a specific realm which are in a persistent storage. Some realms may be unsupported.
     * 
     * @param realm
     * @return DTO array with information of all users in a specific realm
     */
    @Operation(summary = "Gets all users from a single realm", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(ControllerPath.USERS_PREFIX + "/{realm}")
    @Secured(UserRole.FullName.ADMIN)
    public UserDto[] getAllUsersFromRealm(@PathVariable("realm") UserRealm realm) {
        var list = storageService.findAllUsersInRealm(realm);
        return UserService.userListToDtoList(list);
    }

    /**
     * Searches a specific user in the database.. 
     * 
     * @param username must include realm information
     * @param auth
     * @return Information about the requested user - maybe they are not complete
     * @throws MissingDataException
     */
    @Operation(summary = "Gets a unique user", security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses(value = { 
            @ApiResponse(responseCode = "200", description = "Gives information about a desired user"),
            @ApiResponse(responseCode = "403", description = "User is not allowed to see the information"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "The desired user was not found") 
    })
    @GetMapping(ControllerPath.USERS_GET_SINGLE)
    public UserDto getUser(@PathVariable("username") String username, Authentication auth) {
        log.trace("Request for searching a user: {}", username);
        return userService.searchForSingleUser(username, auth);
    }

    /**
     * Removes a user from database.When the realm is not supported for delete operations, no user is deleted.
     * 
     * @param realm
     * @param username
     */
    @Operation(summary = "Deletes a user", security = { @SecurityRequirement(name = "bearer-key") })
    @DeleteMapping(ControllerPath.USERS_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Secured(UserRole.FullName.ADMIN)
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(responseCode = "403", description = "User is not authorized"),
        @ApiResponse(responseCode = "401", description = "User is not authenticated "),
        @ApiResponse(responseCode = "404", description = "The desired user was not found") 
    })
    // TODO change path variable
    public void deleteUser(@PathVariable("realm") UserRealm realm, @PathVariable("username") String username) {
        storageService.deleteUser(username, realm);
    }

    /**
     * Exception handler for {@link UserController} for exceptions which occur during user edit. 
     * 
     * @param ex
     * @return ErrorDTO with all collected information about the error
     */
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = UserNotFoundException.class)
    public ErrorDto handleUserNotFoundException(Exception ex) {
        return new ErrorDtoBuilder("User target not found", HttpStatus.NOT_FOUND)
                .withUrlPath(servletContext.getContextPath())
                .build();
    }

    /**
     * Exception handler for {@link UserController} for exceptions which occur during user edit.
     * 
     * @param ex
     * @return ErrorDTO with all collected information about the error
     */
    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ExceptionHandler({ DuplicateEntryException.class, DataIntegrityViolationException.class })
    public ErrorDto handleDuplicateEntryException(RuntimeException ex) {
        log.warn("Duplicate entry attempt", ex);
        return new ErrorDtoBuilder("Possible duplicate", HttpStatus.CONFLICT).build();
    }

    /**
     * Exception handler for editiation problems. Return status is 400 BAD REQUEST. 
     * 
     * @param ex
     * @return ErrorDto with all collectable information
     */
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UserEditException.class)
    public ErrorDto handleUserEditException(UserEditException ex) {
        return new ErrorDtoBuilder(ex.getMessage(), HttpStatus.BAD_REQUEST)
                .withUrlPath(servletContext.getContextPath())
                .build();
    }

    /**
     * Exception handler for {@link UserController} for exceptions which occur during user edit. 
     * 
     * @param ex
     * @return ErrorDTO with all collected information about the error
     */
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({Exception.class})
    public ErrorDto handleException(Exception ex) {
        log.info("Exception in user controller: {}", ex.getCause());
        log.debug("" +  ex.getStackTrace());
        return new ErrorDtoBuilder(ex.getClass().getName(), HttpStatus.INTERNAL_SERVER_ERROR)
                .withUrlPath(servletContext.getContextPath())
                .build();
    }
}
