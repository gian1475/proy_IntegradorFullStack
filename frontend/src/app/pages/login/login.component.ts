import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.less',
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  email: string = '';
  password: string = '';
  errorMessage = signal<string>('');
  cargando = signal<boolean>(false);

  onLogin(): void {
    if (!this.email || !this.password) {
      this.errorMessage.set('Por favor ingrese correo y contraseña');
      return;
    }

    this.cargando.set(true);
    this.errorMessage.set('');

    this.authService.login(this.email, this.password).subscribe({
      next: () => {
        this.cargando.set(false);
        this.router.navigate(['/catalogo']);
      },
      error: (err) => {
        this.cargando.set(false);
        const msg = err.error?.mensaje || 'Credenciales incorrectas o error en el servidor';
        this.errorMessage.set(msg);
      }
    });
  }
}
