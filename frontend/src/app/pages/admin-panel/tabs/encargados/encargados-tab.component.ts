import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Encargado, RegistroEncargado, UsuarioService } from '../../../../services/usuario.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';
import { contrasenaValida, requisitosContrasena } from '../../../../utils/regla-contrasena';

/** Pestaña "Encargados" del admin: registrar encargados desde el panel (RF09) y ver los que hay. */
@Component({
  selector: 'app-encargados-tab',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './encargados-tab.component.html',
})
export class EncargadosTabComponent implements OnInit {
  private usuarioService = inject(UsuarioService);

  readonly encargados = signal<Encargado[]>([]);
  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');
  readonly guardando = signal(false);

  form: RegistroEncargado = this.formularioVacio();

  ngOnInit(): void {
    this.cargarEncargados();
  }

  cargarEncargados(): void {
    this.usuarioService.listarEncargados().subscribe({
      next: data => this.encargados.set(data),
      error: (err: HttpErrorResponse) => this.mensajeError.set(mensajeDeError(err))
    });
  }

  requisitos(): { texto: string; cumple: boolean }[] {
    return requisitosContrasena(this.form.contrasena);
  }

  // crear perfil 2 (front): mismas validaciones del registro (dni, correo y regla contra) + el turno
  registrar(): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
    const f = this.form;
    if (!f.nombre.trim() || !f.apellido.trim() || !f.dni.trim() || !f.correo.trim() || !f.contrasena) {
      this.mensajeError.set('Complete todos los campos');
      return;
    }
    if (!/^\d{8}$/.test(f.dni.trim())) {
      this.mensajeError.set('El DNI debe tener 8 dígitos');
      return;
    }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.correo.trim())) {
      this.mensajeError.set('El correo no es válido');
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
        this.cargarEncargados();
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        this.mensajeError.set(mensajeDeError(err));
      }
    });
  }

  private formularioVacio(): RegistroEncargado {
    return { nombre: '', apellido: '', dni: '', correo: '', contrasena: '', turno: 'mañana' };
  }
}
