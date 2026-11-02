import type {
  AuthorizationResponse,
  RegisterAuthorizationRequest,
} from "../types/authorization";

import { request } from "./http";

function getIdempotencyKey(beneficiaryId: string): string {
  const storageKey = `authorization-key:${beneficiaryId}`;
  const existingKey = sessionStorage.getItem(storageKey);

  if (existingKey) {
    return existingKey;
  }

  const newKey = crypto.randomUUID();

  sessionStorage.setItem(storageKey, newKey);

  return newKey;
}

export function registerAuthorization(
  beneficiaryId: string,
): Promise<AuthorizationResponse> {
  const body: RegisterAuthorizationRequest = {
    beneficiaryId,
    idempotencyKey: getIdempotencyKey(beneficiaryId),
  };

  return request<AuthorizationResponse>("/authorizations", {
    method: "POST",
    body: JSON.stringify(body),
  });
}