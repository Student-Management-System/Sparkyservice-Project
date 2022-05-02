package net.ssehub.sparkyservice.api.user;

public class IllegalIdentityFormat extends IllegalArgumentException {

    private static final long serialVersionUID = 3948489287145277087L;

    public IllegalIdentityFormat(String message) {
        super(message);
    }
}
