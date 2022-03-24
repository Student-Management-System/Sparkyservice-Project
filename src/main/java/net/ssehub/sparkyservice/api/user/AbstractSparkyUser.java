package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Contains a set of method which probably all implementation of {@link SparkyUser} shares.
 * 
 * @author marcel
 */
abstract class AbstractSparkyUser implements SparkyUser {

    private static final long serialVersionUID = 6445421926583879819L;

    protected int databaseId;

    @Nullable
    protected String fullname;
    
    @Nonnull
    protected final Identity ident;
    
    protected boolean isEnabled;
    
    @Nonnull
    protected UserRole role;
    @Nonnull
    protected Optional<LocalDate> expirationDate = notNull(Optional.ofNullable(null));
    @Nullable
    protected PersonalSettings settings;

    /**
     * Creates a new Abstract user with mandatory information.
     * 
     * @param ident The identity of user
     * @param role
     */
    public AbstractSparkyUser(@Nonnull final Identity ident, @Nonnull final UserRole role) {
        this.ident = ident;
        this.role = role;
    }
    
    @Override
    @Nonnull
    public UserRole getRole() {
        return this.role;
    }

    @Override
    public void setRole(UserRole role) {
        if (role != null) {
            this.role = role;
        }
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    @Nonnull
    public Optional<LocalDate> getExpireDate() {
        return expirationDate;
    }

    @Override
    public void setExpireDate(@Nullable LocalDate expireDate) {
        this.expirationDate = notNull(Optional.ofNullable(expireDate));
    }
    
    /**
     * {@link #setExpireDate(LocalDate)}.
     * @param expireDate
     */
    public void setExpireDate(@Nonnull Optional<LocalDate> expireDate) {
        this.expirationDate = expireDate;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    @Nonnull
    public String getUsername() {
        return ident.asUsername();
    }

//    @Override
//    public void setUsername(String username) {
////        if (username != null) {
////            username = username.trim();
////            username = username.toLowerCase();
////            this.username = notNull(username);
////        }
//        // TODO 
//        throw new UnsupportedOperationException();
//    }

    @Override
    @Nonnull
    public PersonalSettings getSettings() {
        var settings2 = settings;
        if (settings2 == null) {
            settings2 = new PersonalSettings();
            settings = settings2;
        }
        return settings2;
    }

    /**
     * Sets new personal settings to the user.
     * 
     * @param settings
     */
    void setSettings(PersonalSettings settings) {
        this.settings = settings;
    }

    @Override
    public String getFullname() {
        return fullname;
    }

    @Override
    public void setFullname(@Nullable String fullname) {
        this.fullname = fullname;
    }

    @Override
    @Nonnull
    public UserDto ownDto() {
        return this.getRole().getPermissionTool().asDto(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractSparkyUser)) {
            return false;
        }
        AbstractSparkyUser other = (AbstractSparkyUser) obj;
        return databaseId == other.databaseId && Objects.equals(expirationDate, other.expirationDate)
                && Objects.equals(fullname, other.fullname) && Objects.equals(ident, other.ident)
                && isEnabled == other.isEnabled && role == other.role && Objects.equals(settings, other.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(databaseId, expirationDate, fullname, ident, isEnabled, role, settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public @Nonnull Identity getIdentity() {
        return this.ident;
    }
}
