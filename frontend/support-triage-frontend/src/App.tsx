import { Link, Route, Routes } from "react-router-dom";
import Inbox from "./pages/Inbox";
import TicketDetail from "./pages/TicketDetail";

export default function App() {
  return (
    <div style={{ maxWidth: 1100, margin: "0 auto", padding: 16 }}>
      <header style={{ display: "flex", gap: 12, alignItems: "center" }}>
        <h2 style={{ margin: 0 }}>Support Triage</h2>
        <nav style={{ display: "flex", gap: 10 }}>
          <Link to="/">Inbox</Link>
        </nav>
      </header>

      <hr style={{ margin: "16px 0" }} />

      <Routes>
        <Route path="/" element={<Inbox />} />
        <Route path="/tickets/:id" element={<TicketDetail />} />
      </Routes>
    </div>
  );
}
