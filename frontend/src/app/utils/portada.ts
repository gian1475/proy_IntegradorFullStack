/** Imagen que se muestra si el libro no tiene portada en Open Library. */
export const PORTADA_POR_DEFECTO =
  'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=400&q=80';

// portadas: la imagen real del libro sale de Open Library (catalogo abierto de libros) usando su ISBN.
// no se guarda nada en la bd. "default=false" hace q responda error si no hay portada, y ahi se usa la de defecto
export function portadaPorIsbn(isbn: string | null | undefined): string {
  return isbn ? `https://covers.openlibrary.org/b/isbn/${isbn}-M.jpg?default=false` : PORTADA_POR_DEFECTO;
}

/** Para el (error) de la etiqueta <img>: si la portada no cargó, pone la de defecto. */
export function usarPortadaPorDefecto(evento: Event): void {
  const imagen = evento.target as HTMLImageElement;
  if (imagen.src !== PORTADA_POR_DEFECTO) {
    imagen.src = PORTADA_POR_DEFECTO;
  }
}
