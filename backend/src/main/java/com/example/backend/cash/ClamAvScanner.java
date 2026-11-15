package com.example.backend.cash;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClamAvScanner implements AntivirusScanner {

    private final String host;
    private final int port;
    private final int timeoutMillis;

    public ClamAvScanner(
            @Value("${app.antivirus.clamd-host:}") String host,
            @Value("${app.antivirus.clamd-port:3310}") int port,
            @Value("${app.antivirus.timeout-millis:30000}") int timeoutMillis) {
        this.host = host == null ? "" : host.trim();
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void scan(byte[] file) {
        if (host.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le dépôt est désactivé tant qu’un antivirus ClamAV n’est pas configuré.");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            try (DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                output.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
                int offset = 0;
                while (offset < file.length) {
                    int length = Math.min(8192, file.length - offset);
                    output.writeInt(length);
                    output.write(file, offset, length);
                    offset += length;
                }
                output.writeInt(0);
                output.flush();
                ByteArrayOutputStream response = new ByteArrayOutputStream();
                int value;
                while ((value = socket.getInputStream().read()) != -1 && value != 0 && value != '\n') {
                    response.write(value);
                }
                String result = response.toString(StandardCharsets.UTF_8);
                if (!result.endsWith("OK")) {
                    if (result.contains("FOUND")) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                "Le fichier a été refusé par l’analyse antivirus.");
                    }
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "L’analyse antivirus n’a pas pu valider le fichier.");
                }
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le service antivirus est indisponible; le fichier n’a pas été enregistré.",
                    exception);
        }
    }

    @Override
    public boolean isOperational() {
        if (host.isBlank()) return false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.min(timeoutMillis, 2000));
            socket.setSoTimeout(Math.min(timeoutMillis, 2000));
            socket.getOutputStream().write("zPING\0".getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            int value;
            while ((value = socket.getInputStream().read()) != -1 && value != 0 && value != '\n') {
                response.write(value);
            }
            return "PONG".equals(response.toString(StandardCharsets.US_ASCII));
        } catch (IOException exception) {
            return false;
        }
    }
}
