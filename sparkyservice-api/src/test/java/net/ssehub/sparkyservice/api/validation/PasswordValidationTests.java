package net.ssehub.sparkyservice.api.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;

import net.ssehub.sparkyservice.api.conf.SpringConfig;

@ContextConfiguration(classes=SpringConfig.class)
public class PasswordValidationTests {
    
    private class TestDto {
        @ValidPassword
        String password;

        TestDto(String pass) {
            this.password = pass;
        }
    }

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
    
    @BeforeEach
    public void setup() throws Exception {
        if (validator == null) {
            final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }
    }
   
    @ParameterizedTest
    @MethodSource("data")
    public void passwordValidTest(String passInput, boolean expectedValidity) {
        var testObj = new TestDto(passInput);
        var violations = validator.validate(testObj);
        assertEquals(violations.isEmpty(), expectedValidity);
    }
}
