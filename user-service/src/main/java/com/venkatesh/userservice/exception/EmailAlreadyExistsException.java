package com.venkatesh.userservice.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("A user with email '" + email + "' already exists");
    }
}

