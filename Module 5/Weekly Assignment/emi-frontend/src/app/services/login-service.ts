import { computed, inject, Injectable, signal } from '@angular/core';
import { map, Observable } from 'rxjs';
import JwtResponseDTO from '../dto/JwtResponseDTO';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import JwtRequestDTO from '../dto/JwtRequestDTO';
import { API_BASE_URL, TOKEN_KEY } from '../config/api';
import { Role } from '../models/enums';

@Injectable({
  providedIn: 'root',
})
export class LoginService {
  http: HttpClient = inject(HttpClient);
  private apiUrl: string = API_BASE_URL;

  // Signal-based auth state so the whole UI reacts to login/logout.
  private readonly token = signal<string | null>(this.readToken());
  readonly isLoggedIn = computed(() => this.token() !== null);

  // Role decoded from the JWT so the UI can hide privileged actions.
  readonly role = computed<Role | null>(() => this.decodeRole(this.token()));
  // Hierarchy: ADMIN > MANAGER > UNDERWRITER > USER (mirrors the backend RoleHierarchy).
  private readonly rank = computed(() => LoginService.rankOf(this.role()));
  readonly isUnderwriter = computed(() => this.rank() >= 1);
  readonly isManager = computed(() => this.rank() >= 2);
  readonly isAdmin = computed(() => this.rank() >= 3);

  private static rankOf(role: Role | null): number {
    switch (role) {
      case 'ADMIN':
        return 3;
      case 'MANAGER':
        return 2;
      case 'UNDERWRITER':
        return 1;
      case 'USER':
        return 0;
      default:
        return -1;
    }
  }

  login(credentials: JwtRequestDTO): Observable<JwtResponseDTO> {
    // Backend returns the raw JWT string, so read it as text and wrap it in the DTO
    return this.http
      .post(`${this.apiUrl}/login`, credentials, { responseType: 'text' })
      .pipe(map((token) => ({ token })));
  }

  // Calls a protected endpoint so we can verify the token is accepted on every request
  getUserGreeting(): Observable<string> {
    return this.http
      .get(`${this.apiUrl}/loans`)
      .pipe(map(() => 'You are logged in. Welcome!'));
  }

  // Persists the JWT and flips the reactive auth state.
  saveToken(token: string): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(TOKEN_KEY, token);
    }
    this.token.set(token);
  }

  // Clears the JWT and flips the reactive auth state.
  logout(): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(TOKEN_KEY);
    }
    this.token.set(null);
  }

  private readToken(): string | null {
    return typeof localStorage !== 'undefined'
      ? localStorage.getItem(TOKEN_KEY)
      : null;
  }

  // Reads the "role" claim from the JWT payload (base64url-encoded JSON).
  private decodeRole(token: string | null): Role | null {
    if (!token) {
      return null;
    }
    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return null;
      }
      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      const claims = JSON.parse(json) as { role?: Role };
      return claims.role ?? null;
    } catch {
      return null;
    }
  }

  // Centralized, human-readable extraction of a backend/network error message
  getErrorMessage(error: HttpErrorResponse): string {
    // Client-side or network error (server unreachable, CORS, etc.)
    if (error.status === 0) {
      return 'Unable to reach the server. Please check your connection and try again.';
    }
    // Backend returned a plain string body
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }
    // Backend returned a JSON body with a message field
    if (error.error?.message) {
      return error.error.message;
    }
    // Fall back to friendly messages for the common auth statuses
    if (error.status === 401) {
      return 'Invalid username or password.';
    }
    if (error.status === 403) {
      return 'You are not authorized to perform this action.';
    }
    return error.message || `Request failed with status ${error.status}.`;
  }
}
