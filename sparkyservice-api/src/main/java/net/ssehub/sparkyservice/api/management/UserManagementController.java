package net.ssehub.sparkyservice.api.management;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import net.ssehub.sparkyservice.api.storeduser.NewUserDto;
import net.ssehub.sparkyservice.api.storeduser.SettingsDto;
import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.api.storeduser.StoredUserRepository;

/**
 * @author marcel
 */
@RestController
@RequestMapping(value = "/api/management/")
public class UserManagementController {
    
    private class MissingUserDataException extends Exception {
        private static final long serialVersionUID = -4006837022238759225L;
    }
    private class PasswordInvalidException extends Exception {
        private static final long serialVersionUID = 3641149318458501051L;
        String message;
        public PasswordInvalidException(String message) {
            this.message = message;
        }
        public String getMessage() {
            return this.message;
        }
    }

    @Autowired
    private StoredUserRepository repository;
    
    @PutMapping("/user/add")
    public void addLocalUser(@RequestBody @Valid NewUserDto newUserDto) 
            throws MissingUserDataException, PasswordInvalidException {
        var newUser = StoredUserDetails.createStoredLocalUser(newUserDto.username, newUserDto.password, true);
        repository.save(newUser.getTransactionObject());
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
    public void editLocalUser(@RequestBody @NotNull @Valid NewUserDto newUserDto) {
        
    }
  
    
//    @GetMapping("/user/delete")
//    @GetMapping("/user/changepass")
    
        
    /**
     * Handler for an {@link PasswordInvalidException}. 
     * @param ex exception with message why the password is not valid
     * @return
     */
    @ExceptionHandler(value = PasswordInvalidException.class)
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    public String handleUserNotFoundException(PasswordInvalidException ex) {
        return "The given password does not match with our password policy: " + ex.getMessage();
    }
    
    @ExceptionHandler(value = MissingUserDataException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public String handleMissingUserDataException(MissingUserDataException ex) {
        return "Some values are missing in the request.";
    }
    
    private boolean isNewUserDtoValid(NewUserDto userDto) {
        boolean passwordExists = userDto.password != null && userDto.password != "";
        boolean userNameExists = userDto.username != null && userDto.username != "";
        boolean settingsExists = userDto.getPersonalSettings() != null;
        return passwordExists && userNameExists && settingsExists;
    }
}
