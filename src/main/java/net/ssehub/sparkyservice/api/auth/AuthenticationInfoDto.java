package net.ssehub.sparkyservice.api.auth;

import net.ssehub.sparkyservice.api.useraccess.dto.JwtDto;
import net.ssehub.sparkyservice.api.useraccess.dto.UserDto;

/**
 * DTO for authentication information.
 * 
 * @author marcel
 */
//checkstyle: stop visibility modifier check
public class AuthenticationInfoDto {
    public UserDto user = new UserDto();
    public JwtDto jwt = new JwtDto();
}