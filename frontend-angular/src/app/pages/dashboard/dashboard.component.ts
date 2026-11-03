import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  DashboardAgentStatus,
  DashboardChartDatum,
  DashboardMetrics
} from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';

interface DashboardChart {
  title: string;
  description: string;
  data: DashboardChartDatum[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  metrics: DashboardMetrics | null = null;
  agents: DashboardAgentStatus[] = [];
  charts: DashboardChart[] = [];
  isLoading = true;
  errorMessage = '';

  readonly exampleQuestions = [
    'What are customers unhappy about?',
    'Show recent payment issues.',
    'Are there duplicate-feedback trends?',
    'What are the highest-priority themes?'
  ];

  constructor(
    private dashboardService: DashboardService,
    private changeDetector: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.isLoading = true;
    this.errorMessage = '';
    forkJoin({
      metrics: this.dashboardService.getMetrics(),
      agents: this.dashboardService.getAgentStatuses()
    }).subscribe({
      next: ({ metrics, agents }) => {
        this.metrics = metrics;
        this.agents = agents;
        this.charts = [
          { title: 'Sentiment Distribution', description: 'Current analyzed feedback', data: metrics.sentimentDistribution },
          { title: 'Category Distribution', description: 'Current analysis categories', data: metrics.categoryDistribution },
          { title: 'Theme Distribution', description: 'Current analysis themes', data: metrics.themeDistribution },
          { title: 'Priority Distribution', description: 'Current analysis priorities', data: metrics.priorityDistribution },
          { title: 'Feedback by Channel', description: 'All stored feedback', data: metrics.feedbackByChannel },
          { title: 'Feedback by Product', description: 'All stored feedback', data: metrics.feedbackByProduct }
        ];
        this.isLoading = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Dashboard data could not be loaded.';
        this.isLoading = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  barWidth(value: number, data: DashboardChart['data']): number {
    const maximum = Math.max(...data.map((item) => item.count), 0);
    return maximum === 0 ? 0 : (value / maximum) * 100;
  }
}
