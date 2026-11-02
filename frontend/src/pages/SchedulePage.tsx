import { useRef, useState } from "react";
import type { FormEvent } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router";

import InstallmentsTable from "../components/InstallmentsTable";
import StepIndicator from "../components/StepIndicator";
import { useSchedule } from "../hooks/useSchedule";
import { ApiError } from "../services/http";
import { createPortability } from "../services/portabilityService";
import type {
  DestinationAccount,
  PortabilitySummary,
} from "../types/portability";
import { formatCurrency } from "../utils/formatters";

import "./SchedulePage.css";

const accountFields = [
  ["destinationBank", "Banco", "001"],
  ["destinationBranch", "Agência", "1234"],
  ["destinationAccount", "Conta", "56789-0"],
] as const;

export default function SchedulePage() {
  const { beneficiaryId } = useParams();

  if (!beneficiaryId) {
    return <Navigate to="/" replace />;
  }

  return (
    <ScheduleContent
      key={beneficiaryId}
      beneficiaryId={beneficiaryId}
    />
  );
}

function ScheduleContent({
  beneficiaryId,
}: {
  beneficiaryId: string;
}) {
  const navigate = useNavigate();
  const schedule = useSchedule(beneficiaryId);

  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [account, setAccount] = useState<DestinationAccount>({
    destinationBank: "",
    destinationBranch: "",
    destinationAccount: "",
  });
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");

  const requestInProgress = useRef(false);

  const selectedInstallments = schedule.installments.filter(
    (installment) => selectedIds.includes(installment.installmentId),
  );

  const total = selectedInstallments.reduce(
    (sum, installment) => sum + Math.round(installment.amount * 100),
    0,
  ) / 100;

  function toggleInstallment(installmentId: string) {
    setSubmitError("");

    setSelectedIds((current) =>
      current.includes(installmentId)
        ? current.filter((id) => id !== installmentId)
        : [...current, installmentId],
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (requestInProgress.current) {
      return;
    }

    setSubmitError("");

    const destination = {
      destinationBank: account.destinationBank.trim(),
      destinationBranch: account.destinationBranch.trim(),
      destinationAccount: account.destinationAccount.trim(),
    };

    if (selectedInstallments.length === 0) {
      setSubmitError("Selecione pelo menos uma parcela.");
      return;
    }

    if (Object.values(destination).some((value) => !value)) {
      setSubmitError("Informe banco, agência e conta de recebimento.");
      return;
    }

    requestInProgress.current = true;
    setSubmitting(true);

    try {
      const operation = await createPortability({
        beneficiaryId,
        installmentIds: selectedInstallments.map(
          (installment) => installment.installmentId,
        ),
        ...destination,
      });

      const summary: PortabilitySummary = {
        beneficiaryId,
        installments: selectedInstallments,
        ...destination,
      };

      try {
        sessionStorage.setItem(
          `portability-summary:${operation.id}`,
          JSON.stringify(summary),
        );
      } catch {
        // O resumo também será enviado pelo estado da navegação.
      }

      navigate(`/portabilities/${operation.id}`, {
        state: { summary },
      });
    } catch (exception) {
      setSubmitError(
        exception instanceof ApiError
          ? exception.message
          : "Não foi possível solicitar a portabilidade. Verifique sua conexão.",
      );
    } finally {
      requestInProgress.current = false;
      setSubmitting(false);
    }
  }

  const hasSchedule =
    !schedule.loading &&
    !schedule.error &&
    !schedule.empty;

  return (
    <>
      <StepIndicator currentStep={2} />

      <section className="card schedule-card">
        <header className="schedule-heading">
          <h1>Agenda de descontos</h1>

          <p>
            Beneficiário <strong>{beneficiaryId}</strong>
          </p>
        </header>

        {schedule.loading && (
          <p className="schedule-state" role="status">
            Consultando agenda de descontos...
          </p>
        )}

        {!schedule.loading && (schedule.empty || schedule.error) && (
          <div
            className={
              schedule.empty
                ? "schedule-state"
                : "schedule-state schedule-error"
            }
            role={schedule.empty ? "status" : "alert"}
          >
            <p>
              {schedule.error ||
                "Nenhum agendamento de desconto encontrado."}
            </p>

            <button
              type="button"
              className="schedule-secondary-button"
              onClick={schedule.reload}
            >
              Atualizar agenda
            </button>
          </div>
        )}

        {hasSchedule && (
          <form onSubmit={handleSubmit} noValidate aria-busy={submitting}>
            <InstallmentsTable
              installments={schedule.installments}
              selectedIds={selectedIds}
              disabled={submitting}
              onToggle={toggleInstallment}
            />

            <fieldset className="destination-fields" disabled={submitting}>
              <legend>Conta de recebimento</legend>

              <div className="destination-grid">
                {accountFields.map(([name, label, placeholder]) => (
                  <div className="destination-field" key={name}>
                    <label htmlFor={name}>{label}</label>

                    <input
                      id={name}
                      name={name}
                      type="text"
                      value={account[name]}
                      placeholder={placeholder}
                      required
                      onChange={(event) => {
                        setAccount((current) => ({
                          ...current,
                          [name]: event.target.value,
                        }));
                        setSubmitError("");
                      }}
                    />
                  </div>
                ))}
              </div>
            </fieldset>

            {submitError && (
              <p className="schedule-state schedule-error" role="alert">
                {submitError}
              </p>
            )}

            <footer className="schedule-footer">
              <p aria-live="polite">
                {selectedInstallments.length}{" "}
                {selectedInstallments.length === 1
                  ? "parcela selecionada"
                  : "parcelas selecionadas"}
                {" · "}
                <strong>{formatCurrency(total)}</strong>
              </p>

              <button
                type="submit"
                className="schedule-submit"
                disabled={submitting || selectedInstallments.length === 0}
              >
                {submitting
                  ? "Enviando solicitação..."
                  : "Solicitar portabilidade"}
              </button>
            </footer>
          </form>
        )}

        <Link className="schedule-back" to="/">
          Voltar à autorização
        </Link>
      </section>
    </>
  );
}