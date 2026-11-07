package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.AppModule;
import com.example.trazabilidadapi.models.ModuleAccess;
import com.example.trazabilidadapi.models.RolePermission;
import com.example.trazabilidadapi.models.UserSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        return query("{call Access_GetForUser(?)}", stmt -> stmt.setString(1, username),
                rs -> new AccessRow(rs.getString("role"),
                        new ModuleAccess(rs.getString("module_code"), rs.getString("name"), rs.getString("access"))));
    }

    public List<AppModule> getModules() {
        return query("{call Module_GetAll()}", stmt -> { },
                rs -> new AppModule(rs.getString("module_code"), rs.getString("name"), rs.getBoolean("is_enabled")));
    }

    public void setModuleEnabled(String moduleCode, boolean enabled) {
        execute("{call Module_SetEnabled(?, ?)}", stmt -> {
            stmt.setString(1, moduleCode);
            stmt.setBoolean(2, enabled);
        });
    }

    public List<RolePermission> getPermissions() {
        return query("{call RolePermission_GetAll()}", stmt -> { },
                rs -> new RolePermission(rs.getString("role"), rs.getString("module_code"), rs.getString("access")));
    }

    public void setPermission(String role, String moduleCode, String access) {
        execute("{call RolePermission_Set(?, ?, ?)}", stmt -> {
            stmt.setString(1, role);
            stmt.setString(2, moduleCode);
            stmt.setString(3, access);
        });
    }

    public List<UserSummary> getUsers() {
        return query("{call AppUser_GetAll()}", stmt -> { },
                rs -> new UserSummary(rs.getInt("user_id"), rs.getString("username"), rs.getString("name"),
                        rs.getString("role"), rs.getBoolean("is_active"), rs.getObject("created_at", Long.class)));
    }

    public int createUser(String username, String passwordHash, String name, String role) {
        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall("{call AppUser_Create(?, ?, ?, ?, ?)}")) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, name);
            stmt.setString(4, role);
            stmt.registerOutParameter(5, Types.INTEGER);
            stmt.execute();
            return stmt.getInt(5);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public void updateUser(int userId, String role, boolean active) {
        execute("{call AppUser_Update(?, ?, ?)}", stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, role);
            stmt.setBoolean(3, active);
        });
    }

    // --- Ejecucion de procedimientos ---

    private interface Params {
        void apply(CallableStatement stmt) throws SQLException;
    }

    private interface Row<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private <T> List<T> query(String sql, Params params, Row<T> row) {
        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            params.apply(stmt);
            List<T> result = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(row.map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    private void execute(String sql, Params params) {
        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            params.apply(stmt);
            stmt.execute();
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    // Errores de negocio de los procedimientos (130-133): el mensaje se muestra al usuario
    private static RuntimeException translate(SQLException e) {
        String message = String.valueOf(e.getMessage()).replaceFirst("^\\(conn=\\d+\\)\\s*", "");
        return switch (e.getErrorCode()) {
            case 132 -> new IllegalStateException(message);
            case 130, 131, 133 -> new IllegalArgumentException(message);
            default -> new RuntimeException("Error en la base de datos de permisos", e);
        };
    }
}
