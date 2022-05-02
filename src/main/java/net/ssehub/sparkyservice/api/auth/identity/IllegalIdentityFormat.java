package net.ssehub.sparkyservice.api.auth.identity;

public class IllegalIdentityFormat extends IllegalArgumentException {

    private static final long serialVersionUID = 3948489287145277087L;

    public IllegalIdentityFormat(String message) {
        super(message);
    }
}
