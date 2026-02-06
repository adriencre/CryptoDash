import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, FormsModule],
  template: `
    <div class="min-h-screen bg-[#0b0f1a] flex flex-col">
      <header class="shrink-0 border-b border-slate-800 bg-slate-900/80 px-4 py-3 flex items-center justify-between">
        <a routerLink="/login" class="text-lg font-bold text-white tracking-tight">CryptoDash</a>
        <a routerLink="/register" class="text-sm text-slate-400 hover:text-emerald-400 transition-colors">Créer un compte</a>
      </header>
      <div class="flex-1 flex items-center justify-center p-4">
      <div class="w-full max-w-md rounded-2xl border border-slate-700/80 bg-slate-800/40 p-8">
        <h1 class="text-2xl font-bold text-white mb-2">Connexion</h1>
        <p class="text-slate-400 text-sm mb-6">Accédez à votre portefeuille fictif</p>

        @if (errorMessage) {
          <p class="text-rose-400 text-sm mb-4">{{ errorMessage }}</p>
        }

        @if (step2FA) {
          <div class="space-y-4">
            <p class="text-slate-300 text-sm">Entrez le code à 6 chiffres de votre application d'authentification, ou un de vos 12 codes de secours (8 caractères).</p>
            <form (ngSubmit)="onSubmit2FA()" class="space-y-4">
              <div>
                <label for="code2fa" class="block text-sm font-medium text-slate-400 mb-1">Code</label>
                <input id="code2fa" type="text" [(ngModel)]="code2FA" name="code2fa" maxlength="8" autocomplete="one-time-code"
                       placeholder="000000 ou XXXXXXXX"
                       class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-3 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 font-mono text-lg tracking-widest" />
              </div>
              <button type="submit" [disabled]="!code2FA || code2FA.length < 6 || loading"
                      class="w-full rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium py-3 transition-colors">
                @if (loading) { Vérification… } @else { Valider }
              </button>
            </form>
            <button type="button" (click)="step2FA = false; code2FA = ''; errorMessage = ''"
                    class="text-slate-400 hover:text-white text-sm">Retour</button>
          </div>
        } @else {
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-4">
          <div>
            <label for="email" class="block text-sm font-medium text-slate-400 mb-1">Email</label>
            <input id="email" type="email" formControlName="email" autocomplete="email"
                   class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-3 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="vous@exemple.com" />
            @if (form.get('email')?.invalid && form.get('email')?.touched) {
              <p class="text-rose-400 text-xs mt-1">Email requis</p>
            }
          </div>
          <div>
            <label for="password" class="block text-sm font-medium text-slate-400 mb-1">Mot de passe</label>
            <input id="password" type="password" formControlName="password" autocomplete="current-password"
                   class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-3 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="••••••••" />
            @if (form.get('password')?.invalid && form.get('password')?.touched) {
              <p class="text-rose-400 text-xs mt-1">Mot de passe requis</p>
            }
          </div>
          <button type="submit" [disabled]="form.invalid || loading"
                  class="w-full rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium py-3 transition-colors">
            @if (loading) { Connexion… } @else { Se connecter }
          </button>
        </form>

        <p class="mt-6 text-center text-slate-400 text-sm">
          Pas de compte ?
          <a routerLink="/register" class="text-emerald-400 hover:underline">Créer un compte</a>
        </p>
        }
      </div>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  loading = false;
  errorMessage = '';
  step2FA = false;
  code2FA = '';
  tempToken = '';

  onSubmit(): void {
    if (this.form.invalid || this.loading) return;
    this.loading = true;
    this.errorMessage = '';
    this.auth.login(this.form.getRawValue()).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.requires2FA && res.tempToken) {
          this.step2FA = true;
          this.tempToken = res.tempToken;
        } else {
          this.auth.setSessionFromLoginResponse(res);
          window.location.href = '/dashboard';
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Connexion impossible.';
      },
    });
  }

  onSubmit2FA(): void {
    if (!this.code2FA || this.code2FA.length < 6 || this.loading) return;
    this.loading = true;
    this.errorMessage = '';
    this.auth.verify2FA(this.tempToken, this.code2FA.trim()).subscribe({
      next: () => {
        this.loading = false;
        window.location.href = '/dashboard';
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Code invalide ou expiré.';
      },
    });
  }
}
