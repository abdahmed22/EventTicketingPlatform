package EventTicketing.exception;

public class InvalidStateTransitionException extends BaseAppException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
