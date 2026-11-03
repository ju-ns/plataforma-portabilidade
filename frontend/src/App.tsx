import { Navigate, Route, Routes } from "react-router";

import AuthorizationPage from "./pages/AuthorizationPage";
import SchedulePage from "./pages/SchedulePage";
import TrackingPage from "./pages/TrackingPage";

export default function App() {
  return (
    <>
      <header className="app-header">
        <span className="app-brand">Folha</span>

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
            element={<TrackingPage />}
          />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}