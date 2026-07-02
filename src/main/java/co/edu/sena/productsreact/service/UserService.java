package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.auth.UserDto;
import co.edu.sena.productsreact.entity.Role;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String UPLOADS_DIR = "uploads/";

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(), user.getAvatarUrl()))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto changeUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        try {
            Role role = Role.valueOf(newRole);
            user.setRole(role);
            User updated = userRepository.save(user);
            return new UserDto(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole().name(), updated.getAvatarUrl());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido: " + newRole);
        }
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        userRepository.delete(user);
    }

    @Transactional
    public UserDto updateProfile(String currentUsername, String newUsername, String email) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!currentUsername.equals(newUsername) && userRepository.findByUsername(newUsername).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        if (!user.getEmail().equals(email) && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        user.setUsername(newUsername);
        user.setEmail(email);
        User updated = userRepository.save(user);
        return new UserDto(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole().name(), updated.getAvatarUrl());
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Contraseña actual incorrecta");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public UserDto uploadAvatar(String username, MultipartFile file) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        long fileSizeInMB = file.getSize() / (1024 * 1024);
        if (fileSizeInMB > 5) {
            throw new IllegalArgumentException("El archivo no puede exceder 5MB");
        }

        String[] allowedExtensions = {"jpg", "jpeg", "png", "gif"};
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.lastIndexOf(".") == -1) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }

        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        boolean isAllowed = false;
        for (String ext : allowedExtensions) {
            if (ext.equals(fileExtension)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new IllegalArgumentException("Formato de archivo no permitido. Use: JPG, PNG, GIF");
        }

        try {
            // Crear directorio si no existe (con todos los directorios padres)
            Path uploadsPath = Paths.get(UPLOADS_DIR).toAbsolutePath();
            if (!Files.exists(uploadsPath)) {
                Files.createDirectories(uploadsPath);
            }

            // Generar nombre único para el archivo
            String fileName = "avatar_" + user.getId() + "_" + UUID.randomUUID() + "." + fileExtension;
            Path filePath = uploadsPath.resolve(fileName);

            // Guardar archivo
            Files.write(filePath, file.getBytes());

            // Actualizar URL del avatar
            String avatarUrl = "/uploads/" + fileName;
            user.setAvatarUrl(avatarUrl);
            User updated = userRepository.save(user);

            return new UserDto(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole().name(), updated.getAvatarUrl());
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen: " + e.getMessage(), e);
        }
    }
}
