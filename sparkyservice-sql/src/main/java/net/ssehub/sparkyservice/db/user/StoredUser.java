package net.ssehub.sparkyservice.db.user;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sun.istack.Nullable;

import net.ssehub.sparkyservice.db.hibernate.AnnotatedClass;

@Entity
@Table(
    name = "user_stored", 
    uniqueConstraints = {@UniqueConstraint(columnNames = {"userName", "realm"})})
public class StoredUser implements AnnotatedClass {

    /**
     * Unique identifier (primary key) for local user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected int id;

    /**
     * Unique (per REALM!) username which identifies local users. 
     * Max length for a username is 50. This could be a username from 
     * external auth methods or a local one. 
     */
    @Column(nullable = false, length = 50)
    protected String userName;

    @Column
    protected boolean isActive;
    
    @OneToOne(cascade = {CascadeType.ALL})
    protected Password passwordEntity;

    /**
     * The authentication realm of the user. 
     */
    @Column(length = 50)
    protected String realm;
    
    @Column(length = 50)
    protected String role;
    
    @OneToOne(cascade = {CascadeType.ALL}, targetEntity = net.ssehub.sparkyservice.db.user.PersonalSettings.class)
    protected PersonalSettings profileConfiguration;

    public StoredUser() {}
        
    public StoredUser(@Nonnull String userName, 
            Password passwordEntity, 
            @Nonnull String realm, 
            boolean isActive, 
            @Nonnull String role) {
        this.userName = userName;
        this.passwordEntity = passwordEntity;
        this.realm = realm;
        this.isActive = isActive;
        this.role = role;
        this.profileConfiguration = new PersonalSettings();
    }

    public StoredUser(StoredUser user) {
        this.id = user.id;
        this.realm = user.realm;
        this.userName = user.userName;
        this.isActive = user.isActive;
        this.passwordEntity = user.passwordEntity;
        this.profileConfiguration = user.profileConfiguration;
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(@Nonnull String userName) {
        this.userName = userName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Nullable
    public Password getPasswordEntity() {
        return passwordEntity;
    }

    public void setPasswordEntity(@Nonnull Password password) {
        this.passwordEntity = password;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
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

    public void setProfileConfiguration(PersonalSettings profileConfiguration) {
        this.profileConfiguration = profileConfiguration;
    }
}
