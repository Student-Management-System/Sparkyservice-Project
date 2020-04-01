package net.ssehub.sparkyservice.api.storeduser.dto;

import java.io.Serializable;

public class TokenDto implements Serializable {
    private static final long serialVersionUID = 6425173137954338569L;
    String token;
    String expiration;
}