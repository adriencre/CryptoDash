import { TestBed } from '@angular/core/testing';
import { PriceStreamService } from './price-stream.service';

describe('PriceStreamService', () => {
  let service: PriceStreamService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PriceStreamService);
  });

  afterEach(() => {
    service.ngOnDestroy();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should expose prices$ as observable', (done) => {
    service.prices$.subscribe((prices) => {
      expect(Array.isArray(prices)).toBe(true);
      done();
    });
  });

  it('getPriceBySymbol should return observable for symbol', (done) => {
    service.getPriceBySymbol('BTCUSDT').subscribe((tick) => {
      expect(tick === undefined || typeof tick.symbol === 'string').toBe(true);
      done();
    });
  });
});
