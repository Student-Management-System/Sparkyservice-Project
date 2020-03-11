package net.ssehub.sparkyservice.db.user;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import net.ssehub.sparkyservice.db.hibernate.AnnotatedClass;

@Entity
@Table(name = "user_local_password")
public class Password implements AnnotatedClass {
    
    /**
     * This algorythm is used if no other method is specified
     */
    public static final String DEFAULT_HASH_ALGORITHM = "SHA-256";
    
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int passwordId;

    /**
     * Used hashed algorythm for the stored password. 
     * There are no pre-defined valu
     */
    @Column
    public String hashAlgorithm;
    
    @OneToOne
    public StoredUser user;
    
    /**
     * 
     */
    @Column(nullable = false)
    private String passwordString;
    
    /**
     * Default constructor needed by hibernate.
     */
    public Password() {}
    
    /**
     * Takes a plain text password and hash it with the default password algorithm.
     * @param plainTextPassword
     * @throws UnsupportedEncodingException 
     */
    public Password(String plainTextPassword) {
        hashAndSetPassword(plainTextPassword);
        this.hashAlgorithm = DEFAULT_HASH_ALGORITHM;
    }
    
    public Password(String passwordString, String hashAlgorithm) {
        this.passwordString = passwordString;
        this.hashAlgorithm = hashAlgorithm;
    }
    
    /**
     * Method hashes the given password with {@link Password.DEFAULT_HASH_ALGORYTHM}
     * @param plainTextPassword
     * @throws UnsupportedEncodingException 
     */
    public String hashAndSetPassword(String plainTextPassword) {
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance(DEFAULT_HASH_ALGORITHM);
            byte[] hash = digest.digest(plainTextPassword.getBytes(StandardCharsets.UTF_8));
            this.passwordString = new String(hash, "UTF-8");
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace(); // TODO write log file
        }
        return passwordString;
    }
    
    public void setPassword(String passwordHash) {
        this.passwordString = passwordHash;
    }
    
    @Nonnull
    public String getPassword() {
        final String pass = passwordString;
        if (pass == null) {
            return "";
        } else {
            return pass;
        }
    }
}

