package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.auth.UserDto;
import co.edu.sena.productsreact.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        try {
            System.out.println("getAllUsers llamado");
            List<UserDto> users = userService.getAllUsers();
            System.out.println("Usuarios obtenidos: " + users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            System.err.println("Error en getAllUsers: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto> changeUserRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        if (userDetails == null || !userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }
        String newRole = request.get("role");
        UserDto updated = userService.changeUserRole(id, newRole);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        if (userDetails == null || !userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Usuario no autenticado"));
            }
            String username = request.get("username");
            String email = request.get("email");

            if (username == null || username.isBlank() || email == null || email.isBlank()) {
                return ResponseEntity.status(400).body(Map.of("message", "Username y email son requeridos"));
            }

            UserDto updated = userService.updateProfile(userDetails.getUsername(), username, email);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Usuario no autenticado"));
            }

            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            if (currentPassword == null || currentPassword.isBlank() ||
                newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.status(400).body(Map.of("message", "Contraseña actual y nueva son requeridas"));
            }

            userService.changePassword(userDetails.getUsername(), currentPassword, newPassword);
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Usuario no autenticado"));
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("message", "El archivo es requerido"));
            }

            UserDto updated = userService.uploadAvatar(userDetails.getUsername(), file);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error: " + e.getMessage()));
        }
    }
}
