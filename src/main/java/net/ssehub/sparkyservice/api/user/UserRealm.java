package net.ssehub.sparkyservice.api.user;

/**
 * Authentication realm of a user.
 * 
 * @author marcel
 */
public enum UserRealm {
    LOCAL {
        @Override
        public SparkyUserFactory<? extends SparkyUser> getUserFactory() {
            return new LocalUserFactory();
        }
    },
    UNIHI {
        @Override
        public SparkyUserFactory<? extends SparkyUser> getUserFactory() {
            return new LdapUserFactory();
        }
    },
    RECOVERY {
        @Override
        public SparkyUserFactory<? extends SparkyUser> getUserFactory() {
            return new MemoryUserFactory();
        }
    },
    UNKNOWN {
        @Override
        public SparkyUserFactory<? extends SparkyUser> getUserFactory() {
            throw new UnsupportedOperationException();
        }
    };
    
    /**
     * Returns a factory in order to create a {@link SparkyUser} matching a realm.  
     * 
     * @return Abstract Factory for creating users
     */
    public abstract SparkyUserFactory<? extends SparkyUser> getUserFactory();
}
