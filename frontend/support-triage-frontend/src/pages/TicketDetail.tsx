import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import type { Ticket } from "../api/client";


export default function TicketDetail() {
  const { id } = useParams();
  const ticketId = Number(id);

  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // editable fields
  const [status, setStatus] = useState<Ticket["status"]>("OPEN");
  const [priority, setPriority] = useState<Ticket["priority"]>("MEDIUM");
  const [category, setCategory] = useState("");
  const [tags, setTags] = useState("");

  async function load() {
    setError(null);
    try {
      const t = await api.getTicket(ticketId);
      setTicket(t);
      setStatus(t.status);
      setPriority(t.priority);
      setCategory(t.category ?? "");
      setTags((t.tags ?? []).join(", "));
    } catch (e: any) {
      setError(e?.message ?? "Failed to load ticket");
    }
  }

  useEffect(() => {
    if (!Number.isFinite(ticketId)) return;
    load();
  }, [ticketId]);

  async function save() {
    setSaving(true);
    setError(null);
    try {
      const updated = await api.patchTicket(ticketId, {
        status,
        priority,
        category: category.trim() ? category.trim() : null,
        tags: tags
          .split(",")
          .map((t) => t.trim())
          .filter(Boolean),
      });
      setTicket(updated);
    } catch (e: any) {
      setError(e?.message ?? "Failed to save");
    } finally {
      setSaving(false);
    }
  }

  if (!Number.isFinite(ticketId)) return <p>Invalid ticket id.</p>;

  return (
    <div>
      <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
        <Link to="/">← Inbox</Link>
        <h3 style={{ margin: 0 }}>Ticket #{ticketId}</h3>
      </div>

      {error && (
        <div style={{ marginTop: 12, padding: 12, border: "1px solid #ccc" }}>
          {error}
        </div>
      )}

      {!ticket ? (
        <p style={{ marginTop: 12 }}>Loading...</p>
      ) : (
        <div style={{ marginTop: 12, display: "grid", gap: 12 }}>
          <div
            style={{
              border: "1px solid #ddd",
              padding: 12,
              borderRadius: 8,
            }}
          >
            <div style={{ fontSize: 18, fontWeight: 700 }}>{ticket.subject}</div>
            <div style={{ marginTop: 6, opacity: 0.85 }}>
              From: {ticket.requesterEmail}
            </div>
          </div>

          <div
            style={{
              border: "1px solid #ddd",
              padding: 12,
              borderRadius: 8,
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: 12,
            }}
          >
            <label style={{ display: "block" }}>
              Status
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value as any)}
                style={{ width: "100%", padding: 8, marginTop: 6 }}
              >
                <option value="OPEN">OPEN</option>
                <option value="IN_PROGRESS">IN_PROGRESS</option>
                <option value="WAITING_ON_CUSTOMER">WAITING_ON_CUSTOMER</option>
                <option value="RESOLVED">RESOLVED</option>
                <option value="CLOSED">CLOSED</option>
              </select>
            </label>

            <label style={{ display: "block" }}>
              Priority
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as any)}
                style={{ width: "100%", padding: 8, marginTop: 6 }}
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
            </label>

            <label style={{ display: "block" }}>
              Category
              <input
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                style={{ width: "100%", padding: 8, marginTop: 6 }}
                placeholder="AUTH / BILLING / BUG / ..."
              />
            </label>

            <label style={{ display: "block" }}>
              Tags (comma-separated)
              <input
                value={tags}
                onChange={(e) => setTags(e.target.value)}
                style={{ width: "100%", padding: 8, marginTop: 6 }}
                placeholder="login, sev2"
              />
            </label>

            <button
              onClick={save}
              disabled={saving}
              style={{
                gridColumn: "1 / -1",
                padding: 10,
                cursor: saving ? "not-allowed" : "pointer",
              }}
            >
              {saving ? "Saving..." : "Save changes"}
            </button>
          </div>

          <div
            style={{
              border: "1px solid #ddd",
              padding: 12,
              borderRadius: 8,
            }}
          >
            <h4 style={{ marginTop: 0 }}>Body</h4>
            <pre style={{ whiteSpace: "pre-wrap", margin: 0 }}>
              {ticket.body}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}
