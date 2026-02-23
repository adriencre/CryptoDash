import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { PriceStreamService } from '../../core/services/price-stream.service';
import { ApiService } from '../../core/services/api.service';
import { PriceTick } from '../../core/models/price-tick.model';
import { Kline } from '../../core/models/kline.model';
import { getCryptoIconUrl } from '../../core/constants/crypto-icons';

const MAX_REALTIME_POINTS = 80;

export interface PeriodPreset {
  label: string;
  days: string;
}

const PERIODS: PeriodPreset[] = [
  { label: '1 j', days: '1' },
  { label: '7 j', days: '7' },
  { label: '30 j', days: '30' },
  { label: '90 j', days: '90' },
  { label: '365 j', days: '365' },
];

@Component({
  selector: 'app-crypto-detail',
  standalone: true,
  imports: [NgClass, RouterLink, FormsModule],
  template: `
    <div class="mb-6">
      <a routerLink="/dashboard" class="inline-flex items-center gap-2 text-slate-400 hover:text-white text-sm transition-colors">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        Retour au marché
      </a>
    </div>

    @if (!tick) {
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-8 text-center text-slate-500">
        En attente des données pour {{ symbol }}…
      </div>
    } @else {
    <header class="mb-8">
      <div class="flex flex-wrap items-center gap-4">
        @if (getIconUrl(symbol) && !iconFailed) {
          <img [src]="getIconUrl(symbol)!" [alt]="symbol" class="w-14 h-14 rounded-full object-cover ring-2 ring-slate-600/50" (error)="iconFailed = true" />
        }
        @if (!getIconUrl(symbol) || iconFailed) {
          <span class="w-14 h-14 rounded-full bg-slate-600/80 flex items-center justify-center text-white font-bold text-xl shrink-0">{{ symbol.charAt(0) }}</span>
        }
        <div>
          <h1 class="text-3xl font-bold text-white tracking-tight">{{ symbol }}</h1>
          <p class="text-slate-500 text-sm mt-0.5">USDT · Prix en direct</p>
        </div>
      </div>
      <div class="mt-6 flex flex-wrap items-baseline gap-4">
        <span class="text-4xl font-bold text-white tabular-nums">{{ formatPrice(tick.lastPrice) }}</span>
        <span class="inline-flex items-center rounded-lg px-3 py-1.5 text-base font-medium tabular-nums" [ngClass]="changeClasses(tick.priceChangePercent)">
          {{ isPositive(tick.priceChangePercent) ? '+' : '' }}{{ tick.priceChangePercent }} % (24h)
        </span>
      </div>
    </header>

    <section class="mb-8">
      <div class="flex flex-wrap items-center justify-between gap-4 mb-4">
        <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider">Historique du prix</h2>
        <div class="flex flex-wrap gap-1">
          @for (p of periods; track p.label) {
            <button type="button" (click)="selectPeriod(p)" class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors" [ngClass]="periodButtonClasses(p)">
              {{ p.label }}
            </button>
          }
        </div>
      </div>
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-4 w-full shrink-0 relative" style="height: 320px;">
        <div class="w-full absolute inset-x-4 top-4 bottom-4" style="height: 280px;">
          <canvas #chartCanvas class="block w-full" style="height: 280px; width: 100%;"></canvas>
        </div>
        @if (loadingKlines) {
          <p class="absolute inset-0 flex items-center justify-center text-slate-500 text-sm">Chargement de l’historique…</p>
        }
        @if (!loadingKlines && chartDataLength === 0) {
          <p class="absolute inset-0 flex items-center justify-center text-slate-500 text-sm">Aucune donnée pour cette période. Changez de période ou réessayez.</p>
        }
      </div>
    </section>

    <section class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-5">
        <p class="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">Prix actuel</p>
        <p class="text-xl font-bold text-white tabular-nums">{{ formatPrice(tick.lastPrice) }}</p>
      </div>
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-5">
        <p class="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">Haut 24h</p>
        <p class="text-xl font-bold text-emerald-400 tabular-nums">{{ formatPrice(tick.high24h) }}</p>
      </div>
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-5">
        <p class="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">Bas 24h</p>
        <p class="text-xl font-bold text-rose-400 tabular-nums">{{ formatPrice(tick.low24h) }}</p>
      </div>
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-5">
        <p class="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">Volume 24h</p>
        <p class="text-xl font-bold text-white tabular-nums">{{ formatVolume(tick.volume24h) }}</p>
      </div>
    </section>

    <section class="mt-8 rounded-2xl border border-slate-700/80 bg-slate-800/40 p-6">
      <h2 class="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Acheter / Vendre</h2>
      <div class="flex flex-wrap items-end gap-4">
        <div class="min-w-[140px]">
          <label for="tradeAmount" class="block text-slate-400 text-sm font-medium mb-1">Quantité ({{ symbol }})</label>
          <input id="tradeAmount" type="number" min="0.00000001" step="0.00000001" [(ngModel)]="tradeAmount"
                 placeholder="0" class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-3 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 tabular-nums" />
        </div>
        <div class="flex gap-2">
          <button type="button" (click)="onBuy()" [disabled]="!canTrade() || tradeLoading"
                  class="px-5 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 disabled:pointer-events-none text-white font-medium transition-colors">
            {{ tradeLoading ? 'En cours…' : 'Acheter' }}
          </button>
          <button type="button" (click)="onSell()" [disabled]="!canTrade() || tradeLoading"
                  class="px-5 py-3 rounded-xl bg-rose-500/80 hover:bg-rose-500 disabled:opacity-50 disabled:pointer-events-none text-white font-medium transition-colors">
            Vendre
          </button>
        </div>
      </div>
      @if (tradeError) {
        <p class="mt-3 text-rose-400 text-sm">{{ tradeError }}</p>
      }
      @if (tradeSuccess) {
        <p class="mt-3 text-emerald-400 text-sm">
          {{ tradeSuccess }}
          <a routerLink="/wallet" class="ml-1 underline hover:no-underline">Voir mon portefeuille</a>
        </p>
      }
    </section>
    }
  `,
})
export class CryptoDetailComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;

  readonly periods = PERIODS;
  symbol = '';
  fullSymbol = '';
  tick: PriceTick | null = null;
  iconFailed = false;
  chartDataLength = 0;
  loadingKlines = false;
  selectedPeriod: PeriodPreset | null = null;

  tradeAmount: number | null = null;
  tradeError = '';
  tradeSuccess = '';
  tradeLoading = false;

  private readonly destroy$ = new Subject<void>();
  private chart: Chart | null = null;
  private readonly priceHistory: { t: number; y: number }[] = [];
  private pendingChartData: { t: number; y: number }[] | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly priceStream: PriceStreamService,
    private readonly api: ApiService,
    private readonly cdr: ChangeDetectorRef,
  ) {
    Chart.register(...registerables);
  }

  ngOnInit(): void {
    const sym = this.route.snapshot.paramMap.get('symbol') ?? '';
    this.symbol = sym.toUpperCase();
    this.fullSymbol = this.symbol.includes('USDT') ? this.symbol : this.symbol + 'USDT';

    // Charger l'historique 7 j par défaut pour afficher tout de suite l'historique des prix
    const defaultPeriod = PERIODS[1]!; // 7 j
    this.selectedPeriod = defaultPeriod;
    this.loadMarketChart(defaultPeriod);

    this.priceStream.prices$
      .pipe(
        takeUntil(this.destroy$),
        filter((list) => list.some((p) => p.symbol === this.fullSymbol)),
      )
      .subscribe((list) => {
        const current = list.find((p) => p.symbol === this.fullSymbol);
        if (current) {
          this.tick = current;
          if (!this.selectedPeriod) this.appendRealtimePrice(Number(current.lastPrice));
          this.cdr.markForCheck();
        }
      });
  }

  ngAfterViewInit(): void {
    this.initChart();
    if (this.pendingChartData?.length) {
      this.updateChartWithData(this.pendingChartData);
      this.pendingChartData = null;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.chart?.destroy();
    this.chart = null;
  }

  isSelectedPeriod(p: PeriodPreset): boolean {
    return this.selectedPeriod !== null && this.selectedPeriod.days === p.days;
  }

  selectPeriod(p: PeriodPreset): void {
    this.selectedPeriod = p;
    this.loadMarketChart(p);
  }

  private loadMarketChart(p: PeriodPreset): void {
    this.loadingKlines = true;
    this.cdr.markForCheck();
    this.api.getMarketChart(this.fullSymbol, p.days).pipe(takeUntil(this.destroy$)).subscribe({
      next: (klines) => {
        this.loadingKlines = false;
        this.applyKlinesToChart(klines);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingKlines = false;
        this.chartDataLength = 0;
        this.updateChartWithData([]);
        this.cdr.markForCheck();
      },
    });
  }

  private applyKlinesToChart(klines: Kline[]): void {
    const data = klines.map((k) => ({ t: k.openTime, y: k.close }));
    this.chartDataLength = data.length;
    this.pendingChartData = data;
    this.updateChartWithData(data);
  }

  getIconUrl(symbol: string): string | null {
    return getCryptoIconUrl(symbol);
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

  periodButtonClasses(p: PeriodPreset): Record<string, boolean> {
    const sel = this.isSelectedPeriod(p);
    return {
      'bg-emerald-500/20 text-emerald-400': sel,
      'bg-slate-700/50 text-slate-400 hover:bg-slate-600/50': !sel,
    };
  }

  changeClasses(priceChangePercent: string | number): Record<string, boolean> {
    const up = this.isPositive(priceChangePercent);
    return {
      'bg-emerald-500/15 text-emerald-400': up,
      'bg-rose-500/15 text-rose-400': !up,
    };
  }

  canTrade(): boolean {
    const amount = Number(this.tradeAmount);
    return !isNaN(amount) && amount > 0 && !!this.tick;
  }

  onBuy(): void {
    if (!this.canTrade() || this.tradeLoading || !this.tick) return;
    const amount = Number(this.tradeAmount);
    this.tradeError = '';
    this.tradeSuccess = '';
    this.tradeLoading = true;
    this.cdr.markForCheck();
    this.api.buy(this.symbol, amount, Number(this.tick.lastPrice)).subscribe({
      next: () => {
        this.tradeLoading = false;
        this.tradeSuccess = 'Achat effectué.';
        this.tradeAmount = null;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.tradeLoading = false;
        this.tradeError = err.error?.message || 'Achat impossible.';
        this.cdr.markForCheck();
      },
    });
  }

  onSell(): void {
    if (!this.canTrade() || this.tradeLoading || !this.tick) return;
    const amount = Number(this.tradeAmount);
    this.tradeError = '';
    this.tradeSuccess = '';
    this.tradeLoading = true;
    this.cdr.markForCheck();
    this.api.sell(this.symbol, amount, Number(this.tick.lastPrice)).subscribe({
      next: () => {
        this.tradeLoading = false;
        this.tradeSuccess = 'Vente effectuée.';
        this.tradeAmount = null;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.tradeLoading = false;
        this.tradeError = err.error?.message || 'Vente impossible.';
        this.cdr.markForCheck();
      },
    });
  }

  private appendRealtimePrice(price: number): void {
    const t = Date.now();
    this.priceHistory.push({ t, y: price });
    if (this.priceHistory.length > MAX_REALTIME_POINTS) this.priceHistory.shift();
    this.chartDataLength = this.priceHistory.length;
    this.updateChartWithData(this.priceHistory);
  }

  private initChart(): void {
    if (!this.chartCanvas?.nativeElement) return;
    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: [],
        datasets: [
          {
            label: 'Prix (USDT)',
            data: [],
            borderColor: 'rgb(16, 185, 129)',
            backgroundColor: 'rgba(16, 185, 129, 0.08)',
            fill: true,
            tension: 0.3,
            pointRadius: 2,
            pointHoverRadius: 6,
            pointBackgroundColor: 'rgb(16, 185, 129)',
            pointBorderColor: 'rgb(16, 185, 129)',
            pointBorderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { intersect: false, mode: 'index' },
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              title: (items) => {
                if (!items?.length) return '';
                const ts = Number(items[0]!.label);
                if (!ts) return String(items[0]!.label);
                return new Date(ts).toLocaleDateString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });
              },
            },
          },
        },
        scales: {
          x: {
            display: true,
            grid: { color: 'rgba(148, 163, 184, 0.1)' },
            ticks: {
              color: 'rgb(148, 163, 184)',
              maxTicksLimit: 8,
              callback: (value, index, ticks) => {
                const labels = this.chart?.data.labels;
                if (!labels?.length || index >= labels.length) return '';
                const ts = Number(labels![index]);
                if (!ts) return '';
                return new Date(ts).toLocaleDateString('fr-FR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
              },
            },
          },
          y: {
            display: true,
            grid: { color: 'rgba(148, 163, 184, 0.1)' },
            ticks: {
              color: 'rgb(148, 163, 184)',
              callback: (v) => (typeof v === 'number' ? this.formatPrice(v) : v),
            },
          },
        },
      },
    };

    this.chart = new Chart(ctx, config);
  }

  private updateChartWithData(data: { t: number; y: number }[]): void {
    if (!this.chart) return;
    this.chart.data.labels = data.map((d) => d.t);
    this.chart.data.datasets[0]!.data = data.map((d) => d.y);
    this.chart.update('none');
  }
}
