/**
 * Aligné sur le DTO backend PriceTickDto (sérialisé en JSON).
 */
export interface PriceTick {
  symbol: string;
  lastPrice: number | string;
  priceChangePercent: number | string;
  high24h: number | string;
  low24h: number | string;
  volume24h: number | string;
  eventTime: number;
}
