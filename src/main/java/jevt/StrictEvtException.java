package jevt;

public class StrictEvtException extends BadEvtException {
    // int offset;

    public StrictEvtException(/*int offset, */ String message)
    {
        super(message);
        // this.offset = offset;
    }

    public boolean strictOnly() {
        return true;
    }
}
