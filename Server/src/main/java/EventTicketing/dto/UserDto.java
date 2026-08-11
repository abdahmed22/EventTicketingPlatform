package EventTicketing.dto;

import EventTicketing.model.User;
import EventTicketing.model.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public abstract class UserDto {

    public record Response(
            UUID id,
            String name,
            String email,
            String phone,
            UserRole role,
            Instant createdAt
    ) {
        public static Response from(User user) {
            return new Response(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getCreatedAt()
            );
        }
    }

    public record ChangeRoleRequest(UserRole role) {}
}
