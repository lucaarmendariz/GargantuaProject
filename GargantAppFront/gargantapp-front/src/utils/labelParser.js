// Interpreta lo que lee la cámara en la etiqueta de un lote y lo convierte en
// campos del formulario. Admite:
//   - Códigos GS1-128 / GS1 DataMatrix: (01) GTIN, (10) lote, (17) caducidad, (37) cantidad...
//   - QR con JSON: {"lote": "...", "caducidad": "2026-12-31", "sku": "..."}
//   - Texto (QR o leído por OCR): "LOTE: 2611-A  CAD: 31/12/2026  ALB-7761"
//   - Un código sin más (Code128, EAN...): se toma como número de lote.
// Nunca se guarda nada solo: el usuario revisa los campos antes de dar de alta.

import { normalizeText } from './search';

const GS = '\u001d'; // Separador FNC1 de los códigos GS1

// AIs de GS1 que nos interesan: longitud fija o variable (hasta el separador)
const GS1_FIXED = { '01': 14, '02': 14, '11': 6, '13': 6, '15': 6, '17': 6 };
const GS1_VARIABLE = ['10', '21', '30', '37', '240'];

const pad = (n) => String(n).padStart(2, '0');

function isValidDate(year, month, day) {
  const date = new Date(year, month - 1, day);
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
}

function toInputDate(year, month, day) {
  if (year < 100) year += 2000;
  // Día 0 en GS1 o fechas solo con mes: último día de ese mes
  if (!day) day = new Date(year, month, 0).getDate();
  return isValidDate(year, month, day) ? `${year}-${pad(month)}-${pad(day)}` : null;
}

// AAMMDD de GS1 -> AAAA-MM-DD
function gs1Date(value) {
  const match = /^(\d{2})(\d{2})(\d{2})$/.exec(value);
  return match ? toInputDate(Number(match[1]), Number(match[2]), Number(match[3])) : null;
}

// Fechas escritas: 31/12/2026, 31-12-26, 31.12.2026, 2026-12-31, 12/2026
export function parseWrittenDate(text) {
  let match = /\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b/.exec(text);
  if (match) return toInputDate(Number(match[1]), Number(match[2]), Number(match[3]));
  match = /\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2}|\d{4})\b/.exec(text);
  if (match) return toInputDate(Number(match[3]), Number(match[2]), Number(match[1]));
  match = /\b(\d{1,2})[-/.](\d{4})\b/.exec(text);
  if (match) return toInputDate(Number(match[2]), Number(match[1]), 0);
  return null;
}

const normalize = (text) => normalizeText(text).toUpperCase();

function parseGs1(raw) {
  let text = raw.replace(/^\][A-Za-z]\d/, ''); // Prefijo de simbología (]C1, ]d2...)
  const fields = {};

  if (/^\(\d{2,4}\)/.test(text)) {
    // Forma legible: (01)08412345678905(17)261231(10)LOTE1
    for (const match of text.matchAll(/\((\d{2,4})\)([^(]*)/g)) {
      fields[match[1]] = match[2].replace(GS, '').trim();
    }
  } else {
    // Forma cruda: AIs seguidos; los de longitud variable terminan en el separador
    while (text.length >= 2) {
      const ai = Object.keys(GS1_FIXED).find((key) => text.startsWith(key))
        || GS1_VARIABLE.find((key) => text.startsWith(key));
      if (!ai) break;
      text = text.slice(ai.length);
      if (GS1_FIXED[ai]) {
        fields[ai] = text.slice(0, GS1_FIXED[ai]);
        text = text.slice(GS1_FIXED[ai]);
      } else {
        const end = text.indexOf(GS);
        fields[ai] = end === -1 ? text : text.slice(0, end);
        text = end === -1 ? '' : text.slice(end + 1);
      }
      if (text.startsWith(GS)) text = text.slice(1);
    }
  }

  const result = {};
  if (fields['10']) result.lotNumber = fields['10'];
  const expiry = gs1Date(fields['17'] || fields['15'] || '');
  if (expiry) result.expirationDate = expiry;
  const quantity = fields['37'] || fields['30'];
  if (quantity && /^\d+$/.test(quantity)) result.quantity = Number(quantity);
  if (fields['01'] || fields['02']) result.gtin = fields['01'] || fields['02'];
  return result;
}

function looksLikeGs1(raw) {
  return /^(\][A-Za-z]\d)?\(\d{2,4}\)/.test(raw)
    || raw.includes(GS)
    || /^(\][A-Za-z]\d)?0[12]\d{14}(10|11|13|15|17|21|30|37)/.test(raw);
}

function parseJson(raw) {
  let data;
  try {
    data = JSON.parse(raw);
  } catch {
    return null;
  }
  if (!data || typeof data !== 'object' || Array.isArray(data)) return null;
  const lower = Object.fromEntries(Object.entries(data).map(([key, value]) => [key.toLowerCase(), value]));
  const pick = (...keys) => keys.map((key) => lower[key]).find((value) => value !== undefined && value !== null && value !== '');

  const result = {};
  const lot = pick('lotnumber', 'lote', 'lot', 'batch');
  if (lot) result.lotNumber = String(lot).trim();
  const expiry = pick('expirationdate', 'caducidad', 'cad', 'exp', 'expiry');
  if (expiry) result.expirationDate = parseWrittenDate(String(expiry));
  const sku = pick('sku', 'producto', 'product');
  if (sku) result.sku = String(sku).trim().toUpperCase();
  const supplier = pick('supplierreference', 'albaran', 'proveedor', 'referencia');
  if (supplier) result.supplierReference = String(supplier).trim();
  const quantity = pick('quantity', 'cantidad', 'qty');
  if (quantity !== undefined && !Number.isNaN(Number(quantity))) result.quantity = Number(quantity);
  if (!result.expirationDate) delete result.expirationDate;
  return result;
}

