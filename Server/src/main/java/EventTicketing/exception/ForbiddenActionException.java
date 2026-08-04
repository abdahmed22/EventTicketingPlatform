package EventTicketing.exception;

public class ForbiddenActionException extends BaseAppException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}