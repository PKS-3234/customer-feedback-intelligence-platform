import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { environment } from '../../../environments/environment';
import { CopilotService } from './copilot.service';

describe('CopilotService', () => {
  let service: CopilotService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CopilotService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('posts the query to the Spring agent endpoint', () => {
    const response = {
      answer: 'A grounded answer [analytics-overview].',
      agentsUsed: ['Supervisor Agent', 'Analytics Agent', 'Reporting Agent'],
      supportingEvidence: []
    };
    service.query({ query: 'How many feedback records?' }).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(`${environment.apiBaseUrl}/agents/query`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ query: 'How many feedback records?' });
    request.flush(response);
  });
});
