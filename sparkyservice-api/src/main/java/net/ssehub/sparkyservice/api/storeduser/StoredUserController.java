package net.ssehub.sparkyservice.api.storeduser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import net.ssehub.sparkyservice.api.management.MissingDataException;
import net.ssehub.sparkyservice.api.storeduser.EditUserDto;
import net.ssehub.sparkyservice.api.storeduser.NewUserDto;
import net.ssehub.sparkyservice.api.storeduser.SettingsDto;
import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.api.storeduser.StoredUserService;
import net.ssehub.sparkyservice.api.storeduser.UserNotFoundException;
import net.ssehub.sparkyservice.db.user.PersonalSettings;
import net.ssehub.sparkyservice.db.user.StoredUser;

/**
 * @author marcel
 */
@RestController
@RequestMapping(value = "/api/management/")
public class StoredUserController {
    
    @Autowired
    private StoredUserService userService;
    
    @PutMapping("/user/add")
    public void addLocalUser(@RequestBody @NotNull @Valid NewUserDto newUserDto) {
        final var newUser = StoredUserDetails.createStoredLocalUser(newUserDto.username, newUserDto.password, true);
        userService.storeUser(newUser);
//        if (isNewUserDtoValid(newUserDto)) {
//            var passValidator = ValidationFactory.getPasswordValidator();
//            if (passValidator.checkIfValid(newUserDto.password)) {
//                var newUser = StoredUserDetails.createStoredLocalUser(newUserDto.username, newUserDto.password, true);
//                repository.save(newUser.getTransactionObject());
//            } else {
//                throw new PasswordInvalidException(passValidator.getMessage());
//            }
//        } else {
//            throw new MissingUserDataException();
//        }
    }
    
    @PutMapping("/user/edit") 
    public void editLocalUser(@RequestBody @NotNull @Nonnull @Valid EditUserDto userDto) 
            throws MissingDataException, UserNotFoundException {
        
        StoredUser databaseUser = userService.findUserByNameAndRealm(userDto.username, userDto.realm);
        databaseUser = editUserFromDto(databaseUser, userDto);
        userService.storeUser(databaseUser);
    }
    
    public StoredUser writePersonalSettings(StoredUser user, SettingsDto settings) {
        PersonalSettings dbSettings = user.getProfileConfiguration();
        // TODO Marcel: Set settings
        return user;
    }
    
    public void changePassword(StoredUser databaseUser, @Nullable EditUserDto.ChangePasswordDto passwordDto) 
            throws MissingDataException {
        
        if (passwordDto != null) {
            if (!passwordDto.oldPassword.equals(passwordDto.newPassword)) {
                throw new MissingDataException("Passwords does not match");
            }
            var userDetails = new StoredUserDetails(databaseUser);
            userDetails.encodeAndSetPassword(passwordDto.newPassword); // wont be null through dto validation
        }
    }
    
    public StoredUser editUserFromDto(@Nonnull StoredUser databaseUser, @Nonnull EditUserDto userDto) 
            throws MissingDataException {
        
        if (databaseUser.getRealm() == StoredUserDetails.DEFAULT_REALM) {
            changePassword(databaseUser, userDto.passwordDto);
        }
        databaseUser = writePersonalSettings(databaseUser, userDto.settings);
        databaseUser.setUserName(userDto.username);
        return databaseUser;
        
    }
    
//    @PutMapping("/user/admin/edit")
//    @GetMapping("/user/delete")
//    @GetMapping("/user/changepass")
    
    
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public String handleMissingUserDataException(MissingDataException ex) {
        if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
            return "Some values are missing or wrong in the request.";
        } else {
            return ex.getMessage();
        }
    }
    
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public String handleUserNotFoundException(UserNotFoundException ex) {
        if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
            return "User not found.";
        } else {
            return ex.getMessage();
        }
    }
    
}
