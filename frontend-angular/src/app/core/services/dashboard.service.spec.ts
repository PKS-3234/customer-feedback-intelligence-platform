import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { environment } from '../../../environments/environment';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads live dashboard metrics and chart data', () => {
    const metrics = {
      totalFeedbackRecords: 12,
      sentimentDistribution: [{ label: 'NEGATIVE', count: 8 }]
    };
    service.getMetrics().subscribe((result) => {
      expect(result.totalFeedbackRecords).toBe(12);
      expect(result.sentimentDistribution[0].count).toBe(8);
    });

    const request = httpMock.expectOne(`${environment.apiBaseUrl}/dashboard`);
    expect(request.request.method).toBe('GET');
    request.flush(metrics);
  });

  it('loads the configured agent status list', () => {
    service.getAgentStatuses().subscribe((agents) => {
      expect(agents[0].agent).toBe('Supervisor Agent');
      expect(agents[0].lastExecution).toBeNull();
    });

    const request = httpMock.expectOne(`${environment.apiBaseUrl}/dashboard/agents`);
    expect(request.request.method).toBe('GET');
    request.flush([{
      agent: 'Supervisor Agent',
      purpose: 'Routes queries.',
      availability: 'Configured',
      lastExecution: null
    }]);
  });
});
