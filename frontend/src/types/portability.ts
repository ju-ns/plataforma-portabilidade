export interface Installment {
  installmentId: string;
  agreementId: string;
  amount: number;
  deductionDate: string | null;
  status: string;
}

export interface DestinationAccount {
  destinationBank: string;
  destinationBranch: string;
  destinationAccount: string;
}

export interface CreatePortabilityInput extends DestinationAccount {
  beneficiaryId: string;
  installmentIds: string[];
}

export type PortabilityStatus =
  | "CRIADA"
  | "PENDENTE"
  | "CONFIRMADA"
  | "REJEITADA"
  | "PORTABILIDADE_INCERTA";

export interface PortabilityResponse {
  id: string;
  beneficiaryId: string;
  protocol: string | null;
  status: PortabilityStatus;
  rejectionReason: string | null;
}

export interface PortabilitySummary extends DestinationAccount {
  beneficiaryId: string;
  installments: Installment[];
}