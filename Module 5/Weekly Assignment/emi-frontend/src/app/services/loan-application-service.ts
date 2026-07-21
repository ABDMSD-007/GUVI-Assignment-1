import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import {
  LoanApplication,
  LoanApplicationRequest,
} from '../models/loan-application';
import { LoanApplicationStatus } from '../models/enums';

@Injectable({
  providedIn: 'root',
})
export class LoanApplicationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/loan-applications`;

  // --- Customer ---
  apply(request: LoanApplicationRequest): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(this.baseUrl, request);
  }

  getMyApplications(): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(`${this.baseUrl}/my`);
  }

  // --- Underwriter ---
  getPending(): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(`${this.baseUrl}/pending`);
  }

  getByStatus(status: LoanApplicationStatus): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(`${this.baseUrl}/status/${status}`);
  }

  getById(id: number): Observable<LoanApplication> {
    return this.http.get<LoanApplication>(`${this.baseUrl}/${id}`);
  }

  startReview(id: number): Observable<LoanApplication> {
    return this.http.put<LoanApplication>(`${this.baseUrl}/${id}/review`, {});
  }

  approve(id: number, remarks?: string): Observable<LoanApplication> {
    return this.http.put<LoanApplication>(`${this.baseUrl}/${id}/approve`, { remarks });
  }

  reject(id: number, remarks?: string): Observable<LoanApplication> {
    return this.http.put<LoanApplication>(`${this.baseUrl}/${id}/reject`, { remarks });
  }
}
