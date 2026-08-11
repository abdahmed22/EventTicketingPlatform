package EventTicketing.dto;

import EventTicketing.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public abstract class AuthDto {

        private static final String PHONE_REGEX = "^\\+?[0-9]{7,15}$";

        public record RegisterRequest(
                        @NotBlank(message = "Name is required") String name,

                        @NotBlank(message = "Email is required") @Email(message = "Email must be a valid address") String email,

                        @Pattern(regexp = PHONE_REGEX, message = "Enter a valid phone number (7-15 digits, optionally starting with +)") String phone,

                        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password) {
        }

        public record LoginRequest(
                        @NotBlank(message = "Email or phone is required") String identifier,

                        @NotBlank(message = "Password is required") String password) {
        }

        public record LoginResponse(
                        String token,
                        UserSummary user) {
        }

        public record UserSummary(
                        UUID id,
                        String name,
                        String email,
                        String phone,
                        String role) {
                public static UserSummary from(User user) {
                        return new UserSummary(
                                        user.getId(),
                                        user.getName(),
                                        user.getEmail(),
                                        user.getPhone(),
                                        user.getRole().name());
                }
        }
}