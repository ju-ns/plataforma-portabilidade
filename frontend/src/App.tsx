import { Navigate, Route, Routes, useParams } from "react-router";

import StepIndicator from "./components/StepIndicator";
import AuthorizationPage from "./pages/AuthorizationPage";
import SchedulePage from "./pages/SchedulePage";

function TrackingPlaceholder() {
  const { operationId } = useParams();

  return (
    <>
      <StepIndicator currentStep={3} />

      <section className="card setup-card">
        <h1>Acompanhamento da portabilidade</h1>
        <p>Operação: {operationId}</p>
      </section>
    </>
  );
}

export default function App() {
  return (
    <>
      <header className="app-header">
        <span className="app-brand">folha</span>

        <span className="app-description">
          Portabilidade de convênio
        </span>
      </header>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<AuthorizationPage />} />

          <Route
            path="/beneficiaries/:beneficiaryId/schedule"
            element={<SchedulePage />}
          />

          <Route
            path="/portabilities/:operationId"
            element={<TrackingPlaceholder />}
          />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}