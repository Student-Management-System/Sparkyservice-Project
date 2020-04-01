package net.ssehub.sparkyservice.api.jpa.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

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
}
