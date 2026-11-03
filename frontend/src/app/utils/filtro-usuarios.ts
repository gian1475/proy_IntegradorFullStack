/**
 * RF12 y RF13: filtro de la tabla "Usuarios" del admin, por perfil y por un texto que busca
 * en nombre, apellido, DNI y correo (sin importar mayúsculas ni tildes).
 */

export type FiltroPerfil = 'TODOS' | 'PRESTATARIO' | 'ENCARGADO';

/** Lo que se puede buscar de un usuario. */
export interface UsuarioBuscable {
  nombre: string;
  apellido: string;
  dni: string;
  correo: string;
  perfil: string;
}

const PERFILES: Record<string, string> = {
  PRESTATARIO: 'Prestatario',
  ENCARGADO: 'Encargado',
};

export function etiquetaPerfil(perfil: string): string {
  return PERFILES[perfil] ?? perfil;
}

/** Minúsculas y sin tildes: "Ramírez" y "ramirez" son lo mismo al buscar. */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
}

// gestion usuarios (front): primero el perfil y despues el texto en nombre completo, dni o correo
export function filtrarUsuarios<T extends UsuarioBuscable>(usuarios: T[], perfil: FiltroPerfil, texto: string): T[] {
  const t = normalizar(texto);
  return usuarios.filter(u =>
    (perfil === 'TODOS' || u.perfil === perfil)
    && (!t
      || normalizar(`${u.nombre} ${u.apellido}`).includes(t)
      || u.dni.includes(t)
      || normalizar(u.correo).includes(t)));
}
