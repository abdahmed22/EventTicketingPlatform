package EventTicketing.service;

import EventTicketing.dto.UserDto;
import EventTicketing.exception.ForbiddenActionException;
import EventTicketing.exception.ResourceNotFoundException;
import EventTicketing.model.User;
import EventTicketing.model.enums.UserRole;
import EventTicketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDto.Response> listAll() {
        return userRepository.findAll().stream()
                .map(UserDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto.Response getById(UUID id) {
        return UserDto.Response.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<UserDto.Response> listByRole(UserRole role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .map(UserDto.Response::from)
                .toList();
    }

    @Transactional
    public UserDto.Response changeRole(UUID userId, UserRole newRole, User admin) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("Only admins can change user roles");
        }
        // Prevent stripping the last admin
        if (newRole != UserRole.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.ADMIN && !u.getId().equals(userId))
                    .count();
            if (adminCount == 0) {
                throw new IllegalStateException("Cannot demote the only remaining admin");
            }
        }
        User user = findOrThrow(userId);
        user.setRole(newRole);
        return UserDto.Response.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID userId, User admin) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenActionException("Only admins can delete users");
        }
        User user = findOrThrow(userId);
        if (user.getRole() == UserRole.ADMIN) {
            throw new ForbiddenActionException("Cannot delete an admin account");
        }
        userRepository.delete(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
