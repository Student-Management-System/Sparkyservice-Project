package net.ssehub.sparkyservice.api.validation;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.test.context.ContextConfiguration;

import net.ssehub.sparkyservice.api.conf.SpringConfig;

@RunWith(Parameterized.class)
@ContextConfiguration(classes=SpringConfig.class)
public class PasswordValidationTests {
    
    private class TestDto {
        @ValidPassword
        String password;

        TestDto(String pass) {
            this.password = pass;
        }
    }

    @Parameters(name = "{index}: pass({0}) must be valid: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] 
                {
                    { "kjhk324jk", true },
                    { "s", false }, 
                    { "aaaa", false }, 
                    { "123456", true }, 
                    { "ääüö#ä$%&&", true } 
                }  
           );
    }
    
    private Validator validator;
    
    private boolean expectedValidity;
    
    private String passInput; 
    
    @Before
    public void setup() throws Exception {
        if (validator == null) {
            final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }
    }
    
    public PasswordValidationTests(String passInput, boolean outputValid) {
        this.expectedValidity = outputValid;
        this.passInput = passInput;
    }
    
    @Test
    public void passwordValidTest() {
        var testObj = new TestDto(passInput);
        var violations = validator.validate(testObj);
        assertEquals(violations.isEmpty(), expectedValidity);
    }
}
