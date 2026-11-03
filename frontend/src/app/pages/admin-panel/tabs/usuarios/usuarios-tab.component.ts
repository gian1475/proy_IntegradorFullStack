import { Component, ElementRef, Injector, OnInit, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActualizarUsuario, RegistroEncargado, UsuarioAdmin, UsuarioService } from '../../../../services/usuario.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';
import { contrasenaValida, requisitosContrasena } from '../../../../utils/regla-contrasena';
import { FiltroPerfil, etiquetaPerfil, filtrarUsuarios } from '../../../../utils/filtro-usuarios';

/**
 * Pestaña "Usuarios" del admin: registrar encargados (RF09), editar los datos de un prestatario
 * o encargado (RF12) y darlo de baja o reactivarlo (RF13, eliminación lógica: estado 'inactivo').
 */
@Component({
  selector: 'app-usuarios-tab',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './usuarios-tab.component.html',
})
export class UsuariosTabComponent implements OnInit {
  private usuarioService = inject(UsuarioService);
  private injector = inject(Injector);

  /** Campo "Nombres" del formulario de edición: al pulsar Editar se pone el cursor ahí (D10). */
  private campoNombre = viewChild<ElementRef<HTMLInputElement>>('campoNombre');

  readonly usuarios = signal<UsuarioAdmin[]>([]);
  readonly cargando = signal(true);
  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');
  readonly guardando = signal(false);

  // filtros de la tabla
  readonly filtroPerfil = signal<FiltroPerfil>('TODOS');
  readonly busqueda = signal('');
  readonly usuariosFiltrados = computed(() => filtrarUsuarios(this.usuarios(), this.filtroPerfil(), this.busqueda()));

  readonly etiquetaPerfil = etiquetaPerfil;

  form: RegistroEncargado = this.formularioVacio();

  /** RF12: usuario que se está editando (null = no se edita a nadie). */
  edicion: (ActualizarUsuario & { idUsuario: number; perfil: string }) | null = null;

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  cargarUsuarios(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: data => {
        this.usuarios.set(data);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.mensajeError.set(mensajeDeError(err));
      }
    });
  }

  requisitos(): { texto: string; cumple: boolean }[] {
    return requisitosContrasena(this.form.contrasena);
  }

  // crear perfil 2 (front): mismas validaciones del registro (dni, correo y regla contra) + el turno
  registrar(): void {
    this.limpiarMensajes();
    const f = this.form;
    if (!f.nombre.trim() || !f.apellido.trim() || !f.dni.trim() || !f.correo.trim() || !f.contrasena) {
      this.mensajeError.set('Complete todos los campos');
      return;
    }
    const error = this.errorDniCorreo(f.dni, f.correo);
    if (error) {
      this.mensajeError.set(error);
      return;
    }
    if (!contrasenaValida(f.contrasena)) {
      this.mensajeError.set('La contraseña no cumple los requisitos de seguridad');
      return;
    }

    this.guardando.set(true);
    this.usuarioService.registrarEncargado({ ...f, nombre: f.nombre.trim(), apellido: f.apellido.trim(), dni: f.dni.trim(), correo: f.correo.trim() }).subscribe({
      next: nuevo => {
        this.guardando.set(false);
        this.mensajeOk.set(`Encargado ${nuevo.nombre} ${nuevo.apellido} registrado (turno ${nuevo.turno}). Ya puede iniciar sesión con su correo.`);
        this.form = this.formularioVacio();
        this.cargarUsuarios();
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        this.mensajeError.set(mensajeDeError(err));
      }
    });
  }

  // ---------- RF12: editar ----------

  editar(u: UsuarioAdmin): void {
    this.limpiarMensajes();
    this.edicion = { idUsuario: u.idUsuario, perfil: u.perfil, nombre: u.nombre, apellido: u.apellido,
                     dni: u.dni, correo: u.correo, turno: u.turno };
    // el formulario esta arriba de la tabla: despues de dibujarlo se pone el cursor en "Nombres"
    // y el navegador baja/sube solo hasta ahi (focus hace el scroll)
    afterNextRender(() => this.campoNombre()?.nativeElement.focus(), { injector: this.injector });
  }

  cancelarEdicion(): void {
    this.edicion = null;
  }

  // editar usuario (front): mismas validaciones del registro; el backend vuelve a revisar y ve q el dni y el correo no se repitan
  guardarEdicion(): void {
    const e = this.edicion;
    if (!e) return;
    this.limpiarMensajes();
    if (!e.nombre.trim() || !e.apellido.trim() || !e.dni.trim() || !e.correo.trim()) {
      this.mensajeError.set('Complete todos los campos');
      return;
    }
    const error = this.errorDniCorreo(e.dni, e.correo);
    if (error) {
      this.mensajeError.set(error);
      return;
    }

    const datos: ActualizarUsuario = { nombre: e.nombre.trim(), apellido: e.apellido.trim(), dni: e.dni.trim(),
                                       correo: e.correo.trim(), turno: e.perfil === 'ENCARGADO' ? e.turno : null };
    this.guardando.set(true);
    this.usuarioService.actualizar(e.idUsuario, datos).subscribe({
      next: u => {
        this.guardando.set(false);
        this.edicion = null;
        this.mensajeOk.set(`Datos de ${u.nombre} ${u.apellido} actualizados.`);
        this.cargarUsuarios();
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        this.mensajeError.set(mensajeDeError(err));
      }
    });
  }

  // ---------- RF13: dar de baja y reactivar ----------

  // baja logica (front): se confirma y el backend lo deja 'inactivo' (no se borra). ya no puede iniciar sesion
  darDeBaja(u: UsuarioAdmin): void {
    const extra = u.perfil === 'PRESTATARIO' ? ' Sus reservas pendientes o en cola se cancelarán.' : '';
    if (!confirm(`¿Dar de baja a ${u.nombre} ${u.apellido}? No podrá iniciar sesión; su historial se conserva.${extra}`)) return;
    this.limpiarMensajes();
    this.usuarioService.darDeBaja(u.idUsuario).subscribe({
      next: () => {
        this.mensajeOk.set(`Se dio de baja a ${u.nombre} ${u.apellido} (estado inactivo).`);
        this.cargarUsuarios();
      },
      error: (err: HttpErrorResponse) => this.mensajeError.set(mensajeDeError(err))
    });
  }

  reactivar(u: UsuarioAdmin): void {
    if (!confirm(`¿Reactivar a ${u.nombre} ${u.apellido}? Podrá volver a iniciar sesión.`)) return;
    this.limpiarMensajes();
    this.usuarioService.reactivar(u.idUsuario).subscribe({
      next: r => {
        this.mensajeOk.set(`Se reactivó a ${r.nombre} ${r.apellido} (estado ${r.estado}).`);
        this.cargarUsuarios();
      },
      error: (err: HttpErrorResponse) => this.mensajeError.set(mensajeDeError(err))
    });
  }

  /** Mismas reglas del DNI y del correo que en el registro. Vacío si están bien. */
  private errorDniCorreo(dni: string, correo: string): string {
    if (!/^\d{8}$/.test(dni.trim())) return 'El DNI debe tener 8 dígitos';
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(correo.trim())) return 'El correo no es válido';
    return '';
  }

  private limpiarMensajes(): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
  }

  private formularioVacio(): RegistroEncargado {
    return { nombre: '', apellido: '', dni: '', correo: '', contrasena: '', turno: 'mañana' };
  }
}
