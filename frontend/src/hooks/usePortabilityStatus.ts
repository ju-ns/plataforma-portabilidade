import { useEffect, useState } from "react";

import { ApiError } from "../services/http";
import { getPortabilityStatus } from "../services/portabilityService";
import type { PortabilityResponse } from "../types/portability";

interface TrackingState {
  operation: PortabilityResponse | null;
  refreshing: boolean;
  error: string;
  lastCheckedAt: Date | null;
}

export function usePortabilityStatus(
  operationId: string,
  initialOperation: PortabilityResponse | null,
) {
  const [state, setState] = useState<TrackingState>(() => ({
    operation: initialOperation,
    refreshing: true,
    error: "",
    lastCheckedAt: null,
  }));

  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;

    async function refresh() {
      if (controller.signal.aborted) {
        return;
      }

      setState((current) => ({
        ...current,
        refreshing: true,
      }));

      try {
        const operation = await getPortabilityStatus(
          operationId,
          controller.signal,
        );

        if (controller.signal.aborted) {
          return;
        }

        setState({
          operation,
          refreshing: false,
          error: "",
          lastCheckedAt: new Date(),
        });

        if (
          operation.status === "CRIADA" ||
          operation.status === "PENDENTE"
        ) {
          timer = setTimeout(() => {
            void refresh();
          }, 2000);
        }
      } catch (exception) {
        if (controller.signal.aborted) {
          return;
        }

        setState((current) => ({
          ...current,
          refreshing: false,
          error:
            exception instanceof ApiError
              ? exception.message
              : "Não foi possível atualizar o status. Verifique sua conexão.",
        }));
      }
    }

    timer = setTimeout(() => {
      void refresh();
    }, 0);

    return () => {
      controller.abort();

      if (timer !== undefined) {
        clearTimeout(timer);
      }
    };
  }, [operationId, attempt]);

  function reload() {
    setState((current) => ({
      ...current,
      refreshing: true,
      error: "",
    }));

    setAttempt((value) => value + 1);
  }

  return {
    ...state,
    reload,
  };
}