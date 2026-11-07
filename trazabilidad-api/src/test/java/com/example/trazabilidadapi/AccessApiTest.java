package com.example.trazabilidadapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Modulos contratados (SUPERADMIN), permisos por rol y usuarios (ADMIN del cliente). */
class AccessApiTest extends IntegrationTestBase {

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(role)).contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder withToken(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON);
    }

    private void setModule(String code, boolean enabled) throws Exception {
        mvc.perform(as("SUPERADMIN", put("/api/plataforma/modulos/" + code)).content("{\"enabled\":" + enabled + "}"))
                .andExpect(status().isNoContent());
    }

    private void setPermission(String role, String code, String access) throws Exception {
        mvc.perform(as("ADMIN", put("/api/admin/permisos/" + role + "/" + code)).content("{\"access\":\"" + access + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eachUserGetsTheAccessOfItsRole() throws Exception {
        mvc.perform(as("OPERARIO", get("/api/me/permisos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OPERARIO"))
                .andExpect(jsonPath("$.modules[?(@.moduleCode == 'PEDIDOS')].access").value("VER"))
                .andExpect(jsonPath("$.modules[?(@.moduleCode == 'INVENTARIO')].access").value("NINGUNO"));
        mvc.perform(as("ADMIN", get("/api/me/permisos")))
                .andExpect(jsonPath("$.modules[?(@.moduleCode == 'HISTORIAL')].access").value("GESTIONAR"));
    }

    @Test
    void aModuleNotContractedIsClosedEvenForTheAdmin() throws Exception {
        try {
            setModule("PEDIDOS", false);
            mvc.perform(as("ADMIN", get("/api/pedidos"))).andExpect(status().isForbidden());
            mvc.perform(as("ADMIN", get("/api/me/permisos")))
                    .andExpect(jsonPath("$.modules[?(@.moduleCode == 'PEDIDOS')].access").value("NINGUNO"));
        } finally {
            setModule("PEDIDOS", true);
        }
        mvc.perform(as("ADMIN", get("/api/pedidos"))).andExpect(status().isOk());
    }

    @Test
    void theClientAdminDecidesWhatEachRoleCanDo() throws Exception {
        try {
            setPermission("OPERARIO", "PEDIDOS", "NINGUNO");
            mvc.perform(as("OPERARIO", get("/api/pedidos"))).andExpect(status().isForbidden());

            // VER deja consultar el inventario, pero no cambiarlo
            setPermission("OPERARIO", "INVENTARIO", "VER");
            mvc.perform(as("OPERARIO", get("/api/productos"))).andExpect(status().isOk());
            mvc.perform(as("OPERARIO", post("/api/productos")).content("{\"sku\":\"X\",\"name\":\"X\"}"))
                    .andExpect(status().isForbidden());
        } finally {
            setPermission("OPERARIO", "PEDIDOS", "VER");
            setPermission("OPERARIO", "INVENTARIO", "NINGUNO");
        }
        mvc.perform(as("OPERARIO", get("/api/pedidos"))).andExpect(status().isOk());
    }

    @Test
    void eachPanelOnlyOpensForItsRole() throws Exception {
        mvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(as("ENCARGADO", get("/api/admin/usuarios"))).andExpect(status().isForbidden());
        mvc.perform(as("ADMIN", get("/api/plataforma/modulos"))).andExpect(status().isForbidden());
        mvc.perform(as("ADMIN", get("/api/admin/usuarios"))).andExpect(status().isOk());
        mvc.perform(as("SUPERADMIN", get("/api/admin/usuarios"))).andExpect(status().isOk());
        mvc.perform(as("SUPERADMIN", get("/api/plataforma/modulos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        // El panel del cliente no puede dar permisos a su propio rol ni inventar niveles
        mvc.perform(as("ADMIN", put("/api/admin/permisos/ADMIN/PEDIDOS")).content("{\"access\":\"NINGUNO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(as("ADMIN", put("/api/admin/permisos/OPERARIO/PEDIDOS")).content("{\"access\":\"TODO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theAdminCreatesAndDeactivatesUsers() throws Exception {
        String user = "{\"username\":\"nuevo_op\",\"name\":\"Nuevo Operario\",\"role\":\"OPERARIO\",\"password\":\"%s\"}";
        mvc.perform(as("ADMIN", post("/api/admin/usuarios")).content(user.formatted("corta"))).andExpect(status().isBadRequest());
        mvc.perform(as("ADMIN", post("/api/admin/usuarios"))
                        .content(user.replace("OPERARIO", "SUPERADMIN").formatted("larga-segura")))
                .andExpect(status().isBadRequest());
        int id = JsonPath.read(mvc.perform(as("ADMIN", post("/api/admin/usuarios")).content(user.formatted("larga-segura")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$");
        mvc.perform(as("ADMIN", post("/api/admin/usuarios")).content(user.formatted("larga-segura"))).andExpect(status().isConflict());

        // El usuario nuevo entra con su contraseña
        String login = "{\"username\":\"nuevo_op\",\"password\":\"larga-segura\"}";
        String token = "Bearer " + JsonPath.read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
        mvc.perform(withToken(token, get("/api/pedidos"))).andExpect(status().isOk());

        // Al desactivarlo pierde el acceso al momento, aunque su token no haya caducado
        mvc.perform(as("ADMIN", put("/api/admin/usuarios/" + id)).content("{\"active\":false}")).andExpect(status().isNoContent());
        mvc.perform(withToken(token, get("/api/pedidos"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isUnauthorized());
    }

    @Test
    void theAdminCannotLockHimselfOut() throws Exception {
        String token = bearer("ADMIN");
        int id = jdbc.queryForObject("SELECT user_id FROM app_user WHERE username = 'test_admin'", Integer.class);
        mvc.perform(withToken(token, put("/api/admin/usuarios/" + id)).content("{\"active\":false}")).andExpect(status().isConflict());
        mvc.perform(withToken(token, put("/api/admin/usuarios/" + id)).content("{\"role\":\"OPERARIO\"}")).andExpect(status().isConflict());
        // Los usuarios SUPERADMIN no se tocan desde el panel del cliente
        int superadmin = jdbc.queryForObject("SELECT user_id FROM app_user WHERE username = 'superadmin'", Integer.class);
        mvc.perform(withToken(token, put("/api/admin/usuarios/" + superadmin)).content("{\"active\":false}")).andExpect(status().isBadRequest());
    }
}
