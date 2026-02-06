import { Component, OnInit, inject } from '@angular/core';
import { ChangeDetectorRef } from '@angular/core';
import { AsyncPipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { PriceStreamService, ConnectionStatus } from '../../core/services/price-stream.service';
import { ApiService } from '../../core/services/api.service';
import { PriceTick } from '../../core/models/price-tick.model';
import { getCryptoIconUrl } from '../../core/constants/crypto-icons';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AsyncPipe, NgClass, RouterLink],
  template: `
    <div class="mb-8">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold text-white tracking-tight">Marchés</h1>
          <p class="mt-1 text-slate-400 text-sm">Prix en direct via Binance</p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <div class="flex rounded-lg border border-slate-600/80 bg-slate-800/60 p-0.5">
            <button type="button" (click)="filterFavoritesOnly = false" class="px-3 py-1.5 rounded-md text-sm font-medium transition-colors" [ngClass]="!filterFavoritesOnly ? 'bg-slate-700 text-white' : 'text-slate-400 hover:text-white'">
              Tous
            </button>
            <button type="button" (click)="filterFavoritesOnly = true" class="px-3 py-1.5 rounded-md text-sm font-medium transition-colors" [ngClass]="filterFavoritesOnly ? 'bg-slate-700 text-white' : 'text-slate-400 hover:text-white'">
              Favoris seulement
            </button>
          </div>
          <span class="inline-flex items-center gap-2 text-sm px-4 py-2 rounded-full border border-slate-600/80 bg-slate-800/60 backdrop-blur-sm" [ngClass]="statusClasses((status$ | async))">
          <span class="w-2 h-2 rounded-full shrink-0" [class.bg-amber-400]="(status$ | async) === 'connecting'" [class.bg-emerald-400]="(status$ | async) === 'connected'" [class.animate-pulse]="(status$ | async) === 'connecting'" [class.bg-red-400]="(status$ | async) === 'disconnected' || (status$ | async) === 'error'"></span>
          {{ statusLabel((status$ | async)) }}
        </span>
        </div>
      </div>
      <div class="mt-4">
        <label for="market-search" class="sr-only">Rechercher une crypto</label>
        <div class="relative">
          <span class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
          </span>
          <input id="market-search" type="search" placeholder="Rechercher par symbole (ex. BTC, ETH, SOL)…"
            class="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-600/80 bg-slate-800/60 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-500 focus:border-transparent"
            [value]="searchQuery"
            (input)="onSearchInput($any($event.target).value)"
            autocomplete="off" />
          @if (searchQuery) {
            <button type="button" (click)="clearSearch()" class="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full text-slate-400 hover:text-white hover:bg-slate-600/80" aria-label="Effacer la recherche">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
          }
        </div>
      </div>
    </div>

    <div class="overflow-y-auto max-h-[calc(100vh-14rem)] min-h-[200px]">
      @if (filterFavoritesOnly && filteredPrices().length === 0 && prices.length > 0) {
        <div class="rounded-2xl border border-slate-700/80 bg-slate-800/30 p-8 text-center">
          <p class="font-medium text-slate-300">Aucun favori</p>
          <p class="mt-2 text-slate-500 text-sm">Ajoutez des favoris en cliquant sur le cœur des cartes.</p>
        </div>
      } @else if (searchQuery.trim() && filteredPrices().length === 0 && prices.length > 0) {
        <div class="rounded-2xl border border-slate-700/80 bg-slate-800/30 p-8 text-center">
          <p class="font-medium text-slate-300">Aucun résultat pour « {{ searchQuery.trim() }} »</p>
          <p class="mt-2 text-slate-500 text-sm">Modifiez la recherche ou effacez le filtre.</p>
        </div>
      } @else {
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      @for (tick of filteredPrices(); track tick.symbol) {
        <a [routerLink]="['/dashboard', tick.symbol.replace('USDT', '')]" class="block group relative rounded-2xl border border-slate-700/80 bg-slate-800/40 hover:bg-slate-800/70 hover:border-slate-600/80 transition-all duration-200 overflow-hidden focus:outline-none focus:ring-2 focus:ring-slate-500">
          <button type="button" (click)="toggleFavorite(tick.symbol); $event.stopPropagation(); $event.preventDefault()" class="absolute top-3 right-3 z-10 p-1.5 rounded-full text-slate-400 hover:text-rose-400 hover:bg-slate-700/50 transition-colors" [class.text-rose-400]="isFavorite(tick.symbol)" [attr.aria-label]="isFavorite(tick.symbol) ? 'Retirer des favoris' : 'Ajouter aux favoris'">
            @if (isFavorite(tick.symbol)) {
              <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.688 3A5.5 5.5 0 0112 5.052 5.5 5.5 0 0116.313 3c2.973 0 5.437 2.322 5.437 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z"/></svg>
            } @else {
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/></svg>
            }
          </button>
          <div class="p-5">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-3">
                @if (getIconUrl(tick.symbol) && !iconFailed(tick.symbol)) {
                  <img [src]="getIconUrl(tick.symbol)!" [alt]="tick.symbol" class="w-10 h-10 rounded-full object-cover ring-2 ring-slate-600/50"
                       (error)="setIconFailed(tick.symbol)" />
                }
                @if (!getIconUrl(tick.symbol) || iconFailed(tick.symbol)) {
                  <span class="w-10 h-10 rounded-full bg-slate-600/80 flex items-center justify-center text-white font-bold text-sm shrink-0">
                    {{ tick.symbol.replace('USDT', '').charAt(0) }}
                  </span>
                }
                <div>
                  <span class="font-semibold text-white text-lg tracking-tight block">{{ tick.symbol.replace('USDT', '') }}</span>
                  <span class="text-xs font-medium text-slate-500 uppercase tracking-wider">USDT</span>
                </div>
              </div>
            </div>
            <p class="text-2xl font-bold text-white tabular-nums mb-3">{{ formatPrice(tick.lastPrice) }}</p>
            <div class="flex flex-wrap items-center gap-2">
              <span class="inline-flex items-center rounded-lg px-2.5 py-1 text-sm font-medium tabular-nums" [ngClass]="changeClasses(tick.priceChangePercent)">
                {{ isPositive(tick.priceChangePercent) ? '+' : '' }}{{ tick.priceChangePercent }} %
              </span>
              <span class="text-slate-500 text-xs">24h</span>
            </div>
            <p class="mt-4 pt-3 border-t border-slate-700/80 text-slate-500 text-xs">
              Vol. 24h {{ formatVolume(tick.volume24h) }}
            </p>
          </div>
        </a>
      }
      </div>
      }
    </div>

    @if (prices.length === 0) {
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/30 p-8 text-center">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-full bg-slate-700/50 mb-4">
          <svg class="w-7 h-7 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13 10V3L4 14h7v7l9-11h-7z"/>
          </svg>
        </div>
        <p class="font-medium text-slate-300">Aucun prix reçu</p>
        <p class="mt-2 text-slate-500 text-sm max-w-md mx-auto">
          Le backend doit tourner sur <code class="bg-slate-700/80 px-1.5 py-0.5 rounded text-slate-300">http://localhost:8080</code>
          et être connecté à Binance. Lancez <code class="bg-slate-700/80 px-1.5 py-0.5 rounded text-slate-300 text-xs">mvn spring-boot:run</code> dans le dossier backend.
        </p>
      </div>
    }

    <div class="mt-8 rounded-2xl border border-slate-700/80 bg-slate-800/30 p-6">
      <h3 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-2">À venir</h3>
      <p class="text-slate-500 text-sm">Graphique Donut pour la répartition du portefeuille.</p>
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  readonly prices$: Observable<PriceTick[]>;
  readonly status$: Observable<ConnectionStatus>;
  private readonly failedIcons = new Set<string>();
  private readonly api = inject(ApiService);
  private readonly cdr = inject(ChangeDetectorRef);

  prices: PriceTick[] = [];
  favoriteSymbols = new Set<string>();
  filterFavoritesOnly = false;
  public searchQuery = '';

  onSearchInput(value: string): void {
    this.searchQuery = value ?? '';
    this.cdr.markForCheck();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.cdr.markForCheck();
  }

  constructor(private readonly priceStream: PriceStreamService) {
    this.prices$ = this.priceStream.prices$;
    this.status$ = this.priceStream.status$;
  }

  ngOnInit(): void {
    this.prices$.subscribe((p) => {
      this.prices = p ?? [];
      this.cdr.markForCheck();
    });
    this.api.getFavorites().subscribe({
      next: (symbols) => {
        this.favoriteSymbols = new Set(symbols ?? []);
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  filteredPrices(): PriceTick[] {
    let list = this.prices;
    if (this.filterFavoritesOnly) {
      list = list.filter((t) => this.favoriteSymbols.has(t.symbol.replace('USDT', '')));
    }
    const q = (this.searchQuery ?? '').trim().toLowerCase();
    if (!q) return list;
    return list.filter((t) => t.symbol.replace('USDT', '').toLowerCase().includes(q));
  }

  isFavorite(symbol: string): boolean {
    return this.favoriteSymbols.has((symbol || '').replace('USDT', ''));
  }

  toggleFavorite(symbol: string): void {
    const base = (symbol || '').replace('USDT', '').trim();
    if (!base) return;
    if (this.favoriteSymbols.has(base)) {
      this.api.removeFavorite(base).subscribe({
        next: () => {
          this.favoriteSymbols.delete(base);
          this.cdr.markForCheck();
        },
        error: () => {},
      });
    } else {
      this.api.addFavorite(base).subscribe({
        next: () => {
          this.favoriteSymbols.add(base);
          this.cdr.markForCheck();
        },
        error: () => {},
      });
    }
  }

  getIconUrl(symbol: string): string | null {
    return getCryptoIconUrl(symbol);
  }

  setIconFailed(symbol: string): void {
    this.failedIcons.add(symbol);
    this.cdr.markForCheck();
  }

  iconFailed(symbol: string): boolean {
    return this.failedIcons.has(symbol);
  }

  isPositive(value: string | number): boolean {
    return Number(value) >= 0;
  }

  formatPrice(value: string | number): string {
    const n = Number(value);
    if (n >= 1000) return n.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
    if (n >= 1) return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
    return n.toLocaleString('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 8 });
  }

  formatVolume(value: string | number): string {
    const n = Number(value);
    if (n >= 1e9) return (n / 1e9).toFixed(2) + ' B';
    if (n >= 1e6) return (n / 1e6).toFixed(2) + ' M';
    if (n >= 1e3) return (n / 1e3).toFixed(2) + ' K';
    return n.toFixed(0);
  }

  statusLabel(s: ConnectionStatus | null): string {
    switch (s) {
      case 'connected': return 'Connexion Binance OK';
      case 'connecting': return 'Connexion…';
      case 'error': return 'Erreur';
      default: return 'Déconnecté';
    }
  }

  statusClasses(s: ConnectionStatus | null): Record<string, boolean> {
    return {
      'bg-amber-500/20 text-amber-400': s === 'connecting',
      'bg-green-500/20 text-green-400': s === 'connected',
      'bg-red-500/20 text-red-400': s === 'disconnected' || s === 'error',
    };
  }

  changeClasses(priceChangePercent: string | number): Record<string, boolean> {
    const up = this.isPositive(priceChangePercent);
    return {
      'bg-emerald-500/15 text-emerald-400': up,
      'bg-rose-500/15 text-rose-400': !up,
    };
  }
}
