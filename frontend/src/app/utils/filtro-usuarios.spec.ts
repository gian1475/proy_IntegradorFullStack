import { etiquetaPerfil, filtrarUsuarios } from './filtro-usuarios';

// RF12 y RF13: el admin busca al usuario que va a editar o dar de baja
describe('filtro de la pestaña Usuarios (RF12 y RF13)', () => {
  const usuarios = [
    { nombre: 'María', apellido: 'López', dni: '44444444', correo: 'maria@alejandria.edu.pe', perfil: 'ENCARGADO' },
    { nombre: 'Valeria', apellido: 'Flores', dni: '73456783', correo: 'valeria@gmail.com', perfil: 'PRESTATARIO' },
    { nombre: 'José', apellido: 'Ramírez', dni: '70000001', correo: 'jose@gmail.com', perfil: 'PRESTATARIO' },
  ];

  it('sin filtros muestra a todos', () => {
    expect(filtrarUsuarios(usuarios, 'TODOS', '  ')).toHaveLength(3);
  });

  it('filtra por perfil', () => {
    expect(filtrarUsuarios(usuarios, 'ENCARGADO', '').map(u => u.nombre)).toEqual(['María']);
    expect(filtrarUsuarios(usuarios, 'PRESTATARIO', '')).toHaveLength(2);
  });

  it('busca por nombre o apellido aunque se escriba sin tilde', () => {
    expect(filtrarUsuarios(usuarios, 'TODOS', 'jose ramirez').map(u => u.dni)).toEqual(['70000001']);
    expect(filtrarUsuarios(usuarios, 'TODOS', 'lopez').map(u => u.dni)).toEqual(['44444444']);
  });

  it('busca por DNI y por correo', () => {
    expect(filtrarUsuarios(usuarios, 'TODOS', '7345').map(u => u.nombre)).toEqual(['Valeria']);
    expect(filtrarUsuarios(usuarios, 'TODOS', 'GMAIL')).toHaveLength(2);
  });

  it('combina el perfil y el texto', () => {
    expect(filtrarUsuarios(usuarios, 'ENCARGADO', 'valeria')).toHaveLength(0);
  });

  it('muestra el perfil con el nombre del informe', () => {
    expect(etiquetaPerfil('ENCARGADO')).toBe('Encargado');
    expect(etiquetaPerfil('PRESTATARIO')).toBe('Prestatario');
  });
});
