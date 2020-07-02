package net.ssehub.sparkyservice.api.auth;

import net.ssehub.sparkyservice.api.user.dto.TokenDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

public class AuthenticationInfoDto {
    public UserDto user = new UserDto();
    public TokenDto token = new TokenDto();
}