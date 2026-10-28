/**
 * RF03: el estado agregado de un título con el nombre que ve el usuario, y la búsqueda del catálogo
 * por título, autor, categoría o código (ISBN).
 */

// consulta disponibilidad (front): el estado de la bd se muestra con el nombre q usa el RF03
const ETIQUETAS: Record<string, string> = {
  disponible: 'Disponible',
  prestado: 'Prestado en sala',
  reservado: 'Reservado',
  mantenimiento: 'En mantenimiento',
  baja: 'Dado de baja',
};

export function etiquetaEstado(estado: string): string {
  return ETIQUETAS[estado] ?? estado;
}

/** Lo que se puede buscar de un libro. autores: todos los nombres juntos. */
export interface LibroBuscable {
  titulo: string;
  autores: string;
  categoria: string;
  isbn: string;
}

/** Minúsculas y sin tildes: "Tecnología" y "tecnologia" son lo mismo al buscar. */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
}

// consulta disponibilidad (front): un solo texto busca en titulo, autores, categoria e isbn (con o sin guiones)
export function coincideBusqueda(termino: string, libro: LibroBuscable): boolean {
  const t = normalizar(termino);
  if (!t) return true;
  const sinGuiones = t.replace(/-/g, '');
  return normalizar(libro.titulo).includes(t)
    || normalizar(libro.autores).includes(t)
    || normalizar(libro.categoria).includes(t)
    || (sinGuiones.length > 0 && libro.isbn.replace(/-/g, '').includes(sinGuiones));
}
