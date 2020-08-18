package net.ssehub.sparkyservice.api.user.modification;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.util.DateUtil;

/**
 * Provides utilities for a specific user. 
 * 
 * @author marcel
 */
public interface UserModificationService {

    public static final int TOKEN_EXPIRE_TIME_MS = 86_400_000; // 24 hours

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
     * Sets a user which is bound to the implementation permissions. Behavior can change if conditions apply on the
     * user.
     * 
     * @param <T>
     * @param user
     */
//    <T extends User> void setPermissionProvider(User user);

    /**
     * Changes the password of a given use with the given password DTO. This is only done when the
     * correct conditions apply.
     *
     * @param databaseUser - User which password is getting changed
     * @param passwordDto
     */
//    void update(@Nullable SparkyUser databaseUser, @Nullable UserDto.ChangePasswordDto passwordDto);

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

    /**
     * Returns a date where a JWT token of user should expire. 
     * 
     * @param user
     * @return Date where the validity of a JWT token should end for the given user
     */
    default @Nonnull java.util.Date createJwtExpirationDate(SparkyUser user) {
        @Nonnull java.util.Date expirationDate;
        @Nonnull Supplier<LocalDate> defaultServiceExpirationDate = () -> LocalDate.now().plusYears(10);
        
        if (user.getRole() == UserRole.SERVICE) {
            expirationDate = notNull(
                 user.getExpireDate()
                     .map(DateUtil::toUtilDate)
                     .orElse(DateUtil.toUtilDate(defaultServiceExpirationDate.get()))
            );
        } else {
            expirationDate = new java.util.Date(System.currentTimeMillis() + TOKEN_EXPIRE_TIME_MS);
        }
        return expirationDate;
    }
}

