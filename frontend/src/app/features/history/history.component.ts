import { Component, OnInit, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { ApiService, TransactionDto } from '../../core/services/api.service';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [NgClass],
  template: `
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-white tracking-tight">Historique des transactions</h1>
      <p class="mt-1 text-slate-400 text-sm">Achats et ventes sur votre portefeuille fictif</p>
    </div>

    @if (loading) {
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-8 text-center text-slate-500">
        Chargement…
      </div>
    } @else if (error) {
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-8 text-center text-rose-400">
        {{ error }}
      </div>
    } @else {
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 overflow-hidden">
        @if (transactions.length) {
          <ul class="divide-y divide-slate-700/50">
            @for (tx of transactions; track tx.id) {
              <li class="p-4 flex flex-wrap items-center justify-between gap-4 hover:bg-slate-800/50 transition-colors">
                <div class="flex items-center gap-4">
                  <span class="w-10 h-10 rounded-full flex items-center justify-center shrink-0" [ngClass]="tx.type === 'BUY' || tx.type === 'RECEIVE' || tx.type === 'DEPOSIT' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-rose-500/20 text-rose-400'">
                    @if (tx.type === 'BUY') {
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/></svg>
                    } @else if (tx.type === 'DEPOSIT') {
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"/></svg>
                    } @else if (tx.type === 'SEND') {
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/></svg>
                    } @else if (tx.type === 'RECEIVE') {
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4m0 0l7-7m-7 7l7 7"/></svg>
                    } @else {
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/></svg>
                    }
                  </span>
                  <div>
                    <p class="font-medium text-white">{{ txLabel(tx) }}</p>
                    <p class="text-slate-500 text-sm">{{ formatDate(tx.createdAt) }}</p>
                  </div>
                </div>
                <div class="text-right">
                  <p class="font-medium tabular-nums" [ngClass]="tx.type === 'BUY' || tx.type === 'RECEIVE' || tx.type === 'DEPOSIT' ? 'text-emerald-400' : 'text-rose-400'">
                    {{ (tx.type === 'BUY' || tx.type === 'RECEIVE' || tx.type === 'DEPOSIT') ? '+' : '-' }}{{ formatAmount(tx.amount) }} {{ tx.symbol }}
                  </p>
                  @if (tx.type === 'BUY' || tx.type === 'SELL') {
                    <p class="text-slate-400 text-sm tabular-nums">{{ formatPrice(tx.totalUsdt) }} USDT</p>
                  }
                </div>
              </li>
            }
          </ul>
        } @else {
          <div class="p-8 text-center text-slate-500">
            Aucune transaction. Vos achats et ventes apparaîtront ici.
          </div>
        }
      </div>
    }
  `,
})
export class HistoryComponent implements OnInit {
  private readonly api = inject(ApiService);

  transactions: TransactionDto[] = [];
  loading = true;
  error = '';

  ngOnInit(): void {
    this.api.getHistory(100).subscribe({
      next: (list) => {
        this.transactions = list;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Impossible de charger l\'historique.';
      },
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  formatAmount(n: number): string {
    const num = Number(n);
    if (num >= 1000) return num.toLocaleString('fr-FR', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
    if (num >= 1) return num.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
    return num.toLocaleString('fr-FR', { minimumFractionDigits: 4, maximumFractionDigits: 8 });
  }

  formatPrice(n: number): string {
    const num = Number(n);
    if (num >= 1000) return num.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
    if (num >= 1) return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
    return num.toLocaleString('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 8 });
  }

  txLabel(tx: TransactionDto): string {
    if (tx.type === 'BUY') return `Achat ${tx.symbol}`;
    if (tx.type === 'SELL') return `Vente ${tx.symbol}`;
    if (tx.type === 'DEPOSIT') return `Dépôt USDT (carte)`;
    if (tx.type === 'SEND') return `Envoyé à ${tx.counterpartyAccountName ?? '?'} · ${tx.symbol}`;
    if (tx.type === 'RECEIVE') return `Reçu de ${tx.counterpartyAccountName ?? '?'} · ${tx.symbol}`;
    return `${tx.type} ${tx.symbol}`;
  }
}
