import { useRef, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router";

import StepIndicator from "../components/StepIndicator";
import { registerAuthorization } from "../services/authorizationService";
import { ApiError } from "../services/http";

import "./AuthorizationPage.css";

export default function AuthorizationPage() {
  const navigate = useNavigate();

  const [beneficiaryId, setBeneficiaryId] = useState("");
  const [consent, setConsent] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const requestInProgress = useRef(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (requestInProgress.current) {
      return;
    }

    setError("");
    setNotice("");

    const normalizedBeneficiaryId = beneficiaryId.trim();

    if (!normalizedBeneficiaryId) {
      setError("Informe o código do beneficiário.");
      return;
    }

    if (!consent) {
      setError("Confirme sua autorização para continuar.");
      return;
    }

    requestInProgress.current = true;
    setSubmitting(true);

    try {
      const authorization = await registerAuthorization(
        normalizedBeneficiaryId,
      );

      if (authorization.status === "CRIADA") {
        setNotice(
          "Sua autorização ainda está sendo processada. Aguarde um momento e clique novamente para verificar.",
        );
        return;
      }

      if (
        authorization.status !== "AUTORIZADA" ||
        !authorization.authorizationId
      ) {
        setError(
          "Não foi possível confirmar a autorização. Verifique a situação antes de iniciar outra solicitação.",
        );
        return;
      }

      navigate(
        `/beneficiaries/${encodeURIComponent(
            authorization.beneficiaryId,
        )}/schedule`,
);

    } catch (exception) {
      setError(
        exception instanceof ApiError
          ? exception.message
          : "Não foi possível concluir a solicitação. Verifique sua conexão e tente novamente.",
      );
    } finally {
      requestInProgress.current = false;
      setSubmitting(false);
    }
  }

  return (
    <>
      <StepIndicator currentStep={1} />

      <section
        className="card authorization-card"
        aria-labelledby="authorization-title"
      >
        <h1 id="authorization-title">Autorizar portabilidade</h1>

        <p className="authorization-description">
          Informe o beneficiário para consultar a agenda de descontos.
        </p>

        <form onSubmit={handleSubmit} noValidate aria-busy={submitting}>
          <div className="authorization-field">
            <label htmlFor="beneficiaryId">
              Código do beneficiário
            </label>

            <input
              id="beneficiaryId"
              name="beneficiaryId"
              type="text"
              value={beneficiaryId}
              placeholder="BEN001"
              required
              disabled={submitting}
              autoComplete="off"
              spellCheck={false}
              aria-describedby="beneficiary-help"
              onChange={(event) => {
                setBeneficiaryId(event.target.value);
                setError("");
                setNotice("");
              }}
            />

            <small id="beneficiary-help">Exemplo: BEN001</small>
          </div>

          <label className="authorization-consent">
            <input
              name="consent"
              type="checkbox"
              checked={consent}
              required
              disabled={submitting}
              onChange={(event) => {
                setConsent(event.target.checked);
                setError("");
              }}
            />

            <span>
              Autorizo a consulta da minha agenda de descontos e a
              portabilidade das parcelas selecionadas.
            </span>
          </label>

          {error && (
            <p className="authorization-error" role="alert">
              {error}
            </p>
          )}

          {notice && (
            <p className="authorization-notice" role="status">
              {notice}
            </p>
          )}

          <button
            className="authorization-submit"
            type="submit"
            disabled={submitting}
          >
            {submitting
              ? "Aguarde..."
              : notice
                ? "Verificar autorização"
                : "Autorizar e continuar"}
          </button>
        </form>
      </section>
    </>
  );
}