package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.AppModule;
import com.example.trazabilidadapi.models.ModuleAccess;
import com.example.trazabilidadapi.models.RolePermission;
import com.example.trazabilidadapi.models.UserSummary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.function.Supplier;

/** Modulos contratados, permisos por rol y usuarios del cliente (procedimientos del bloque 5). */
@Repository
public class AccessRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Una fila de Access_GetForUser: el rol del usuario y su acceso a un modulo. */
    public record AccessRow(String role, ModuleAccess module) {
    }

    /** Rol del usuario y su acceso a cada modulo. Lista vacia si no existe o esta desactivado. */
    public List<AccessRow> accessForUser(String username) {
        return run(() -> jdbcTemplate.query("CALL Access_GetForUser(?)", (rs, i) -> new AccessRow(rs.getString("role"),
                new ModuleAccess(rs.getString("module_code"), rs.getString("name"), rs.getString("access"))), username));
    }

    public List<AppModule> getModules() {
        return run(() -> jdbcTemplate.query("CALL Module_GetAll()", (rs, i) ->
                new AppModule(rs.getString("module_code"), rs.getString("name"), rs.getBoolean("is_enabled"))));
    }

    public void setModuleEnabled(String moduleCode, boolean enabled) {
        run(() -> jdbcTemplate.update("CALL Module_SetEnabled(?, ?)", moduleCode, enabled));
    }

    public List<RolePermission> getPermissions() {
        return run(() -> jdbcTemplate.query("CALL RolePermission_GetAll()", (rs, i) ->
                new RolePermission(rs.getString("role"), rs.getString("module_code"), rs.getString("access"))));
    }

    public void setPermission(String role, String moduleCode, String access) {
        run(() -> jdbcTemplate.update("CALL RolePermission_Set(?, ?, ?)", role, moduleCode, access));
    }

    public List<UserSummary> getUsers() {
        return run(() -> jdbcTemplate.query("CALL AppUser_GetAll()", (rs, i) ->
                new UserSummary(rs.getInt("user_id"), rs.getString("username"), rs.getString("name"),
                        rs.getString("role"), rs.getBoolean("is_active"), rs.getObject("created_at", Long.class))));
    }

    public int createUser(String username, String passwordHash, String name, String role) {
        return run(() -> jdbcTemplate.execute("{call AppUser_Create(?, ?, ?, ?, ?)}", (CallableStatement stmt) -> {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, name);
            stmt.setString(4, role);
            stmt.registerOutParameter(5, Types.INTEGER);
            stmt.execute();
            return stmt.getInt(5);
        }));
    }

    public void updateUser(int userId, String role, boolean active) {
        run(() -> jdbcTemplate.update("CALL AppUser_Update(?, ?, ?)", userId, role, active));
    }

    // Errores de negocio de los procedimientos (130-133): el mensaje se muestra al usuario
    private static <T> T run(Supplier<T> call) {
        try {
            return call.get();
        } catch (DataAccessException e) {
            if (!(e.getMostSpecificCause() instanceof SQLException sql)) throw e;
            String message = String.valueOf(sql.getMessage()).replaceFirst("^\\(conn=\\d+\\)\\s*", "");
            throw switch (sql.getErrorCode()) {
                case 132 -> new IllegalStateException(message);
                case 130, 131, 133 -> new IllegalArgumentException(message);
                default -> e;
            };
        }
    }
}
