package net.ssehub.sparkyservice.api.user;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.extraction.UserExtractionService;
import net.ssehub.sparkyservice.api.user.modification.UserEditException;
import net.ssehub.sparkyservice.api.user.storage.DuplicateEntryException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Provides methods for working with while checking the current authentication/authorization context.
 * 
 * @author marcel
 */
@Service
public class UserService {

    private Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserStorageService storageService;

    @Autowired
    private UserExtractionService transformerService;

    /**
     * Modify values of a user specified by a DTO. User can only edit himself or needs to be an admin in order to modify
     * other user values. Authorization information will be extracted from authentication context. 
     * 
     * @param userDto
     * @param auth
     * @return Modified user representation
     */
    public UserDto modifyUser(@Nonnull UserDto userDto, @Nonnull Authentication auth) {
        SparkyUser authenticatedUser = transformerService.extract(auth);
        Predicate<SparkyUser> selfEdit = user -> user.getUsername().equals(userDto.username);
        if (authenticatedUser.getRole() == UserRole.ADMIN || selfEdit.test(authenticatedUser)) {
            SparkyUser targetUser = storageService.findUser(userDto.username);
            authenticatedUser.getRole().getPermissionTool().update(targetUser, userDto);
            storageService.commit(targetUser);
            var editedUser = storageService.refresh(targetUser);
            return editedUser.ownDto();
        } else {
            log.info("User {} tries to modify the data of other user without admin privileges",
                    authenticatedUser.getUsername());
            log.debug("Edit target was: {}", userDto.username);
            throw new AccessDeniedException("Not allowed to modify other users data");
        }
    }

    /**
     * Searches for user informations in the database about the requested user. Only information for which the
     * authenticated user is permitted to see, are present in the returned DTO.
     * 
     * @param username Identifies the desired user
     * @param auth Probably holds authentication information - when not the access is denied
     * @return User information
     */
    public UserDto searchForSingleUser(String username, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .filter(a -> a.getAuthority().equals(UserRole.ADMIN.getAuthority()))
                .findAny()
                .isPresent();
        if (!isAdmin && !username.equals(auth.getName())) {
            log.info("The user \" {} \" tried to access not allowed user data", username);
            throw new AccessDeniedException("Modifying this user is not allowed.");
        }
        var user = storageService.findUser(username);
        return user.ownDto();
    }

    /**
     * Creates a new user with the desired username in the {@link UserRealm#LOCAL}. The username must be unique in the 
     * realm otherwise an exception is thrown.
     * 
     * @param username
     * @return The information about the created user
     * @throws UserEditException
     */
    public UserDto createLocalUser(@Nonnull String username) throws UserEditException {
        try {
            LocalUserDetails newUser = storageService.addUser(username);
            log.debug("Created new user: {}", username);
            return UserRole.ADMIN.getPermissionTool().asDto(newUser);
        } catch (DuplicateEntryException e) {
            log.debug("No user added: Duplicate entry");
            throw (e);
        }
    }

    /**
     * Transform a list of users into an array of respective DTOs.
     * 
     * @param userList
     * @return DTO array with information from the user list
     */
    public static UserDto[] userListToDtoList(List<SparkyUser> userList) {
        var util = UserRole.ADMIN.getPermissionTool();
        return userList.stream().map(util::asDto).toArray(size -> new UserDto[size]);
    }
}
