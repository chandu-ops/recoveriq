INSERT INTO customers (name, email, phone, created_at) VALUES
('Ananya Rao', 'ananya.rao@example.com', '9800000001', now()),
('Vikram Shah', 'vikram.shah@example.com', '9800000002', now()),
('Divya Menon', 'divya.menon@example.com', '9800000003', now()),
('Rahul Verma', 'rahul.verma@example.com', '9800000004', now()),
('Sneha Iyer', 'sneha.iyer@example.com', '9800000005', now());

-- Historical successful payments to give customers a payment-history signal.
-- Ananya: strong history (12). Vikram: moderate (6). Others: little/none.
INSERT INTO payments (customer_id, amount, currency, status, failure_category, created_at, updated_at)
SELECT 1, 500.00, 'INR', 'SUCCEEDED', NULL, now(), now() FROM generate_series(1, 12);

INSERT INTO payments (customer_id, amount, currency, status, failure_category, created_at, updated_at)
SELECT 2, 500.00, 'INR', 'SUCCEEDED', NULL, now(), now() FROM generate_series(1, 6);

INSERT INTO payments (customer_id, amount, currency, status, failure_category, created_at, updated_at)
VALUES (3, 500.00, 'INR', 'SUCCEEDED', NULL, now(), now());

-- Rahul and Sneha: no successful payment history (new customers).

-- The at-risk payments driving each recovery case:
INSERT INTO payments (customer_id, amount, currency, status, failure_category, created_at, updated_at) VALUES
(1, 20000.00, 'INR', 'FAILED', 'NETWORK_FAILURE', now(), now()),
(2, 10000.00, 'INR', 'FAILED', 'INSUFFICIENT_FUNDS', now(), now()),
(3, 50000.00, 'INR', 'FAILED', 'MANDATE_CANCELLED', now(), now()),
(4, 8000.00, 'INR', 'FAILED', 'BANK_DECLINED', now(), now()),
(5, 15000.00, 'INR', 'FAILED', 'CARD_EXPIRED', now(), now());

INSERT INTO recovery_cases
(customer_id, payment_id, amount_at_risk, failure_category, status, retries_used, contacts_used, messages_used, human_escalations_used, created_at, updated_at)
VALUES
(1, (SELECT id FROM payments WHERE customer_id = 1 AND status = 'FAILED'), 20000.00, 'NETWORK_FAILURE', 'OPEN', 0, 0, 0, 0, now(), now()),
(2, (SELECT id FROM payments WHERE customer_id = 2 AND status = 'FAILED'), 10000.00, 'INSUFFICIENT_FUNDS', 'OPEN', 1, 0, 0, 0, now(), now()),
(3, (SELECT id FROM payments WHERE customer_id = 3 AND status = 'FAILED'), 50000.00, 'MANDATE_CANCELLED', 'OPEN', 0, 0, 0, 0, now(), now()),
(4, (SELECT id FROM payments WHERE customer_id = 4 AND status = 'FAILED'), 8000.00, 'BANK_DECLINED', 'OPEN', 2, 0, 0, 0, now(), now()),
(5, (SELECT id FROM payments WHERE customer_id = 5 AND status = 'FAILED'), 15000.00, 'CARD_EXPIRED', 'OPEN', 0, 0, 0, 0, now(), now());