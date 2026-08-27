import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extracts a human-readable message from an error thrown by HttpClient.
 * Understands the { message } and { errors: { field: message } } shapes
 * produced by the backend's GlobalExceptionHandler, and falls back to a
 * generic message otherwise.
 */
export function extractErrorMessage(
  err: unknown,
  fallback = 'Something went wrong. Please try again.'
): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Cannot reach the server. Please check your connection and try again.';
    }

    const body = err.error;
    if (body && typeof body === 'object') {
      const errors = (body as { errors?: Record<string, string> }).errors;
      if (errors && typeof errors === 'object') {
        const messages = Object.values(errors).filter(Boolean);
        if (messages.length) {
          return messages.join(' ');
        }
      }

      const message = (body as { message?: string }).message;
      if (typeof message === 'string' && message.trim()) {
        return message;
      }
    }
  }

  return fallback;
}
