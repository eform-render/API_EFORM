package co.edu.sena.productsreact.config;

import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileSystemConfig {

    private static final String UPLOADS_DIR = "uploads/";

    @PostConstruct
    public void createUploadsDirectory() {
        try {
            Path uploadsPath = Paths.get(UPLOADS_DIR).toAbsolutePath();
            if (!Files.exists(uploadsPath)) {
                Files.createDirectories(uploadsPath);
                System.out.println("Directorio de uploads creado: " + uploadsPath);
            }
        } catch (Exception e) {
            System.err.println("Error creando directorio de uploads: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
