package net.ssehub.sparkyservice.api.persistence;

/**
 * Provides exception which is a sign for a duplicate entry in a persistent storage. 
 * 
 * @author marcel
 */
public class DuplicateEntryException extends RuntimeException {

    private static final long serialVersionUID = -2372148342324372993L;
    private final Object entry;

    /**
     * Sign that the operation can't be completed because the desired 
     * value can't saved to because there is already the same value (or the same identifier) in the storage.
     * 
     * @param entry The object which caused the failure
     */
    public DuplicateEntryException(Object entry) {
        this.entry = entry;
    }

    /**
     * The entry which caused the failure.
     * 
     * @return The duplicate entry
     */
    public Object getEntry() {
        return entry;
    }
}
