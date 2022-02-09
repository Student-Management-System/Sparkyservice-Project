package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.ssehub.sparkyservice.api.auth.Identity;
import net.ssehub.sparkyservice.api.jpa.token.JpaJwtToken;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

@ParametersAreNonnullByDefault
public class JwtToken {

    private int remainingRefreshes;
    private boolean locked;
    @Nullable
    private Date expirationDate;
    @Nonnull
    private String subject;
    @Nonnull
    private Collection<UserRole> tokenPermissionRoles;
    @Nonnull
    private UUID jti;

    public JwtToken(final UUID jti, final Date expirationDate, final String subject,
            UserRole permission) {
        this(jti, expirationDate, subject, notNull(Arrays.asList(permission)));
    }

    public JwtToken(final UUID jit, final Date expirationDate, final String subject, 
            Collection<UserRole> permissionRoles) {
        super();
        this.expirationDate = expirationDate;
        this.subject = subject; 
        this.jti = jit;
        remainingRefreshes = 0;
        locked = false;
        tokenPermissionRoles = permissionRoles;
    }

    /**
     * Copy constructor.
     * @param copyMe
     */
    public JwtToken(JwtToken copyMe) {
        super();
        this.remainingRefreshes = copyMe.remainingRefreshes;
        this.locked = copyMe.locked;
        this.expirationDate = copyMe.expirationDate;
        this.subject = copyMe.subject;
        this.tokenPermissionRoles = copyMe.tokenPermissionRoles;
        this.jti = copyMe.jti;
    }

    public JwtToken(final JpaJwtToken jpaTokenObj) {
        super();
        this.subject = new Identity(jpaTokenObj.getUser().getNickname(), jpaTokenObj.getUser().getRealm()).asUsername();
        this.remainingRefreshes = jpaTokenObj.getRemainingRefreshes();
        this.locked = jpaTokenObj.isLocked();
        this.jti = notNull(
            UUID.fromString(jpaTokenObj.getJti())
        );
        var role = jpaTokenObj.getUser().getRole();
        this.tokenPermissionRoles = notNull(Arrays.asList(role)); // does not represents the actual value in the token
    }

    public JpaJwtToken getJpa(UserStorageService service) throws UserNotFoundException {
        var user = service.findUser(subject);
        return new JpaJwtToken(notNull(jti.toString()), remainingRefreshes, locked, user.getJpa());
    }

    public JpaJwtToken getJpa(User user) {
        return new JpaJwtToken(notNull(jti.toString()), remainingRefreshes, locked, user);
    }

    public int getRemainingRefreshes() {
        return remainingRefreshes;
    }

    public void setRemainingRefreshes(int remainingRefreshes) {
        this.remainingRefreshes = remainingRefreshes;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Nullable
    public java.util.Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(java.util.Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getSubject() {
        return subject;
    }

    public Collection<UserRole> getTokenPermissionRoles() {
        return tokenPermissionRoles;
    }

    public void setTokenPermissionRoles(Collection<UserRole> tokenPermissionRoles) {
        this.tokenPermissionRoles = tokenPermissionRoles;
    }

    public void setTokenPermissionRoles(UserRole tokenPermissionRole) {
        this.tokenPermissionRoles = notNull(Arrays.asList(tokenPermissionRole));
    }

    @Nonnull
    public UUID getJti() {
        return jti;
    }

    public void setJti(UUID jti) {
        this.jti = jti;
    }

    @Override
    public String toString() {
        return "JwtToken [remainingRefreshes=" + remainingRefreshes + ", locked=" + locked + ", expirationDate="
            + expirationDate + ", userInfo=" + subject + ", tokenPermissionRoles=" + tokenPermissionRoles
            + ", jti=" + jti + "]";
    }

    public JwtToken copy() {
        return new JwtToken(this);
    }
}