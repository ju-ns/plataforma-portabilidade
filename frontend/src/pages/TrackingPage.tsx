import { useState } from "react";
import {
  Link,
  Navigate,
  useLocation,
  useParams,
} from "react-router";

import OperationStatusCard from "../components/OperationStatusCard";
import StepIndicator from "../components/StepIndicator";
import { usePortabilityStatus } from "../hooks/usePortabilityStatus";
import type {
  PortabilityResponse,
  PortabilitySummary,
} from "../types/portability";
import { formatCurrency } from "../utils/formatters";

import "./TrackingPage.css";

interface TrackingLocationState {
  summary?: PortabilitySummary;
  operation?: PortabilityResponse;
}

interface TrackingContentProps {
  operationId: string;
  initialOperation: PortabilityResponse | null;
  initialSummary: PortabilitySummary | null;
}

function readSavedSummary(
  operationId: string,
): PortabilitySummary | null {
  try {
    const saved = sessionStorage.getItem(
      `portability-summary:${operationId}`,
    );

    return saved ? (JSON.parse(saved) as PortabilitySummary) : null;
  } catch {
    return null;
  }
}

export default function TrackingPage() {
  const { operationId } = useParams();
  const location = useLocation();

  const navigationState =
    location.state as TrackingLocationState | null;

  if (!operationId) {
    return <Navigate to="/" replace />;
  }

  const initialOperation =
    navigationState?.operation?.id === operationId
      ? navigationState.operation
      : null;

  return (
    <TrackingContent
      key={operationId}
      operationId={operationId}
      initialOperation={initialOperation}
      initialSummary={navigationState?.summary ?? null}
    />
  );
}

function TrackingContent({
  operationId,
  initialOperation,
  initialSummary,
}: TrackingContentProps) {
  const tracking = usePortabilityStatus(
    operationId,
    initialOperation,
  );

  const [savedSummary] = useState(
    () => initialSummary ?? readSavedSummary(operationId),
  );

  const summary =
    savedSummary?.beneficiaryId === tracking.operation?.beneficiaryId
      ? savedSummary
      : null;

  const waiting =
    tracking.operation?.status === "CRIADA" ||
    tracking.operation?.status === "PENDENTE";

  const lastChecked = tracking.lastCheckedAt?.toLocaleTimeString(
    "pt-BR",
    {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    },
  );

  const updateMessage = tracking.error
    ? "Atualização automática pausada"
    : waiting
      ? "Atualizando automaticamente a cada 2 segundos"
      : tracking.operation
        ? "Atualização automática encerrada"
        : "Consultando operação";

  return (
    <>
      <StepIndicator currentStep={3} />

      <section
        className="card tracking-card"
        aria-labelledby="tracking-title"
      >
        <h1 id="tracking-title">
          Acompanhamento da portabilidade
        </h1>

        <OperationStatusCard
          operation={tracking.operation}
          error={tracking.error}
        />

        <dl className="tracking-details">
          <div>
            <dt>Operação</dt>

            <dd>
              <span title={operationId}>
                #{operationId.slice(0, 8)}
              </span>
            </dd>
          </div>

          <div>
            <dt>Parcelas</dt>

            <dd>
              {summary ? (
                <ul className="tracking-installments">
                  {summary.installments.map((installment) => (
                    <li key={installment.installmentId}>
                      {installment.installmentId}
                      {" · "}
                      {formatCurrency(installment.amount)}
                    </li>
                  ))}
                </ul>
              ) : (
                "Resumo indisponível nesta sessão."
              )}
            </dd>
          </div>

          <div>
            <dt>Conta de destino</dt>

            <dd>
              {summary
                ? `${summary.destinationBank} / ${summary.destinationBranch} / ${summary.destinationAccount}`
                : "Não disponível nesta sessão."}
            </dd>
          </div>
        </dl>

        <button
          className="tracking-refresh"
          type="button"
          onClick={tracking.reload}
          disabled={tracking.refreshing}
        >
          {tracking.refreshing
            ? "Atualizando..."
            : tracking.error
              ? "Tentar novamente"
              : "Atualizar agora"}
        </button>

        <p className="tracking-update-info">
          {updateMessage}
          {lastChecked && ` · última verificação às ${lastChecked}`}
        </p>

        <Link className="tracking-back" to="/">
          Voltar ao início
        </Link>
      </section>
    </>
  );
}