import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="w-56 shrink-0 bg-slate-900 border-r border-slate-800 flex flex-col min-h-screen">
      <div class="p-5 border-b border-slate-800">
        <a routerLink="/dashboard" class="block text-lg font-bold text-white tracking-tight hover:text-emerald-400 transition-colors">CryptoDash</a>
        <p class="text-slate-500 text-xs mt-0.5">Trading fictif</p>
      </div>
      <nav class="p-3 flex-1 flex flex-col gap-1.5">
        <a routerLink="/dashboard" routerLinkActive="!bg-emerald-500/15 !text-emerald-400 !border-emerald-500/40" [routerLinkActiveOptions]="{ exact: true }"
           class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-300 hover:bg-slate-800 hover:text-white border border-transparent transition-colors">
          <span class="w-8 h-8 rounded-lg bg-slate-700/80 flex items-center justify-center text-sm shrink-0">📊</span>
          <span>Dashboard</span>
        </a>
        <a routerLink="/leaderboard" routerLinkActive="!bg-emerald-500/15 !text-emerald-400 !border-emerald-500/40"
           class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-300 hover:bg-slate-800 hover:text-white border border-transparent transition-colors">
          <span class="w-8 h-8 rounded-lg bg-slate-700/80 flex items-center justify-center text-sm shrink-0">🏆</span>
          <span>Classement</span>
        </a>
        <a routerLink="/wallet" routerLinkActive="!bg-emerald-500/15 !text-emerald-400 !border-emerald-500/40"
           class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-300 hover:bg-slate-800 hover:text-white border border-transparent transition-colors">
          <span class="w-8 h-8 rounded-lg bg-slate-700/80 flex items-center justify-center text-sm shrink-0">💰</span>
          <span>Portefeuille</span>
        </a>
        <a routerLink="/history" routerLinkActive="!bg-emerald-500/15 !text-emerald-400 !border-emerald-500/40"
           class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-300 hover:bg-slate-800 hover:text-white border border-transparent transition-colors">
          <span class="w-8 h-8 rounded-lg bg-slate-700/80 flex items-center justify-center text-sm shrink-0">📜</span>
          <span>Historique</span>
        </a>
        <a routerLink="/settings" routerLinkActive="!bg-emerald-500/15 !text-emerald-400 !border-emerald-500/40"
           class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-300 hover:bg-slate-800 hover:text-white border border-transparent transition-colors">
          <span class="w-8 h-8 rounded-lg bg-slate-700/80 flex items-center justify-center text-sm shrink-0">⚙️</span>
          <span>Paramètres</span>
        </a>
      </nav>
      @if (auth.currentUser(); as user) {
        <div class="p-3 border-t border-slate-800 mt-auto">
          <p class="text-slate-400 text-xs truncate px-2 mb-2" [title]="user.email">{{ user.email }}</p>
          <button type="button" (click)="auth.logout()"
                  class="w-full text-left px-3 py-2 rounded-xl text-slate-400 hover:bg-rose-500/20 hover:text-rose-400 text-sm transition-colors">
            Déconnexion
          </button>
        </div>
      }
    </aside>
  `,
})
export class SidebarComponent {
  constructor(protected auth: AuthService) {}
}
