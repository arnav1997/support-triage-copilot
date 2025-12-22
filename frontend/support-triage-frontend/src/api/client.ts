export type TicketStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "WAITING_ON_CUSTOMER"
  | "RESOLVED"
  | "CLOSED";

export type TicketPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export type Ticket = {
  id: number;
  subject: string;
  requesterEmail: string;
  body: string;
  status: TicketStatus;
  priority: TicketPriority;
  category?: string | null;
  tags: string[];
  createdAt: string;
  updatedAt: string;
};

export type CreateTicketRequest = {
  subject: string;
  requesterEmail: string;
  body: string;
  status?: TicketStatus;
  priority?: TicketPriority;
  category?: string;
  tags?: string[];
};

export type UpdateTicketRequest = Partial<
  Pick<Ticket, "subject" | "body" | "status" | "priority" | "category" | "tags">
>;

async function http<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`${res.status} ${res.statusText}: ${text}`);
  }

  // 204 safety
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  listTickets: () => http<Ticket[]>("/api/tickets"),
  getTicket: (id: number) => http<Ticket>(`/api/tickets/${id}`),
  createTicket: (req: CreateTicketRequest) =>
    http<Ticket>("/api/tickets", { method: "POST", body: JSON.stringify(req) }),
  patchTicket: (id: number, req: UpdateTicketRequest) =>
    http<Ticket>(`/api/tickets/${id}`, {
      method: "PATCH",
      body: JSON.stringify(req),
    }),
};
