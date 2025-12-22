-- Tickets
create table tickets (
  id bigserial primary key,
  subject varchar(255) not null,
  requester_email varchar(320) not null,
  body text not null,
  status varchar(32) not null,
  priority varchar(32) not null,
  category varchar(64),
  tags text[] not null default '{}',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_tickets_status on tickets(status);
create index idx_tickets_priority on tickets(priority);
create index idx_tickets_created_at on tickets(created_at);

-- AI runs
create table ai_runs (
  id bigserial primary key,
  ticket_id bigint not null references tickets(id) on delete cascade,
  type varchar(64) not null,
  provider varchar(64) not null,
  model varchar(128) not null,
  prompt_version varchar(64) not null,
  input_json jsonb not null,
  output_json jsonb not null,
  latency_ms int not null,
  created_at timestamptz not null default now()
);

create index idx_ai_runs_ticket_id on ai_runs(ticket_id);
create index idx_ai_runs_created_at on ai_runs(created_at);

-- Ticket notes
create table ticket_notes (
  id bigserial primary key,
  ticket_id bigint not null references tickets(id) on delete cascade,
  type varchar(32) not null,
  body text not null,
  created_at timestamptz not null default now()
);

create index idx_ticket_notes_ticket_id on ticket_notes(ticket_id);
create index idx_ticket_notes_created_at on ticket_notes(created_at);
