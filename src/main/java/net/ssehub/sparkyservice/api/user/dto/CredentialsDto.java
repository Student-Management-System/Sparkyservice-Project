package net.ssehub.sparkyservice.api.user.dto;

/**
 * DTO for login credentials.
 * 
 * @author marcel
 */
//checkstyle: stop visibility modifier check
public class CredentialsDto {

    public CredentialsDto() {

    }

    public CredentialsDto(String username, String password) {
        super();
        this.username = username;
        this.password = password;
    }

    public String username;
    public String password;
}
