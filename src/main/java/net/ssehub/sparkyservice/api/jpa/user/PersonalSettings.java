package net.ssehub.sparkyservice.api.jpa.user;

import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.apache.commons.lang.builder.HashCodeBuilder;

import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;

/**
 * JPA representation of settings. 
 * In the current state of this application, this object is always used as generel representation of settings in
 * the whole project because it does not contain any logic. 
 * Disadvantage of this is, that it is hard to keep in sync with the database. Instances of this object must be bound to 
 * a {@link User}. When saving an instance to a storage, do not use it outside of the bounded user object (for example
 * in {@link SparkyUser}). 
 * <br><br>
 * Strictly separate the use cases and create a new instance for each of them.
 * 
 * @author marcel
 */
@Entity
@Table(name = "user_configuration")
public class PersonalSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int configurationId;

    @OneToOne
    @PrimaryKeyJoinColumn
    private User user; 

    @Column
    private boolean email_receive = false;

    @Column
    private String email_address;
    
    @Column
    private boolean wantsAi = false;

    @Column(name = "CONTENT", length = 512)
    private String payload;

    public PersonalSettings() {
    }

    public PersonalSettings(PersonalSettings copyMe) {
        this();
        email_address = copyMe.email_address;
        email_receive = copyMe.email_receive;
        wantsAi = copyMe.wantsAi;
        payload = copyMe.payload;
    }
    
    public PersonalSettings(SettingsDto dto) {
        email_address = dto.email_address;
        email_receive = dto.email_receive;
        wantsAi = dto.wantsAi;
        payload = dto.payload;
    }

    public int getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(int configurationId) {
        this.configurationId = configurationId;
    }

    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public boolean isEmail_receive() {
        return email_receive;
    }
    
    public void setEmail_receive(boolean email_receive) {
        this.email_receive = email_receive;
    }
    
    public String getEmail_address() {
        return email_address;
    }
    
    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }
    
    public boolean isWantsAi() {
        return wantsAi;
    }
    
    public void setWantsAi(boolean wantsAi) {
        this.wantsAi = wantsAi;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object object) {
        Optional<PersonalSettings> check = Optional.ofNullable(object)
            .filter(PersonalSettings.class::isInstance)
            .map(PersonalSettings.class::cast)
            .filter(s -> email_receive == s.email_receive)
            .filter(s -> wantsAi == s.wantsAi)
            .filter(s -> configurationId == s.configurationId);
        boolean isEqual = check.isPresent();
        if (!isEqual) {
            return false;
        }
        if (email_address != null) {
            isEqual = email_address.equals(check.get().email_address);
        } else {
            isEqual = check.get().email_address == null;
        }
        if (!isEqual && payload == null) {
            isEqual = payload.equals(check.get().payload);
        } else {
            isEqual = check.get().payload == null;
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(email_receive)
            .append(wantsAi)
            .append(configurationId)
            .append(email_receive)
            .toHashCode();
    }
}
