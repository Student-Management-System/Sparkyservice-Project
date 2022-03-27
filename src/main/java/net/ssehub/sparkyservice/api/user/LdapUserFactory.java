package net.ssehub.sparkyservice.api.user;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.security.authentication.AuthenticationManager;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Provides Factory methods for {@link LdapUser}. 
 * 
 * @author marcel
 */
@ParametersAreNonnullByDefault
public final class LdapUserFactory implements SparkyUserFactory<LdapUser> {

    @Override
    @Nonnull
    public LdapUser create(UserDto dto) {
        return create(dto.username, null, dto.role, false /* TODO */);
    }

    @Override
    @Nonnull
    public LdapUser create(User jpaUser) {
        return new LdapUser(jpaUser);
    }

    /**
     * Implementation of {@link SparkyUser} for {@link UserRealm#UNIHI}. 
     * Specials:<br>
     * 
     * <ul><li>  Doesn't contains a password and can't change a password
     * </li><li>  Provides a DN which can used by an {@link AuthenticationManager}
     * </li></ul>
     * 
     * @param nickname - nickname of the user (not the username!)
     * @param password - Is ignored
     * @param role - Mandatory
     * @param isEnabled Account is disabled when false
     * @return New instance of LDAP user
     */
    @Nonnull
    @Override
    public LdapUser create(@Nullable String nickname, @Nullable Password password, @Nullable UserRole role,
            boolean isEnabled) {
        if (nickname == null || role == null) {
            throw new IllegalArgumentException("Username and role are mandatory");
        }
        return new LdapUser(nickname, role, isEnabled);
    }
}
