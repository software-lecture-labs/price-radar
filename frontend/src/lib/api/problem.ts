/**
 * RFC 9457 `application/problem+json`, which is what the backend's
 * GlobalExceptionHandler returns for every error.
 */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Field-level validation messages, keyed by field name. */
  errors?: Record<string, string>;
}

export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.problem = problem;
  }

  /** Validation messages, keyed by field name; empty when not a 400. */
  get fieldErrors(): Record<string, string> {
    return this.problem.errors ?? {};
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
