import { describe, expect, it } from 'vitest';
import { normalizeGtin, parseLabel, parseWrittenDate } from '../utils/labelParser';

const products = [
  { productId: 5, sku: 'GAR-HUE-001' },
  { productId: 3, sku: 'MON-OIL-003' },
];

describe('lectura de etiquetas de lotes', () => {
  it('interpreta un GS1-128 en forma legible', () => {
    const result = parseLabel('(01)08412345678905(17)261231(10)LOTE-2611A', { products });
    expect(result).toMatchObject({ format: 'GS1', lotNumber: 'LOTE-2611A', expirationDate: '2026-12-31', gtin: '08412345678905' });
  });

  it('interpreta un GS1 crudo con separador FNC1 y día 00 (fin de mes)', () => {
    const raw = ']C1010841234567890517270200' + '10ABC123\u001d' + '3750';
    expect(parseLabel(raw)).toMatchObject({ format: 'GS1', lotNumber: 'ABC123', expirationDate: '2027-02-28', quantity: 50 });
  });

  it('interpreta un QR con JSON y reconoce el producto', () => {
    const raw = '{"lote":"LOT-HUE-2611-C","caducidad":"2026-11-30","sku":"gar-hue-001","albaran":"ALB-GRANJA-1200","cantidad":360}';
    expect(parseLabel(raw, { products })).toMatchObject({
      format: 'JSON', lotNumber: 'LOT-HUE-2611-C', expirationDate: '2026-11-30',
      productId: 5, sku: 'GAR-HUE-001', supplierReference: 'ALB-GRANJA-1200', quantity: 360,
    });
  });

  it('saca los campos de un texto leído por OCR, con tildes y minúsculas', () => {
    const raw = 'Aceite de oliva MON-OIL-003\nLote: 2610-a   Cad: 31/12/27\nAlbarán ALB-JAEN-905  Cantidad: 200';
    expect(parseLabel(raw, { products, source: 'ocr' })).toMatchObject({
      format: 'OCR', lotNumber: '2610-A', expirationDate: '2027-12-31',
      supplierReference: 'ALB-JAEN-905', quantity: 200, productId: 3,
    });
  });

  it('entiende el formato de los registros OCR de planta', () => {
    const raw = 'OCR_RAW: MON-CHE-001 || LOTE: 05-19A || CAD: 31-12-2026';
    expect(parseLabel(raw, { source: 'ocr' })).toMatchObject({ lotNumber: '05-19A', expirationDate: '2026-12-31' });
  });

  it('un código suelto es el número de lote, salvo que sea un SKU', () => {
    expect(parseLabel('LOT-2026-05-19A')).toMatchObject({ format: 'codigo', lotNumber: 'LOT-2026-05-19A' });
    expect(parseLabel('GAR-HUE-001', { products })).toMatchObject({ productId: 5 });
    expect(parseLabel('GAR-HUE-001', { products }).lotNumber).toBeUndefined();
  });

  it('reconoce el producto por el código de barras (GTIN) de la etiqueta GS1', () => {
    const catalog = [...products, { productId: 9, sku: 'GAR-PAT-002', gtin: '08412345678905' }];
    expect(parseLabel('(01)08412345678905(10)L1', { products: catalog })).toMatchObject({ productId: 9, sku: 'GAR-PAT-002', lotNumber: 'L1' });
    // Sin producto con ese código: se devuelve el GTIN para poder asociarlo después
    const unknown = parseLabel('(01)08400000000007(10)L2', { products: catalog });
    expect(unknown.productId).toBeUndefined();
    expect(unknown.gtin).toBe('08400000000007');
  });

  it('un EAN-13 suelto es el código del producto, no el número de lote', () => {
    const catalog = [{ productId: 9, sku: 'GAR-PAT-002', gtin: '08412345678905' }];
    expect(parseLabel('8412345678905', { products: catalog })).toMatchObject({ productId: 9, gtin: '08412345678905' });
    expect(parseLabel('8412345678905', { products: catalog }).lotNumber).toBeUndefined();
  });

  it('normaliza códigos de barras a 14 dígitos', () => {
    expect(normalizeGtin('8 412345 678905')).toBe('08412345678905');
    expect(normalizeGtin('12345670')).toBe('00000012345670');
    expect(normalizeGtin('LOT-1')).toBeNull();
    expect(normalizeGtin('')).toBeNull();
  });

  it('no inventa datos de un texto sin información', () => {
    const result = parseLabel('Hola que tal', { source: 'ocr' });
    expect(result.lotNumber).toBeUndefined();
    expect(result.expirationDate).toBeUndefined();
  });

  it('lee fechas escritas de varias formas y descarta las imposibles', () => {
    expect(parseWrittenDate('31.12.2026')).toBe('2026-12-31');
    expect(parseWrittenDate('2026-03-05')).toBe('2026-03-05');
    expect(parseWrittenDate('02/2028')).toBe('2028-02-29');
    expect(parseWrittenDate('31/02/2026')).toBeNull();
  });
});
