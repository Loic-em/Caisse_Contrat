package com.example.backend.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.backend.accounts.AccountRequest;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountService;
import com.example.backend.accounts.UserAccount;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class AuthControllerTests {

    private static final String PASSWORD = "demo-pass-12345";

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserSignatureRepository signatures;

    @Autowired
    private AuthController authController;

    @Test
    void requiresFreshPasswordAuthenticationAndKeepsSignaturePrivateToItsOwner() throws Exception {
        UserAccount signer = createDirector("SIGNER-001", "signer");
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(signer.getEmail(), PASSWORD));
        MockHttpSession session = new MockHttpSession();
        assertThrows(AuthenticationException.class, () -> authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(signer.getEmail(), "wrong-password")));
        authController.reauthenticate(new AuthController.ReauthenticationRequest(PASSWORD), authentication, session);

        byte[] png = createPng();
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
        assertThrows(IllegalArgumentException.class, () -> authController.saveSignature(
                new AuthController.SignatureRequest("data:image/png;base64,AAAA"), authentication));
        authController.saveSignature(new AuthController.SignatureRequest(dataUrl), authentication);
        assertNotNull(authController.getSignature(authentication).get("dataUrl"));
        assertNotNull(signatures.findById(signer.getId()).orElseThrow());
        assertNotNull(signer.getId());

        UserAccount administrator = accountService.createInitialAdministratorWithPassword(
                new AccountRequest(
                        "ADMIN-001",
                        "Moussa",
                        "Ndiaye",
                        "admin@digin.example",
                        "Direction",
                        "Administrateur",
                        AccountRole.ADMINISTRATEUR,
                        null,
                        null),
                passwordEncoder.encode(PASSWORD));
        Authentication adminAuthentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(administrator.getEmail(), PASSWORD));
        assertThrows(ResponseStatusException.class, () -> authController.saveSignature(
                new AuthController.SignatureRequest(dataUrl), adminAuthentication));
    }

    private UserAccount createDirector(String employeeId, String emailPrefix) {
        return accountService.createWithPassword(
                new AccountRequest(
                        employeeId,
                        "Awa",
                        "Diallo",
                        emailPrefix + "@digin.example",
                        "Direction",
                        "Directrice générale",
                        AccountRole.DIRECTEUR_GENERAL,
                        null,
                        null),
                passwordEncoder.encode(PASSWORD));
    }

    private byte[] createPng() throws Exception {
        BufferedImage image = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
