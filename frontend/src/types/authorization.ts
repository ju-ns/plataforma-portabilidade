export interface RegisterAuthorizationRequest {
  beneficiaryId: string;
  idempotencyKey: string;
}

export interface AuthorizationResponse {
  id: string;
  beneficiaryId: string;
  authorizationId: string | null;
  status: "CRIADA" | "AUTORIZADA" | "AUTORIZACAO_INCERTA";
  createdAt: string;
  authorizedAt: string | null;
}