import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import type { CreateTicketRequest, Ticket } from "../api/client";

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

export default function Inbox() {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // simple create form state
  const [subject, setSubject] = useState("");
  const [requesterEmail, setRequesterEmail] = useState("");
  const [body, setBody] = useState("");
  const [priority, setPriority] = useState<"LOW" | "MEDIUM" | "HIGH" | "URGENT">(
    "MEDIUM"
  );
  const [category, setCategory] = useState("");
  const [tags, setTags] = useState(""); // comma-separated

  const canSubmit = useMemo(() => {
    return subject.trim() && requesterEmail.trim() && body.trim();
  }, [subject, requesterEmail, body]);

  async function refresh() {
    setError(null);
    setLoading(true);
    try {
      const data = await api.listTickets();
      setTickets(data);
    } catch (e: any) {
      setError(e?.message ?? "Failed to load tickets");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  async function onCreate() {
    setError(null);
    const req: CreateTicketRequest = {
      subject: subject.trim(),
      requesterEmail: requesterEmail.trim(),
      body: body.trim(),
      priority,
      category: category.trim() ? category.trim() : undefined,
      tags: tags
        .split(",")
        .map((t) => t.trim())
        .filter(Boolean),
    };

    try {
      await api.createTicket(req);
      setSubject("");
      setRequesterEmail("");
      setBody("");
      setPriority("MEDIUM");
      setCategory("");
      setTags("");
      await refresh();
    } catch (e: any) {
      setError(e?.message ?? "Failed to create ticket");
    }
  }

  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr 420px", gap: 16 }}>
      <section>
        <h3 style={{ marginTop: 0 }}>Inbox</h3>

        {loading && <p>Loading...</p>}
        {error && (
          <div style={{ padding: 12, border: "1px solid #ccc" }}>{error}</div>
        )}

        {!loading && tickets.length === 0 && <p>No tickets yet.</p>}

        <div style={{ display: "grid", gap: 10 }}>
          {tickets.map((t) => (
            <div
              key={t.id}
              style={{ border: "1px solid #ddd", padding: 12, borderRadius: 8 }}
            >
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <Link to={`/tickets/${t.id}`} style={{ fontWeight: 600 }}>
                  {t.subject}
                </Link>
                <span>
                  {t.priority} · {t.status}
                </span>
              </div>
              <div style={{ fontSize: 12, opacity: 0.8, marginTop: 6 }}>
                {t.requesterEmail} · {formatDate(t.createdAt)}
              </div>
              {t.tags?.length > 0 && (
                <div style={{ marginTop: 8, fontSize: 12 }}>
                  Tags: {t.tags.join(", ")}
                </div>
              )}
            </div>
          ))}
        </div>
      </section>

      <aside style={{ border: "1px solid #ddd", padding: 12, borderRadius: 8 }}>
        <h3 style={{ marginTop: 0 }}>Create ticket</h3>

        <label style={{ display: "block", marginTop: 10 }}>
          Subject
          <input
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            style={{ width: "100%", padding: 8, marginTop: 6 }}
            placeholder="e.g., Can’t log in"
          />
        </label>

        <label style={{ display: "block", marginTop: 10 }}>
          Requester email
          <input
            value={requesterEmail}
            onChange={(e) => setRequesterEmail(e.target.value)}
            style={{ width: "100%", padding: 8, marginTop: 6 }}
            placeholder="customer@example.com"
          />
        </label>

        <label style={{ display: "block", marginTop: 10 }}>
          Body
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            style={{ width: "100%", padding: 8, marginTop: 6, height: 120 }}
            placeholder="Describe the issue..."
          />
        </label>

        <label style={{ display: "block", marginTop: 10 }}>
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

        <label style={{ display: "block", marginTop: 10 }}>
          Category
          <input
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            style={{ width: "100%", padding: 8, marginTop: 6 }}
            placeholder="AUTH / BILLING / BUG / ..."
          />
        </label>

        <label style={{ display: "block", marginTop: 10 }}>
          Tags (comma-separated)
          <input
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            style={{ width: "100%", padding: 8, marginTop: 6 }}
            placeholder="login, sev2"
          />
        </label>

        <button
          disabled={!canSubmit}
          onClick={onCreate}
          style={{
            width: "100%",
            padding: 10,
            marginTop: 14,
            cursor: canSubmit ? "pointer" : "not-allowed",
          }}
        >
          Create
        </button>
      </aside>
    </div>
  );
}
