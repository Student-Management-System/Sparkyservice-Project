package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.builder.HashCodeBuilder;

import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;

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
    protected String username;
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
     * @param username
     * @param role
     */
    public AbstractSparkyUser(@Nonnull String username, @Nonnull UserRole role) {
        this.username = username;
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

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    @Nonnull
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        if (username != null) {
            this.username = username;
        }
    }

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

    /**
     * Generated equals method.
     * 
     * @param obj
     * @return <code>true</code> when fields of this abstract class are equal
     */
    @SuppressWarnings("null")
    private boolean isEquals(Object obj) {
        var localSettings = settings;
        var localFullname = fullname;
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractSparkyUser other = (AbstractSparkyUser) obj;
        if (databaseId != other.databaseId) {
            return false;
        }
        if (!expirationDate.equals(other.expirationDate)) {
            return false;
        }
        if (localFullname == null) {
            if (other.fullname != null) {
                return false;
            }
        } else if (!localFullname.equals(other.fullname)) {
            return false;
        }
        if (isEnabled != other.isEnabled) {
            return false;
        }
        if (role != other.role) {
            return false;
        }
        if (localSettings == null) {
            if (other.settings != null) {
                return false;
            }
        } else if (!localSettings.equals(other.settings)) {
            return false;
        }
        if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the given object in manner of this (abstract) class. When the returned optional has a value, 
     * it is safe to assume the object is an instance of this type. 
     * Only check the special cases of the implementation afterwards.
     * <br>
     * Example usage: (just an example - {@link #settings} are already checked by this method <br>
     * <code>
     * return Optional.ofNullable(object) <br>
     *     .flatMap(obj -> super.equalsCheck(obj, this))<br>
     *     .map(user -> user.getSettings())<br>
     *     .filter(Objects::nonNull)<br>
     *     .filter(notNull(settings)::equals)<br>
     *     .isPresent();
     * </code>
     * @param <T>
     * @param object - Check target
     * @param classInstance - Is casted to this instance
     * @return Optional instance of the class given class - empty optional when the object is not an instance 
     * nor equals.
     */
    @SuppressWarnings("unchecked")
    <T extends AbstractSparkyUser> Optional<T> equalsCheck(@Nullable Object object, @Nonnull T classInstance) {
        return (Optional<T>) Optional.ofNullable(object)
                .filter(classInstance.getClass()::isInstance)
                .map(classInstance.getClass()::cast)
                .filter(this::isEquals);
    }

    /**
     * Hashcodebuilder for {@link SparkyUser}. This builder contains all fields used by this abstract class.
     * 
     * @return HashCodeBuilder which already has all fields of this abstract class appended
     */
    HashCodeBuilder getHashCodeBuilder() {
        return new HashCodeBuilder(17, 37)
            .append(this.databaseId)
            .append(this.isEnabled)
            .append(this.expirationDate)
            .append(this.username)
            .append(this.fullname)
            .append(this.settings);
    }

    /**
     * {@inheritDoc}
     * <br>
     * It is recommended to make use of {@link #equalsCheck(Object, AbstractSparkyUser)}.
     */
    public abstract boolean equals(Object object);

    /**
     * {@inheritDoc}
     * <br> 
     * It is recommended to make use of {@link #getHashCodeBuilder()} to get a hash of all fields used by this abstract
     * implementation.
     */
    public abstract int hashCode();
}
