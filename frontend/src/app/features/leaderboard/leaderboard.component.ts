import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { ApiService, LeaderboardEntryDto } from '../../core/services/api.service';
import { NgClass, DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [NgClass, DecimalPipe],
  template: `
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-white tracking-tight">Classement Mondial</h1>
      <p class="mt-1 text-slate-400 text-sm">Les meilleurs traders fictifs de CryptoDash</p>
    </div>

    @if (loading) {
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-8 text-center text-slate-500">Chargement du classement…</div>
    } @else {
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 overflow-hidden">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="border-b border-slate-700/80 bg-slate-900/50">
              <th class="px-6 py-4 text-xs font-semibold text-slate-400 uppercase tracking-wider">Rang</th>
              <th class="px-6 py-4 text-xs font-semibold text-slate-400 uppercase tracking-wider">Trader</th>
              <th class="px-6 py-4 text-xs font-semibold text-slate-400 uppercase tracking-wider text-right">Valeur Totale</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-700/50">
            @for (entry of leaderboard; track entry.rank) {
              <tr class="hover:bg-slate-800/50 transition-colors">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="flex items-center gap-2">
                    @if (entry.rank === 1) {
                      <span class="w-8 h-8 rounded-full bg-yellow-500/20 text-yellow-500 flex items-center justify-center font-bold">🥇</span>
                    } @else if (entry.rank === 2) {
                      <span class="w-8 h-8 rounded-full bg-slate-300/20 text-slate-300 flex items-center justify-center font-bold">🥈</span>
                    } @else if (entry.rank === 3) {
                      <span class="w-8 h-8 rounded-full bg-orange-500/20 text-orange-500 flex items-center justify-center font-bold">🥉</span>
                    } @else {
                      <span class="w-8 h-8 rounded-full bg-slate-700/50 text-slate-400 flex items-center justify-center font-medium text-sm">{{ entry.rank }}</span>
                    }
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="flex items-center gap-3">
                    <div class="w-10 h-10 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold border border-emerald-500/30">
                      {{ (entry.accountName || entry.email).charAt(0).toUpperCase() }}
                    </div>
                    <div>
                      <div class="text-white font-semibold">{{ entry.accountName || 'Trader Anonyme' }}</div>
                      <div class="text-slate-500 text-xs truncate max-w-[150px]">{{ entry.email }}</div>
                    </div>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right">
                  <div class="text-white font-bold tabular-nums">{{ entry.totalValueUsdt | number:'1.0-2' }} USDT</div>
                  <div class="text-emerald-400 text-xs font-medium">ROI: +{{ ((entry.totalValueUsdt - 10000) / 10000 * 100) | number:'1.0-2' }} %</div>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class LeaderboardComponent implements OnInit {
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);

  leaderboard: LeaderboardEntryDto[] = [];
  loading = true;

  ngOnInit(): void {
    this.api.getLeaderboard().subscribe({
      next: (data) => {
        this.leaderboard = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
