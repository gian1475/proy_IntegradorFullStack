import { coincideBusqueda, etiquetaEstado } from './estado-libro';

// RF03: consultar el stock y el estado agregado de un título, buscando por título, autor, categoría o código
describe('consulta de disponibilidad (RF03)', () => {
  const cleanCode = {
    titulo: 'Clean Code',
    autores: 'Robert Martin',
    categoria: 'Tecnología',
    isbn: '9780132350884',
  };

  it('muestra el estado con los nombres del RF03', () => {
    expect(etiquetaEstado('disponible')).toBe('Disponible');
    expect(etiquetaEstado('prestado')).toBe('Prestado en sala');
    expect(etiquetaEstado('reservado')).toBe('Reservado');
    expect(etiquetaEstado('mantenimiento')).toBe('En mantenimiento');
  });

  it('busca por título y por autor', () => {
    expect(coincideBusqueda('clean', cleanCode)).toBe(true);
    expect(coincideBusqueda('martin', cleanCode)).toBe(true);
  });

  it('busca por categoría aunque se escriba sin tilde', () => {
    expect(coincideBusqueda('tecnologia', cleanCode)).toBe(true);
  });

  it('busca por código ISBN con o sin guiones', () => {
    expect(coincideBusqueda('9780132350884', cleanCode)).toBe(true);
    expect(coincideBusqueda('978-0132350884', cleanCode)).toBe(true);
  });

  it('sin texto muestra todo y con otro texto no coincide', () => {
    expect(coincideBusqueda('  ', cleanCode)).toBe(true);
    expect(coincideBusqueda('rayuela', cleanCode)).toBe(false);
  });
});
