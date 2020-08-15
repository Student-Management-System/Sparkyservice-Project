package net.ssehub.sparkyservice.api.user.modification;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;

public final class UserModificationServiceFactory {

    /**
     * Disabled.
     */
    private UserModificationServiceFactory() {
        throw new Error();
    }

    /**
     * Creates a utility object with. The given role "decides" how
     * powerful (regarding to modifying fields, changing conditions and provided informations) the tool will be.
     *  
     * @param role - The permissions of the utility
     * @return A utility for modifying and accessing users
     */
     public static UserModifcationService from(UserRole role) {
        UserModifcationService util;
        switch(role) {
        case ADMIN:
            util = new AdminUserModificationImpl(new DefaultUserModificationImpl());
            break;
        case SERVICE:
        case DEFAULT:
        default:
            util = new DefaultUserModificationImpl();
            break;
        }
        return util;
    }
}
