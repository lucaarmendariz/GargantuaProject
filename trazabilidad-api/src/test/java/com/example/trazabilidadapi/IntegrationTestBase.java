package com.example.trazabilidadapi;

import com.example.trazabilidadapi.models.AppUser;
import com.example.trazabilidadapi.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.MountableFile;

/** Levanta una MariaDB real con los scripts del proyecto. Solo necesita Docker. */
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    @SuppressWarnings("resource")
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("traceability_lean")
            .withUsername("trazabilidad_app")
            .withPassword("test-password")
            .withCopyFileToContainer(MountableFile.forHostPath("database/01_schema.sql"), "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("database/02_procedures.sql"), "/docker-entrypoint-initdb.d/02_procedures.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("database/03_seeds.sql"), "/docker-entrypoint-initdb.d/03_seeds.sql");

    static {
        DB.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::getJdbcUrl);
        registry.add("spring.datasource.username", DB::getUsername);
        registry.add("spring.datasource.password", DB::getPassword);
        // Si se pierden conexiones, el test falla en 2 s en vez de en 30 s
        registry.add("spring.datasource.hikari.connection-timeout", () -> "2000");
        registry.add("app.jwt.secret", () -> "clave-solo-para-tests-de-al-menos-32-caracteres");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:5173");
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    protected String bearer(String role) {
        AppUser user = AppUser.builder().username("test").name("Usuario Test").role(role).build();
        return "Bearer " + jwtService.issueToken(user);
    }
}
