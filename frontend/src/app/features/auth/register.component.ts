import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-[#0b0f1a] flex flex-col">
      <header class="shrink-0 border-b border-slate-800 bg-slate-900/80 px-4 py-3 flex items-center justify-between">
        <a routerLink="/login" class="text-lg font-bold text-white tracking-tight">CryptoDash</a>
        <a routerLink="/login" class="text-sm text-slate-400 hover:text-emerald-400 transition-colors">Se connecter</a>
      </header>
      <div class="flex-1 flex items-center justify-center p-4">
      <div class="w-full max-w-md rounded-2xl border border-slate-700/80 bg-slate-800/40 p-8">
        <h1 class="text-2xl font-bold text-white mb-2">Créer un compte</h1>
        <p class="text-slate-400 text-sm mb-6">Trading fictif CryptoDash</p>

        @if (errorMessage) {
          <p class="text-rose-400 text-sm mb-4">{{ errorMessage }}</p>
        }

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-4">
          <div>
            <label for="email" class="block text-sm font-medium text-slate-400 mb-1">Email</label>
            <input id="email" type="email" formControlName="email" autocomplete="email"
                   class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-3 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="vous@exemple.com" />
            @if (form.get('email')?.invalid && form.get('email')?.touched) {
              <p class="text-rose-400 text-xs mt-1">Email valide requis</p>
            }
          </div>
          <div>
            <label for="password" class="block text-sm font-medium text-slate-400 mb-1">Mot de passe (min. 8 caractères)</label>
            <input id="password" type="password" formControlName="password" autocomplete="new-password"
                   class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-3 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="••••••••" />
            @if (form.get('password')?.invalid && form.get('password')?.touched) {
              <p class="text-rose-400 text-xs mt-1">Au moins 8 caractères</p>
            }
          </div>
          <button type="submit" [disabled]="form.invalid || loading"
                  class="w-full rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium py-3 transition-colors">
            @if (loading) { Création… } @else { Créer mon compte }
          </button>
        </form>

        <p class="mt-6 text-center text-slate-400 text-sm">
          Déjà un compte ?
          <a routerLink="/login" class="text-emerald-400 hover:underline">Se connecter</a>
        </p>
      </div>
      </div>
    </div>
  `,
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });
  loading = false;
  errorMessage = '';

  onSubmit(): void {
    if (this.form.invalid || this.loading) return;
    this.loading = true;
    this.errorMessage = '';
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading = false;
        window.location.href = '/dashboard';
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Création impossible.';
      },
    });
  }
}
