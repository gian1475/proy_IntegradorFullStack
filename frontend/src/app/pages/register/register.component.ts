import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
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
  aceptoTerminos: boolean = false;

  errorMessage = signal<string>('');
  cargando = signal<boolean>(false);

  onRegister(): void {
    if (!this.nombre || !this.apellido || !this.dni || !this.correo || !this.contrasena) {
      this.errorMessage.set('Por favor completa todos los campos requeridos');
      return;
    }

    if (this.dni.length !== 8) {
      this.errorMessage.set('El DNI debe contener exactamente 8 dígitos');
      return;
    }

    if (this.contrasena !== this.confirmarContrasena) {
      this.errorMessage.set('Las contraseñas no coinciden');
      return;
    }

    this.cargando.set(true);
    this.errorMessage.set('');

    this.authService.registro({
      nombre: this.nombre,
      apellido: this.apellido,
      dni: this.dni,
      correo: this.correo,
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
