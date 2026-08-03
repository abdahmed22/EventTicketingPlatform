package EventTicketing.service;

import EventTicketing.dto.authDto;
import EventTicketing.exception.DuplicateResourceException;
import EventTicketing.model.User;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.userRepository;
import EventTicketing.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class authService {

    private final userRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public authDto.UserSummary register(authDto.RegisterRequest request) {
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

        return authDto.UserSummary.from(userRepository.save(user));
    }

    public authDto.LoginResponse login(authDto.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifier(), request.password())
        );
        User user = (User) authentication.getPrincipal();
        String token = jwtService.generateToken(user);
        return new authDto.LoginResponse(token, authDto.UserSummary.from(user));
    }

    private String normalizePhone(String phone) {
        return (phone == null || phone.isBlank()) ? null : phone;
    }
}