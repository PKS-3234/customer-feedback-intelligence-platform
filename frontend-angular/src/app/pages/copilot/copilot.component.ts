import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AgentQueryResponse } from '../../core/models/agent.model';
import { CopilotService } from '../../core/services/copilot.service';

@Component({
  selector: 'app-copilot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './copilot.component.html',
  styleUrl: './copilot.component.scss'
})
export class CopilotComponent {
  query = '';
  result: AgentQueryResponse | null = null;
  isQuerying = false;
  errorMessage = '';

  constructor(
    private copilotService: CopilotService,
    private changeDetector: ChangeDetectorRef
  ) {}

  ask(): void {
    const normalizedQuery = this.query.trim();
    if (normalizedQuery.length < 2) {
      this.errorMessage = 'Enter at least two characters to ask a question.';
      return;
    }

    this.errorMessage = '';
    this.result = null;
    this.isQuerying = true;
    this.copilotService.query({ query: normalizedQuery }).subscribe({
      next: (result) => {
        this.result = result;
        this.isQuerying = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'The Copilot query failed.';
        this.isQuerying = false;
        this.changeDetector.markForCheck();
      }
    });
  }
}
