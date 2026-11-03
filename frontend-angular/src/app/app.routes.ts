import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { CsvUploadComponent } from './pages/data-operations/upload/csv-upload.component';
import { BatchListComponent } from './pages/data-operations/batches/batch-list.component';
import { BatchDetailComponent } from './pages/data-operations/batches/batch-detail.component';
import { AnalysisComponent } from './pages/analysis/analysis.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'data-operations/upload', component: CsvUploadComponent },
  { path: 'data-operations/batches', component: BatchListComponent },
  { path: 'data-operations/batches/:batchId', component: BatchDetailComponent },
  { path: 'analysis', component: AnalysisComponent },
  { path: '**', redirectTo: '' }
];
