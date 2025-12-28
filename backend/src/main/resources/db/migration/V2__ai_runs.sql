-- Extend ai_runs to support failures and keep output optional on error
alter table ai_runs
  add column if not exists status varchar(16) not null default 'SUCCESS';

alter table ai_runs
  add column if not exists error_message text;

-- allow output_json to be null when status=ERROR
alter table ai_runs
  alter column output_json drop not null;

-- allow latency_ms to be null if desired (optional)
alter table ai_runs
  alter column latency_ms drop not null;
