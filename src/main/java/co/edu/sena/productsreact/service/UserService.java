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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String UPLOADS_DIR = "uploads";

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
    public UserDto updateProfile(String currentEmail, String newUsername, String newEmail) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!currentEmail.equals(newEmail) && userRepository.findByEmail(newEmail).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (!user.getUsername().equals(newUsername) && userRepository.findByUsername(newUsername).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        user.setUsername(newUsername);
        user.setEmail(newEmail);
        User updated = userRepository.save(user);
        return new UserDto(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole().name(), updated.getAvatarUrl());
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Contraseña actual incorrecta");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public UserDto uploadAvatar(String email, MultipartFile file) throws IOException {
        log.info("Iniciando carga de avatar para usuario: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado: {}", email);
                    return new ResourceNotFoundException("Usuario no encontrado");
                });

        if (file == null || file.isEmpty()) {
            log.warn("Archivo vacío para usuario: {}", email);
            throw new IllegalArgumentException("El archivo está vacío");
        }

        long fileSizeInMB = file.getSize() / (1024 * 1024);
        if (fileSizeInMB > 5) {
            log.warn("Archivo demasiado grande para usuario {}: {}MB", email, fileSizeInMB);
            throw new IllegalArgumentException("El archivo no puede exceder 5MB");
        }

        String[] allowedExtensions = {"jpg", "jpeg", "png", "gif"};
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.lastIndexOf(".") == -1) {
            log.warn("Nombre de archivo inválido para usuario: {}", email);
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
            log.warn("Formato no permitido para usuario {}: {}", email, fileExtension);
            throw new IllegalArgumentException("Formato de archivo no permitido. Use: JPG, PNG, GIF");
        }

        try {
            // Crear directorio si no existe
            Path uploadsPath = Paths.get(UPLOADS_DIR).toAbsolutePath();
            log.debug("Ruta de uploads: {}", uploadsPath);

            if (!Files.exists(uploadsPath)) {
                log.info("Creando directorio de uploads: {}", uploadsPath);
                Files.createDirectories(uploadsPath);
            }

            // Generar nombre único para el archivo
            String fileName = "avatar_" + user.getId() + "_" + UUID.randomUUID() + "." + fileExtension;
            Path filePath = uploadsPath.resolve(fileName);
            log.info("Guardando archivo en: {}", filePath);

            // Guardar archivo
            Files.write(filePath, file.getBytes());
            log.info("Archivo guardado exitosamente: {}", filePath);

            // Actualizar URL del avatar
            String avatarUrl = "/uploads/" + fileName;
            user.setAvatarUrl(avatarUrl);
            User updated = userRepository.save(user);
            log.info("Avatar actualizado para usuario {}: {}", email, avatarUrl);

            return new UserDto(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole().name(), updated.getAvatarUrl());
        } catch (IOException e) {
            log.error("Error al guardar la imagen para usuario {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Error al guardar la imagen: " + e.getMessage(), e);
        }
    }
}
