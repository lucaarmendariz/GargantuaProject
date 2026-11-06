package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.models.Product;
import com.example.trazabilidadapi.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    // 1. Obtener los productos activos (Para la app de planta diaria)
    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return repository.findAll();
    }

    // 2. NUEVO - Obtener todo el catálogo (Incluye inactivos para administración)
    @Transactional(readOnly = true)
    public List<Product> getAllProductsForAdmin() {
        return repository.findAllAdmin();
    }

    // 3. Buscar un producto específico por su ID único
    @Transactional(readOnly = true)
    public Product getProductById(int id) {
        return repository.findById(id);
    }

    // 4. Registrar un nuevo producto maestro utilizando el parámetro OUT
    @Transactional(rollbackFor = Exception.class)
    public Integer insertProduct(Product product) throws SQLException {
        product.setGtin(normalizeGtin(product.getGtin()));
        return repository.insertProduct(product);
    }

    // 5. NUEVO - Actualizar datos del producto maestro (SKU, Nombre, Descripción, Estado, código de barras)
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Product product) {
        product.setGtin(normalizeGtin(product.getGtin()));
        repository.updateProduct(product);
    }

    /**
     * Código de barras comercial a 14 dígitos (formato GTIN-14), como llega en el (01) de las
     * etiquetas GS1. Así un EAN-13 "8412345678905" y su GTIN-14 "08412345678905" son el mismo.
     * Vacío = sin código.
     */
    public static String normalizeGtin(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{8}|\\d{12}|\\d{13}|\\d{14}")) {
            throw new IllegalArgumentException("El código de barras debe tener 8, 12, 13 o 14 dígitos.");
        }
        return "0".repeat(14 - digits.length()) + digits;
    }

    // 6. NUEVO - Eliminar producto (Baja lógica mediante el procedimiento Product_Delete)
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(int id) {
        repository.deleteProduct(id);
    }
}