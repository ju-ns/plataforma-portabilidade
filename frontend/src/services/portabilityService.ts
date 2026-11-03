import type {
  CreatePortabilityInput,
  PortabilityResponse,
} from "../types/portability";

import { request } from "./http";

export function createPortability(
  input: CreatePortabilityInput,
): Promise<PortabilityResponse> {
  const normalizedInput = {
    ...input,
    installmentIds: [...input.installmentIds].sort(),
  };

  const storageKey = `portability-key:${JSON.stringify(normalizedInput)}`;

  let idempotencyKey = sessionStorage.getItem(storageKey);

  if (!idempotencyKey) {
    idempotencyKey = crypto.randomUUID();
    sessionStorage.setItem(storageKey, idempotencyKey);
  }

  return request<PortabilityResponse>("/portabilities", {
    method: "POST",
    body: JSON.stringify({
      ...normalizedInput,
      idempotencyKey,
    }),
  });
}

export function getPortabilityStatus(
  operationId: string,
  signal?: AbortSignal,
): Promise<PortabilityResponse> {
  return request<PortabilityResponse>(
    `/portabilities/${encodeURIComponent(operationId)}`,
    { signal },
  );
}