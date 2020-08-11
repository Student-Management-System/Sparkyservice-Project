package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.NewUserDto;

/**
 * Class for authentication with SpringSecurity.  This class should be mainly used for authenticated users which are 
 * stored in the {@link #DEFAULT_REALM}. 
 * 
 * @author Marcel
 */
@ParametersAreNonnullByDefault
public class LocalUserDetails extends User implements UserDetails {

    public @Nonnull static final UserRealm DEFAULT_REALM = UserRealm.LOCAL;
    public static final String DEFAULT_ALGO = "BCRYPT";
    private static final long serialVersionUID = 1L;
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Nonnull
    private final UserRealm realm = UserRealm.LOCAL;

    @Nullable
    private  PasswordEncoder encoder;

    /**
     * Default constructor only used for testing purposes.
     */
    LocalUserDetails() {
        super("", new Password(""), UserRealm.UNKNOWN, false, UserRole.DEFAULT);
    }

    /**
     * Creates a LocalUserDetails with fields from an existing one. (Clones a user).
     * 
     * @param userData - User which is cloned
     */
    public LocalUserDetails(User userData) {
        super(userData);
        log.debug("New LocalUserDetails created.");
    }

    /**
     * Constructor whith necessary fields. 
     * 
     * @param userName
     * @param passwordEntity
     * @param realm
     * @param isActive
     * @param role
     */
    private LocalUserDetails(String userName, @Nullable Password passwordEntity, UserRealm realm, boolean isActive, 
            UserRole role) {
        super(userName, passwordEntity, realm, isActive, role);
        log.debug("New LocalUserDetails created.");
    }
    
    /**
     * Creates a new user in the {@link LocalUserDetails#DEFAULT_REALM} and encodes the password. <br>
     * This type should only be used for users which aren't use any other authentication methods (realms) than 
     * a {@link UserDetailsService}. Default expiration time are 6 month.
     * 
     * @param userName - Used name of the user (unique per realm!)
     * @param rawPassword - Plain text password (will be hashed)
     * @param role - The users permission role
     * @return new instance of StoredUserDetails. 
     */
    public static @Nonnull LocalUserDetails newLocalUser(String userName, String rawPassword, UserRole role) {
        var newUser = new LocalUserDetails(userName, null, DEFAULT_REALM, true, role);
        newUser.encodeAndSetPassword(rawPassword);
        newUser.expirationTime = LocalDate.now().plusMonths(6);
        return newUser;
    }
    
    
    /**
     * Performs a transformation from DTO object to a local user. 
     * 
     * @param newUser - Valid DTO (username and password required)
     * @return User with the values of the DTO
     */
    public static LocalUserDetails createNewUserFromDto(NewUserDto newUser) {
        String username = newUser.username;
        String password = newUser.password;
        if (username != null && password != null) {
            UserRole role = Optional.ofNullable(newUser.role).orElse(UserRole.DEFAULT);
            LocalUserDetails newLocalUser =  LocalUserDetails.newLocalUser(username, password, notNull(role));
            final var settings = newUser.personalSettings;
            if (newUser.expirationTime != null) {
                newLocalUser.expirationTime = newUser.expirationTime;
            }
            if (settings != null) {                
                PersonalSettings.applyPersonalSettingsDto(newLocalUser, settings);
            }
            return newLocalUser;
        } else {
            throw new IllegalArgumentException("The NewUserDto hast null values which are not allowed");
        }
    }

    /**
     * Encodes a raw string to bcrypt hash sets the local password entity.
     * @param rawPassword
     * @return the hashed value - never null but may be empty
     */
    public String encodeAndSetPassword(String rawPassword) {
        var passwordEntityLocal = passwordEntity;
        if (passwordEntityLocal != null) {
            @Nonnull final String encodedPass = encode(rawPassword);
            passwordEntityLocal.setPasswordString(encodedPass);
            passwordEntityLocal.setHashAlgorithm(DEFAULT_ALGO);
        } else {
            @Nonnull final String encodedPass = encode(rawPassword);
            passwordEntityLocal = new Password(encodedPass, DEFAULT_ALGO);
        }
        setPasswordEntity(passwordEntityLocal);
        return getPassword();
    }

    /**
     * Encodes a string to hashed password using the default algorithm. 
     * 
     * @param rawPassword - Plain password as string
     * @return Encoded password
     */
    private @Nonnull String encode(String rawPassword) {
        final String encodedPass = getEncoder().encode(rawPassword);
        final String nonNullPass = Optional.ofNullable(encodedPass).orElseThrow(
            () -> new RuntimeException("BCryptPasswordEncoder returned null - this should not happen"));
        return notNull(nonNullPass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(getRole());
    }

    /**
     * Returns the hashed password of the user. If the user is not in the default realm, it will return an empty string
     * 
     * @return The stored password - never be null but may be empty if the user isn't in the default realm.
     */
    public @Nonnull String getPassword() {
        Password passwordEntityLocal = passwordEntity;
        if (passwordEntityLocal == null) {
            if (realm == UserRealm.LOCAL) {
                throw new RuntimeException("User has no password even though he is in the local realm. This "
                        + "shouldn't be happen.");
            } else {
                log.warn("User is in local realm but doesn't have a password.");
                passwordEntityLocal = new Password("", "PLAIN");
            }
        }
        return passwordEntityLocal.getPasswordString();
    }

    /**
     * Returns springs default {@link BCryptPasswordEncoder}. 
     * @return default instance of {@link BCryptPasswordEncoder}
     */
    public @Nonnull PasswordEncoder getEncoder() {
        PasswordEncoder encoder2 = encoder;
        if (encoder2 == null) {
            encoder2 = new BCryptPasswordEncoder();
            encoder = encoder2;
        }
        return encoder2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsername() {
        return userName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAccountNonExpired() {
        return getExpirationTime().map(date -> date.isAfter(LocalDate.now())).orElse(true);
    }

    /**
     * {@inheritDoc}
     */    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return isActive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
