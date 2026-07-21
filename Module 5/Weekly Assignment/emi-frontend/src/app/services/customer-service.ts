import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import { Customer } from '../models/customer';

@Injectable({
  providedIn: 'root',
})
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/customers`;

  register(customer: Customer): Observable<Customer> {
    return this.http.post<Customer>(`${this.baseUrl}/register`, customer);
  }
}
