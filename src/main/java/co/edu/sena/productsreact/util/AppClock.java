package co.edu.sena.productsreact.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Centraliza la hora "actual" del sistema en la zona horaria de Colombia,
 * para evitar que el servidor (que corre en UTC en Render) guarde
 * marcas de tiempo desfasadas respecto a la hora local del cliente.
 */
public final class AppClock {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private AppClock() {
    }

    public static LocalDateTime nowBogota() {
        return LocalDateTime.now(BOGOTA);
    }
}
