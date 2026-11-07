package com.example.trazabilidadapi.config;

import com.example.trazabilidadapi.util.SqlScriptSplitter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SOLO DESARROLLO (perfil "dev"): deja la base de datos recién creada y con los datos de prueba.
 *
 * <ul>
 *   <li>Al arrancar la API, antes de atender peticiones, borra todas las tablas y ejecuta
 *       01_schema.sql, 02_procedures.sql, 03_seeds.sql y 04_demo_data.sql.</li>
 *   <li>Mientras la API está en marcha vigila esos scripts y, si alguno cambia, repite el reinicio.</li>
 * </ul>
 *
 * Por seguridad solo actúa contra una base de datos local (localhost, 127.0.0.1 o la del equipo
 * vista desde Docker). Se desactiva con {@code app.dev.reset-db=false}.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.dev.reset-db", havingValue = "true", matchIfMissing = true)
public class DevDatabaseReset {

    private static final Logger log = LoggerFactory.getLogger(DevDatabaseReset.class);

    static final List<String> SCRIPTS = List.of(
            "01_schema.sql", "02_procedures.sql", "03_seeds.sql", "04_demo_data.sql");

    private static final Set<String> LOCAL_HOSTS = Set.of(
            "localhost", "127.0.0.1", "::1", "[::1]", "host.docker.internal");

    private final DataSource dataSource;
    private final Path scriptsDir;
    private String lastFingerprint;

    public DevDatabaseReset(DataSource dataSource,
                            @Value("${app.dev.db-scripts-dir:database}") String scriptsDir) {
        this.dataSource = dataSource;
        this.scriptsDir = Path.of(scriptsDir).toAbsolutePath().normalize();
    }

    /** Se ejecuta al crear el bean: antes de que arranquen el servidor web y las tareas programadas. */
    @PostConstruct
    void resetOnStartup() {
        for (String script : SCRIPTS) {
            if (!Files.isRegularFile(scriptsDir.resolve(script))) {
                throw new IllegalStateException("Perfil dev: no se encuentra " + scriptsDir.resolve(script)
                        + ". Arranca la API desde la carpeta trazabilidad-api o ajusta app.dev.db-scripts-dir.");
            }
        }
        lastFingerprint = fingerprint();
        reset("arranque de la API");
    }

    /** Vigila los scripts: si alguno cambia (y deja de cambiar), reinicia la base de datos. */
    @Scheduled(initialDelayString = "${app.dev.db-watch-ms:3000}", fixedDelayString = "${app.dev.db-watch-ms:3000}")
    void resetWhenScriptsChange() {
        String current = fingerprint();
        if (current.equals(lastFingerprint)) {
            return;
        }
        // Espera a que el editor termine de guardar antes de ejecutar nada
        sleep(500);
        if (!current.equals(fingerprint())) {
            return;
        }
        lastFingerprint = current;
        try {
            reset("cambio en los scripts de database/");
        } catch (RuntimeException e) {
            // Un error en un script no tumba la API: se avisa y se espera al siguiente cambio
            log.error("Perfil dev: no se pudo reiniciar la base de datos tras el cambio. Corrige el script y guárdalo de nuevo.", e);
        }
    }

    synchronized void reset(String reason) {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            checkIsLocal(conn.getMetaData().getURL());
            log.warn("Perfil dev: reiniciando la base de datos ({}). Se borran todos los datos.", reason);
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                for (String script : SCRIPTS) {
                    runScript(stmt, script);
                }
                // 01_schema.sql desactiva las claves foráneas al borrar; se dejan como estaban
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Perfil dev: falló el reinicio de la base de datos", e);
        }
        log.warn("Perfil dev: base de datos reiniciada con los datos de prueba en {} ms.", System.currentTimeMillis() - start);
    }

    private void runScript(Statement stmt, String script) throws SQLException {
        String sql = read(scriptsDir.resolve(script));
        List<String> statements = SqlScriptSplitter.split(sql);
        for (int i = 0; i < statements.size(); i++) {
            try {
                stmt.execute(statements.get(i));
            } catch (SQLException e) {
                throw new SQLException(script + ", sentencia " + (i + 1) + " de " + statements.size()
                        + ": " + e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
            }
        }
        log.info("Perfil dev: {} ejecutado ({} sentencias)", script, statements.size());
    }

    /** Fecha de modificación y tamaño de cada script. Se compara por sondeo porque en Docker sobre
     *  Windows los cambios en carpetas montadas no generan eventos del sistema de ficheros. */
    private String fingerprint() {
        List<String> parts = new ArrayList<>();
        for (String script : SCRIPTS) {
            Path path = scriptsDir.resolve(script);
            try {
                parts.add(script + ":" + Files.getLastModifiedTime(path).toMillis() + ":" + Files.size(path));
            } catch (IOException e) {
                parts.add(script + ":ausente");
            }
        }
        return String.join("|", parts);
    }

    static void checkIsLocal(String jdbcUrl) {
        String host = hostOf(jdbcUrl);
        if (host == null || !LOCAL_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Perfil dev: la base de datos " + jdbcUrl
                    + " no es local; no se reinicia. Quita el perfil dev para usar esta base de datos.");
        }
    }

    static String hostOf(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:")) {
            return null;
        }
        try {
            return URI.create(jdbcUrl.substring(5)).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Perfil dev: no se pudo leer " + path, e);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
