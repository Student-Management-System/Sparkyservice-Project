package net.ssehub.sparkyservice.api.auth;

import java.time.LocalDateTime;
import java.util.function.Supplier;

public interface AuthSpecifications {

    public Supplier<LocalDateTime> getAuthExpirationDuration();

    public int getAuthRefreshes();

}
