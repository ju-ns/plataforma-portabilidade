import type { Installment } from "../types/portability";
import { formatCurrency, formatDate } from "../utils/formatters";

interface InstallmentsTableProps {
  installments: Installment[];
  selectedIds: string[];
  disabled: boolean;
  onToggle: (installmentId: string) => void;
}

export default function InstallmentsTable({
  installments,
  selectedIds,
  disabled,
  onToggle,
}: InstallmentsTableProps) {
  return (
    <div className="installments-table-container">
      <table className="installments-table">
        <caption className="visually-hidden">
          Parcelas da agenda de descontos
        </caption>

        <thead>
          <tr>
            <th scope="col">Sel.</th>
            <th scope="col">Parcela</th>
            <th scope="col">Valor</th>
            <th scope="col">Data do desconto</th>
            <th scope="col">Status</th>
          </tr>
        </thead>

        <tbody>
          {installments.map((installment) => {
            const selected = selectedIds.includes(
              installment.installmentId,
            );

            const eligible = installment.status === "ABERTA";

            return (
              <tr
                key={installment.installmentId}
                className={selected ? "installment-selected" : ""}
              >
                <td>
                  <input
                    type="checkbox"
                    checked={selected}
                    disabled={disabled || !eligible}
                    aria-label={`Selecionar parcela ${installment.installmentId}`}
                    onChange={() => onToggle(installment.installmentId)}
                  />
                </td>

                <td>
                  <strong>{installment.installmentId}</strong>

                  <small>
                    Convênio {installment.agreementId}
                  </small>
                </td>

                <td>{formatCurrency(installment.amount)}</td>

                <td>{formatDate(installment.deductionDate)}</td>

                <td>
                  <span className="installment-status">
                    <span aria-hidden="true">○</span>
                    {eligible ? "Aberta" : installment.status}
                  </span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}