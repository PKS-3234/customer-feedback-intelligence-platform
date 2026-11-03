import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardAgentStatus, DashboardMetrics } from '../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly baseUrl = `${environment.apiBaseUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getMetrics(): Observable<DashboardMetrics> {
    return this.http.get<DashboardMetrics>(this.baseUrl);
  }

  getAgentStatuses(): Observable<DashboardAgentStatus[]> {
    return this.http.get<DashboardAgentStatus[]>(`${this.baseUrl}/agents`);
  }
}
