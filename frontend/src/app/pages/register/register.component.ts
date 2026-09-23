import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { HeaderComponent } from '../../components/header/header.component';

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

  onRegister(): void {
    if (!this.nombre || !this.apellido || !this.dni || !this.correo || !this.contrasena) {
      this.errorMessage.set('Por favor completa todos los campos');
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
