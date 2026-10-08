package pt.isep.dssmv.mapaisep.exceptions;

public class InvalidDataException extends IllegalArgumentException {
    public InvalidDataException(String message) {
        super(message);
    }
}