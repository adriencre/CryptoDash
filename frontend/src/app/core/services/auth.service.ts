import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, LoginResponse, ProfileResponse, RegisterRequest, UpdateProfileRequest } from '../models/auth.model';

const TOKEN_KEY = 'cryptodash_token';
const USER_KEY = 'cryptodash_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = '/api/auth';

  private readonly tokenSignal = signal<string | null>(this.getStoredToken());
  private readonly userSignal = signal<{ userId: string; email: string } | null>(this.getStoredUser());

  readonly isLoggedIn = computed(() => !!this.tokenSignal());
  readonly currentUser = computed(() => this.userSignal());

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  getToken(): string | null {
    return this.tokenSignal();
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request).pipe(
      tap((res) => this.setSession(res)),
    );
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request);
  }

  verify2FA(tempToken: string, code: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/2fa/verify`, { tempToken, code }).pipe(
      tap((res) => this.setSession(res)),
    );
  }

  get2FAStatus(): Observable<{ twoFactorEnabled: boolean }> {
    return this.http.get<{ twoFactorEnabled: boolean }>(`${this.baseUrl}/2fa/status`);
  }

  setup2FA(): Observable<{ secret: string; qrCodeUrl: string }> {
    return this.http.post<{ secret: string; qrCodeUrl: string }>(`${this.baseUrl}/2fa/setup`, {});
  }

  enable2FA(code: string): Observable<string[]> {
    return this.http.post<string[]>(`${this.baseUrl}/2fa/enable`, { code });
  }

  disable2FA(password: string, code: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/2fa/disable`, { password, code });
  }

  regenerateBackupCodes(password: string, code: string): Observable<string[]> {
    return this.http.post<string[]>(`${this.baseUrl}/2fa/backup-codes/regenerate`, { password, code });
  }

  getProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${this.baseUrl}/profile`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(`${this.baseUrl}/profile`, request);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    this.router.navigate(['/login']);
  }

  private setSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify({ userId: res.userId, email: res.email }));
    this.tokenSignal.set(res.token);
    this.userSignal.set({ userId: res.userId, email: res.email });
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private getStoredUser(): { userId: string; email: string } | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as { userId: string; email: string };
    } catch {
      return null;
    }
  }

  /** À appeler après login quand requires2FA est false (connexion directe). */
  setSessionFromLoginResponse(res: LoginResponse): void {
    if (res.token && res.userId && res.email) {
      this.setSession({ token: res.token, userId: res.userId, email: res.email });
    }
  }
}
