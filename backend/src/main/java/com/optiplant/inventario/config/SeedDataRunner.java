package com.optiplant.inventario.config;

import com.optiplant.inventario.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Carga los datos de prueba del dominio "ferretería" (Sección 1, regla global
 * 4: `docker compose up` debe quedar listo sin pasos manuales). Ya no es una
 * migración Flyway porque, a diferencia del resto de {@code db/migration},
 * este script es contenido de demo/QA opcional, no un cambio de esquema: se
 * puede querer levantar el sistema sin él (bases limpias para pruebas
 * automatizadas, entornos donde los datos de ejemplo estorban), algo que
 * Flyway no permite desactivar por migración individual.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    private static final String SEED_SCRIPT_LOCATION = "classpath:seed/ferreteria.sql";

    /** Primer usuario que siembra el script; su presencia indica que ya se cargó. */
    private static final String MARKER_EMAIL = "gerente.central@ferreteria.local";

    private final UsuarioRepository usuarioRepository;
    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByEmail(MARKER_EMAIL)) {
            log.info("Datos de prueba ya presentes (usuario '{}' existe); se omite la siembra.", MARKER_EMAIL);
            return;
        }

        log.info("Cargando datos de prueba desde {}", SEED_SCRIPT_LOCATION);
        Resource resource = resourceLoader.getResource(SEED_SCRIPT_LOCATION);
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, resource);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron cargar los datos de prueba (" + SEED_SCRIPT_LOCATION + ")", ex);
        }
        log.info("Datos de prueba cargados correctamente.");
    }
}
