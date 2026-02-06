/**
 * URLs des icônes (CoinGecko, small size).
 * Clé = symbole sans USDT (ex. BTC, ETH).
 */
export const CRYPTO_ICONS: Record<string, string> = {
  BTC: 'https://assets.coingecko.com/coins/images/1/small/bitcoin.png',
  ETH: 'https://assets.coingecko.com/coins/images/279/small/ethereum.png',
  BNB: 'https://assets.coingecko.com/coins/images/825/small/bnb-icon2_2x.png',
  SOL: 'https://assets.coingecko.com/coins/images/4128/small/solana.png',
  XRP: 'https://assets.coingecko.com/coins/images/44/small/xrp-symbol-white-128.png',
  ADA: 'https://assets.coingecko.com/coins/images/975/small/cardano.png',
  DOGE: 'https://assets.coingecko.com/coins/images/5/small/dogecoin.png',
  AVAX: 'https://assets.coingecko.com/coins/images/12559/small/Avalanche_Circle_RedWhite_Trans.png',
  LINK: 'https://assets.coingecko.com/coins/images/877/small/chainlink-new-logo.png',
  DOT: 'https://assets.coingecko.com/coins/images/12171/small/polkadot.png',
  MATIC: 'https://assets.coingecko.com/coins/images/4713/small/matic-token-icon.png',
  LTC: 'https://assets.coingecko.com/coins/images/2/small/litecoin.png',
  ATOM: 'https://assets.coingecko.com/coins/images/1481/small/cosmos_hub.png',
  UNI: 'https://assets.coingecko.com/coins/images/12504/small/uni.jpg',
  APT: 'https://assets.coingecko.com/coins/images/26455/small/aptos_round.png',
  ARB: 'https://assets.coingecko.com/coins/images/16547/small/photo_2023-03-29_21.47.00.jpeg',
  OP: 'https://assets.coingecko.com/coins/images/25244/small/Optimism.png',
  TRX: 'https://assets.coingecko.com/coins/images/1094/small/tron-logo.png',
  SHIB: 'https://assets.coingecko.com/coins/images/11939/small/shiba.png',
  PEPE: 'https://assets.coingecko.com/coins/images/29850/small/pepe-token.jpeg',
  NEAR: 'https://assets.coingecko.com/coins/images/10365/small/near.jpg',
  SUI: 'https://assets.coingecko.com/coins/images/26375/small/sui_asset.jpeg',
  SEI: 'https://assets.coingecko.com/coins/images/28205/small/Sei_Logo_-_Transparent.png',
  TIA: 'https://assets.coingecko.com/coins/images/31967/small/tia.jpg',
  INJ: 'https://assets.coingecko.com/coins/images/12882/small/Secondary_Symbol.png',
  FIL: 'https://assets.coingecko.com/coins/images/12817/small/filecoin.png',
  ICP: 'https://assets.coingecko.com/coins/images/14495/small/Internet_Computer_logo.png',
  XLM: 'https://assets.coingecko.com/coins/images/100/small/Stellar_symbol_black_RGB.png',
  AAVE: 'https://assets.coingecko.com/coins/images/12645/small/aave-token-round.png',
  MKR: 'https://assets.coingecko.com/coins/images/1364/small/Mark_Maker.png',
  SAND: 'https://assets.coingecko.com/coins/images/12129/small/sandbox_logo.jpg',
  MANA: 'https://assets.coingecko.com/coins/images/878/small/decentraland-mana.png',
  VET: 'https://assets.coingecko.com/coins/images/1167/small/VET_Token_Icon.png',
  ETC: 'https://assets.coingecko.com/coins/images/453/small/ethereum-classic-logo.png',
  STX: 'https://assets.coingecko.com/coins/images/2069/small/Stacks_logo_full.png',
  IMX: 'https://assets.coingecko.com/coins/images/17233/small/immutableX-symbol-BLK-Hero.png',
  RNDR: 'https://assets.coingecko.com/coins/images/11636/small/rndr.png',
  FET: 'https://assets.coingecko.com/coins/images/5681/small/Fetch.jpg',
};

export function getCryptoIconUrl(symbol: string): string | null {
  const base = symbol.replace(/USDT$/i, '');
  return CRYPTO_ICONS[base] ?? null;
}
