import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Kline } from '../models/kline.model';

export interface WalletPositionDto {
  symbol: string;
  amount: number;
}

export interface WalletSummaryDto {
  positions: WalletPositionDto[];
}

export interface TransactionDto {
  id: string;
  type: 'BUY' | 'SELL' | 'SEND' | 'RECEIVE';
  symbol: string;
  amount: number;
  priceUsdt: number;
  totalUsdt: number;
  createdAt: string;
  counterpartyAccountName?: string | null;
}

export interface PerformancePointDto {
  timestamp: string;
  totalUsdt: number;
}

export interface PnlSummaryDto {
  realisedUsdt: number;
  unrealisedUsdt: number;
  totalUsdt: number;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = '/api';

  constructor(private http: HttpClient) {}

  get<T>(path: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`);
  }

  getMarketChart(symbol: string, days: string): Observable<Kline[]> {
    const sym = symbol.includes('USDT') ? symbol : symbol + 'USDT';
    return this.http.get<Kline[]>(`${this.baseUrl}/crypto/${encodeURIComponent(sym)}/market_chart`, {
      params: { days },
    });
  }

  getWallet(): Observable<WalletSummaryDto> {
    return this.http.get<WalletSummaryDto>(`${this.baseUrl}/wallet`);
  }

  getPerformance(period: '7d' | '30d' | '90d'): Observable<PerformancePointDto[]> {
    return this.http.get<PerformancePointDto[]>(`${this.baseUrl}/wallet/performance`, {
      params: { period },
    });
  }

  getPnl(): Observable<PnlSummaryDto> {
    return this.http.get<PnlSummaryDto>(`${this.baseUrl}/wallet/pnl`);
  }

  getFavorites(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/favorites`);
  }

  addFavorite(symbol: string): Observable<void> {
    const base = (symbol || '').replace('USDT', '').trim() || symbol;
    return this.http.post<void>(`${this.baseUrl}/favorites/${encodeURIComponent(base)}`, null);
  }

  removeFavorite(symbol: string): Observable<void> {
    const base = (symbol || '').replace('USDT', '').trim() || symbol;
    return this.http.delete<void>(`${this.baseUrl}/favorites/${encodeURIComponent(base)}`);
  }

  getHistory(size = 50): Observable<TransactionDto[]> {
    return this.http.get<TransactionDto[]>(`${this.baseUrl}/history`, {
      params: { size: String(size) },
    });
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, body);
  }

  buy(symbol: string, amount: number, priceUsdt: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/wallet/buy`, { symbol, amount }, {
      params: { priceUsdt },
    });
  }

  sell(symbol: string, amount: number, priceUsdt: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/wallet/sell`, { symbol, amount }, {
      params: { priceUsdt },
    });
  }

  sendCrypto(recipientIdentifier: string, symbol: string, amount: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/wallet/send`, {
      recipientIdentifier: recipientIdentifier.trim(),
      symbol: symbol.replace('USDT', '') || 'BTC',
      amount,
    });
  }
}
