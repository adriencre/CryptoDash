import { Component, inject, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import type { ProfileResponse } from '../../core/models/auth.model';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="w-full">
      <h1 class="text-2xl font-bold text-white mb-6">Paramètres</h1>

      <section class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-6 mb-6">
        <h2 class="text-lg font-semibold text-white mb-4">Profil (compte de trading)</h2>
        <p class="text-slate-400 text-sm mb-4">Informations d'identification comme sur une plateforme de trading réelle.</p>
        @if (profileLoading) {
          <p class="text-slate-400">Chargement du profil…</p>
        } @else {
          <form (ngSubmit)="saveProfile()" class="space-y-4">
            @if (profileError) {
              <p class="text-rose-400 text-sm">{{ profileError }}</p>
            }
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label for="firstName" class="block text-sm font-medium text-slate-400 mb-1">Prénom</label>
                <input id="firstName" type="text" [(ngModel)]="profileForm.firstName" name="firstName" autocomplete="given-name"
                       class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="Jean" />
              </div>
              <div>
                <label for="lastName" class="block text-sm font-medium text-slate-400 mb-1">Nom</label>
                <input id="lastName" type="text" [(ngModel)]="profileForm.lastName" name="lastName" autocomplete="family-name"
                       class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="Dupont" />
              </div>
            </div>
            <div>
              <label for="email" class="block text-sm font-medium text-slate-400 mb-1">Adresse email</label>
              <input id="email" type="email" [(ngModel)]="profileForm.email" name="email" autocomplete="email"
                     class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="vous@exemple.com" />
            </div>
            <div>
              <label for="accountName" class="block text-sm font-medium text-slate-400 mb-1">Nom de compte (pour recevoir des virements)</label>
              <input id="accountName" type="text" [(ngModel)]="profileForm.accountName" name="accountName" autocomplete="username"
                     class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="mon_compte_crypto" />
              <p class="text-slate-500 text-xs mt-1">Lettres, chiffres et underscore. Les autres pourront vous envoyer des crypto avec ce nom ou votre email.</p>
            </div>
            <div>
              <label for="phone" class="block text-sm font-medium text-slate-400 mb-1">Téléphone</label>
              <input id="phone" type="tel" [(ngModel)]="profileForm.phone" name="phone" autocomplete="tel"
                     class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="+33 6 12 34 56 78" />
            </div>
            <div>
              <label for="addressLine1" class="block text-sm font-medium text-slate-400 mb-1">Adresse (ligne 1)</label>
              <input id="addressLine1" type="text" [(ngModel)]="profileForm.addressLine1" name="addressLine1" autocomplete="street-address"
                     class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="123 rue de la Paix" />
            </div>
            <div>
              <label for="addressLine2" class="block text-sm font-medium text-slate-400 mb-1">Adresse (ligne 2)</label>
              <input id="addressLine2" type="text" [(ngModel)]="profileForm.addressLine2" name="addressLine2" autocomplete="off"
                     class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="Bâtiment, étage (optionnel)" />
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label for="postalCode" class="block text-sm font-medium text-slate-400 mb-1">Code postal</label>
                <input id="postalCode" type="text" [(ngModel)]="profileForm.postalCode" name="postalCode" autocomplete="postal-code"
                       class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="75001" />
              </div>
              <div>
                <label for="city" class="block text-sm font-medium text-slate-400 mb-1">Ville</label>
                <input id="city" type="text" [(ngModel)]="profileForm.city" name="city" autocomplete="address-level2"
                       class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="Paris" />
              </div>
              <div>
                <label for="country" class="block text-sm font-medium text-slate-400 mb-1">Pays</label>
                <input id="country" type="text" [(ngModel)]="profileForm.country" name="country" autocomplete="country-name"
                       class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="France" />
              </div>
            </div>
            <button type="submit" [disabled]="profileSaving"
                    class="rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium px-4 py-2 transition-colors">
              @if (profileSaving) { Enregistrement… } @else { Enregistrer le profil }
            </button>
          </form>
        }
      </section>

      <section class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-6 mb-6">
        <h2 class="text-lg font-semibold text-white mb-4">Sécurité</h2>

        @if (loadingStatus) {
          <p class="text-slate-400">Chargement…</p>
        } @else if (twoFactorEnabled === false && !setupData) {
          <p class="text-slate-400 text-sm mb-4">La double authentification (2FA) renforce la sécurité de votre compte.</p>
          <button type="button" (click)="startSetup2FA()" [disabled]="loading"
                  class="rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium px-4 py-2 transition-colors">
            Activer la 2FA
          </button>
        } @else if (setupData && !backupCodesShown) {
          <div class="space-y-4">
            <p class="text-slate-300 text-sm">Scannez ce QR code avec une application d'authentification (Google Authenticator, Authy, etc.) puis saisissez le code à 6 chiffres.</p>
            <div class="flex flex-wrap gap-6 items-start">
              <img [src]="qrCodeImageUrl" alt="QR Code 2FA" class="rounded-lg border border-slate-600 w-44 h-44" />
              <div>
                <p class="text-slate-400 text-xs mb-1">Ou saisissez ce secret manuellement :</p>
                <code class="block text-slate-300 text-sm break-all bg-slate-900/80 px-3 py-2 rounded-lg">{{ setupData.secret }}</code>
              </div>
            </div>
            <div>
              <label for="setup-code" class="block text-sm font-medium text-slate-400 mb-1">Code à 6 chiffres</label>
              <input id="setup-code" type="text" [(ngModel)]="setupCode" name="setupCode" maxlength="6" inputmode="numeric" pattern="[0-9]*"
                     placeholder="000000"
                     class="w-full max-w-xs rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 font-mono text-lg tracking-widest" />
            </div>
            @if (errorMessage) {
              <p class="text-rose-400 text-sm">{{ errorMessage }}</p>
            }
            <div class="flex gap-3">
              <button type="button" (click)="confirmEnable2FA()" [disabled]="!setupCode || setupCode.length !== 6 || loading"
                      class="rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium px-4 py-2 transition-colors">
                Confirmer
              </button>
              <button type="button" (click)="cancelSetup()" [disabled]="loading"
                      class="rounded-xl border border-slate-600 text-slate-400 hover:bg-slate-700/50 px-4 py-2 transition-colors">
                Annuler
              </button>
            </div>
          </div>
        } @else if (backupCodesShown && backupCodes.length) {
          <div class="space-y-4">
            <p class="text-amber-400 text-sm font-medium">Enregistrez ces 12 codes de secours en lieu sûr. Chaque code ne peut être utilisé qu'une seule fois.</p>
            <div class="grid grid-cols-2 gap-2 font-mono text-sm">
              @for (code of backupCodes; track code) {
                <div class="bg-slate-900/80 rounded-lg px-3 py-2 text-slate-300 break-all">{{ code }}</div>
              }
            </div>
            <button type="button" (click)="finishBackupCodes()"
                    class="rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-medium px-4 py-2 transition-colors">
              J'ai enregistré les codes
            </button>
          </div>
        } @else if (twoFactorEnabled === true) {
          <p class="text-slate-400 text-sm mb-4">La double authentification est activée.</p>
          @if (errorMessage) {
            <p class="text-rose-400 text-sm mb-4">{{ errorMessage }}</p>
          }
          <div class="space-y-4">
            <div>
              <h3 class="text-slate-300 font-medium text-sm mb-2">Désactiver la 2FA</h3>
              <div class="flex flex-wrap gap-2 items-end">
                <input type="password" [(ngModel)]="disablePassword" placeholder="Mot de passe" name="disablePassword"
                       class="rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 w-48" />
                <input type="text" [(ngModel)]="disableCode" placeholder="Code 2FA" name="disableCode" maxlength="8"
                       class="rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 w-32 font-mono" />
                <button type="button" (click)="disable2FA()" [disabled]="!disablePassword || !disableCode || loading"
                        class="rounded-xl border border-rose-500/60 text-rose-400 hover:bg-rose-500/20 px-4 py-2 transition-colors">
                  Désactiver
                </button>
              </div>
            </div>
            <div>
              <h3 class="text-slate-300 font-medium text-sm mb-2">Régénérer les codes de secours</h3>
              <div class="flex flex-wrap gap-2 items-end">
                <input type="password" [(ngModel)]="regenPassword" placeholder="Mot de passe" name="regenPassword"
                       class="rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 w-48" />
                <input type="text" [(ngModel)]="regenCode" placeholder="Code 2FA" name="regenCode" maxlength="8"
                       class="rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 w-32 font-mono" />
                <button type="button" (click)="regenerateBackupCodes()" [disabled]="!regenPassword || !regenCode || loading"
                        class="rounded-xl bg-slate-600 hover:bg-slate-500 text-white px-4 py-2 transition-colors">
                  Régénérer
                </button>
              </div>
            </div>
          </div>
          @if (backupCodesShown && backupCodes.length) {
            <div class="mt-6 p-4 rounded-xl bg-slate-900/80 border border-slate-600">
              <p class="text-amber-400 text-sm font-medium mb-2">Nouveaux codes de secours (à enregistrer) :</p>
              <div class="grid grid-cols-2 gap-2 font-mono text-sm text-slate-300">
                @for (code of backupCodes; track code) {
                  <div class="break-all">{{ code }}</div>
                }
              </div>
              <button type="button" (click)="backupCodes = []; backupCodesShown = false" class="mt-3 text-slate-400 hover:text-white text-sm">
                Masquer
              </button>
            </div>
          }
        }
      </section>
    </div>
  `,
})
export class SettingsComponent implements OnInit {
  private auth = inject(AuthService);

  profileLoading = true;
  profileSaving = false;
  profileError = '';
  profileForm: {
    email: string;
    accountName: string;
    firstName: string;
    lastName: string;
    phone: string;
    addressLine1: string;
    addressLine2: string;
    postalCode: string;
    city: string;
    country: string;
  } = {
    email: '',
    accountName: '',
    firstName: '',
    lastName: '',
    phone: '',
    addressLine1: '',
    addressLine2: '',
    postalCode: '',
    city: '',
    country: '',
  };

  loadingStatus = true;
  loading = false;
  twoFactorEnabled: boolean | null = null;
  errorMessage = '';

  setupData: { secret: string; qrCodeUrl: string } | null = null;
  setupCode = '';
  backupCodes: string[] = [];
  backupCodesShown = false;

  disablePassword = '';
  disableCode = '';
  regenPassword = '';
  regenCode = '';

  get qrCodeImageUrl(): string {
    if (!this.setupData?.qrCodeUrl) return '';
    return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(this.setupData.qrCodeUrl)}`;
  }

  ngOnInit(): void {
    this.auth.get2FAStatus().subscribe({
      next: (res) => {
        this.twoFactorEnabled = res.twoFactorEnabled;
        this.loadingStatus = false;
      },
      error: () => {
        this.loadingStatus = false;
        this.twoFactorEnabled = false;
      },
    });
    this.auth.getProfile().subscribe({
      next: (p: ProfileResponse) => {
        this.profileForm = {
          email: p.email ?? '',
          accountName: p.accountName ?? '',
          firstName: p.firstName ?? '',
          lastName: p.lastName ?? '',
          phone: p.phone ?? '',
          addressLine1: p.addressLine1 ?? '',
          addressLine2: p.addressLine2 ?? '',
          postalCode: p.postalCode ?? '',
          city: p.city ?? '',
          country: p.country ?? '',
        };
        this.profileLoading = false;
      },
      error: () => {
        this.profileLoading = false;
      },
    });
  }

  saveProfile(): void {
    this.profileSaving = true;
    this.profileError = '';
    this.auth.updateProfile({
      email: this.profileForm.email || null,
      accountName: this.profileForm.accountName || null,
      firstName: this.profileForm.firstName || null,
      lastName: this.profileForm.lastName || null,
      phone: this.profileForm.phone || null,
      addressLine1: this.profileForm.addressLine1 || null,
      addressLine2: this.profileForm.addressLine2 || null,
      postalCode: this.profileForm.postalCode || null,
      city: this.profileForm.city || null,
      country: this.profileForm.country || null,
    }).subscribe({
      next: () => {
        this.profileSaving = false;
      },
      error: (err) => {
        this.profileError = err.error?.message || 'Erreur lors de l\'enregistrement.';
        this.profileSaving = false;
      },
    });
  }

  startSetup2FA(): void {
    this.loading = true;
    this.errorMessage = '';
    this.auth.setup2FA().subscribe({
      next: (res) => {
        this.setupData = res;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors de la configuration.';
        this.loading = false;
      },
    });
  }

  cancelSetup(): void {
    this.setupData = null;
    this.setupCode = '';
    this.errorMessage = '';
  }

  confirmEnable2FA(): void {
    if (!this.setupCode || this.setupCode.length !== 6) return;
    this.loading = true;
    this.errorMessage = '';
    this.auth.enable2FA(this.setupCode.trim()).subscribe({
      next: (codes) => {
        this.backupCodes = codes;
        this.backupCodesShown = true;
        this.setupData = null;
        this.setupCode = '';
        this.twoFactorEnabled = true;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Code invalide.';
        this.loading = false;
      },
    });
  }

  finishBackupCodes(): void {
    this.backupCodes = [];
    this.backupCodesShown = false;
  }

  disable2FA(): void {
    if (!this.disablePassword || !this.disableCode) return;
    this.loading = true;
    this.errorMessage = '';
    this.auth.disable2FA(this.disablePassword, this.disableCode.trim()).subscribe({
      next: () => {
        this.twoFactorEnabled = false;
        this.disablePassword = '';
        this.disableCode = '';
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Mot de passe ou code incorrect.';
        this.loading = false;
      },
    });
  }

  regenerateBackupCodes(): void {
    if (!this.regenPassword || !this.regenCode) return;
    this.loading = true;
    this.errorMessage = '';
    this.auth.regenerateBackupCodes(this.regenPassword, this.regenCode.trim()).subscribe({
      next: (codes) => {
        this.backupCodes = codes;
        this.backupCodesShown = true;
        this.regenPassword = '';
        this.regenCode = '';
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Mot de passe ou code incorrect.';
        this.loading = false;
      },
    });
  }
}
