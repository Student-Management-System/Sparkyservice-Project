package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;

/**
 * Class for authentication with SpringSecurity. This class should be mainly used for authenticated users which are
 * stored in the {@link #DEFAULT_REALM}.
 * 
 * @author Marcel
 */
@ParametersAreNonnullByDefault
public class LocalUserDetails extends AbstractSparkyUser implements SparkyUser {

    @Nonnull
    public static final UserRealm DEFAULT_REALM = UserRealm.LOCAL;
    
    @Nonnull
    public static final UserRealm ASSOCIATED_REALM = UserRealm.LOCAL;
    public static final String DEFAULT_ALGO = BCryptPasswordEncoder.class.getSimpleName().toLowerCase();
    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private Password passwordEntity;
    private @Nullable  PasswordEncoder encoder;

    /**
     * Default constructor only used for testing purposes.
     */
    @Deprecated
    LocalUserDetails() {
        this("user", null, false, UserRole.DEFAULT);
    }

    /**
     * Creates a LocalUserDetails with fields from an JPA user.
     * 
     * @param jpaUser
     */
    LocalUserDetails(User jpaUser) {
        this(Identity.of(jpaUser), null, jpaUser.isActive(), jpaUser.getRole());
        var pwEntity = jpaUser.getPasswordEntity();
        if (pwEntity == null) {
            throw new IllegalArgumentException("Passwords are mandatory for local users");
        }
        passwordEntity = pwEntity;
        databaseId = jpaUser.getId();
        jpaUser.getExpirationDate().ifPresent(this::setExpireDate);
        this.setSettings(jpaUser.getProfileConfiguration());
        this.setFullname(jpaUser.getFullName());
    }

    /**
     * Create a new instance with values from an DTO. 
     * 
     * @param dto - Username, Role, passwordDto can't be null otherwise an exception is thrown.
     */
    @SuppressWarnings("null")
    LocalUserDetails(UserDto dto) {
        super(Identity.of(dto.username), dto.role);
        expirationDate = Optional.ofNullable(dto.expirationDate);
        encodeAndSetPassword(dto.passwordDto.newPassword);
        fullname = dto.fullName;
        role = dto.role;
        settings = new PersonalSettings(dto.settings);
    }
    /**
     * Constructor with necessary fields.
     * 
     * @param nickname The nickname without realm information
     * @param passwordEntity
     * @param isActive
     * @param role
     */
    LocalUserDetails(String nickname, @Nullable Password passwordEntity, boolean isActive, UserRole role) {
        this(new Identity(nickname, ASSOCIATED_REALM), passwordEntity, isActive, role);
        this.passwordEntity = passwordEntity;
        log.trace("New LocalUserDetails created: {}", getUsername());
    }
    
    
    /**
     * Creates an user with necessary fields. It preserves the old identity. 
     * 
     * @param ident
     * @param role
     * @param isActive
     * @param passwordEntity
     */
    private LocalUserDetails(Identity ident, @Nullable Password passwordEntity, boolean isActive, UserRole role) {
        super(ident, role);
        setEnabled(isActive);
        if (ident.realm() != ASSOCIATED_REALM) {
            log.debug("Preserving the identity of one user {}", ident.asUsername());
        }
    }

    /**
     * Creates a new user in the {@link LocalUserDetails#DEFAULT_REALM} and encodes the password. <br>
     * This type should only be used for users which aren't use any other authentication methods (realms) than a
     * {@link UserDetailsService}. Default expiration time are 6 month.
     * 
     * @param nickname The username without realm information
     * @param rawPassword
     *                    - Plain text password (will be hashed)
     * @param role
     *                    - The users permission role
     * @return An instance of SparkyUser in the local realm.
     */
    public static @Nonnull LocalUserDetails newLocalUser(String nickname, String rawPassword, UserRole role) {
        var newUser = new LocalUserDetails(nickname, null, true, role);
        newUser.encodeAndSetPassword(rawPassword);
        newUser.setExpireDate(LocalDate.now().plusMonths(6));
        return newUser;
    }

    /**
     * Encodes a raw string to bcrypt hash sets the local password entity.
     * 
     * @param rawPassword
     * @return the hashed value - never null but may be empty
     */
    public Password encodeAndSetPassword(String rawPassword) {
        final Password passwordEntity = new Password(encode(rawPassword), DEFAULT_ALGO);
        this.passwordEntity = passwordEntity;
        return passwordEntity;
    }

    /**
     * Encodes a string to hashed password using the default algorithm.
     * 
     * @param rawPassword
     *                    - Plain password as string
     * @return Encoded password
     */
    private @Nonnull String encode(String rawPassword) {
        final String encodedPass = getEncoder().encode(rawPassword);
        final String nonNullPass = Optional.ofNullable(encodedPass).orElseThrow(
            () -> new RuntimeException("BCryptPasswordEncoder returned null - this should not happen"));
        return notNull(nonNullPass);
    }

    /**
     * Returns the (probably encoded password) of the user. For more information use {@link #getPasswordEntity()}.
     * @return password of the user
     */
    @Override
    public @Nonnull String getPassword() {
        return getPasswordEntity().getPasswordString();
    }

    /**
     * Returns springs default {@link BCryptPasswordEncoder}.
     * 
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

    @Override
    public boolean isAccountNonLocked() {
        return isEnabled();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @Nonnull
    public User getJpa() {
        var jpaUser = new User(
            ident.nickname(), ident.realm(), isEnabled(), getRole()
        );
        jpaUser.setPasswordEntity(new Password(getPassword()));
        jpaUser.setProfileConfiguration(new PersonalSettings(getSettings()));
        jpaUser.setExpirationDate(getExpireDate());
        jpaUser.setId(super.databaseId);
        jpaUser.setFullName(fullname);
        return jpaUser;
    }

    @Override
    public void updatePassword(ChangePasswordDto dto, UserRole role) {
        final String newPw = dto.newPassword;
        if (newPw != null) {
            switch(role) {
            case ADMIN:
                encodeAndSetPassword(newPw);
                break;
            case SERVICE:
                //nothing - not allowed to change own password
                break;
            case DEFAULT:
            default:
                defaultUpdatePassword(dto.oldPassword, newPw);
                break; 
            }
        }
    }

    /**
     * Updates the password with default permissions. The oldPassword must match with the stored one as plain text.
     * 
     * @param oldPassword
     * @param newPassword
     */
    public void defaultUpdatePassword(@Nullable String oldPassword, String newPassword) {
        if (getEncoder().matches(oldPassword, getPassword())) {
            encodeAndSetPassword(newPassword);
        } else {
            log.debug("Password for user {} not changed because they old password didn't matched.", getUsername());
        }
    }

    /**
     * Returns the password entity. 
     * 
     * @return Immutable password entity
     */
    public @Nonnull Password getPasswordEntity() {
        Password passwordEntityLocal = passwordEntity;
        if (passwordEntityLocal == null) {
            log.error("User is in local realm but doesn't have a password.");
            throw new RuntimeException(
                    "User has no password even though he is in the local realm. This shouldn't be happen.");
        }
        return passwordEntityLocal;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof LocalUserDetails)) {
            return false;
        }
        LocalUserDetails other = (LocalUserDetails) obj;
        return Objects.equals(encoder, other.encoder) && Objects.equals(passwordEntity, other.passwordEntity);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(encoder, passwordEntity);
        return result;
    }
}
