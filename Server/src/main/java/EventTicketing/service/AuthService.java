package EventTicketing.service;

import EventTicketing.dto.AuthDto;
import EventTicketing.exception.DuplicateResourceException;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.model.User;
import EventTicketing.model.enums.OrganizerApplicationStatus;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.OrganizerApplicationRepository;
import EventTicketing.repository.UserRepository;
import EventTicketing.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizerApplicationRepository organizerApplicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthDto.UserSummary register(AuthDto.RegisterRequest request) {
        String phone = normalizePhone(request.phone());

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("An account with this phone number already exists");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .phone(phone)
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.CUSTOMER)
                .build();

        return AuthDto.UserSummary.from(userRepository.save(user));
    }

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        Optional<User> existingUser = userRepository.findByEmailOrPhone(request.identifier(), request.identifier());

        if (existingUser.isEmpty()) {
            rejectIfPendingApplication(request.identifier());
            throw new BadCredentialsException("Invalid email/phone or password");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifier(), request.password())
        );
        User user = (User) authentication.getPrincipal();
        String token = jwtService.generateToken(user);
        return new AuthDto.LoginResponse(token, AuthDto.UserSummary.from(user));
    }

    private void rejectIfPendingApplication(String identifier) {
        organizerApplicationRepository.findByStatusAndIdentifier(OrganizerApplicationStatus.PENDING, identifier)
                .ifPresent(application -> {
                    throw new ForbiddenActionException("Your organizer application is still under review");
                });
    }

    private String normalizePhone(String phone) {
        return (phone == null || phone.isBlank()) ? null : phone;
    }
}