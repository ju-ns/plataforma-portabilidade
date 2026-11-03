import type {
  PortabilityResponse,
  PortabilityStatus,
} from "../types/portability";

interface OperationStatusCardProps {
  operation: PortabilityResponse | null;
  error: string;
}

interface StatusPresentation {
  tone: "pending" | "success" | "error" | "warning" | "neutral";
  symbol: string;
  title: string;
  description: string;
  badge: string;
}

const presentations: Record<PortabilityStatus, StatusPresentation> = {
  CRIADA: {
    tone: "pending",
    symbol: "◷",
    title: "Preparando solicitação",
    description: "Sua solicitação foi registrada e está sendo processada.",
    badge: "Em processamento",
  },

  PENDENTE: {
    tone: "pending",
    symbol: "◷",
    title: "Aguardando confirmação",
    description:
      "A Central está processando a operação. Esta tela atualiza sozinha a cada 2 segundos.",
    badge: "Pendente",
  },

  CONFIRMADA: {
    tone: "success",
    symbol: "✓",
    title: "Portabilidade confirmada",
    description: "A portabilidade foi confirmada pela Central.",
    badge: "Confirmada",
  },

  REJEITADA: {
    tone: "error",
    symbol: "×",
    title: "Portabilidade rejeitada",
    description: "A Central não informou o motivo da rejeição.",
    badge: "Rejeitada",
  },

  PORTABILIDADE_INCERTA: {
    tone: "warning",
    symbol: "!",
    title: "Resultado não confirmado",
    description:
      "É necessário verificar o resultado com a Central antes de iniciar outra solicitação.",
    badge: "Resultado incerto",
  },
};

export default function OperationStatusCard({
  operation,
  error,
}: OperationStatusCardProps) {
  let presentation: StatusPresentation;

  if (error) {
    presentation = {
      tone: "error",
      symbol: "!",
      title: "Não foi possível atualizar",
      description: error,
      badge: "Consulta interrompida",
    };
  } else if (operation) {
    presentation = presentations[operation.status];

    if (operation.status === "REJEITADA" && operation.rejectionReason) {
      presentation = {
        ...presentation,
        description: operation.rejectionReason,
      };
    }
  } else {
    presentation = {
      tone: "neutral",
      symbol: "◷",
      title: "Consultando operação",
      description: "Aguarde enquanto buscamos o status da portabilidade.",
      badge: "Consultando",
    };
  }

  return (
    <div
      className={`tracking-status tracking-status--${presentation.tone}`}
      role={error ? "alert" : "status"}
    >
      <span className="tracking-status-icon" aria-hidden="true">
        {presentation.symbol}
      </span>

      <h2>{presentation.title}</h2>
      <p>{presentation.description}</p>

      <span className="tracking-status-badge">
        {presentation.badge}
      </span>
    </div>
  );
}