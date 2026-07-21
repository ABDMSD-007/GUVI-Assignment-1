import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // 1. Retrieve the token from wherever you store it
  const token =
    typeof localStorage !== 'undefined' ? localStorage.getItem('token') : null;
  // 2. If a token exists, clone the request and add the Authorization header
  if (token) {
    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    // Pass the cloned request with the header to the next handler
    return next(clonedRequest);
  }
  // 3. If there is no token, let the original request pass through untouched
  return next(req);
};
