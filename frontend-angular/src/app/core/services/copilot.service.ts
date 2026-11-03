import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AgentQueryRequest, AgentQueryResponse } from '../models/agent.model';

@Injectable({
  providedIn: 'root'
})
export class CopilotService {
  private readonly endpoint = `${environment.apiBaseUrl}/agents/query`;

  constructor(private http: HttpClient) {}

  query(request: AgentQueryRequest): Observable<AgentQueryResponse> {
    return this.http.post<AgentQueryResponse>(this.endpoint, request);
  }
}
