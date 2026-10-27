import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { HeaderComponent } from '../../components/header/header.component';
import { contrasenaValida, requisitosContrasena } from '../../utils/regla-contrasena';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HeaderComponent],
  templateUrl: './register.component.html',
  styleUrl: './register.component.less',
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  nombre: string = '';
  apellido: string = '';
  dni: string = '';
  correo: string = '';
  contrasena: string = '';
  confirmarContrasena: string = '';

  errorMessage = signal<string>('');
  cargando = signal<boolean>(false);

  /** Misma regla que ValidadorContrasena del backend (los criterios están en utils/regla-contrasena.ts). */
  // regla contra (front): los mismos 5 criterios del backend, se van marcando mientras escribe
  requisitosContrasena(): { texto: string; cumple: boolean }[] {
    return requisitosContrasena(this.contrasena);
  }

  contrasenaValida(): boolean {
    return contrasenaValida(this.contrasena);
  }

  // crear perfil 3 (front): el formulario de registro siempre crea un prestatario
  onRegister(): void {
    if (!this.nombre.trim() || !this.apellido.trim() || !this.dni || !this.correo || !this.contrasena) {
      this.errorMessage.set('Por favor completa todos los campos');
      return;
    }

    // validar datos (front): las mismas reglas de ValidadorUsuario del backend
    if (!/^\d{8}$/.test(this.dni.trim())) {
      this.errorMessage.set('El DNI debe tener 8 dígitos');
      return;
    }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(this.correo.trim())) {
      this.errorMessage.set('El correo no es válido');
      return;
    }

    // regla contra: si falta algun criterio no deja enviar
    if (!this.contrasenaValida()) {
      this.errorMessage.set('La contraseña no cumple los requisitos de seguridad');
      return;
    }

    if (this.contrasena !== this.confirmarContrasena) {
      this.errorMessage.set('Las contraseñas no coinciden');
      return;
    }

    this.cargando.set(true);
    this.errorMessage.set('');

    this.authService.registro({
      nombre: this.nombre.trim(),
      apellido: this.apellido.trim(),
      dni: this.dni.trim(),
      correo: this.correo.trim(),
      contrasena: this.contrasena
    }).subscribe({
      next: () => {
        this.cargando.set(false);
        this.router.navigate(['/catalogo']);
      },
      error: (err) => {
        this.cargando.set(false);
        const msg = err.error?.mensaje || 'Error al registrar usuario';
        this.errorMessage.set(msg);
      }
    });
  }
}
