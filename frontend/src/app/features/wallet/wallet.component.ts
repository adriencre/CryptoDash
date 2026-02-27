import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef, inject, ChangeDetectorRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, interval } from 'rxjs';
import { takeUntil, startWith } from 'rxjs/operators';
import { ApiService, WalletSummaryDto, WalletPositionDto, TransactionDto, PerformancePointDto, PnlSummaryDto } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { PriceStreamService } from '../../core/services/price-stream.service';
import { PriceTick } from '../../core/models/price-tick.model';
import { getCryptoIconUrl } from '../../core/constants/crypto-icons';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NgClass, DecimalPipe } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

export type TotalCurrency = 'USDT' | 'USD' | 'EUR' | 'BTC' | 'SOL';

const COINGECKO_EUR_URL = 'https://api.coingecko.com/api/v3/simple/price?ids=tether&vs_currencies=eur';

const DONUT_COLORS = ['rgb(249, 115, 22)', 'rgb(168, 85, 247)', 'rgb(16, 185, 129)', 'rgb(59, 130, 246)', 'rgb(234, 179, 8)'];

const PERFORMANCE_PERIODS: { label: string; value: '7d' | '30d' | '90d' }[] = [
  { label: '7 j', value: '7d' },
  { label: '30 j', value: '30d' },
  { label: '90 j', value: '90d' },
];

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [RouterLink, FormsModule, NgClass, DecimalPipe],
  template: `
    <div class="mb-8">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold text-white tracking-tight">Mon Portefeuille</h1>
          @if (!loading && !error && wallet?.positions?.length && totalValue != null) {
            <div class="mt-2 flex flex-wrap items-baseline gap-3">
              @if (displayTotal != null) {
                <span class="text-3xl font-bold text-white tabular-nums">{{ displayTotal.formatted }} {{ displayTotal.suffix }}</span>
              }
              @if (change24hPercent != null) {
                <span class="inline-flex items-center gap-1 text-sm font-medium tabular-nums" [ngClass]="change24hPercent >= 0 ? 'text-emerald-400' : 'text-rose-400'">
                  <svg class="w-4 h-4 shrink-0" [class.rotate-180]="change24hPercent < 0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"/></svg>
                  {{ change24hPercent >= 0 ? '+' : '' }}{{ formatPrice(change24hUsdt ?? 0) }} ({{ change24hPercent >= 0 ? '+' : '' }}{{ change24hPercent.toFixed(2) }} %)
                </span>
              }
            </div>
          } @else if (!loading && !error) {
            <p class="mt-1 text-slate-400 text-sm">Solde fictif · Mise à jour en temps réel</p>
          }
        </div>
        @if (!loading && !error && wallet?.positions?.length) {
          <div class="flex flex-wrap items-center gap-3">
            <select [(ngModel)]="totalDisplayCurrency" (ngModelChange)="onTotalCurrencyChange()"
                    class="rounded-lg border border-slate-600 bg-slate-800/80 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50">
              <option value="USDT">USDT</option>
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
              <option value="BTC">BTC</option>
              <option value="SOL">SOL</option>
            </select>
            <a routerLink="/dashboard" class="rounded-xl bg-emerald-500 hover:bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white transition-colors">+ Acheter</a>
            <button type="button" (click)="openSendModal()" class="rounded-xl border border-slate-600 bg-slate-800/80 hover:bg-slate-700 px-4 py-2.5 text-sm font-medium text-white transition-colors">Envoyer</button>
            <button type="button" (click)="openReceiveModal()" class="rounded-xl border border-slate-600 bg-slate-800/80 hover:bg-slate-700 px-4 py-2.5 text-sm font-medium text-white transition-colors">Recevoir</button>
            <button type="button" (click)="openDepositModal()" class="rounded-xl border border-slate-600 bg-slate-800/80 hover:bg-slate-700 px-4 py-2.5 text-sm font-medium text-white transition-colors">Déposer</button>
          </div>
        }
      </div>
    </div>

    @if (loading) {
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-8 text-center text-slate-500">Chargement…</div>
    } @else if (error) {
      <div class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-8 text-center text-rose-400">{{ error }}</div>
    } @else {
      <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div class="xl:col-span-2 space-y-6">
          <section class="rounded-2xl border border-slate-700/80 bg-slate-800/40 overflow-hidden">
            <div class="p-4 border-b border-slate-700/80 flex items-center justify-between">
              <h2 class="text-sm font-semibold text-slate-300 uppercase tracking-wider">Vos Actifs</h2>
            </div>
            @if (wallet?.positions?.length) {
              <div class="divide-y divide-slate-700/50">
                @for (pos of (wallet?.positions ?? []); track pos.symbol) {
                  <div class="p-4 flex flex-wrap items-center gap-4 hover:bg-slate-800/50 transition-colors">
                    <div class="flex items-center gap-3 min-w-0">
                      @if (getIconUrl(pos.symbol) && !iconFailed(pos.symbol)) {
                        <img [src]="getIconUrl(pos.symbol)!" [alt]="pos.symbol" class="w-10 h-10 rounded-full object-cover ring-2 ring-slate-600/50" (error)="setIconFailed(pos.symbol)" />
                      } @else {
                        <span class="w-10 h-10 rounded-full bg-slate-600/80 flex items-center justify-center text-white font-bold shrink-0">{{ pos.symbol.charAt(0) }}</span>
                      }
                      <div>
                        <div class="font-semibold text-white">{{ pos.symbol }}</div>
                        <div class="text-slate-400 text-sm tabular-nums">
                          {{ getPositionValue(pos) != null ? formatPrice(getPositionValue(pos)!) : '—' }} · {{ formatAmount(asNumber(pos.amount)) }} {{ pos.symbol }}
                        </div>
                      </div>
                    </div>
                    <div class="flex-1 min-w-0 flex flex-wrap items-center justify-end gap-4">
                      <div class="text-right">
                        @if (pos.symbol === 'USDT') {
                          <span class="text-white font-medium tabular-nums">1.00</span>
                          <span class="text-slate-500 text-xs ml-1">+0 %</span>
                        } @else {
                          @if (getPositionPrice(pos) != null) {
                            <span class="text-white font-medium tabular-nums">{{ formatPrice(asNumber(getPositionPrice(pos))) }}</span>
                            @if (getPriceChangePercent(pos) != null) {
                              <span class="text-xs ml-1 tabular-nums" [ngClass]="asNumber(getPriceChangePercent(pos)) >= 0 ? 'text-emerald-400' : 'text-rose-400'">
                                {{ asNumber(getPriceChangePercent(pos)) >= 0 ? '+' : '' }}{{ getPriceChangePercent(pos) }} %
                              </span>
                            }
                          } @else {
                            <span class="text-slate-500">—</span>
                          }
                        }
                      </div>
                      <div class="w-24">
                        <div class="flex justify-between text-xs text-slate-400 mb-1">
                          <span>Allocation</span>
                          <span class="tabular-nums">{{ getAllocationPercent(pos) != null ? (getAllocationPercent(pos)! * 100).toFixed(0) : 0 }} %</span>
                        </div>
                        <div class="h-2 rounded-full bg-slate-700/80 overflow-hidden">
                          <div class="h-full rounded-full transition-all duration-300" [style.width.%]="getAllocationPercent(pos) != null ? getAllocationPercent(pos)! * 100 : 0" [style.background]="getAllocationColor(pos.symbol)"></div>
                        </div>
                      </div>
                    </div>
                    @if (pos.symbol !== 'USDT') {
                      <a [routerLink]="['/dashboard', pos.symbol]" class="text-emerald-400 hover:underline text-sm shrink-0">Voir cours</a>
                    }
                  </div>
                }
              </div>
            } @else {
              <p class="p-8 text-center text-slate-500">Aucune position. Votre solde USDT sera créé au premier chargement.</p>
            }
          </section>

          <section class="rounded-2xl border border-slate-700/80 bg-slate-800/40 overflow-hidden">
            <div class="p-4 border-b border-slate-700/80 flex flex-wrap items-center justify-between gap-4">
              <h2 class="text-sm font-semibold text-slate-300 uppercase tracking-wider">Évolution du portefeuille</h2>
              <div class="flex flex-wrap gap-1">
                @for (p of performancePeriods; track p.value) {
                  <button type="button" (click)="selectPerformancePeriod(p.value)" class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors" [ngClass]="performancePeriod === p.value ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40' : 'border border-slate-600 text-slate-400 hover:bg-slate-700/50'">
                    {{ p.label }}
                  </button>
                }
              </div>
            </div>
            <div class="p-4">
              @if (performanceLoading) {
                <div class="h-[280px] flex items-center justify-center text-slate-500 text-sm">Chargement…</div>
              } @else if (!performanceData.length) {
                <div class="h-[280px] flex items-center justify-center text-slate-500 text-sm">Aucun historique pour l’instant. Les points s’enregistrent au fil du temps.</div>
              } @else {
                <div class="relative w-full" style="height: 280px;">
                  <canvas #performanceCanvas class="block w-full" style="height: 280px; width: 100%;"></canvas>
                </div>
              }
            </div>
          </section>

          <section class="rounded-2xl border border-slate-700/80 bg-slate-800/40 overflow-hidden">
            <div class="p-4 border-b border-slate-700/80 flex items-center justify-between">
              <h2 class="text-sm font-semibold text-slate-300 uppercase tracking-wider">Historique Récent</h2>
              <a routerLink="/history" class="text-emerald-400 hover:underline text-sm">Voir tout</a>
            </div>
            @if (recentTransactions.length) {
              <ul class="divide-y divide-slate-700/50">
                @for (tx of recentTransactions; track tx.id) {
                  <li class="p-3 flex items-center justify-between gap-3 hover:bg-slate-800/50 transition-colors">
                    <div class="flex items-center gap-3 min-w-0">
                      <span class="w-8 h-8 rounded-full flex items-center justify-center shrink-0" [ngClass]="tx.type === 'BUY' || tx.type === 'RECEIVE' || tx.type === 'DEPOSIT' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-rose-500/20 text-rose-400'">
                        @if (tx.type === 'BUY') {
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/></svg>
                        } @else if (tx.type === 'DEPOSIT') {
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"/></svg>
                        } @else if (tx.type === 'SEND') {
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/></svg>
                        } @else if (tx.type === 'RECEIVE') {
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4m0 0l7-7m-7 7l7 7"/></svg>
                        } @else {
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/></svg>
                        }
                      </span>
                      <div class="min-w-0">
                        <p class="text-sm font-medium text-white truncate">{{ txLabel(tx) }}</p>
                        <p class="text-slate-500 text-xs">{{ formatTransactionDate(tx.createdAt) }}</p>
                      </div>
                    </div>
                    <div class="text-right shrink-0">
                      <span class="text-sm font-medium tabular-nums" [ngClass]="tx.type === 'BUY' || tx.type === 'RECEIVE' || tx.type === 'DEPOSIT' ? 'text-emerald-400' : 'text-rose-400'">
                        {{ (tx.type === 'BUY' || tx.type === 'RECEIVE' || tx.type === 'DEPOSIT') ? '+' : '-' }}{{ formatAmount(tx.amount) }} {{ tx.symbol }}
                      </span>
                      @if (tx.type === 'BUY' || tx.type === 'SELL') {
                        <span class="text-slate-400 text-xs tabular-nums block">{{ formatPrice(tx.totalUsdt) }} USDT</span>
                      }
                    </div>
                  </li>
                }
              </ul>
            } @else {
              <div class="p-6 text-center text-slate-500 text-sm">
                Aucune transaction récente. Les achats et ventes apparaîtront ici.
              </div>
            }
          </section>
        </div>

        <div class="space-y-6">
          <section class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-6">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-sm font-semibold text-slate-300 uppercase tracking-wider">Mon Portefeuille</h2>
              @if (totalValue != null && displayTotal != null) {
                <span class="text-white font-semibold tabular-nums">TOTAL {{ displayTotal.formatted }} {{ displayTotal.suffix }}</span>
              }
            </div>
            <div class="relative w-full aspect-square max-w-[280px] mx-auto">
              <canvas #donutCanvas class="w-full h-full"></canvas>
            </div>
            @if (donutLabels().length) {
              <div class="flex flex-wrap justify-center gap-x-4 gap-y-1 mt-4 text-xs">
                @for (lab of donutLabels(); track lab.symbol) {
                  <span class="flex items-center gap-1.5">
                    <span class="w-2.5 h-2.5 rounded-full shrink-0" [style.background]="lab.color"></span>
                    <span class="text-slate-400">{{ lab.symbol }}</span>
                  </span>
                }
              </div>
            }
          </section>

          <section class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-6">
            <h2 class="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">Performance</h2>
            @if (bestPerformer(); as best) {
              <div class="mb-3">
                <p class="text-slate-500 text-xs mb-0.5">Meilleure perf. (24h)</p>
                <p class="text-emerald-400 font-medium">{{ best.symbol }} ({{ best.percent >= 0 ? '+' : '' }}{{ best.percent }} %)</p>
              </div>
            }
            @if (worstPerformer(); as worst) {
              <div>
                <p class="text-slate-500 text-xs mb-0.5">Moins bonne perf. (24h)</p>
                <p class="text-rose-400 font-medium">{{ worst.symbol }} ({{ worst.percent }} %)</p>
              </div>
            }
            @if (!bestPerformer() && !worstPerformer()) {
              <p class="text-slate-500 text-sm">Données 24h en attente…</p>
            }
          </section>

          <section class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-6">
            <h2 class="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">P&amp;L</h2>
            @if (pnlLoading) {
              <p class="text-slate-500 text-sm">Chargement…</p>
            } @else if (pnl) {
              <div class="space-y-3">
                <div>
                  <p class="text-slate-500 text-xs mb-0.5">P&amp;L réalisé</p>
                  <p class="font-medium tabular-nums" [ngClass]="pnl.realisedUsdt >= 0 ? 'text-emerald-400' : 'text-rose-400'">
                    {{ pnl.realisedUsdt >= 0 ? '+' : '' }}{{ formatPrice(pnl.realisedUsdt) }} USDT
                  </p>
                </div>
                <div>
                  <p class="text-slate-500 text-xs mb-0.5">P&amp;L non réalisé</p>
                  <p class="font-medium tabular-nums" [ngClass]="pnl.unrealisedUsdt >= 0 ? 'text-emerald-400' : 'text-rose-400'">
                    {{ pnl.unrealisedUsdt >= 0 ? '+' : '' }}{{ formatPrice(pnl.unrealisedUsdt) }} USDT
                  </p>
                </div>
              </div>
            } @else {
              <p class="text-slate-500 text-sm">Données en attente.</p>
            }
          </section>

          <section class="rounded-2xl border border-slate-700/80 bg-slate-800/40 p-6">
            <div class="flex items-center gap-2 mb-3">
              <h2 class="text-sm font-semibold text-slate-300 uppercase tracking-wider">Analyse IA</h2>
              <span class="rounded px-2 py-0.5 text-xs font-medium bg-slate-600/80 text-slate-300">BETA</span>
            </div>
            <p class="text-slate-500 text-sm">Votre portefeuille est fortement diversifié. Analyse détaillée à venir.</p>
          </section>
        </div>
      </div>
    }

    @if (showSendModal) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60" (click)="closeSendModal()">
        <div class="rounded-2xl border border-slate-700 bg-slate-800 w-full max-w-md shadow-xl" (click)="$event.stopPropagation()">
          <div class="p-6 border-b border-slate-700">
            <h2 class="text-lg font-semibold text-white">Envoyer des crypto</h2>
            <p class="text-slate-400 text-sm mt-1">Indiquez le destinataire (email ou nom de compte) et l'actif.</p>
          </div>
          <form (ngSubmit)="submitSend()" class="p-6 space-y-4">
            @if (sendError) {
              <p class="text-rose-400 text-sm">{{ sendError }}</p>
            }
            @if (sendSuccess) {
              <p class="text-emerald-400 text-sm">{{ sendSuccess }}</p>
            }
            <div>
              <label for="send-recipient" class="block text-sm font-medium text-slate-400 mb-1">Destinataire (email ou nom de compte)</label>
              <input id="send-recipient" type="text" [(ngModel)]="sendForm.recipientIdentifier" name="recipientIdentifier" required
                     class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="alice_crypto ou alice&#64;mail.com" />
            </div>
            <div>
              <label for="send-symbol" class="block text-sm font-medium text-slate-400 mb-1">Actif</label>
              @if (cryptoPositions().length) {
                <select id="send-symbol" [(ngModel)]="sendForm.symbol" name="symbol" required
                        class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/50">
                  @for (pos of cryptoPositions(); track pos.symbol) {
                    <option [value]="pos.symbol">{{ pos.symbol }}</option>
                  }
                </select>
              } @else {
                <p class="text-slate-500 text-sm py-2">Aucun actif à envoyer. Achetez d'abord des crypto sur le tableau de bord.</p>
              }
            </div>
            <div>
              <label for="send-amount" class="block text-sm font-medium text-slate-400 mb-1">Quantité</label>
              <input id="send-amount" type="number" step="any" min="0" [(ngModel)]="sendForm.amount" name="amount" required
                     class="w-full rounded-xl border border-slate-600 bg-slate-800/80 px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50" placeholder="0.001" />
            </div>
            <div class="flex gap-3 pt-2">
              <button type="submit" [disabled]="sending || !cryptoPositions().length" class="flex-1 rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium py-2.5 transition-colors">
                @if (sending) { Envoi… } @else { Envoyer }
              </button>
              <button type="button" (click)="closeSendModal()" class="rounded-xl border border-slate-600 text-slate-400 hover:bg-slate-700/50 px-4 py-2.5 transition-colors">Annuler</button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (showReceiveModal) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60" (click)="closeReceiveModal()">
        <div class="rounded-2xl border border-slate-700 bg-slate-800 w-full max-w-md shadow-xl" (click)="$event.stopPropagation()">
          <div class="p-6 border-b border-slate-700">
            <h2 class="text-lg font-semibold text-white">Recevoir des crypto</h2>
            <p class="text-slate-400 text-sm mt-1">Partagez votre nom de compte ou email pour recevoir des virements.</p>
          </div>
          <div class="p-6 space-y-4">
            <div>
              <label class="block text-sm font-medium text-slate-400 mb-1">Votre identifiant pour recevoir</label>
              <div class="flex items-center gap-2">
                <code class="flex-1 rounded-xl border border-slate-600 bg-slate-900/80 px-4 py-3 text-white font-mono text-sm break-all">{{ myAccountName || '—' }}</code>
                @if (myAccountName) {
                  <button type="button" (click)="copyAccountName()" class="shrink-0 rounded-xl bg-slate-600 hover:bg-slate-500 px-4 py-3 text-white text-sm font-medium transition-colors">
                    {{ copiedToClipboard ? 'Copié' : 'Copier' }}
                  </button>
                }
              </div>
              @if (!myAccountName) {
                <p class="text-slate-500 text-xs mt-2">Définissez un nom de compte dans <a routerLink="/settings" class="text-emerald-400 hover:underline">Paramètres</a> ou utilisez votre email.</p>
              }
            </div>
            <button type="button" (click)="closeReceiveModal()" class="w-full rounded-xl border border-slate-600 text-slate-400 hover:bg-slate-700/50 py-2.5 transition-colors">Fermer</button>
          </div>
        </div>
      </div>
    }

    @if (showDepositModal) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4" style="background: rgba(0,0,0,0.75); backdrop-filter: blur(4px);" (click)="onDepositBackdropClick($event)">
        <div class="rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden" style="background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); border: 1px solid rgba(100,116,139,0.4);" (click)="$event.stopPropagation()">

          <!-- Header -->
          <div class="p-6 border-b" style="border-color: rgba(100,116,139,0.3);">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl flex items-center justify-center" style="background: linear-gradient(135deg, #10b981, #059669);">
                  <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"/></svg>
                </div>
                <div>
                  <h2 class="text-lg font-bold text-white">Ajouter des USDT</h2>
                  <p class="text-xs" style="color: rgba(148,163,184,0.8);">Paiement sécurisé simulé</p>
                </div>
              </div>
              <button type="button" (click)="closeDepositModal()" class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors" style="color: #94a3b8;" onmouseenter="this.style.background='rgba(100,116,139,0.2)'" onmouseleave="this.style.background='transparent'">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>

            <!-- Steps indicator -->
            @if (depositStep < 3) {
              <div class="flex items-center gap-2 mt-5">
                @for (s of [1,2,3]; track s) {
                  <div class="flex items-center gap-2">
                    <div class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all duration-300"
                         [style.background]="depositStep >= s ? 'linear-gradient(135deg,#10b981,#059669)' : 'rgba(30,41,59,1)'"
                         [style.border]="depositStep >= s ? 'none' : '1px solid rgba(100,116,139,0.5)'"
                         [style.color]="depositStep >= s ? 'white' : '#64748b'">
                      {{ s }}
                    </div>
                    @if (s < 3) {
                      <div class="flex-1 h-px transition-all duration-300" style="min-width: 40px;"
                           [style.background]="depositStep > s ? 'linear-gradient(90deg,#10b981,#059669)' : 'rgba(100,116,139,0.3)'"></div>
                    }
                  </div>
                }
                <span class="ml-2 text-xs" style="color:#64748b;">
                  @if (depositStep === 1) { Montant }
                  @else if (depositStep === 2) { Paiement }
                </span>
              </div>
            }
          </div>

          <!-- Step 1 : Choisir le montant -->
          @if (depositStep === 1) {
            <div class="p-6 space-y-5">
              <div>
                <label class="block text-sm font-medium mb-3" style="color:#94a3b8;">Montants suggérés</label>
                <div class="grid grid-cols-3 gap-2">
                  @for (preset of depositPresets; track preset) {
                    <button type="button" (click)="selectPreset(preset)"
                            class="py-3 rounded-xl text-sm font-semibold transition-all duration-200"
                            [style.background]="depositForm.amount === preset ? 'linear-gradient(135deg,rgba(16,185,129,0.25),rgba(5,150,105,0.25))' : 'rgba(30,41,59,0.8)'"
                            [style.border]="depositForm.amount === preset ? '1px solid rgba(16,185,129,0.6)' : '1px solid rgba(100,116,139,0.3)'"
                            [style.color]="depositForm.amount === preset ? '#10b981' : '#94a3b8'">
                      {{ preset | number:'1.0-0' }} USDT
                    </button>
                  }
                </div>
              </div>
              <div>
                <label for="deposit-custom" class="block text-sm font-medium mb-2" style="color:#94a3b8;">Ou saisir un montant personnalisé</label>
                <div class="relative">
                  <input id="deposit-custom" type="number" step="1" min="1" [(ngModel)]="depositForm.amount" name="depositCustom"
                         class="w-full rounded-xl px-4 py-3 pr-20 text-white text-lg font-semibold focus:outline-none transition-all"
                         style="background: rgba(15,23,42,0.8); border: 1px solid rgba(100,116,139,0.4); color: white;"
                         onfocus="this.style.borderColor='rgba(16,185,129,0.6)'"
                         onblur="this.style.borderColor='rgba(100,116,139,0.4)'"
                         placeholder="0" />
                  <span class="absolute right-4 top-1/2 -translate-y-1/2 text-sm font-bold" style="color:#10b981;">USDT</span>
                </div>
              </div>
              @if (depositError) {
                <p class="text-sm" style="color:#f87171;">{{ depositError }}</p>
              }
              <button type="button" (click)="goToPaymentStep()"
                      class="w-full py-3 rounded-xl font-bold text-white transition-all duration-200 text-sm"
                      style="background: linear-gradient(135deg, #10b981, #059669);">
                Continuer vers le paiement →
              </button>
            </div>
          }

          <!-- Step 2 : Formulaire carte -->
          @if (depositStep === 2) {
            <div class="p-6 space-y-5">
              <!-- Card preview -->
              <div class="rounded-2xl p-5 relative overflow-hidden" style="background: linear-gradient(135deg, #1e3a5f 0%, #0f2844 50%, #1a1a4e 100%); min-height: 140px;">
                <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 20% 50%, white 1px, transparent 1px), radial-gradient(circle at 80% 20%, white 1px, transparent 1px); background-size: 30px 30px;"></div>
                <div class="flex justify-between items-start mb-6">
                  <div>
                    <p class="text-xs" style="color:rgba(255,255,255,0.5);">Solde à créditer</p>
                    <p class="text-2xl font-bold text-white">{{ depositForm.amount | number:'1.0-2' }} <span style="color:#10b981;">USDT</span></p>
                  </div>
                  <div class="w-12 h-8 rounded-md" style="background: linear-gradient(135deg, #f59e0b, #d97706);"></div>
                </div>
                <div class="flex justify-between items-end">
                  <div>
                    <p class="text-xs mb-1" style="color:rgba(255,255,255,0.5);">Numéro</p>
                    <p class="text-sm font-mono text-white tracking-widest">{{ cardDisplay }}</p>
                  </div>
                  <div class="text-right">
                    <p class="text-xs mb-1" style="color:rgba(255,255,255,0.5);">Expire</p>
                    <p class="text-sm font-mono text-white">{{ cardForm.expiry || 'MM/AA' }}</p>
                  </div>
                </div>
              </div>

              <!-- Champs -->
              <div class="space-y-4">
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider mb-2" style="color:#64748b;">Titulaire de la carte</label>
                  <input type="text" [(ngModel)]="cardForm.name" name="cardName" autocomplete="cc-name"
                         class="w-full rounded-xl px-4 py-3 text-white focus:outline-none transition-all"
                         style="background: rgba(15,23,42,0.8); border: 1px solid rgba(100,116,139,0.4);"
                         onfocus="this.style.borderColor='rgba(16,185,129,0.6)'"
                         onblur="this.style.borderColor='rgba(100,116,139,0.4)'"
                         placeholder="Jean Dupont" />
                </div>
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider mb-2" style="color:#64748b;">Numéro de carte</label>
                  <input type="text" [(ngModel)]="cardForm.number" name="cardNumber" autocomplete="cc-number" maxlength="19"
                         (input)="onCardNumberInput($event)"
                         class="w-full rounded-xl px-4 py-3 text-white font-mono tracking-widest focus:outline-none transition-all"
                         style="background: rgba(15,23,42,0.8); border: 1px solid rgba(100,116,139,0.4);"
                         onfocus="this.style.borderColor='rgba(16,185,129,0.6)'"
                         onblur="this.style.borderColor='rgba(100,116,139,0.4)'"
                         placeholder="0000 0000 0000 0000" />
                </div>
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wider mb-2" style="color:#64748b;">Date d'expiration</label>
                    <input type="text" [(ngModel)]="cardForm.expiry" name="cardExpiry" autocomplete="cc-exp" maxlength="5"
                           (input)="onExpiryInput($event)"
                           class="w-full rounded-xl px-4 py-3 text-white font-mono focus:outline-none transition-all"
                           style="background: rgba(15,23,42,0.8); border: 1px solid rgba(100,116,139,0.4);"
                           onfocus="this.style.borderColor='rgba(16,185,129,0.6)'"
                           onblur="this.style.borderColor='rgba(100,116,139,0.4)'"
                           placeholder="MM/AA" />
                  </div>
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wider mb-2" style="color:#64748b;">CVV</label>
                    <input type="password" [(ngModel)]="cardForm.cvv" name="cardCvv" autocomplete="cc-csc" maxlength="4"
                           class="w-full rounded-xl px-4 py-3 text-white font-mono focus:outline-none transition-all"
                           style="background: rgba(15,23,42,0.8); border: 1px solid rgba(100,116,139,0.4);"
                           onfocus="this.style.borderColor='rgba(16,185,129,0.6)'"
                           onblur="this.style.borderColor='rgba(100,116,139,0.4)'"
                           placeholder="•••" />
                  </div>
                </div>
              </div>

              @if (depositError) {
                <p class="text-sm" style="color:#f87171;">{{ depositError }}</p>
              }

              <div class="flex gap-3">
                <button type="button" (click)="depositStep = 1; depositError = ''"
                        class="rounded-xl px-4 py-3 text-sm font-medium transition-colors"
                        style="border: 1px solid rgba(100,116,139,0.4); color: #94a3b8; background:transparent;"
                        onmouseenter="this.style.background='rgba(100,116,139,0.1)'"
                        onmouseleave="this.style.background='transparent'">← Retour</button>
                <button type="button" (click)="submitPayment()" [disabled]="depositing"
                        class="flex-1 py-3 rounded-xl font-bold text-white transition-all duration-200 text-sm"
                        style="background: linear-gradient(135deg, #10b981, #059669);">
                  @if (depositing) { <span>Traitement…</span> } @else { Payer {{ depositForm.amount | number:'1.0-2' }} USDT }
                </button>
              </div>

              <p class="text-center text-xs" style="color:#475569;">
                <svg class="w-3 h-3 inline mr-1" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd"/></svg>
                Paiement simulé · Aucune donnée réelle transmise
              </p>
            </div>
          }

          <!-- Step 3 : Traitement -->
          @if (depositStep === 3) {
            <div class="p-12 flex flex-col items-center gap-6">
              <div class="relative w-20 h-20">
                <div class="absolute inset-0 rounded-full" style="border: 3px solid rgba(16,185,129,0.2);"></div>
                <div class="absolute inset-0 rounded-full animate-spin" style="border: 3px solid transparent; border-top-color: #10b981;"></div>
                <div class="absolute inset-0 rounded-full animate-spin" style="border: 3px solid transparent; border-right-color: #059669; animation-duration: 1.5s;"></div>
                <div class="absolute inset-2 rounded-full flex items-center justify-center" style="background: rgba(16,185,129,0.1);">
                  <svg class="w-8 h-8" style="color:#10b981;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"/></svg>
                </div>
              </div>
              <div class="text-center">
                <p class="text-white font-bold text-lg">Traitement en cours…</p>
                <p class="text-sm mt-1" style="color:#64748b;">Vérification du paiement · Ne fermez pas cette fenêtre</p>
              </div>
              <div class="w-full rounded-full h-1 overflow-hidden" style="background: rgba(30,41,59,1);">
                <div class="h-full rounded-full" style="background: linear-gradient(90deg,#10b981,#059669); animation: depositProgress 2s ease-in-out forwards;"></div>
              </div>
            </div>
          }

          <!-- Step 4 : Succès -->
          @if (depositStep === 4) {
            <div class="p-10 flex flex-col items-center gap-5">
              <div class="w-20 h-20 rounded-full flex items-center justify-center" style="background: linear-gradient(135deg, rgba(16,185,129,0.2), rgba(5,150,105,0.2)); border: 2px solid rgba(16,185,129,0.4);">
                <svg class="w-10 h-10" style="color:#10b981;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
              </div>
              <div class="text-center">
                <p class="text-2xl font-bold text-white">+{{ depositForm.amount | number:'1.0-2' }} USDT</p>
                <p class="font-semibold mt-1" style="color:#10b981;">Dépôt confirmé !</p>
                <p class="text-sm mt-2" style="color:#64748b;">Votre solde USDT a été mis à jour.</p>
              </div>
              <div class="w-full rounded-xl p-4" style="background: rgba(16,185,129,0.08); border: 1px solid rgba(16,185,129,0.2);">
                <div class="flex justify-between text-sm">
                  <span style="color:#64748b;">Montant</span>
                  <span class="font-semibold text-white">{{ depositForm.amount | number:'1.0-2' }} USDT</span>
                </div>
                <div class="flex justify-between text-sm mt-2">
                  <span style="color:#64748b;">Méthode</span>
                  <span class="font-semibold text-white">Carte •••• {{ cardForm.number ? cardForm.number.slice(-4) : '****' }}</span>
                </div>
                <div class="flex justify-between text-sm mt-2">
                  <span style="color:#64748b;">Statut</span>
                  <span style="color:#10b981;" class="font-semibold">✓ Approuvé</span>
                </div>
              </div>
              <button type="button" (click)="closeDepositModal()"
                      class="w-full py-3 rounded-xl font-bold text-white transition-all"
                      style="background: linear-gradient(135deg, #10b981, #059669);">Fermer</button>
            </div>
          }

        </div>
      </div>
    }

    <style>
      &#64;keyframes depositProgress {
        from { width: 0%; }
        to { width: 100%; }
      }
    </style>
  `,
})
export class WalletComponent implements OnInit, OnDestroy, AfterViewInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly priceStream = inject(PriceStreamService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly http = inject(HttpClient);
  private readonly destroy$ = new Subject<void>();

  @ViewChild('donutCanvas') donutCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('performanceCanvas') performanceCanvas!: ElementRef<HTMLCanvasElement>;

  wallet: WalletSummaryDto | null = null;
  prices: PriceTick[] = [];
  recentTransactions: TransactionDto[] = [];
  loading = true;
  error = '';
  totalDisplayCurrency: TotalCurrency = 'USDT';
  eurRate: number | null = null;
  private iconFailedSet = new Set<string>();
  private donutChart: Chart<'doughnut'> | null = null;

  performancePeriods = PERFORMANCE_PERIODS;
  performancePeriod: '7d' | '30d' | '90d' = '7d';
  performanceData: { t: number; y: number }[] = [];
  performanceLoading = false;
  pnl: PnlSummaryDto | null = null;
  pnlLoading = false;
  private performanceChart: Chart<'line'> | null = null;

  showSendModal = false;
  showReceiveModal = false;
  showDepositModal = false;
  sendForm = { recipientIdentifier: '', symbol: 'BTC', amount: 0 };
  depositForm = { amount: 1000 };
  sendError = '';
  sendSuccess = '';
  depositError = '';
  depositSuccess = '';
  sending = false;
  depositing = false;

  // Payment flow
  depositStep = 1; // 1=montant, 2=carte, 3=traitement, 4=succès
  depositPresets = [100, 500, 1000, 2500, 5000, 10000];
  cardForm = { name: '', number: '', expiry: '', cvv: '' };

  get cardDisplay(): string {
    const raw = this.cardForm.number.replace(/\D/g, '');
    const padded = raw.padEnd(16, '\u2022');
    return padded.match(/.{1,4}/g)?.join(' ') ?? '•••• •••• •••• ••••';
  }
  myAccountName = '';
  copiedToClipboard = false;

  ngOnInit(): void {
    Chart.register(...registerables);

    this.api.getWallet().subscribe({
      next: (w) => {
        this.wallet = w;
        this.loading = false;
        this.cdr.markForCheck();
        this.updateDonutChart();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Impossible de charger le portefeuille.';
        this.cdr.markForCheck();
      },
    });

    this.priceStream.prices$.pipe(takeUntil(this.destroy$)).subscribe((list) => {
      this.prices = list;
      this.cdr.detectChanges();
      this.updateDonutChart();
    });

    this.api.getHistory(10).subscribe({
      next: (list) => {
        this.recentTransactions = list;
        this.cdr.markForCheck();
      },
      error: () => { },
    });

    this.auth.getProfile().subscribe({
      next: (p) => {
        this.myAccountName = p.accountName?.trim() || p.email?.trim() || '';
        this.cdr.markForCheck();
      },
      error: () => { },
    });

    interval(60_000).pipe(startWith(0), takeUntil(this.destroy$)).subscribe(() => this.fetchEurRate());

    this.loadPerformance('7d');
    this.pnlLoading = true;
    this.api.getPnl().subscribe({
      next: (p) => {
        this.pnl = p;
        this.pnlLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.pnlLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  selectPerformancePeriod(period: '7d' | '30d' | '90d'): void {
    this.performancePeriod = period;
    this.loadPerformance(period);
  }

  loadPerformance(period: '7d' | '30d' | '90d'): void {
    this.performanceLoading = true;
    this.cdr.markForCheck();
    this.api.getPerformance(period).subscribe({
      next: (points) => {
        this.performanceData = points.map((pt) => ({
          t: new Date(pt.timestamp).getTime(),
          y: pt.totalUsdt,
        }));
        this.performanceLoading = false;
        if (!this.performanceData.length) {
          this.performanceChart?.destroy();
          this.performanceChart = null;
        }
        this.cdr.markForCheck();
        setTimeout(() => {
          if (this.performanceData.length && this.performanceCanvas?.nativeElement && !this.performanceChart) {
            this.initPerformanceChart();
          }
          this.updatePerformanceChart();
        }, 0);
      },
      error: () => {
        this.performanceLoading = false;
        this.performanceData = [];
        this.performanceChart?.destroy();
        this.performanceChart = null;
        this.cdr.markForCheck();
      },
    });
  }

  private initPerformanceChart(): void {
    if (!this.performanceCanvas?.nativeElement) return;
    const ctx = this.performanceCanvas.nativeElement.getContext('2d');
    if (!ctx) return;
    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: [],
        datasets: [
          {
            label: 'Valeur (USDT)',
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
              label: (item) => (item.raw != null ? `Valeur: ${this.formatPrice(Number(item.raw))} USDT` : ''),
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
                const labels = this.performanceChart?.data.labels;
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
    this.performanceChart = new Chart(ctx, config);
  }

  private updatePerformanceChart(): void {
    if (!this.performanceChart) return;
    this.performanceChart.data.labels = this.performanceData.map((d) => d.t);
    this.performanceChart.data.datasets[0]!.data = this.performanceData.map((d) => d.y);
    this.performanceChart.update('none');
  }

  openSendModal(): void {
    this.showSendModal = true;
    this.sendError = '';
    this.sendSuccess = '';
    const firstCrypto = this.wallet?.positions?.find((p) => p.symbol !== 'USDT');
    this.sendForm = { recipientIdentifier: '', symbol: firstCrypto?.symbol ?? 'BTC', amount: 0 };
    this.cdr.markForCheck();
  }

  closeSendModal(): void {
    this.showSendModal = false;
    this.sendError = '';
    this.sendSuccess = '';
    this.cdr.markForCheck();
  }

  openReceiveModal(): void {
    this.showReceiveModal = true;
    this.copiedToClipboard = false;
    if (!this.myAccountName) {
      this.auth.getProfile().subscribe({
        next: (p) => {
          this.myAccountName = p.accountName?.trim() || p.email?.trim() || '';
          this.cdr.markForCheck();
        },
      });
    }
    this.cdr.markForCheck();
  }

  closeReceiveModal(): void {
    this.showReceiveModal = false;
    this.cdr.markForCheck();
  }

  openDepositModal(): void {
    this.showDepositModal = true;
    this.depositError = '';
    this.depositSuccess = '';
    this.depositStep = 1;
    this.depositForm = { amount: 1000 };
    this.cardForm = { name: '', number: '', expiry: '', cvv: '' };
    this.cdr.markForCheck();
  }

  closeDepositModal(): void {
    if (this.depositStep === 3) return; // Empêcher la fermeture pendant le traitement
    this.showDepositModal = false;
    this.depositError = '';
    this.depositSuccess = '';
    this.depositStep = 1;
    this.cdr.markForCheck();
  }

  onDepositBackdropClick(event: MouseEvent): void {
    if (this.depositStep !== 3) {
      this.closeDepositModal();
    }
  }

  selectPreset(amount: number): void {
    this.depositForm = { amount };
    this.depositError = '';
    this.cdr.markForCheck();
  }

  goToPaymentStep(): void {
    const amount = Number(this.depositForm.amount);
    if (!amount || amount < 1) {
      this.depositError = 'Le montant minimum est 1 USDT.';
      this.cdr.markForCheck();
      return;
    }
    if (amount > 1000000) {
      this.depositError = 'Le montant maximum est 1 000 000 USDT.';
      this.cdr.markForCheck();
      return;
    }
    this.depositError = '';
    this.depositStep = 2;
    this.cdr.markForCheck();
  }

  onCardNumberInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    // Formater le numéro avec des espaces tous les 4 chiffres
    const raw = input.value.replace(/\D/g, '').substring(0, 16);
    const formatted = raw.match(/.{1,4}/g)?.join(' ') ?? raw;
    this.cardForm.number = formatted;
    setTimeout(() => { input.value = formatted; }, 0);
    this.cdr.markForCheck();
  }

  onExpiryInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let val = input.value.replace(/\D/g, '').substring(0, 4);
    if (val.length >= 3) val = val.substring(0, 2) + '/' + val.substring(2);
    this.cardForm.expiry = val;
    setTimeout(() => { input.value = val; }, 0);
    this.cdr.markForCheck();
  }

  submitPayment(): void {
    this.depositError = '';
    // Validation basique
    if (!this.cardForm.name.trim()) {
      this.depositError = 'Veuillez indiquer le nom du titulaire.';
      this.cdr.markForCheck();
      return;
    }
    const cardRaw = this.cardForm.number.replace(/\D/g, '');
    if (cardRaw.length < 16) {
      this.depositError = 'Numéro de carte invalide (16 chiffres requis).';
      this.cdr.markForCheck();
      return;
    }
    if (!this.cardForm.expiry || this.cardForm.expiry.length < 5) {
      this.depositError = 'Date d\'expiration invalide (MM/AA).';
      this.cdr.markForCheck();
      return;
    }
    if (!this.cardForm.cvv || this.cardForm.cvv.length < 3) {
      this.depositError = 'CVV invalide (3 ou 4 chiffres).';
      this.cdr.markForCheck();
      return;
    }

    this.depositing = true;
    this.depositStep = 3; // Écran de traitement
    this.cdr.markForCheck();

    // Simuler un délai de traitement (2.2s pour voir l'animation)
    setTimeout(() => {
      this.api.deposit(Number(this.depositForm.amount)).subscribe({
        next: () => {
          this.depositing = false;
          this.depositStep = 4; // Succès
          this.cdr.markForCheck();
          // Mettre à jour le wallet et l'historique
          this.api.getWallet().subscribe((w) => {
            this.wallet = w;
            this.updateDonutChart();
            this.cdr.markForCheck();
          });
          this.api.getHistory(10).subscribe((list) => {
            this.recentTransactions = list;
            this.cdr.markForCheck();
          });
        },
        error: (err) => {
          this.depositing = false;
          this.depositStep = 2;
          this.depositError = err.error?.message || 'Erreur lors du dépôt. Veuillez réessayer.';
          this.cdr.markForCheck();
        },
      });
    }, 2200);
  }

  submitSend(): void {
    this.sendError = '';
    this.sendSuccess = '';
    const amount = Number(this.sendForm.amount);
    if (!this.sendForm.recipientIdentifier?.trim()) {
      this.sendError = 'Indiquez le destinataire.';
      return;
    }
    if (!amount || amount <= 0) {
      this.sendError = 'Indiquez une quantité valide.';
      return;
    }
    this.sending = true;
    this.cdr.markForCheck();
    this.api.sendCrypto(this.sendForm.recipientIdentifier.trim(), this.sendForm.symbol, amount).subscribe({
      next: () => {
        this.sending = false;
        this.sendSuccess = 'Virement envoyé.';
        this.cdr.markForCheck();
        this.api.getWallet().subscribe((w) => {
          this.wallet = w;
          this.cdr.markForCheck();
        });
        this.api.getHistory(10).subscribe((list) => {
          this.recentTransactions = list;
          this.cdr.markForCheck();
        });
        setTimeout(() => {
          this.closeSendModal();
        }, 1500);
      },
      error: (err) => {
        this.sending = false;
        this.sendError = err.error?.message || 'Erreur lors de l\'envoi.';
        this.cdr.markForCheck();
      },
    });
  }


  copyAccountName(): void {
    if (!this.myAccountName) return;
    navigator.clipboard.writeText(this.myAccountName).then(() => {
      this.copiedToClipboard = true;
      this.cdr.markForCheck();
      setTimeout(() => {
        this.copiedToClipboard = false;
        this.cdr.markForCheck();
      }, 2000);
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.updateDonutChart();
      if (this.performanceData.length) {
        this.initPerformanceChart();
        this.updatePerformanceChart();
      }
    }, 100);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.donutChart) {
      this.donutChart.destroy();
      this.donutChart = null;
    }
    if (this.performanceChart) {
      this.performanceChart.destroy();
      this.performanceChart = null;
    }
  }

  get totalValue(): number | null {
    if (!this.wallet?.positions?.length) return null;
    let sum = 0;
    for (const pos of this.wallet.positions) {
      const v = this.getPositionValue(pos);
      if (v == null) return null;
      sum += v;
    }
    return sum;
  }

  get change24hUsdt(): number | null {
    const total = this.totalValue;
    if (total == null || total === 0) return null;
    let weighted = 0;
    for (const pos of this.wallet?.positions ?? []) {
      const value = this.getPositionValue(pos);
      if (value == null) continue;
      const pct = this.getPriceChangePercent(pos);
      if (pct == null) continue;
      weighted += value * Number(pct) / 100;
    }
    return weighted;
  }

  get change24hPercent(): number | null {
    const total = this.totalValue;
    const change = this.change24hUsdt;
    if (total == null || total === 0 || change == null) return null;
    return (change / total) * 100;
  }

  getAllocationPercent(pos: WalletPositionDto): number | null {
    const total = this.totalValue;
    const value = this.getPositionValue(pos);
    if (total == null || total === 0 || value == null) return null;
    return value / total;
  }

  getPriceChangePercent(pos: WalletPositionDto): number | string | null {
    if (pos.symbol === 'USDT') return 0;
    const fullSymbol = pos.symbol + 'USDT';
    const tick = this.prices.find((p) => p.symbol === fullSymbol);
    return tick ? tick.priceChangePercent : null;
  }

  bestPerformer(): { symbol: string; percent: number } | null {
    let best: { symbol: string; percent: number } | null = null;
    for (const pos of this.wallet?.positions ?? []) {
      if (pos.symbol === 'USDT') continue;
      const pct = this.getPriceChangePercent(pos);
      if (pct == null) continue;
      const n = Number(pct);
      if (best == null || n > best.percent) best = { symbol: pos.symbol, percent: n };
    }
    return best;
  }

  worstPerformer(): { symbol: string; percent: number } | null {
    let worst: { symbol: string; percent: number } | null = null;
    for (const pos of this.wallet?.positions ?? []) {
      if (pos.symbol === 'USDT') continue;
      const pct = this.getPriceChangePercent(pos);
      if (pct == null) continue;
      const n = Number(pct);
      if (worst == null || n < worst.percent) worst = { symbol: pos.symbol, percent: n };
    }
    return worst;
  }

  donutLabels(): { symbol: string; color: string }[] {
    const total = this.totalValue;
    if (!total || !this.wallet?.positions?.length) return [];
    return this.wallet.positions.map((pos, i) => ({
      symbol: pos.symbol,
      color: DONUT_COLORS[i % DONUT_COLORS.length],
    }));
  }

  getAllocationColor(symbol: string): string {
    const idx = this.wallet?.positions?.findIndex((p) => p.symbol === symbol) ?? 0;
    return DONUT_COLORS[idx % DONUT_COLORS.length];
  }

  private updateDonutChart(): void {
    if (!this.donutCanvas?.nativeElement) return;
    const total = this.totalValue;
    if (total == null || !this.wallet?.positions?.length) return;

    const values = this.wallet.positions.map((p) => this.getPositionValue(p)).filter((v): v is number => v != null);
    const labels = this.wallet.positions.map((p) => p.symbol);
    const colors = labels.map((_, i) => DONUT_COLORS[i % DONUT_COLORS.length]);

    if (this.donutChart) {
      this.donutChart.data.labels = labels;
      this.donutChart.data.datasets[0]!.data = values;
      this.donutChart.data.datasets[0]!.backgroundColor = colors;
      this.donutChart.update('none');
      return;
    }

    const ctx = this.donutCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: colors,
          borderWidth: 0,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        cutout: '65%',
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (item) => {
                const value = item.raw as number;
                const pct = total ? ((value / total) * 100).toFixed(1) : '0';
                return `${item.label}: ${this.formatPrice(value)} USDT (${pct} %)`;
              },
            },
          },
        },
      },
    };
    this.donutChart = new Chart(ctx, config);
  }

  get displayTotal(): { formatted: string; suffix: string } | null {
    const totalUsdt = this.totalValue;
    if (totalUsdt == null) return null;
    const cur = this.totalDisplayCurrency;
    if (cur === 'USDT' || cur === 'USD') {
      return { formatted: this.formatPrice(totalUsdt), suffix: cur };
    }
    if (cur === 'EUR') {
      if (this.eurRate == null) return null;
      return { formatted: this.formatPrice(totalUsdt * this.eurRate), suffix: 'EUR' };
    }
    if (cur === 'BTC') {
      const tick = this.prices.find((p) => p.symbol === 'BTCUSDT');
      if (!tick) return null;
      const btcPrice = Number(tick.lastPrice);
      if (!btcPrice) return null;
      return { formatted: this.formatAmount(totalUsdt / btcPrice), suffix: 'BTC' };
    }
    if (cur === 'SOL') {
      const tick = this.prices.find((p) => p.symbol === 'SOLUSDT');
      if (!tick) return null;
      const solPrice = Number(tick.lastPrice);
      if (!solPrice) return null;
      return { formatted: this.formatAmount(totalUsdt / solPrice), suffix: 'SOL' };
    }
    return { formatted: this.formatPrice(totalUsdt), suffix: 'USDT' };
  }

  onTotalCurrencyChange(): void {
    this.cdr.markForCheck();
  }

  private fetchEurRate(): void {
    this.http.get<{ tether?: { eur?: number } }>(COINGECKO_EUR_URL).subscribe({
      next: (data) => {
        const rate = data?.tether?.eur;
        if (typeof rate === 'number' && rate > 0) {
          this.eurRate = rate;
          this.cdr.markForCheck();
        }
      },
      error: () => { },
    });
  }

  getPositionPrice(pos: WalletPositionDto): number | string | null {
    if (pos.symbol === 'USDT') return 1;
    const fullSymbol = pos.symbol + 'USDT';
    const tick = this.prices.find((p) => p.symbol === fullSymbol);
    return tick ? tick.lastPrice : null;
  }

  getPositionValue(pos: WalletPositionDto): number | null {
    const amount = Number(pos.amount);
    if (pos.symbol === 'USDT') return amount;
    const price = this.getPositionPrice(pos);
    if (price == null) return null;
    return amount * Number(price);
  }

  getIconUrl(symbol: string): string | null {
    return getCryptoIconUrl(symbol);
  }

  iconFailed(symbol: string): boolean {
    return this.iconFailedSet.has(symbol);
  }

  setIconFailed(symbol: string): void {
    this.iconFailedSet.add(symbol);
    this.cdr.markForCheck();
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

  asNumber(value: unknown): number {
    return Number(value);
  }

  formatTransactionDate(iso: string): string {
    return new Date(iso).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  cryptoPositions(): WalletPositionDto[] {
    return (this.wallet?.positions ?? []).filter((p) => p.symbol !== 'USDT');
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
