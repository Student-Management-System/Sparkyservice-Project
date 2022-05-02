package net.ssehub.sparkyservice.api.auth.ldap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.ssehub.sparkyservice.api.auth.identity.SparkyUserFactory;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;
import net.ssehub.sparkyservice.api.persistence.jpa.user.Password;
import net.ssehub.sparkyservice.api.persistence.jpa.user.User;
import net.ssehub.sparkyservice.api.useraccess.UserRole;
import net.ssehub.sparkyservice.api.useraccess.dto.UserDto;

@ParametersAreNonnullByDefault
class LdapUserFactory implements SparkyUserFactory<LdapUser> {
    @Nonnull
    private final UserRealm associatedRealm;

    LdapUserFactory(UserRealm associatedRealm) {
        super();
        this.associatedRealm = associatedRealm;
    }

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

    @Nonnull
    @Override
    public LdapUser create(@Nullable String nickname, @Nullable Password password, @Nullable UserRole role,
        boolean isEnabled) {
        if (nickname == null || role == null) {
            throw new IllegalArgumentException("Username and role are mandatory");
        }
        return new LdapUser(nickname, associatedRealm, role, isEnabled);
    }
}