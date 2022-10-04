package oleg.sopilnyak.test.endpoint.exception;

/**
 * Exception occurred if system is not found appropriate resource
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
