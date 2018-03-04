package ar.edu.itba.ati.ati_soft.interfaces;

/**
 * Exception to be thrown when an image file type is not supported
 */
public class UnsupportedImageFileException extends RuntimeException {

    /**
     * Default constructor.
     */
    public UnsupportedImageFileException() {
        super();
    }

    /**
     * Constructor which can set a message.
     *
     * @param msg The detail message, which is saved for later retrieval by the {@link #getMessage()} method.
     */
    public UnsupportedImageFileException(String msg) {
        super(msg);
    }

    /**
     * Constructor which can set a cause.
     *
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public UnsupportedImageFileException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor which can set a message and a cause.
     *
     * @param msg   The detail message, which is saved for later retrieval by the {@link #getMessage()} method.
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public UnsupportedImageFileException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
