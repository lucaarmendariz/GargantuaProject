package com.example.trazabilidadapi.controllers;

import com.example.trazabilidadapi.models.AppModule;
import com.example.trazabilidadapi.models.AppUser;
import com.example.trazabilidadapi.models.CreateUserRequest;
import com.example.trazabilidadapi.models.RolePermission;
import com.example.trazabilidadapi.models.UpdateUserRequest;
import com.example.trazabilidadapi.models.UserProfile;
import com.example.trazabilidadapi.models.UserSummary;
import com.example.trazabilidadapi.services.AccessService;
import com.example.trazabilidadapi.services.AccessService.UserAccess;
import com.example.trazabilidadapi.services.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Permisos del usuario conectado (/api/me), panel del cliente (/api/admin, ADMIN)
 * y panel del proveedor (/api/plataforma, SUPERADMIN). Los roles se comprueban en SecurityConfig.
 */
@RestController
@RequestMapping("/api")
public class AccessController {

    private final AccessService service;
    private final JwtService jwtService;

    public AccessController(AccessService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    /** Lo que el usuario conectado puede hacer: el front muestra u oculta modulos y botones con esto. */
    @GetMapping("/me/permisos")
    public UserAccess myAccess(@AuthenticationPrincipal Jwt jwt) {
        return service.forUser(jwt.getSubject());
    }

    @GetMapping("/me/perfil")
    public UserProfile profile(@AuthenticationPrincipal Jwt jwt) {
        return service.getProfile(jwt.getSubject());
    }

    /** Foto de quien firma un movimiento de la trazabilidad (la ve cualquier usuario que haya entrado). */
    @GetMapping("/usuarios/{username}/foto")
    public Map<String, String> avatar(@PathVariable String username) {
        return Collections.singletonMap("avatar", service.getAvatar(username));
    }

    /** Perfil guardado y un token nuevo, que lleva el nombre que se ve en la cabecera. */
    public record SavedProfile(UserProfile perfil, String token) {
    }

    /** Cambiar el propio nombre y la foto: {"name": "...", "avatar": "data:image/jpeg;base64,..." o null} */
    @PutMapping("/me/perfil")
    public SavedProfile updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> body) {
        UserProfile profile = service.updateProfile(jwt.getSubject(), body.get("name"), body.get("avatar"));
        AppUser user = AppUser.builder().username(profile.username()).name(profile.name()).role(profile.role()).build();
        return new SavedProfile(profile, jwtService.issueToken(user));
    }

    /** Cambiar la propia contraseña: {"actual": "...", "nueva": "..."} */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> body) {
        service.changePassword(jwt.getSubject(), body.get("actual"), body.get("nueva"));
        return ResponseEntity.noContent().build();
    }

    // --- Panel del proveedor ---

    @GetMapping("/plataforma/modulos")
    public List<AppModule> modules() {
        return service.getModules();
    }

    @PutMapping("/plataforma/modulos/{moduleCode}")
    public ResponseEntity<Void> setModule(@PathVariable String moduleCode, @RequestBody Map<String, Boolean> body) {
        service.setModuleEnabled(moduleCode, body.get("enabled"));
        return ResponseEntity.noContent().build();
    }

    // --- Panel del cliente ---

    @GetMapping("/admin/permisos")
    public List<RolePermission> permissions() {
        return service.getPermissions();
    }

    @PutMapping("/admin/permisos/{role}/{moduleCode}")
    public ResponseEntity<Void> setPermission(@PathVariable String role, @PathVariable String moduleCode,
                                              @RequestBody Map<String, String> body) {
        service.setPermission(role, moduleCode, body.get("access"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/usuarios")
    public List<UserSummary> users() {
        return service.getUsers();
    }

    @PostMapping("/admin/usuarios")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createUser(request));
    }

    @PutMapping("/admin/usuarios/{userId}")
    public ResponseEntity<Void> updateUser(@PathVariable int userId, @RequestBody UpdateUserRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        service.updateUser(userId, request, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    // Reglas de negocio incumplidas: el mensaje se muestra tal cual en el panel
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
