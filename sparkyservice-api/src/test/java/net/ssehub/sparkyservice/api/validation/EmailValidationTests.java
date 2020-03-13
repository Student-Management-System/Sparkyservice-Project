package net.ssehub.sparkyservice.api.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;

import net.ssehub.sparkyservice.api.conf.SpringConfig;

@ContextConfiguration(classes=SpringConfig.class)
public class EmailValidationTests {
    
    private class TestDto {
        @Email
        String email;

        TestDto(String email) {
            this.email = email;
        }
    }

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
    
    @BeforeEach
    public void setup() throws Exception {
        if (validator == null) {
            final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }
    }
    
    @ParameterizedTest
    @MethodSource("data")
    public void passwordValidTest(String emailInput, boolean expectedValidity) {
        var testObj = new TestDto(emailInput);
        var violations = validator.validate(testObj);
        assertEquals(violations.isEmpty(), expectedValidity, "The given value didn't pass the validity test");
    }
}
