package EventTicketing.controller;

import EventTicketing.dto.UserDto;
import EventTicketing.model.User;
import EventTicketing.model.enums.UserRole;
import EventTicketing.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** List all users, optionally filtered by role. */
    @GetMapping
    public ResponseEntity<List<UserDto.Response>> listUsers(
            @RequestParam(required = false) UserRole role) {
        if (role != null) {
            return ResponseEntity.ok(userService.listByRole(role));
        }
        return ResponseEntity.ok(userService.listAll());
    }

    /** Get a single user by UUID. */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto.Response> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    /** Change a user's role (CUSTOMER ↔ ORGANIZER ↔ ADMIN). */
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserDto.Response> changeRole(
            @PathVariable UUID id,
            @RequestBody UserDto.ChangeRoleRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(userService.changeRole(id, request.role(), admin));
    }

    /** Delete a user account (non-admin accounts only). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User admin) {
        userService.deleteUser(id, admin);
        return ResponseEntity.noContent().build();
    }
}
