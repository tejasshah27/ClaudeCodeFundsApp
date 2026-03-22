import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FundDetail, FundSummary } from '../models/fund.model';

@Injectable({ providedIn: 'root' })
export class FundService {
  constructor(private http: HttpClient) {}

  getFunds(): Observable<(FundSummary | FundDetail)[]> {
    return this.http.get<(FundSummary | FundDetail)[]>('/api/funds');
  }

  getFund(id: string): Observable<FundDetail> {
    return this.http.get<FundDetail>(`/api/funds/${id}`);
  }

  save(id: string, data: Partial<FundDetail>): Observable<void> {
    return this.http.put<void>(`/api/funds/${id}`, { action: 'SAVE', ...data });
  }

  submit(id: string, data: Partial<FundDetail>): Observable<void> {
    return this.http.put<void>(`/api/funds/${id}`, { action: 'SUBMIT', ...data });
  }

  approve(id: string): Observable<void> {
    return this.http.post<void>(`/api/funds/${id}/approve`, {});
  }

  reject(id: string): Observable<void> {
    return this.http.post<void>(`/api/funds/${id}/reject`, {});
  }
}
