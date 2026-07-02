package co.edu.sena.productsreact.config;

import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileSystemConfig {

    @PostConstruct
    public void createUploadsDirectory() {
        try {
            // Usar directorio relativo al working directory
            Path uploadsPath = Paths.get("uploads").toAbsolutePath();
            if (!Files.exists(uploadsPath)) {
                Files.createDirectories(uploadsPath);
                System.out.println("✓ Directorio de uploads creado en: " + uploadsPath);
            } else {
                System.out.println("✓ Directorio de uploads ya existe en: " + uploadsPath);
            }
        } catch (Exception e) {
            System.err.println("✗ Error creando directorio de uploads: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
