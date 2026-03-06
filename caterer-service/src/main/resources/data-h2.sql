-- Caterer Profiles seed data
INSERT INTO caterer_profiles (id, user_id, business_name, owner_name, email, phone,
    city, address, fssai_number, fssai_verified, is_vegetarian, is_non_vegetarian,
    is_vegan, min_guests, max_guests, price_per_plate_min, price_per_plate_max,
    experience_years, profile_status, rating, total_reviews, deleted, created_at, updated_at)
VALUES
    ('cat-001', 'usr-004', 'Lakshmi Catering Services', 'Lakshmi Devi',
     'lakshmi@gmail.com', '9988776655', 'Hyderabad',
     '12-3-456, Banjara Hills, Hyderabad', 'FSSAI123456789', true,
     true, true, false, 50, 500, 250.00, 600.00, 12,
     'ACTIVE', 4.7, 128, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('cat-002', 'usr-cat-002', 'Royal Feast Caterers', 'Suresh Reddy',
     'suresh@gmail.com', '9876512345', 'Vijayawada',
     '8-1-22, MG Road, Vijayawada', 'FSSAI987654321', true,
     true, true, false, 100, 1000, 350.00, 800.00, 8,
     'ACTIVE', 4.5, 89, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Menu Items seed data
INSERT INTO menu_items (id, caterer_id, name, description, category,
    cuisine_type, is_vegetarian, price_per_plate, is_available, created_at, updated_at)
VALUES
    ('menu-001', 'cat-001', 'Hyderabadi Biryani', 'Authentic dum biryani with saffron',
     'MAIN_COURSE', 'HYDERABADI', false, 180.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu-002', 'cat-001', 'Gulab Jamun', 'Soft khoya balls in sugar syrup',
     'DESSERT', 'NORTH_INDIAN', true, 40.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu-003', 'cat-001', 'Pesarattu', 'Green moong dal dosa with ginger chutney',
     'BREAKFAST', 'ANDHRA', true, 60.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('menu-004', 'cat-002', 'Andhra Chicken Curry', 'Spicy Andhra style chicken curry',
     'MAIN_COURSE', 'ANDHRA', false, 200.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Occasions seed data
INSERT INTO caterer_occasions (caterer_id, occasion)
VALUES
    ('cat-001', 'WEDDING'), ('cat-001', 'BIRTHDAY'), ('cat-001', 'CORPORATE'),
    ('cat-001', 'ENGAGEMENT'), ('cat-001', 'FESTIVAL'),
    ('cat-002', 'WEDDING'), ('cat-002', 'CORPORATE'), ('cat-002', 'ANNIVERSARY');
