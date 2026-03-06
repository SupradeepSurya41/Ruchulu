-- Seed bookings for H2 testing
INSERT INTO bookings (id, customer_id, customer_name, customer_email, customer_phone,
    caterer_id, caterer_name, occasion, event_date, event_city, event_address,
    guest_count, price_per_plate, total_amount, advance_amount, balance_amount,
    status, payment_status, special_requests, deleted, created_at, updated_at)
VALUES
    ('bk-001', 'usr-001', 'Ravi Kumar', 'ravi@gmail.com', '9876543210',
     'cat-001', 'Lakshmi Catering Services', 'WEDDING',
     DATEADD('DAY', 30, CURRENT_DATE), 'Hyderabad',
     'Taj Krishna, Banjara Hills, Hyderabad',
     200, 450.00, 90000.00, 18000.00, 72000.00,
     'CONFIRMED', 'ADVANCE_PAID', 'No onion, no garlic for 20 guests',
     false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('bk-002', 'usr-002', 'Priya Sharma', 'priya@gmail.com', '9123456780',
     'cat-001', 'Lakshmi Catering Services', 'BIRTHDAY',
     DATEADD('DAY', 15, CURRENT_DATE), 'Hyderabad',
     '8-2-120 Road No. 2, Banjara Hills',
     50, 350.00, 17500.00, 3500.00, 14000.00,
     'PENDING', 'UNPAID', NULL,
     false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Booking timeline events
INSERT INTO booking_events (id, booking_id, event_type, description, performed_by, created_at)
VALUES
    ('evt-001', 'bk-001', 'CREATED', 'Booking created by customer', 'usr-001', CURRENT_TIMESTAMP),
    ('evt-002', 'bk-001', 'CONFIRMED', 'Confirmed by caterer', 'cat-001', CURRENT_TIMESTAMP),
    ('evt-003', 'bk-001', 'ADVANCE_PAID', 'Advance payment of ₹18,000 received', 'usr-001', CURRENT_TIMESTAMP),
    ('evt-004', 'bk-002', 'CREATED', 'Booking created by customer', 'usr-002', CURRENT_TIMESTAMP);
