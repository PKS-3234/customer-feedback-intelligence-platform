import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  readonly title = 'Customer Feedback Intelligence Platform';
  readonly subtitle = 'Agentic AI Customer Feedback Decision-Support Platform';
  readonly apiBaseUrl = environment.apiBaseUrl;
  readonly aiServiceBaseUrl = environment.aiServiceBaseUrl;
  readonly isProduction = environment.production;

  readonly architectureLayers = [
    { name: 'Frontend', tech: 'Angular 21 + TypeScript + SCSS', port: '4200', status: 'Ready (Skeleton)' },
    { name: 'Business API', tech: 'Java 21 + Spring Boot 3 + Maven', port: '8080', status: 'Ready (Foundation)' },
    { name: 'AI Service', tech: 'Python 3.12 + FastAPI', port: '8000', status: 'Ready (Foundation)' },
    { name: 'Database', tech: 'PostgreSQL 16 + PGVector Extension', port: '5432', status: 'Active (Docker)' }
  ];
}
