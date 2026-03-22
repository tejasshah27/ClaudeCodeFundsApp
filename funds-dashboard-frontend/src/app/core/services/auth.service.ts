import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { UserRole } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private roleSubject = new BehaviorSubject<UserRole | null>(null);
  role$ = this.roleSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  get role(): UserRole | null {
    return this.roleSubject.value;
  }

  login(username: string, password: string): Observable<{ role: UserRole }> {
    return this.http.post<{ role: UserRole }>('/api/auth/login', { username, password })
      .pipe(tap(res => this.roleSubject.next(res.role)));
  }

  logout(): Observable<unknown> {
    return this.http.post('/api/auth/logout', {})
      .pipe(tap(() => {
        this.roleSubject.next(null);
        this.router.navigate(['/login']);
      }));
  }

  isAuthenticated(): boolean {
    return this.roleSubject.value !== null;
  }
}
