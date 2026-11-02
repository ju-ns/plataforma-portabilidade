import { useEffect, useState } from "react";

import { getSchedule } from "../services/scheduleService";
import { ApiError } from "../services/http";
import type { Installment } from "../types/portability";

interface ScheduleState {
  installments: Installment[];
  loading: boolean;
  empty: boolean;
  error: string;
}

function initialState(): ScheduleState {
  return {
    installments: [],
    loading: true,
    empty: false,
    error: "",
  };
}

export function useSchedule(beneficiaryId: string) {
  const [state, setState] = useState<ScheduleState>(initialState);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadSchedule() {
      try {
        const installments = await getSchedule(
          beneficiaryId,
          controller.signal,
        );

        if (controller.signal.aborted) {
          return;
        }

        setState({
          installments,
          loading: false,
          empty: installments.length === 0,
          error: "",
        });
      } catch (exception) {
        if (controller.signal.aborted) {
          return;
        }

        setState({
          installments: [],
          loading: false,
          empty: exception instanceof ApiError && exception.status === 404,
          error:
            exception instanceof ApiError
              ? exception.message
              : "Não foi possível consultar a agenda. Verifique sua conexão.",
        });
      }
    }

    void loadSchedule();

    return () => controller.abort();
  }, [beneficiaryId, attempt]);

  function reload() {
    setState(initialState());
    setAttempt((value) => value + 1);
  }

  return {
    ...state,
    reload,
  };
}