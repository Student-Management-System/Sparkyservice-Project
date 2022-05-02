package net.ssehub.sparkyservice.api.auth.memory;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ssehub.sparkyservice.api.auth.identity.AbstractSparkyUser;
import net.ssehub.sparkyservice.api.auth.identity.Identity;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;
import net.ssehub.sparkyservice.api.persistence.NoTransactionUnitException;
import net.ssehub.sparkyservice.api.persistence.jpa.user.Password;
import net.ssehub.sparkyservice.api.persistence.jpa.user.User;
import net.ssehub.sparkyservice.api.useraccess.UserRole;
import net.ssehub.sparkyservice.api.useraccess.dto.UserDto.ChangePasswordDto;

/**
 * User implementation of a Memory user. Those user only "live" in the process memory and can't create JPA objects for
 * database operations.
 *
 * @author marcel
 */
// public for test cases
public class MemoryUser extends AbstractSparkyUser {

    private static final long serialVersionUID = 2606418064897651578L;
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Nullable
    private Password password;

    /**
     * A memory user. Password can't be changed again!
     * 
     * @param nickname The username without realm information
     * @param password
     * @param role
     */
    public MemoryUser(@Nonnull String nickname, @Nonnull UserRealm realm, @Nullable Password password,
        @Nonnull UserRole role) {
        super(new Identity(nickname, realm), role);
        this.password = password;
    }

    @Override
    public boolean isEnabled() {
        return true; // always enabled;
    }

    @Override
    @Nullable
    public String getPassword() {
        final Password password = this.password;
        String pwString;
        if (password == null) {
            pwString = null;
        } else {
            pwString = password.getPasswordString();
        }
        return pwString;
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof MemoryUser)) {
            return false;
        }
        MemoryUser other = (MemoryUser) obj;
        return Objects.equals(password, other.password);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(password);
        return result;
    }
}
