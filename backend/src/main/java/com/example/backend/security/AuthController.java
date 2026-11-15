package com.example.backend.security;

import com.example.backend.accounts.AccountResponse;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountStatus;
import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserAccountRepository accounts;
    private final UserSignatureRepository signatures;
    private final Clock clock;
    private final int visaReauthenticationMinutes;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            UserAccountRepository accounts,
            UserSignatureRepository signatures,
            @Value("${APP_CASH_VISA_REAUTH_MINUTES:5}") int visaReauthenticationMinutes) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.accounts = accounts;
        this.signatures = signatures;
        this.clock = Clock.systemUTC();
        if (visaReauthenticationMinutes < 1 || visaReauthenticationMinutes > 60) {
            throw new IllegalArgumentException("APP_CASH_VISA_REAUTH_MINUTES doit être compris entre 1 et 60.");
        }
        this.visaReauthenticationMinutes = visaReauthenticationMinutes;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrfToken(@RequestAttribute("_csrf") CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public AccountResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        UserAccount account = accounts.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Le compte authentifié est introuvable."));
        return AccountResponse.from(account);
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public AccountResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connectez-vous pour continuer.");
        }
        UserAccount account = accounts.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return AccountResponse.from(account);
    }

    @PostMapping("/reauthenticate")
    public Map<String, String> reauthenticate(
            @Valid @RequestBody ReauthenticationRequest request,
            Authentication currentAuthentication,
            HttpSession session) {
        if (currentAuthentication == null || !currentAuthentication.isAuthenticated()
                || "anonymousUser".equals(currentAuthentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connectez-vous pour continuer.");
        }
        Authentication verified = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        currentAuthentication.getName(), request.password()));
        Instant authenticatedAt = clock.instant();
        session.setAttribute("cashVisaReauthenticatedAt", authenticatedAt.toString());
        session.setAttribute("cashVisaReauthenticatedBy", verified.getName());
        return Map.of(
                "message", "Identité confirmée.",
                "expiresAt", authenticatedAt.plusSeconds(visaReauthenticationMinutes * 60L).toString());
    }

    @GetMapping("/signature")
    @Transactional(readOnly = true)
    public Map<String, String> getSignature(Authentication authentication) {
        UserAccount account = requireSignatureOwner(authentication);
        UserSignature signature = signatures.findById(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune signature enregistrée."));
        return Map.of(
                "dataUrl",
                "data:image/png;base64," + Base64.getEncoder().encodeToString(signature.getSignaturePng()));
    }

    @PutMapping("/signature")
    @Transactional
    public Map<String, String> saveSignature(
            @Valid @RequestBody SignatureRequest request,
            Authentication authentication) {
        UserAccount account = requireSignatureOwner(authentication);
        String prefix = "data:image/png;base64,";
        if (!request.dataUrl().startsWith(prefix)) {
            throw new IllegalArgumentException("La signature doit être un fichier PNG.");
        }
        byte[] image;
        try {
            image = Base64.getDecoder().decode(request.dataUrl().substring(prefix.length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Le fichier de signature PNG est invalide.");
        }
        if (image.length < 8 || image.length > 256 * 1024
                || image[0] != (byte) 0x89 || image[1] != 0x50
                || image[2] != 0x4e || image[3] != 0x47
                || image[4] != 0x0d || image[5] != 0x0a
                || image[6] != 0x1a || image[7] != 0x0a) {
            throw new IllegalArgumentException("La signature doit être un fichier PNG de 256 Ko maximum.");
        }
        byte[] sanitizedImage = sanitizePng(image);
        Instant now = clock.instant();
        signatures.findById(account.getId()).ifPresentOrElse(
                existing -> existing.update(sanitizedImage, now),
                () -> signatures.save(new UserSignature(account.getId(), sanitizedImage, now)));
        return Map.of("message", "Signature enregistrée.");
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record ReauthenticationRequest(@NotBlank String password) {
    }

    public record SignatureRequest(@NotBlank @Size(max = 350000) String dataUrl) {
    }

    private UserAccount requireSignatureOwner(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connectez-vous pour continuer.");
        }
        UserAccount account = accounts.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le compte est désactivé.");
        }
        if (account.getRole() == AccountRole.ADMINISTRATEUR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "L’administrateur ne peut pas gérer de signature de caisse.");
        }
        return account;
    }

    private byte[] sanitizePng(byte[] image) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
            if (input == null) {
                throw new IllegalArgumentException("Le fichier de signature PNG est illisible.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Le fichier de signature PNG est invalide.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 2400 || height > 600) {
                    throw new IllegalArgumentException(
                            "La signature PNG ne peut pas dépasser 2 400 × 600 pixels.");
                }
                BufferedImage decoded = reader.read(0);
                BufferedImage clean = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = clean.createGraphics();
                try {
                    graphics.drawImage(decoded, 0, 0, null);
                } finally {
                    graphics.dispose();
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(clean, "png", output) || output.size() > 256 * 1024) {
                    throw new IllegalArgumentException(
                            "La signature PNG nettoyée dépasse la limite de 256 Ko.");
                }
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Le fichier de signature PNG est invalide.", exception);
        }
    }
}
