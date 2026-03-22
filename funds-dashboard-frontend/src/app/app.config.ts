import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { HttpInterceptorFn } from '@angular/common/http';
import { routes } from './app.routes';

const withCredentialsInterceptor: HttpInterceptorFn = (req, next) =>
  next(req.clone({ withCredentials: true }));

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withFetch(), withInterceptors([withCredentialsInterceptor])),
  ]
};
