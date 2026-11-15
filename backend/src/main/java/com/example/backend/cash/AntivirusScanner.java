package com.example.backend.cash;

public interface AntivirusScanner {

    void scan(byte[] file);

    boolean isOperational();
}
