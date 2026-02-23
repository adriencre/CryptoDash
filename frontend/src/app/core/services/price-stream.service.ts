import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp, RxStompState } from '@stomp/rx-stomp';
import { BehaviorSubject, Observable, shareReplay, map, takeUntil, Subject } from 'rxjs';
import SockJS from 'sockjs-client';
import { PriceTick } from '../models/price-tick.model';

const TOPIC_PRICES = '/topic/prices';

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

@Injectable({ providedIn: 'root' })
export class PriceStreamService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly rxStomp = new RxStomp();
  private readonly pricesMap = new Map<string, PriceTick>();
  private readonly pricesSubject = new BehaviorSubject<PriceTick[]>([]);
  private readonly connectionStatus$ = new BehaviorSubject<ConnectionStatus>('connecting');

  /** Statut de la connexion WebSocket (backend → Binance). */
  readonly status$: Observable<ConnectionStatus> = this.connectionStatus$.asObservable();

  /** Flux des prix mis à jour (une seule source, partagé). */
  readonly prices$: Observable<PriceTick[]> = this.pricesSubject.asObservable().pipe(shareReplay(1));

  constructor() {
    this.connectAndSubscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.rxStomp.deactivate();
  }

  /** Prix pour un symbole donné. */
  getPriceBySymbol(symbol: string): Observable<PriceTick | undefined> {
    return this.prices$.pipe(
      takeUntil(this.destroy$),
      map((list) => list.find((p) => p.symbol === symbol)),
      shareReplay(1)
    );
  }

  /** Met à jour les prix avec des données initiales (ex. venant d'une API REST). */
  setInitialPrices(ticks: PriceTick[]): void {
    if (!ticks?.length) return;
    ticks.forEach((t) => this.pricesMap.set(t.symbol, t));
    this.pricesSubject.next(Array.from(this.pricesMap.values()));
  }

  private connectAndSubscribe(): void {
    const wsUrl = this.getWebSocketUrl();
    // Spring expose /ws avec SockJS : le client doit utiliser SockJS, pas un WebSocket brut.
    this.rxStomp.configure({
      webSocketFactory: () => new SockJS(wsUrl) as unknown as WebSocket,
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.rxStomp.connected$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.connectionStatus$.next('connected');
      this.rxStomp
        .watch(TOPIC_PRICES)
        .pipe(takeUntil(this.destroy$))
        .subscribe((message) => {
          try {
            const body = JSON.parse(message.body || '{}') as PriceTick;
            if (body?.symbol) {
              this.pricesMap.set(body.symbol, body);
              this.pricesSubject.next(Array.from(this.pricesMap.values()));
            }
          } catch {
            // ignore invalid payloads
          }
        });
    });

    this.rxStomp.stompErrors$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.connectionStatus$.next('error');
    });
    this.rxStomp.connectionState$.pipe(takeUntil(this.destroy$)).subscribe((state) => {
      if (state === RxStompState.CONNECTING) this.connectionStatus$.next('connecting');
      if (state === RxStompState.CLOSED || state === RxStompState.CLOSING) this.connectionStatus$.next('disconnected');
    });

    this.rxStomp.activate();
  }

  /** URL HTTP du backend pour SockJS (ex. http://localhost:8080). */
  private getWebSocketUrl(): string {
    const host = typeof window !== 'undefined' && (window as unknown as { __BACKEND_HOST__?: string }).__BACKEND_HOST__ != null
      ? (window as unknown as { __BACKEND_HOST__: string }).__BACKEND_HOST__
      : 'localhost:8080';
    return `http://${host}/ws`;
  }
}
