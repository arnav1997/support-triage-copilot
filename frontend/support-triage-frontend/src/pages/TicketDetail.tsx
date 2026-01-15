import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import type {
  Ticket,
  TicketNote,
  AiTriageSuggestion,
  AiSummaryResponse,
  ReplyTone,
  AiReplyDraftResponse,
} from "../api/client";

function formatAiSummaryNoteBody(subject: string | undefined, r: AiSummaryResponse) {
  const s = (subject ?? "").trim();
  const title = s ? `AI Summary — ${s}` : "AI Summary";

  const lines: string[] = [];
  lines.push(title);
  lines.push("");
  lines.push((r.summary ?? "").trim());

  const points = (r.keyPoints ?? [])
    .map((x) => (x ?? "").trim())
    .filter(Boolean);

  if (points.length) {
    lines.push("");
    lines.push("Key points:");
    for (const p of points) lines.push(`- ${p}`);
  }

  return lines.join("\n").trim();
}

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

  const [notes, setNotes] = useState<TicketNote[]>([]);
  const [noteBody, setNoteBody] = useState("");
  const [noteSaving, setNoteSaving] = useState(false);

  // --- AI triage state ---
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const [aiSuggestion, setAiSuggestion] = useState<AiTriageSuggestion | null>(null);
  const [aiApplying, setAiApplying] = useState(false);

  // --- AI summary state ---
  const [sumLoading, setSumLoading] = useState(false);
  const [sumError, setSumError] = useState<string | null>(null);
  const [sumResult, setSumResult] = useState<AiSummaryResponse | null>(null);
  const [sumSavingNote, setSumSavingNote] = useState(false);

    // --- AI reply draft state ---
  const [replyTone, setReplyTone] = useState<ReplyTone>("EMPATHETIC");
  const [replyLoading, setReplyLoading] = useState(false);
  const [replyError, setReplyError] = useState<string | null>(null);
  const [replyResult, setReplyResult] = useState<AiReplyDraftResponse | null>(null);
  const [replyText, setReplyText] = useState("");
  const [copied, setCopied] = useState(false);

  const hasSavedNoteFromSummary = useMemo(() => {
    return !!sumResult?.savedNoteId;
  }, [sumResult]);

  async function load() {
    setError(null);
    try {
      const t = await api.getTicket(ticketId);
      setTicket(t);

      setReplyResult(null);
      setReplyText("");
      setReplyError(null);
      setCopied(false);
      
      setStatus(t.status);
      setPriority(t.priority);
      setCategory(t.category ?? "");
      setTags((t.tags ?? []).join(", "));

      const ns = await api.listNotes(ticketId);
      setNotes(ns);
    } catch (e: any) {
      setError(e?.message ?? "Failed to load ticket");
    }
  }

  useEffect(() => {
    if (!Number.isFinite(ticketId)) return;
    load();
  }, [ticketId]);

  async function refreshNotes() {
    const ns = await api.listNotes(ticketId);
    setNotes(ns);
  }

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

  async function addNote() {
    const bodyTrimmed = noteBody.trim();
    if (!bodyTrimmed) return;

    setNoteSaving(true);
    setError(null);
    try {
      const created = await api.createNote(ticketId, { type: "note", body: bodyTrimmed });
      setNotes((prev) => [created, ...prev]);
      setNoteBody("");
    } catch (e: any) {
      setError(e?.message ?? "Failed to add note");
    } finally {
      setNoteSaving(false);
    }
  }

  async function suggestTriage() {
    setAiLoading(true);
    setAiError(null);
    try {
      const s = await api.triageTicket(ticketId);
      setAiSuggestion(s);
    } catch (e: any) {
      setAiError(e?.message ?? "AI triage failed");
    } finally {
      setAiLoading(false);
    }
  }

  async function applySuggestion(part: "all" | "category" | "priority" | "tags") {
    if (!aiSuggestion) return;

    setAiApplying(true);
    setError(null);

    const patch: any = {};
    if (part === "all" || part === "category") patch.category = aiSuggestion.category;
    if (part === "all" || part === "priority") patch.priority = aiSuggestion.priority;
    if (part === "all" || part === "tags") patch.tags = aiSuggestion.tags;

    try {
      const updated = await api.patchTicket(ticketId, patch);

      // update main ticket + form state
      setTicket(updated);
      setStatus(updated.status);
      setPriority(updated.priority);
      setCategory(updated.category ?? "");
      setTags((updated.tags ?? []).join(", "));

      // optional: keep showing suggestion after apply
    } catch (e: any) {
      setError(e?.message ?? "Failed to apply AI suggestion");
    } finally {
      setAiApplying(false);
    }
  }

  async function summarize(saveAsNote: boolean) {
    setSumLoading(true);
    setSumError(null);
    try {
      const r = await api.summarizeTicket(ticketId, saveAsNote);
      setSumResult(r);

      // If backend saved a note, refresh notes so it shows up.
      if (r.savedNoteId) {
        await refreshNotes();
      }
    } catch (e: any) {
      setSumError(e?.message ?? "AI summary failed");
    } finally {
      setSumLoading(false);
    }
  }

  async function saveSummaryAsNoteFromCurrent() {
    if (!ticket || !sumResult) return;

    setSumSavingNote(true);
    setError(null);
    try {
      const body = formatAiSummaryNoteBody(ticket.subject, sumResult);
      const created = await api.createNote(ticketId, { type: "ai_summary", body });
      setNotes((prev) => [created, ...prev]);

      // mark as saved (without re-running the model)
      setSumResult((prev) => (prev ? { ...prev, savedNoteId: created.id } : prev));
    } catch (e: any) {
      setError(e?.message ?? "Failed to save AI summary as note");
    } finally {
      setSumSavingNote(false);
    }
  }

  async function generateReplyDraft() {
    setReplyLoading(true);
    setReplyError(null);
    setCopied(false);

    try {
      const r = await api.replyDraft(ticketId, replyTone);
      setReplyResult(r);
      setReplyText(r.draft ?? "");
    } catch (e: any) {
      setReplyError(e?.message ?? "AI reply draft failed");
    } finally {
      setReplyLoading(false);
    }
  }

  async function copyReplyDraft() {
    const text = (replyText ?? "").trim();
    if (!text) return;

    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1200);
    } catch {
      // fallback for older browsers / insecure contexts
      try {
        const ta = document.createElement("textarea");
        ta.value = text;
        ta.style.position = "fixed";
        ta.style.left = "-9999px";
        document.body.appendChild(ta);
        ta.focus();
        ta.select();
        document.execCommand("copy");
        document.body.removeChild(ta);
        setCopied(true);
        window.setTimeout(() => setCopied(false), 1200);
      } catch (err) {
        setReplyError("Copy failed (browser blocked clipboard access).");
      }
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
        <div
          style={{
            marginTop: 12,
            display: "grid",
            gridTemplateColumns: "1fr 380px",
            gap: 12,
            alignItems: "start",
          }}
        >
          {/* LEFT */}
          <div style={{ display: "grid", gap: 12 }}>
            <div style={{ border: "1px solid #ddd", padding: 12, borderRadius: 8 }}>
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

            <div style={{ border: "1px solid #ddd", padding: 12, borderRadius: 8 }}>
              <h4 style={{ marginTop: 0 }}>Body</h4>
              <pre style={{ whiteSpace: "pre-wrap", margin: 0 }}>{ticket.body}</pre>
            </div>
          </div>

          {/* RIGHT: ACTIVITY */}
            <aside style={{ border: "1px solid #ddd", padding: 12, borderRadius: 8 }}>
            <h4 style={{ marginTop: 0 }}>Activity</h4>

          {/* AI SUMMARY PANEL */}
            <div style={{ marginBottom: 12, padding: 10, border: "1px solid #eee", borderRadius: 8 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
                <div style={{ fontWeight: 600 }}>AI summary</div>
                <div style={{ display: "flex", gap: 8 }}>
                  <button
                    onClick={() => summarize(false)}
                    disabled={sumLoading}
                    style={{ padding: "8px 10px", cursor: sumLoading ? "not-allowed" : "pointer" }}
                  >
                    {sumLoading ? "Thinking..." : "Summarize"}
                  </button>
                  <button
                    onClick={() => summarize(true)}
                    disabled={sumLoading}
                    style={{ padding: "8px 10px", cursor: sumLoading ? "not-allowed" : "pointer" }}
                    title="Generates summary and auto-saves as an internal note"
                  >
                    {sumLoading ? "Thinking..." : "Summarize + Save"}
                  </button>
                </div>
              </div>

              {sumError && (
                <div style={{ marginTop: 10, padding: 10, border: "1px solid #f0c", borderRadius: 8 }}>
                  {sumError}
                </div>
              )}

              {!sumResult ? (
                <div style={{ marginTop: 8, fontSize: 13, opacity: 0.8 }}>
                  No summary yet.
                </div>
              ) : (
                <div style={{ marginTop: 10, display: "grid", gap: 10 }}>
                  <div style={{ fontSize: 13 }}>
                    <div style={{ fontWeight: 600, marginBottom: 6 }}>Summary</div>
                    <div style={{ whiteSpace: "pre-wrap" }}>{sumResult.summary}</div>

                    {sumResult.keyPoints?.length > 0 && (
                      <div style={{ marginTop: 10 }}>
                        <div style={{ fontWeight: 600, marginBottom: 6 }}>Key points</div>
                        <ul style={{ margin: 0, paddingLeft: 18 }}>
                          {sumResult.keyPoints.map((kp, idx) => (
                            <li key={idx}>{kp}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    <div style={{ marginTop: 10, fontSize: 12, opacity: 0.75 }}>
                      {sumResult.savedNoteId ? (
                        <>Saved as note (id: {sumResult.savedNoteId})</>
                      ) : (
                        <>Not saved yet.</>
                      )}
                    </div>
                  </div>

                  {!hasSavedNoteFromSummary && (
                    <button
                      onClick={saveSummaryAsNoteFromCurrent}
                      disabled={sumSavingNote}
                      style={{ padding: 10, cursor: sumSavingNote ? "not-allowed" : "pointer" }}
                      title="Saves the currently displayed summary as a note (no extra AI call)"
                    >
                      {sumSavingNote ? "Saving..." : "Save this summary as note"}
                    </button>
                  )}
                </div>
              )}
            </div>

          {/* AI REPLY DRAFT PANEL */}
            <div
              style={{
                marginBottom: 12,
                padding: 10,
                border: "1px solid #eee",
                borderRadius: 8,
              }}
            >
              <div style={{ fontWeight: 600, marginBottom: 8 }}>Reply draft</div>

              <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <label style={{ display: "flex", gap: 8, alignItems: "center", flex: 1 }}>
                  Tone
                  <select
                    value={replyTone}
                    onChange={(e) => setReplyTone(e.target.value as ReplyTone)}
                    style={{ flex: 1, padding: 8 }}
                  >
                    <option value="EMPATHETIC">EMPATHETIC</option>
                    <option value="PROFESSIONAL">PROFESSIONAL</option>
                    <option value="CONCISE">CONCISE</option>
                  </select>
                </label>

                <button
                  onClick={generateReplyDraft}
                  disabled={replyLoading}
                  style={{
                    padding: "8px 10px",
                    cursor: replyLoading ? "not-allowed" : "pointer",
                    whiteSpace: "nowrap",
                  }}
                >
                  {replyLoading ? "Thinking..." : "Generate"}
                </button>
              </div>

              {replyError && (
                <div style={{ marginTop: 10, padding: 10, border: "1px solid #f0c", borderRadius: 8 }}>
                  {replyError}
                </div>
              )}

              <textarea
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                placeholder="Generate a draft reply..."
                style={{
                  width: "100%",
                  padding: 8,
                  height: 140,
                  marginTop: 10,
                  resize: "vertical",
                }}
              />

              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 8 }}>
                <button
                  onClick={copyReplyDraft}
                  disabled={!replyText.trim()}
                  style={{
                    padding: "8px 10px",
                    cursor: replyText.trim() ? "pointer" : "not-allowed",
                  }}
                >
                  {copied ? "Copied!" : "Copy"}
                </button>

                <div style={{ fontSize: 12, opacity: 0.7 }}>
                  {replyResult?.aiRunId ? <>aiRunId: {replyResult.aiRunId}</> : null}
                </div>
              </div>
            </div>

          {/* AI TRIAGE PANEL */}
            <div style={{ marginBottom: 12, padding: 10, border: "1px solid #eee", borderRadius: 8 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
                <div style={{ fontWeight: 600 }}>AI triage</div>
                <button
                  onClick={suggestTriage}
                  disabled={aiLoading}
                  style={{
                    padding: "8px 10px",
                    cursor: aiLoading ? "not-allowed" : "pointer",
                  }}
                >
                  {aiLoading ? "Thinking..." : "Suggest triage"}
                </button>
              </div>

              {aiError && (
                <div style={{ marginTop: 10, padding: 10, border: "1px solid #f0c", borderRadius: 8 }}>
                  {aiError}
                </div>
              )}

              {!aiSuggestion ? (
                <div style={{ marginTop: 8, fontSize: 13, opacity: 0.8 }}>
                  No suggestions yet.
                </div>
              ) : (
                <div style={{ marginTop: 10, display: "grid", gap: 10 }}>
                  <div style={{ fontSize: 13 }}>
                    <div><b>Category:</b> {aiSuggestion.category}</div>
                    <div><b>Priority:</b> {aiSuggestion.priority}</div>
                    <div><b>Tags:</b> {aiSuggestion.tags?.length ? aiSuggestion.tags.join(", ") : "(none)"}</div>
                    <div style={{ marginTop: 8, opacity: 0.9 }}>
                      <b>Rationale:</b> {aiSuggestion.rationale}
                    </div>
                    <div style={{ marginTop: 8, fontSize: 12, opacity: 0.7 }}>
                      aiRunId: {aiSuggestion.aiRunId}
                    </div>
                  </div>

                  <div style={{ display: "grid", gap: 8 }}>
                    <button
                      onClick={() => applySuggestion("all")}
                      disabled={aiApplying}
                      style={{ padding: 10, cursor: aiApplying ? "not-allowed" : "pointer" }}
                    >
                      {aiApplying ? "Applying..." : "Apply all"}
                    </button>

                    <div style={{ display: "flex", gap: 8 }}>
                      <button
                        onClick={() => applySuggestion("category")}
                        disabled={aiApplying}
                        style={{ flex: 1, padding: 10, cursor: aiApplying ? "not-allowed" : "pointer" }}
                      >
                        Apply category
                      </button>
                      <button
                        onClick={() => applySuggestion("priority")}
                        disabled={aiApplying}
                        style={{ flex: 1, padding: 10, cursor: aiApplying ? "not-allowed" : "pointer" }}
                      >
                        Apply priority
                      </button>
                    </div>

                    <button
                      onClick={() => applySuggestion("tags")}
                      disabled={aiApplying}
                      style={{ padding: 10, cursor: aiApplying ? "not-allowed" : "pointer" }}
                    >
                      Apply tags
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div style={{ marginBottom: 10, fontWeight: 600 }}>Notes</div>

            <textarea
              value={noteBody}
              onChange={(e) => setNoteBody(e.target.value)}
              placeholder="Add a note..."
              style={{ width: "100%", padding: 8, height: 90 }}
            />

            <button
              onClick={addNote}
              disabled={noteSaving || !noteBody.trim()}
              style={{
                width: "100%",
                padding: 10,
                marginTop: 8,
                cursor: noteSaving ? "not-allowed" : "pointer",
              }}
            >
              {noteSaving ? "Adding..." : "Add note"}
            </button>

            <div style={{ marginTop: 12, display: "grid", gap: 10 }}>
              {notes.length === 0 ? (
                <div style={{ fontSize: 13, opacity: 0.8 }}>No notes yet.</div>
              ) : (
                notes.map((n) => (
                  <div key={n.id} style={{ border: "1px solid #eee", padding: 10, borderRadius: 8 }}>
                    <div style={{ fontSize: 12, opacity: 0.7 }}>
                      {n.type} · {new Date(n.createdAt).toLocaleString()}
                    </div>
                    <div style={{ marginTop: 6, whiteSpace: "pre-wrap" }}>{n.body}</div>
                  </div>
                ))
              )}
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
