package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.jpa.token.JpaJwtToken;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

@ParametersAreNonnullByDefault
public class JwtToken {

    private int remainingRefreshes;
    private boolean locked;
    @Nullable
    private Date expirationDate;
    @Nonnull
    private SparkysAuthPrincipal userInfo;
    @Nonnull
    private Collection<UserRole> tokenPermissionRoles;
    @Nonnull
    private UUID jti;

    public JwtToken(final UUID jti, final Date expirationDate, final SparkysAuthPrincipal userInfo,
            UserRole permission) {
        this(jti, expirationDate, userInfo, notNull(Arrays.asList(permission)));
    }

    public JwtToken(final UUID jit, final Date expirationDate, final SparkysAuthPrincipal userInfo, 
            Collection<UserRole> permissionRoles) {
        super();
        this.expirationDate = expirationDate;
        this.userInfo = userInfo;
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
        this.userInfo = copyMe.userInfo;
        this.tokenPermissionRoles = copyMe.tokenPermissionRoles;
        this.jti = copyMe.jti;
    }

    public JwtToken(final JpaJwtToken jpaTokenObj) {
        super();
        this.userInfo = new SparkysAuthPrincipal() {
            
            @Override
            @Nonnull
            public UserRealm getRealm() {
                return jpaTokenObj.getUser().getRealm();
            }
            
            @Override
            @Nonnull
            public String getName() {
                return jpaTokenObj.getUser().getUserName();
            }

            @Override
            @Nonnull
            public String asString() {
                return jpaTokenObj.getUser().getUserName() + "@" + jpaTokenObj.getUser().getRealm();
            }
        };
        this.remainingRefreshes = jpaTokenObj.getRemainingRefreshes();
        this.locked = jpaTokenObj.isLocked();
        this.jti = notNull(
            UUID.fromString(jpaTokenObj.getJti())
        );
        var role = jpaTokenObj.getUser().getRole();
        this.tokenPermissionRoles = notNull(Arrays.asList(role)); // does not represents the actual value in the token
    }

    public JpaJwtToken getJpa(UserStorageService service) throws UserNotFoundException {
        var user = service.findUserByNameAndRealm(userInfo.getName(), userInfo.getRealm());
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

    public SparkysAuthPrincipal getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(SparkysAuthPrincipal userInfo) {
        this.userInfo = userInfo;
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
            + expirationDate + ", userInfo=" + userInfo + ", tokenPermissionRoles=" + tokenPermissionRoles
            + ", jti=" + jti + "]";
    }

    public JwtToken copy() {
        return new JwtToken(this);
    }
}