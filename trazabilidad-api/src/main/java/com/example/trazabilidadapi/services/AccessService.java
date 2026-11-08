package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.models.AppModule;
import com.example.trazabilidadapi.models.CreateUserRequest;
import com.example.trazabilidadapi.models.ModuleAccess;
import com.example.trazabilidadapi.models.RolePermission;
import com.example.trazabilidadapi.models.UpdateUserRequest;
import com.example.trazabilidadapi.models.UserSummary;
import com.example.trazabilidadapi.repositories.AccessRepository;
import com.example.trazabilidadapi.repositories.AccessRepository.AccessRow;
import com.example.trazabilidadapi.models.AppUser;
import com.example.trazabilidadapi.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.trazabilidadapi.models.UserProfile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Set;

/**
 * Permisos de la app. Se leen de la base de datos en cada peticion, asi que desactivar a un
 * usuario, cambiarle el rol o apagar un modulo surte efecto al momento, sin esperar a que caduque su token.
 */
@Service
public class AccessService {

    public static final String VER = "VER";
    public static final String GESTIONAR = "GESTIONAR";
    private static final List<String> LEVELS = List.of("NINGUNO", VER, GESTIONAR);
    private static final Set<String> CLIENT_ROLES = Set.of("ADMIN", "ENCARGADO", "OPERARIO");
    private static final int MIN_PASSWORD = 8;

    private final AccessRepository repository;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccessService(AccessRepository repository, UserRepository users, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    // Solo imagenes de mapa de bits en base64 (no SVG: puede llevar codigo). El front las reduce a 256 px.
    private static final Pattern AVATAR = Pattern.compile("^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/]+={0,2}$");
    private static final int MAX_AVATAR = 300_000;

    public UserProfile getProfile(String username) {
        return repository.getProfile(username);
    }

    /** El propio usuario cambia su nombre y su foto (avatar null = quitar la foto). */
    public UserProfile updateProfile(String username, String name, String avatar) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 100) {
            throw new IllegalArgumentException("El nombre es obligatorio y admite 100 caracteres.");
        }
        if (avatar != null && (avatar.length() > MAX_AVATAR || !AVATAR.matcher(avatar).matches())) {
            throw new IllegalArgumentException("La foto debe ser una imagen JPEG, PNG o WebP de menos de 200 KB.");
        }
        repository.updateProfile(username, trimmed, avatar);
        return repository.getProfile(username);
    }

    /** El propio usuario cambia su contraseña: tiene que saber la actual. */
    public void changePassword(String username, String current, String newPassword) {
        AppUser user = users.findByUsername(username);
        if (user == null || current == null || !passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        checkPassword(newPassword);
        repository.setPassword(username, passwordEncoder.encode(newPassword));
    }

    // BCrypt solo tiene en cuenta los primeros 72 bytes: una contraseña mas larga no seria la que se cree
    private static void checkPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres (y como mucho 72).");
        }
    }

    /** Acceso de un usuario activo. null si no existe o esta desactivado. */
    public record UserAccess(String role, List<ModuleAccess> modules) {

        public boolean hasRole(String... roles) {
            return List.of(roles).contains(role);
        }

        /** Tiene al menos ese nivel en alguno de los modulos indicados. */
        public boolean can(String level, String... moduleCodes) {
            List<String> codes = List.of(moduleCodes);
            return modules.stream().anyMatch(m -> codes.contains(m.moduleCode())
                    && LEVELS.indexOf(m.access()) >= LEVELS.indexOf(level));
        }
    }

    public UserAccess forUser(String username) {
        List<AccessRow> rows = repository.accessForUser(username);
        if (rows.isEmpty()) return null;
        return new UserAccess(rows.get(0).role(), rows.stream().map(AccessRow::module).toList());
    }

    // --- Panel del proveedor (SUPERADMIN) ---

    public List<AppModule> getModules() {
        return repository.getModules();
    }

    public void setModuleEnabled(String moduleCode, Boolean enabled) {
        if (enabled == null) throw new IllegalArgumentException("Indica si el módulo está activo.");
        repository.setModuleEnabled(moduleCode, enabled);
    }

    // --- Panel del cliente (ADMIN) ---

    public List<RolePermission> getPermissions() {
        return repository.getPermissions();
    }

    public void setPermission(String role, String moduleCode, String access) {
        repository.setPermission(role, moduleCode, access);
    }

    public List<UserSummary> getUsers() {
        return repository.getUsers();
    }

    public int createUser(CreateUserRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String name = request.name() == null ? "" : request.name().trim();
        if (username.isEmpty() || name.isEmpty()) {
            throw new IllegalArgumentException("El usuario y el nombre son obligatorios.");
        }
        // Tamaños de las columnas de app_user
        if (username.length() > 50 || name.length() > 100) {
            throw new IllegalArgumentException("El usuario admite 50 caracteres y el nombre 100.");
        }
        checkPassword(request.password());
        return repository.createUser(username, passwordEncoder.encode(request.password()), name, request.role());
    }

    /** El administrador no puede desactivarse ni quitarse el rol a si mismo: se quedaria fuera. */
    public void updateUser(int userId, UpdateUserRequest request, String currentUsername) {
        UserSummary user = repository.getUsers().stream().filter(u -> u.userId() == userId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        String role = request.role() == null ? user.role() : request.role();
        boolean active = request.active() == null ? user.active() : request.active();
        if (!CLIENT_ROLES.contains(role)) {
            throw new IllegalArgumentException("El rol debe ser ADMIN, ENCARGADO u OPERARIO.");
        }
        if (user.username().equals(currentUsername) && (!active || !role.equals(user.role()))) {
            throw new IllegalStateException("No puedes desactivarte ni cambiarte el rol a ti mismo.");
        }
        repository.updateUser(userId, role, active);
    }
}
