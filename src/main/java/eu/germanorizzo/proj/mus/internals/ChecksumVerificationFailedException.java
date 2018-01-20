package eu.germanorizzo.proj.mus.internals;

@SuppressWarnings("serial")
public class ChecksumVerificationFailedException extends Exception {
    ChecksumVerificationFailedException(String message) {
        super(message);
    }
}
