package net.ssehub.sparkyservice.api.user.modification;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Provides utilities for a specific user. 
 * 
 * @author marcel
 */
public interface UserModificationService {

    /**
     * Creates a utility object with. The given role "decides" how
     * powerful (regarding to modifying fields, changing conditions and provided informations) the tool will be.
     *  
     * @param role - The permissions of the utility
     * @return A utility for modifying and accessing users
     */
     public static UserModificationService from(UserRole role) {
        UserModificationService util;
        switch(role) {
        case ADMIN:
            util = new AdminUserModificationImpl(new DefaultUserModificationImpl());
            break;
        case SERVICE:
        case DEFAULT:
        default:
            util = new DefaultUserModificationImpl();
            break;
        }
        return util;
    }

    /**
     * Edit values of a given user with values from a DTO. This can be done in two modes
     * 
     * @param databaseUser
     * @param userDto
     */
    void update(SparkyUser databaseUser, UserDto userDto);

    /**
     * DTO object from the given. Maybe not all fields are present. This depends on the current permission provider. 
     * 
     * @param user
     * @return DTO with values from the given user
     */
    UserDto asDto(SparkyUser user);
}

