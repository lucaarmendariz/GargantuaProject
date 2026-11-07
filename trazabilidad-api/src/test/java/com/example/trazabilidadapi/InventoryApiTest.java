package com.example.trazabilidadapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryApiTest extends IntegrationTestBase {

    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON);
    }

    private int createProduct(String sku) throws Exception {
        String id = mvc.perform(admin(post("/api/productos"))
                        .content("{\"sku\":\"" + sku + "\",\"name\":\"Prueba\",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Integer.parseInt(id);
    }

    @Test
    void repeatedRequestsDoNotExhaustConnections() throws Exception {
        for (int i = 0; i < 30; i++) {
            mvc.perform(admin(get("/api/historial/lote/1"))).andExpect(status().isOk());
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"luca\",\"password\":\"mala\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void lotKeepsDecimalsAndEpochSeconds() throws Exception {
        int productId = createProduct("TEST-DECIMALES");
        String lotId = mvc.perform(admin(post("/api/lotes")).content(
                        "{\"productId\":" + productId + ",\"lotNumber\":\"TEST-LOTE-DEC\",\"quantity\":2.5,"
                                + "\"measureUnit\":\"KILOS\",\"expirationDate\":4102444800,\"supplierReference\":\"x\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(admin(get("/api/lotes/" + lotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", closeTo(2.5, 0.0001)))
                .andExpect(jsonPath("$.expirationDate").value(4102444800L))
                .andExpect(jsonPath("$.createdAt").isNumber());
    }

    @Test
    void statusReturnsEpochSecondsInUtc() throws Exception {
        // Dato de prueba: 2026-05-19 14:00:00 UTC
        mvc.perform(admin(get("/api/trazabilidad/lotes/LOT-2026-05-19A/status")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastUpdate").isNumber())
                .andExpect(jsonPath("$.initialQuantity", closeTo(50.0, 0.0001)));
        String body = mvc.perform(admin(get("/api/historial/lote/1"))).andReturn().getResponse().getContentAsString();
        Object timestamps = JsonPath.read(body, "$[?(@.actionType == 'VENDIDO')].actionTimestamp");
        org.junit.jupiter.api.Assertions.assertTrue(timestamps.toString().contains("1779199200"), timestamps.toString());
    }

    @Test
    void missingLotReturns404() throws Exception {
        mvc.perform(admin(get("/api/trazabilidad/lotes/NO-EXISTE/status"))).andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/historial/lote/99999"))).andExpect(status().isNotFound());
    }

    @Test
    void duplicatedSkuReturns400() throws Exception {
        createProduct("TEST-DUPLICADO");
        mvc.perform(admin(post("/api/productos")).content("{\"sku\":\"TEST-DUPLICADO\",\"name\":\"Otro\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productCanBeEditedAndDeactivated() throws Exception {
        int id = createProduct("TEST-EDITAR");
        mvc.perform(admin(put("/api/productos/" + id)).content(
                        "{\"productId\":" + id + ",\"sku\":\"TEST-EDITAR\",\"name\":\"Editado\",\"description\":\"d\",\"active\":true}"))
                .andExpect(status().isOk());
        mvc.perform(admin(delete("/api/productos/" + id))).andExpect(status().isNoContent());
        mvc.perform(admin(get("/api/productos/" + id)))
                .andExpect(jsonPath("$.name").value("Editado"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void productBarcodeIsNormalizedAndUnique() throws Exception {
        // Un EAN-13 se guarda como GTIN-14, igual que llega en el (01) de las etiquetas GS1
        String id = mvc.perform(admin(post("/api/productos"))
                        .content("{\"sku\":\"TEST-GTIN-1\",\"name\":\"Con código\",\"gtin\":\"8 412345 678912\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(admin(get("/api/productos/" + id))).andExpect(jsonPath("$.gtin").value("08412345678912"));

        // El mismo código en otro producto: conflicto, también si se escribe con 14 dígitos
        mvc.perform(admin(post("/api/productos")).content("{\"sku\":\"TEST-GTIN-2\",\"name\":\"Otro\",\"gtin\":\"08412345678912\"}"))
                .andExpect(status().isConflict());
        int other = createProduct("TEST-GTIN-3");
        mvc.perform(admin(put("/api/productos/" + other)).content(
                        "{\"productId\":" + other + ",\"sku\":\"TEST-GTIN-3\",\"name\":\"Otro\",\"active\":true,\"gtin\":\"8412345678912\"}"))
                .andExpect(status().isConflict());

        // Formato no válido y código vacío (sin código de barras)
        mvc.perform(admin(post("/api/productos")).content("{\"sku\":\"TEST-GTIN-4\",\"name\":\"Mal\",\"gtin\":\"12AB\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(put("/api/productos/" + id)).content(
                        "{\"productId\":" + id + ",\"sku\":\"TEST-GTIN-1\",\"name\":\"Con código\",\"active\":true,\"gtin\":\"\"}"))
                .andExpect(status().isOk());
        mvc.perform(admin(get("/api/productos/" + id))).andExpect(jsonPath("$.gtin").doesNotExist());
    }
}
