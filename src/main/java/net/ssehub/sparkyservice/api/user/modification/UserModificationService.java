package net.ssehub.sparkyservice.api.user.modification;

import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Provides utilities for a specific user. 
 * 
 * @author marcel
 */
public interface UserModificationService {

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

