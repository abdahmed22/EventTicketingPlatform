package EventTicketing.exception;

public abstract class BaseAppException extends RuntimeException {
    protected BaseAppException(String message) {
        super(message);
    }
}