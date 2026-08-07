export interface FieldError {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors: FieldError[];
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly errorCode: string | undefined,
    readonly status: number | undefined,
    readonly fieldErrors: FieldError[] = []
  ) {
    super(message);
    this.name = 'ApiError';
  }

  fieldMessage(field: string): string | undefined {
    return this.fieldErrors.find((fe) => fe.field === field)?.message;
  }
}