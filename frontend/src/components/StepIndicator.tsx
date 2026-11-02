import "./StepIndicator.css";

interface StepIndicatorProps {
  currentStep: 1 | 2 | 3;
}

const steps = ["Autorização", "Parcelas", "Acompanhamento"];

export default function StepIndicator({
  currentStep,
}: StepIndicatorProps) {
  return (
    <nav aria-label="Etapas da portabilidade">
      <ol className="step-indicator">
        {steps.map((label, index) => {
          const step = index + 1;
          const completed = step < currentStep;
          const active = step === currentStep;

          const className = [
            "step-item",
            active ? "step-item--active" : "",
            completed ? "step-item--completed" : "",
          ]
            .filter(Boolean)
            .join(" ");

          return (
            <li
              key={label}
              className={className}
              aria-current={active ? "step" : undefined}
            >
            <span className="step-number" aria-hidden="true">
                {completed ? "✓" : step}
              </span>

              <span>{label}</span>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}