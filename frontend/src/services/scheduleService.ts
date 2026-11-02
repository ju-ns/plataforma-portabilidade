import type { Installment } from "../types/portability";
import { request } from "./http";

export function getSchedule(
  beneficiaryId: string,
  signal?: AbortSignal,
): Promise<Installment[]> {
  return request<Installment[]>(
    `/beneficiaries/${encodeURIComponent(beneficiaryId)}/schedule`,
    { signal },
  );
}