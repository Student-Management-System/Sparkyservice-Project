package net.ssehub.sparkyservice.db.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import net.ssehub.sparkyservice.db.hibernate.AnnotatedClass;

@Entity
@Table(name = "user_configuration")
public class PersonalSettings implements AnnotatedClass {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int configurationId;

    @OneToOne
    @PrimaryKeyJoinColumn
    public StoredUser user; 
    
    @Column(nullable = true)
    public boolean testVal2 = false;
    @Column(nullable = true)
    public boolean testVal3 = false;
    @Column(nullable = true)
    public boolean testVal4 = false;
    @Column(nullable = true)
    public boolean testVal5 = false;
}
