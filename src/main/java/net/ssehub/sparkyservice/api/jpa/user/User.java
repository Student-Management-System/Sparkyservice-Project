package net.ssehub.sparkyservice.api.jpa.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;
import net.ssehub.sparkyservice.api.util.SparkyUtil;

/**
 * Represents a user with JPA annotations. Although contains static methods to modify a users values.
 *
 * @author marcel
 */

@Entity
@Table(name = "user_stored", uniqueConstraints = { @UniqueConstraint(columnNames = { "userName", "realm" }) })
@ParametersAreNonnullByDefault
public class User {

    public static final int TOKEN_EXPIRE_TIME_MS = 86_400_000; // 24 hours

    /**
     * Returns a date where a JWT token of user should expire. 
     * 
     * @param user
     * @return Date where the validity of a JWT token should end for the given user
     */
    public @Nonnull static java.util.Date getJwtExpirationDate(User user) {
        @Nonnull java.util.Date expirationDate;
        @Nonnull Supplier<LocalDate> defaultServiceExpirationDate = () -> LocalDate.now().plusYears(10);
        
        if (user.getRole() == UserRole.SERVICE) {
            expirationDate = notNull(
                 user.getExpirationDate()
                     .map(SparkyUtil::toJavaUtilDate)
                     .orElseGet(() -> SparkyUtil.toJavaUtilDate(defaultServiceExpirationDate.get()))
            );
        } else {
            expirationDate = new java.util.Date(System.currentTimeMillis() + TOKEN_EXPIRE_TIME_MS);
        }
        return expirationDate;
    }

    /**
     * Changes the values of the given user with values from the DTO. This happens recursive (it will
     * change {@link SettingsDto} and {@link ChangePasswordDto} as well). Does not support changing the realm.<br><br>
     * 
     * This is done with "limit permissions". Which means some fields which should only be changed with higher 
     * permissions won't be changed and are skipped. Changing the password is only supported if the user is in 
     * {@link LocalUserDetails#DEFAULT_REALM}.
     * 
     * Those permissions are:
     * <ul><li> The old password must be provided in order to change it
     * </li><li> User can not modify roles: {@link User#setRole(Enum)}
     * </li></ul>
     * 
     * @param databaseUser User which values should be changed
     * @param userDto Transfer object which holds the new data
     */
    public static void defaultUserDtoEdit(User databaseUser, UserDto userDto ) {
        editUserFromDto(databaseUser, userDto, false);
    }

    /**
     * Changes the values of the given user with values from the DTO. This happens recursive (it will
     * change {@link SettingsDto} and {@link ChangePasswordDto} as well). Does not support changing the realm.<br><br>
     * 
     * Any other data will be modified. Changing the password is only supported if the user is in 
     * {@link LocalUserDetails#DEFAULT_REALM}.
     * 
     * @param databaseUser User which values should be changed
     * @param userDto Holds the new data
     */
    public static void adminUserDtoEdit(User databaseUser, UserDto userDto) {
        editUserFromDto(databaseUser, userDto, true);
    }

    /**
     * Edit values of a given user with values from a DTO. Thes can be done in two modes: <br>
     * <ul><li>admin mode: Will modify all fields without edit conditions (like the old password have to match) 
     * </li><li> default (user) mode: The edit is done under certain restrictions: Password is only changed if 
     * the old one is provided. 
     * </li></ul>
     * 
     * @param databaseUser
     * @param userDto
     * @param adminMode
     */
    private static void editUserFromDto(User databaseUser, UserDto userDto, boolean adminMode) {
        if (userDto.settings != null && userDto.username != null) {
            PersonalSettings.applyPersonalSettingsDto(databaseUser, notNull(userDto.settings));
            databaseUser.setUserName(notNull(userDto.username));
            boolean changePassword = userDto.passwordDto != null 
                    && databaseUser.getRealm() == LocalUserDetails.DEFAULT_REALM;
            boolean adminPassChange = adminMode && changePassword && userDto.passwordDto.newPassword != null;
            if (adminPassChange) {
                adminApplyNewPasswordFromDto(databaseUser, userDto.passwordDto.newPassword);
            } else if (changePassword) {
                defaultApplyNewPasswordFromDto(databaseUser, userDto.passwordDto);
            }
            if (adminMode) {    
                databaseUser.setExpirationDate(userDto.expirationDate);
                databaseUser.setFullName(userDto.fullName);
                databaseUser.setRole(userDto.role);
                databaseUser.getProfileConfiguration().setPayload(userDto.settings.payload);
            }
        }
    }

