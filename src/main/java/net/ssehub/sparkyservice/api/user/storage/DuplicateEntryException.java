package net.ssehub.sparkyservice.api.user.storage;

public class DuplicateEntryException extends RuntimeException {

    private static final long serialVersionUID = -2372148342324372993L;
    private final Object entry;

    public DuplicateEntryException(Object entry) {
        this.entry = entry;
    }

    public Object getEntry() {
        return entry;
    }
}
