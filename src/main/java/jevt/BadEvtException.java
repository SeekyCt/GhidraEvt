package jevt;

public class BadEvtException extends Exception {
    // int offset;

    public BadEvtException(/*int offset, */ String message)
    {
        super(message);
        // this.offset = offset;
    }
}
