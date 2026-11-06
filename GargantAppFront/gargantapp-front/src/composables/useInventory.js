import { ref, computed } from 'vue';
import apiClient from '../api';
import { errorMessage } from '../utils/errors';

export function useInventory() {
  const products = ref([]);
  const lots = ref([]);
  const loading = ref(true);
  const searchQuery = ref('');

  // Carga el catálogo completo usando la ruta de administración para poder ver e interactuar con los inactivos
  const loadProducts = async () => {
    try {
      products.value = await apiClient.get('/productos/admin');
    } catch (error) {
      console.error("Error al cargar el inventario industrial", error);
    }
  };

  const loadProductLots = async () => {
    try {
      lots.value = await apiClient.get('/lotes');
    } catch (error) {
      console.error("Error al cargar los lotes", error);
    } finally {
      loading.value = false;
    }
  };

  const initInventory = async () => {
    loading.value = true;
    await Promise.all([loadProducts(), loadProductLots()]);
  };

  const getProductLots = (productId) => {
    return lots.value.filter(lot => lot.productId === productId);
  };

  // Desactivar un producto (Baja lógica)
  const deleteProduct = async (productId) => {
    try {
      await apiClient.delete(`/productos/${productId}`);
      await loadProducts(); // Recargamos catálogo
    } catch (error) {
      alert("Error al dar de baja el producto maestro.");
    }
  };

 // Modificar o Reactivar un producto
// Devuelve true si se guardó
const updateProduct = async (productData) => {
  try {
    await apiClient.put(`/productos/${productData.productId}`, productData);
    await loadProducts();
    return true;
  } catch (error) {
    console.error("Fallo al actualizar el producto:", error);
    alert(`Error al actualizar la ficha: ${await errorMessage(error, 'No se pudo guardar el producto.')}`);
    return false;
  }
};

  const filteredProducts = computed(() => {
    const query = searchQuery.value.toLowerCase().trim();
    if (!query) return products.value;

    return products.value.filter(prod => 
      prod.sku?.toLowerCase().includes(query) ||
      prod.name?.toLowerCase().includes(query) ||
      prod.description?.toLowerCase().includes(query) ||
      prod.gtin?.includes(query.replace(/[\s-]/g, ''))
    );
  });

  return {
    products,
    lots,
    loading,
    searchQuery,
    initInventory,
    loadProducts,
    loadProductLots,
    getProductLots,
    deleteProduct,
    updateProduct,
    filteredProducts
  };
}