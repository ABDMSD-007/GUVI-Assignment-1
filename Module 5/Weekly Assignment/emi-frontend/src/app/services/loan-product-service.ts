import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import { LoanProduct, LoanProductRequest } from '../models/loan-product';

@Injectable({
  providedIn: 'root',
})
export class LoanProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/loan-products`;

  getAll(): Observable<LoanProduct[]> {
    return this.http.get<LoanProduct[]>(this.baseUrl);
  }

  getActive(): Observable<LoanProduct[]> {
    return this.http.get<LoanProduct[]>(`${this.baseUrl}/active`);
  }

  getByCode(loanCode: string): Observable<LoanProduct> {
    return this.http.get<LoanProduct>(`${this.baseUrl}/${loanCode}`);
  }

  create(request: LoanProductRequest): Observable<LoanProduct> {
    return this.http.post<LoanProduct>(this.baseUrl, request);
  }

  update(loanCode: string, request: LoanProductRequest): Observable<LoanProduct> {
    return this.http.put<LoanProduct>(`${this.baseUrl}/${loanCode}`, request);
  }

  deactivate(loanCode: string): Observable<string> {
    return this.http.delete(`${this.baseUrl}/${loanCode}`, { responseType: 'text' });
  }
}
