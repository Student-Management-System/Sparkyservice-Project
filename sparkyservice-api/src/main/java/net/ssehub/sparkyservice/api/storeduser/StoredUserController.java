package net.ssehub.sparkyservice.api.storeduser;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller for storing user into the local realm.
 * @author marcel
 */
@RestController
@RequestMapping(value = "/user")
public class StoredUserController {
    
    @Autowired
    private StoredUserService service;
    
    @PutMapping
    public Boolean storeNewUser(@RequestBody @NotNull NewUserDto newUser) {
        service.storeNewUser(newUser);
        return true;
    }
}
