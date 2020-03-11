package net.ssehub.sparkyservice.api.validation;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Email;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.test.context.ContextConfiguration;

import net.ssehub.sparkyservice.api.conf.SpringConfig;

@RunWith(Parameterized.class)
@ContextConfiguration(classes=SpringConfig.class)
public class EmailValidationTests {
    
    private class TestDto {
        @Email
        String email;

        TestDto(String email) {
            this.email = email;
        }
    }

    @Parameters(name = "{index}: Email({0}) valid: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] 
                {
                    { "test@test.com", true },
                    { "test", false }, 
                    { "test@", false }, 
                    { "a@f.de", true }, 
                    { "a@test.co.uk", true },
                    { "test@test", /*false localdomains don't have tld endings*/ true },
                    { "@test.com", false },
                    { "@ä" , false}
                }  
           );
    }
    
    private Validator validator;
    
    private boolean expectedValidity;
    
    private String emailInput; 
    
    @Before
    public void setup() throws Exception {
        if (validator == null) {
            final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }
    }
    
    public EmailValidationTests(String passInput, boolean outputValid) {
        this.expectedValidity = outputValid;
        this.emailInput = passInput;
    }
    
    @Test
    public void passwordValidTest() {
        var testObj = new TestDto(emailInput);
        var violations = validator.validate(testObj);
        assertEquals("The given value didn't pass the validity test", violations.isEmpty(), expectedValidity);
    }
}