const LOT_TOKEN = '([A-Z0-9][A-Z0-9\\-/.]{0,39}[A-Z0-9]|[A-Z0-9])';

function parseText(raw) {
  const text = normalize(raw);
  const result = {};

  // Lote: "LOTE: X", "LOT X", "L: X", "Nº LOTE X"
  const lot = new RegExp(`(?:\\bLOTE|\\bLOT|\\bBATCH|\\bL(?=\\s*[:.]))\\s*(?:N[O°º]?\\.?\\s*)?[:.#]?\\s*${LOT_TOKEN}`).exec(text);
  if (lot) result.lotNumber = lot[1];

  // Caducidad: lo que sigue a CAD/EXP/CONSUMIR ANTES...; si no, la primera fecha que aparezca
  const expiryLabel = /(?:\bCAD(?:UCIDAD)?|\bEXP(?:IRY)?|\bF\.?\s*CAD|CONSUMIR\s+(?:PREFERENTEMENTE\s+)?ANTES\s+(?:DEL?\s+)?|BEST\s+BEFORE)\s*[:.]?\s*(.{0,20})/.exec(text);
  const expiry = parseWrittenDate(expiryLabel ? expiryLabel[1] : text);
  if (expiry) result.expirationDate = expiry;

  // Albarán / referencia del proveedor
  const supplier = /\b(ALB-[A-Z0-9-]+)/.exec(text) || /\bALBARAN\s*(?:N[O°º]?\.?)?\s*[:#]?\s*([A-Z0-9][A-Z0-9-]*)/.exec(text);
  if (supplier) result.supplierReference = supplier[1];

  // SKU escrito
  const sku = /\bSKU\s*[:#]?\s*([A-Z0-9][A-Z0-9-]+)/.exec(text);
  if (sku) result.sku = sku[1];

  // Cantidad
  const quantity = /\b(?:CANT(?:IDAD)?|QTY|UDS|UNIDADES|PESO\s+NETO)\s*[:.]?\s*(\d+(?:[.,]\d+)?)/.exec(text);
  if (quantity) result.quantity = Number(quantity[1].replace(',', '.'));

  return result;
}

// Código de barras comercial (EAN-8/13, UPC-12, GTIN-14) a 14 dígitos, como lo guarda la API.
// Devuelve null si no es un código válido.
export function normalizeGtin(value) {
  const digits = String(value ?? '').replace(/[\s-]/g, '');
  return /^(\d{8}|\d{12}|\d{13}|\d{14})$/.test(digits) ? digits.padStart(14, '0') : null;
}

// Busca el producto del catálogo por su código de barras (GTIN) o por su SKU escrito en la etiqueta
function findProduct(raw, result, products) {
  const gtin = normalizeGtin(result.gtin);
  const byGtin = gtin && products.find((product) => normalizeGtin(product.gtin) === gtin);
  if (byGtin) return byGtin;
  const text = normalize(raw);
  const bySku = (sku) => products.find((product) => normalize(product.sku) === normalize(sku));
  const product = (result.sku && bySku(result.sku))
    || products.find((item) => item.sku && text.includes(normalize(item.sku)));
  return product || null;
}

/**
 * @param {string} raw texto leído por el escáner o el OCR
 * @param {{ products?: Array<{productId:number, sku:string}>, source?: 'barcode'|'ocr' }} options
 * @returns {{ lotNumber?, expirationDate?, supplierReference?, quantity?, productId?, sku?, gtin?, format, raw }}
 */
export function parseLabel(raw, { products = [], source = 'barcode' } = {}) {
  const text = String(raw ?? '').trim();
  if (!text) return { format: 'vacio', raw: text };

  let result;
  let format;
  if (looksLikeGs1(text)) {
    result = parseGs1(text);
    format = 'GS1';
  } else if ((result = parseJson(text))) {
    format = 'JSON';
  } else {
    result = parseText(text);
    format = source === 'ocr' ? 'OCR' : 'texto';
    // Un código suelto sin etiquetas: si son solo cifras de EAN/UPC es el código de
    // barras del producto; si coincide con un SKU, el producto; si no, el número de lote
    const single = /^[A-Za-z0-9][A-Za-z0-9\-/.]{1,59}$/.test(text);
    if (source !== 'ocr' && single && Object.keys(result).length === 0) {
      const asProduct = products.find((product) => normalize(product.sku) === normalize(text));
      if (normalizeGtin(text)) result.gtin = normalizeGtin(text);
      else if (asProduct) result.sku = asProduct.sku;
      else result.lotNumber = text;
      format = 'codigo';
    }
  }

  const product = findProduct(text, result, products);
  if (product) {
    result.productId = product.productId;
    result.sku = product.sku;
  }
  return { ...result, format, raw: text };
}
