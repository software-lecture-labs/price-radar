export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function normalizeProblem(value: unknown, status: number): ProblemDetail {
  if (!isRecord(value)) {
    return { status };
  }

  return {
    type: typeof value.type === "string" ? value.type : undefined,
    title: typeof value.title === "string" ? value.title : undefined,
    status,
    detail: typeof value.detail === "string" ? value.detail : undefined,
    instance: typeof value.instance === "string" ? value.instance : undefined,
    errors: isRecord(value.errors)
      ? Object.fromEntries(
          Object.entries(value.errors).filter(
            (entry): entry is [string, string] => typeof entry[1] === "string",
          ),
        )
      : undefined,
  };
}

export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(status: number, problem: unknown) {
    const normalizedProblem = normalizeProblem(problem, status);
    super(
      normalizedProblem.detail ?? normalizedProblem.title ?? `Request failed with status ${status}`,
    );
    this.name = "ApiError";
    this.status = status;
    this.problem = normalizedProblem;
  }

  get fieldErrors(): Record<string, string> {
    return this.problem.errors ?? {};
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
