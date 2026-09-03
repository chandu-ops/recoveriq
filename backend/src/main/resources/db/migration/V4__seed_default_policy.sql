INSERT INTO policies
(name, max_retries, max_contacts, max_messages, max_human_escalations, max_recovery_window_days, active, created_at)
VALUES
('default', 2, 3, 3, 1, 7, true, now());