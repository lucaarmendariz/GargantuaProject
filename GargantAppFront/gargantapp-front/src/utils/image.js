// Recorta la foto al cuadrado central y la reduce a size x size en JPEG: unos pocos KB,
// que caben en la base de datos y en el límite de la API.
export async function resizeToSquare(file, size = 256) {
  if (!file.type.startsWith('image/')) {
    throw new Error('El fichero no es una imagen.');
  }
  const bitmap = await createImageBitmap(file);
  const side = Math.min(bitmap.width, bitmap.height);
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  canvas.getContext('2d').drawImage(
    bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, size, size,
  );
  bitmap.close();
  return canvas.toDataURL('image/jpeg', 0.85);
}
