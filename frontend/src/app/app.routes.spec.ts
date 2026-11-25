import { routes } from './app.routes';

// D19: las pantallas de cada perfil se descargan recien cuando alguien entra a ellas (lazy loading de Angular).
// asi el prestatario no descarga el codigo de los paneles del admin y del encargado, y el paquete inicial pesa menos.
// los guards (autorizacion) siguen igual: se revisan ANTES de descargar la pantalla
describe('rutas: los paneles se cargan al entrar (D19)', () => {
  const protegidas: [string, string][] = [
    ['catalogo', 'PRESTATARIO'],
    ['admin', 'ADMINISTRADOR'],
    ['encargado', 'ENCARGADO'],
  ];

  for (const [ruta] of protegidas) {
    it(`/${ruta} se carga con loadComponent y mantiene su guard`, async () => {
      const r = routes.find(x => x.path === ruta)!;
      expect(r.component).toBeUndefined();        // ya no va en el paquete inicial
      expect(r.canActivate?.length).toBe(1);      // el guard del perfil sigue ahi
      const componente = await r.loadComponent!();
      expect(componente).toBeTruthy();            // la pantalla se puede cargar
    });
  }

  it('el inicio, el login y el registro siguen en el paquete inicial (son la primera pantalla)', () => {
    for (const ruta of ['', 'login', 'registro']) {
      expect(routes.find(x => x.path === ruta)!.component).toBeDefined();
    }
  });
});
