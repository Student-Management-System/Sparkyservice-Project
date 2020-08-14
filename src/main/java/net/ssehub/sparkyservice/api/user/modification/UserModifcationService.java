package net.ssehub.sparkyservice.api.user.modification;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.util.SparkyUtil;

/**
 * Provides utilities for a specific user. 
 * 
 * @author marcel
 */
public interface UserModifcationService {

    public static final int TOKEN_EXPIRE_TIME_MS = 86_400_000; // 24 hours

    /**
     * Sets a user which is bound to the implementation permissions. Behavior can change if conditions apply on the
     * user.
     * 
     * @param <T>
     * @param user
     */
    <T extends User> void setPermissionProvider(User user);

    /**
     * Changes the password of a given use with the given password DTO. This is only done when the
     * correct conditions apply.
     *
     * @param databaseUser - User which password is getting changed
     * @param passwordDto
     */
    void changePasswordFromDto(@Nullable User databaseUser, @Nullable UserDto.ChangePasswordDto passwordDto);

    /**
     * Edit values of a given user with values from a DTO. This can be done in two modes
     * 
     * @param databaseUser
     * @param userDto
     */
    void changeUserValuesFromDto(User databaseUser, UserDto userDto);

    /**
     * DTO object from the given. Maybe not all fields are present. This depends on the current permission provider. 
     * 
     * @param user
     * @return DTO with values from the given user
     */
    UserDto userAsDto(User user);

    /**
     * Returns a date where a JWT token of user should expire. 
     * 
     * @param user
     * @return Date where the validity of a JWT token should end for the given user
     */
    default @Nonnull java.util.Date createJwtExpirationDate(User user) {
        @Nonnull java.util.Date expirationDate;
        @Nonnull Supplier<LocalDate> defaultServiceExpirationDate = () -> LocalDate.now().plusYears(10);
        
        if (user.getRole() == UserRole.SERVICE) {
            expirationDate = notNull(
                 user.getExpirationDate()
                     .map(SparkyUtil::toJavaUtilDate)
                     .orElseGet(() -> SparkyUtil.toJavaUtilDate(defaultServiceExpirationDate.get()))
            );
        } else {
            expirationDate = new java.util.Date(System.currentTimeMillis() + TOKEN_EXPIRE_TIME_MS);
        }
        return expirationDate;
    }

    /**
     * Set a new password entity to the given user.
     *  
     * @param user
     * @param newPassword
     */
    static void applyPassword(@Nonnull User user, @Nonnull String newPassword) {
        LocalUserDetails localUser;
        if (user instanceof LocalUserDetails) {
            localUser = (LocalUserDetails) user;
            localUser.encodeAndSetPassword(newPassword);
        } else if (user.getRealm() == UserRealm.LOCAL) {
            localUser = new LocalUserDetails(user);
            localUser.encodeAndSetPassword(newPassword);
            user.setPasswordEntity(localUser.getPasswordEntity()); // make pass by reference possible.
        }
    }
}