    /**
     * Try to apply a new password to the given user. The {@link ChangePasswordDto#oldPassword} must
     * match the one which is already stored in the database. Otherwise the password won't be changed.
     * 
     * @param databaseUser user who's password should be changed
     * @param passwordDto contains old and new password (both values can be null)
     */
    public static void defaultApplyNewPasswordFromDto(@Nullable User databaseUser,
                                                      @Nullable UserDto.ChangePasswordDto passwordDto) {
        if (passwordDto != null && databaseUser != null) {
            @Nullable String newPassword = passwordDto.newPassword;
            @Nullable String oldPassword = passwordDto.oldPassword;
            if (newPassword != null && oldPassword != null) {
                applyPasswordFromDto(databaseUser, newPassword, oldPassword, false);
            }
        }
    }

    /**
     * Apply a new password to the given user. 
     * 
     * @param databaseUser User where the password should be changed
     * @param newPassword New raw password
     */
    public static void adminApplyNewPasswordFromDto(@Nullable User databaseUser, @Nullable String newPassword) {
        if (databaseUser != null && newPassword != null) {
            applyPasswordFromDto(databaseUser, newPassword, "", true);
        }
    }

    /**
     * Changes the password of a given use with the given password DTO. <br>
     * In case of an admin mode, only a new password must be provided in order to succeed.
     * 
     * @param databaseUser User to edit
     * @param newPassword New raw password (necessary)
     * @param oldPassword The old hashed password (only on non adminEdits necessary)
     * @param adminEdit Decide if the old password must match with the new one
     */
    private static void applyPasswordFromDto(User databaseUser, String newPassword, String oldPassword, 
            boolean adminEdit) {
        if (!newPassword.isBlank()) {
            LocalUserDetails localUser;
            if (databaseUser instanceof LocalUserDetails) {
                localUser = (LocalUserDetails) databaseUser;
                if (adminEdit || localUser.getEncoder().matches(oldPassword, localUser.getPassword())) {
                    localUser.encodeAndSetPassword(newPassword);
                } 
            } else if (databaseUser.getRealm() == UserRealm.LOCAL){
                localUser = new LocalUserDetails(databaseUser);
                localUser.encodeAndSetPassword(newPassword);
                databaseUser.setPasswordEntity(localUser.getPasswordEntity()); // make pass by reference possible.
            }
        }
    }

    public static UserDto userAsDto(User user) {
        var dto = new UserDto();
        dto.realm = user.getRealm();
        dto.role = user.getRole();
        dto.settings = user.getProfileConfiguration().asDto();
        dto.username = user.getUserName();
        dto.fullName = user.fullName;
        dto.expirationDate = user.expirationTime;
        return dto;
    }

    /**
     * Unique identifier (primary key) for local user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected int id;

    @Nonnull
    @Column(nullable = false, length = 50)
    protected String userName;

    @Nullable
    @Column(length = 255)
    protected String fullName;

    @Column
    protected boolean isActive;

    @OneToOne(cascade = { CascadeType.ALL })
    @Nullable
    protected Password passwordEntity;

    @Nonnull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    protected UserRealm realm;

    @Nonnull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    protected UserRole role;

    @OneToOne(cascade = {
            CascadeType.ALL }, targetEntity = net.ssehub.sparkyservice.api.jpa.user.PersonalSettings.class)
    protected PersonalSettings profileConfiguration;

    @Nullable
    @Column
    protected LocalDate expirationTime; //only used in LOCAL realm

    /**
     * Default constructor used by hibernate.
     */
    @SuppressWarnings("unused")
    private User() {
        role = UserRole.DEFAULT;
        userName = "";
        realm = UserRealm.UNKNOWN;
    }

