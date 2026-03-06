-- =============================================================
--  Ruchulu — H2 Seed Data
--  Loaded automatically when profile = h2
--  Passwords = BCrypt of "Test@1234"
-- =============================================================

INSERT INTO users (id, first_name, last_name, email, phone, password_hash,
                   role, city, account_status, email_verified, phone_verified,
                   auth_provider, deleted, created_at, updated_at)
VALUES
    ('usr-001', 'Ravi',    'Kumar',   'ravi@gmail.com',    '9876543210',
     '$2a$12$FpRQ3w5TqDTiEIwrVyS1OuqkrqeQ8QkMJwBTXIpqJlBpwV5YiJ/0.',
     'CUSTOMER', 'Hyderabad', 'ACTIVE', true, false, 'LOCAL', false,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('usr-002', 'Priya',   'Sharma',  'priya@gmail.com',   '9123456780',
     '$2a$12$FpRQ3w5TqDTiEIwrVyS1OuqkrqeQ8QkMJwBTXIpqJlBpwV5YiJ/0.',
     'CUSTOMER', 'Vijayawada', 'ACTIVE', true, false, 'LOCAL', false,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('usr-003', 'Venkat',  'Rao',     'venkat@gmail.com',  '9000000001',
     '$2a$12$FpRQ3w5TqDTiEIwrVyS1OuqkrqeQ8QkMJwBTXIpqJlBpwV5YiJ/0.',
     'ADMIN', 'Hyderabad', 'ACTIVE', true, false, 'LOCAL', false,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('usr-004', 'Lakshmi', 'Devi',    'lakshmi@gmail.com', '9988776655',
     '$2a$12$FpRQ3w5TqDTiEIwrVyS1OuqkrqeQ8QkMJwBTXIpqJlBpwV5YiJ/0.',
     'CATERER', 'Hyderabad', 'ACTIVE', true, false, 'LOCAL', false,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
