package com.example.backend.cash;

public class CashRequestNotFoundException extends RuntimeException {

    public CashRequestNotFoundException(String message) {
        super(message);
    }
}
