interface ApiErrorBody {
  status: number;
  error: string;
  errors?: {
    fieldName: string;
    message: string;
  }[];
}

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);

    this.name = "ApiError";
    this.status = status;
  }
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);

  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`/api${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as
      | ApiErrorBody
      | null;

    const validationMessages = body?.errors
      ?.map((item) => item.message)
      .join(" ");

    throw new ApiError(
      response.status,
      validationMessages ||
        body?.error ||
        "Não foi possível concluir a solicitação.",
    );
  }

  return response.json() as Promise<T>;
}