    public User(String userName, @Nullable Password passwordEntity, UserRealm realm, boolean isActive, UserRole role) {
        this.userName = userName;
        this.passwordEntity = passwordEntity;
        this.realm = realm;
        this.isActive = isActive;
        this.role = role;
        this.profileConfiguration = new PersonalSettings();
    }

    public User(final User user) {
        this.id = user.id;
        this.realm = user.realm;
        this.role = user.role;
        this.userName = user.userName;
        this.isActive = user.isActive;
        this.passwordEntity = user.passwordEntity;
        this.fullName = user.fullName;
        this.profileConfiguration = user.profileConfiguration;
        this.expirationTime = user.expirationTime;
    }

    /**
     * Users database ID for unique identification of that entry. 
     * 
     * @return Primary key
     */
    public int getId() {
        return id;
    }

    /**
     * Users database ID for unqiue identification of that entry. 
     * Typically set through OR mapper.
     * 
     * @param id Primary key
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Is unique per realm and is never null or empty. It can be used as identifier
     * in combination with the realm.
     * 
     * @return name of the user which is unique per realm
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Full name of the user. Can be null and is may not be unique.
     * @return First + Last Name of the user
     */
    @Nullable
    public String getFullName() {
        return fullName;
    }

    /**
     * Setter for the full name of the user. Should contain first and last name.
     * @param fullName string
     */
    public void setFullName(@Nullable String fullName) {
        this.fullName = fullName;
    }

    /**
     * Overrides the old username - it have to pe unique per realm and max length
     * 50.
     * 
     * @param userName unique string per realm
     */
    public void setUserName(String userName) {
        if (userName.length() > 50) {
            throw new IllegalArgumentException("The max length for a username is 50");
        }
        this.userName = userName;
    }

    /**
     * Indicator if the user is currently enabled. When set to false, the is user is not able to authenticate 
     * himself anymore.
     * 
     * @return - Boolean if the user is enabled or not. 
     */
    public boolean isActive() {
        return isActive;
    }

    
    /**
     * Indicator if the user is currently enabled. When set to false, the is user is not able to authenticate 
     * himself anymore.
     * 
     * @param isActive
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Nullable
    public Password getPasswordEntity() {
        return passwordEntity;
    }

    public void setPasswordEntity(@Nullable Password password) {
        this.passwordEntity = password;
    }

    /**
     * Sets a single authority role to the user. Old role will be overridden. Null values will be ignored
     * 
     * @param role Users permission role
     */
    public void setRole(@Nullable UserRole role) {
        if (role != null) {
            this.role = role;
        }
    }

    /**
     * {@link PersonalSettings} of the user where extra settings are stored like
     * email addressees.
     * 
     * @return associated profile of this StoredUser - if no exists, a new will be
     *         generated
     */
    @Nonnull
    public PersonalSettings getProfileConfiguration() {
        final var profileConfiguration2 = profileConfiguration;
        if (profileConfiguration2 != null) {
            return profileConfiguration2;
        } else {
            final var conf = new PersonalSettings();
            profileConfiguration = conf;
            return conf;
        }
    }
    
    public Optional<LocalDate> getExpirationDate() {
        return Optional.ofNullable(expirationTime);
    }

    public void setExpirationDate(@Nullable LocalDate expirationTime) {
        this.expirationTime = expirationTime;
    }

    public void setProfileConfiguration(@Nullable PersonalSettings profileConfiguration) {
        this.profileConfiguration = profileConfiguration;
    }

    public UserRealm getRealm() {
        return realm;
    }

    public void setRealm(UserRealm realm) {
        this.realm = realm;
    }

    public UserRole getRole() {
        return role;
    }

    public UserDto asDto() {
        return User.userAsDto(this);
    }
}
