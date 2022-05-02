package net.ssehub.sparkyservice.api.auth.identity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import net.ssehub.sparkyservice.api.persistence.NoTransactionUnitException;
import net.ssehub.sparkyservice.api.persistence.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.persistence.jpa.user.User;
import net.ssehub.sparkyservice.api.useraccess.UserRole;
import net.ssehub.sparkyservice.api.useraccess.dto.UserDto;
import net.ssehub.sparkyservice.api.useraccess.dto.UserDto.ChangePasswordDto;
import net.ssehub.sparkyservice.api.util.NullHelpers;

/**
 * User/Account for SparkyService.
 * 
 * @author marcel
 */
public interface SparkyUser extends UserDetails {

    /**
     * Permission role of the account.
     * 
     * @return Single permissions role.
     */
    @Nonnull
    UserRole getRole();

    /**
     * Sets the permission role of the user which could change the behaviour.
     * 
     * @param role
     */
    void setRole(UserRole role);

    /**
     * Disables/enables the account.
     * 
     * @param isEnabled
     */
    void setEnabled(boolean isEnabled);

    /**
     * Expiration date of this account.
     * 
     * @return The date where the account expires
     */
    @Nonnull
    Optional<LocalDate> getExpireDate();

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean isAccountNonExpired() {
        return getExpireDate().map(LocalDate.now()::isBefore).orElse(true); // default is enabled.
    }

    /**
     * Sets the expiration date of the account.
     * 
     * @param expireDate
     */
    void setExpireDate(LocalDate expireDate);

    /**
     * Gets the current user role as GrantedAuthority collection for spring.
     */
    @Override
    @Nonnull
    default Collection<? extends GrantedAuthority> getAuthorities() {
        return NullHelpers.notNull(Arrays.asList(getRole()));
    }

    /**
     * Returns a storage user.
     * 
     * @return User with JPA annotations for persistent save
     * @throws NoTransactionUnitException when the implementation can't provide an JPA object for transaction
     */
    @Nonnull
    User getJpa() throws NoTransactionUnitException;

    /**
     * Returns settings of a user.
     * 
     * @return Settings of the user (passed by reference)
     */
    @Nonnull
    PersonalSettings getSettings();

    /**
     * {@inheritDoc}
     * 
     * Is unique and contains realm information. 
     * To get the username (nickname) without realm, use {@link #getIdentity()}.
     */
    @Nonnull
    @Override
    String getUsername();

    /**
     * Can be used for user identification across multiple realms.
     * @return  Gives the identity object to distinct between realm and the nickname. 
     */
    @Nonnull
    Identity getIdentity();
    
    /**
     * Changes the password of the user to a new one with the given permissions.
     * 
     * @param passwordDto Contains password information
     * @param role - Defines the permissions of the user who tries to change the password
     */
    void updatePassword(@Nonnull ChangePasswordDto passwordDto, @Nonnull UserRole role);

    /**
     * Full name of the user. Can be null and is may not be unique.
     *
     * @return First + Last Name of the user
     */
    String getFullname();

    /**
     * Full name of the user. Can be null and is may not be unique.
     * 
     * @param fullname - Example: <code> Max Musterman </code>
     */
    void setFullname(@Nullable String fullname);

    /**
     * Returns the hashed password of the user. If the user is not in the default realm, it will return an empty string
     * 
     * @return The stored password - never be null but may be empty if the user isn't in the default realm.
     */
    String getPassword();

    /**
     * Checks if the given object is equal to the current instance. It checks the field of the instance instead of 
     * just the reference.
     * 
     * @param object
     * @return <code> true </code> when the object fields are equal
     */
    @Override
    boolean equals(Object object);

    /**
     * Custom hashcode implementation for this class. Generates a custom hash for the current instance.
     * 
     * @return Hashed integer using fields
     */
    @Override
    int hashCode();
    
    /**
     * Returns the user object as DTO while respecting the permissions of the current user. Some information may be 
     * omitted when this user hasn't full permissions. 
     * 
     * @return This object as DTO
     */
    @Nonnull
    UserDto ownDto();
}
