package EventTicketing.configuration;

import EventTicketing.model.User;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.userRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final userRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.email}")
    private String adminEmail;

    @Value("${admin.seed.password:}")
    private String configuredPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        String password = configuredPassword.isBlank() ? generateRandomPassword() : configuredPassword;

        User admin = User.builder()
                .name("Platform Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);

        if (configuredPassword.isBlank()) {
            log.warn("No ADMIN_PASSWORD env var set - generated a one-time admin password for {}: {}", adminEmail, password);
            log.warn("This is only ever logged once and isn't stored anywhere in plaintext. Set ADMIN_PASSWORD to control it explicitly.");
        } else {
            log.info("Seeded admin account: {}", adminEmail);
        }
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}