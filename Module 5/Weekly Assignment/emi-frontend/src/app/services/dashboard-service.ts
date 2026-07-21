import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import { DashboardDto } from '../models/dashboard';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);

  getDashboard(): Observable<DashboardDto> {
    return this.http.get<DashboardDto>(`${API_BASE_URL}/dashboard`);
  }
}
