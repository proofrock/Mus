package eu.germanorizzo.proj.mus.internals;

@SuppressWarnings("serial")
public class ChecksumVerificationFailedException extends Exception {

    public ChecksumVerificationFailedException() {
        super();
    }

    public ChecksumVerificationFailedException(String message) {
        super(message);
    }

}
