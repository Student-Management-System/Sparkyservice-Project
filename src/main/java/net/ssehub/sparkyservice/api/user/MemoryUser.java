package net.ssehub.sparkyservice.api.user;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;
import net.ssehub.sparkyservice.api.user.storage.NoTransactionUnitException;

/**
 * User implementation of a Memory user. Those user only "live" in the process memory and can't create 
 * JPA objects for database operations. 
 *
 * @author marcel
 */
public class MemoryUser extends AbstractSparkyUser implements SparkyUser {

    public static final UserRealm ASSOCIATED_REALM = UserRealm.MEMORY;
    private static final long serialVersionUID = 2606418064897651578L;
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Nonnull
    private Password password;

    /**
     * A memory user. Password can't be changed again!
     * 
     * @param nickname The username without realm information
     * @param password
     * @param role
     */
    public MemoryUser(@Nonnull String nickname, @Nonnull Password password, @Nonnull UserRole role) {
        super(new Identity(nickname, UserRealm.MEMORY), role);
        this.password = password;
    }

    @Override
    public boolean isEnabled() {
        return true; // always enabled;
    }

    @Override
    public String getPassword() {
        return password.getPasswordString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // memory user are always enabled
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @Nonnull
    public User getJpa() {
        throw new NoTransactionUnitException(MemoryUser.class.getName() + " can't produce a JPA User instance");
    }

    @Override
    public void updatePassword(@Nonnull ChangePasswordDto passwordDto, @Nonnull UserRole role) {
        // nothing
        log.debug("Skipped attempt to change password on memory user");
    }

    @Override
    public boolean equals(Object object) {
        return Optional.ofNullable(object)
            .flatMap(obj -> super.equalsCheck(obj, this))
            .map(user -> user.password)
            .filter(password::equals)
            .isPresent();
    }

    @Override
    public int hashCode() {
        return super.getHashCodeBuilder().append(password).toHashCode();
    }
}
