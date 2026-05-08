-- =============================================================
-- initialize_data.sql
-- ExpenseTrack Sample Data
--
-- Purpose : Populates all tables with realistic sample data.
--           Every table receives at least 15 rows.
-- Usage   : mysql -u root -p < database/initialize_data.sql
-- Order   : Run AFTER create_schema.sql
-- =============================================================

USE expense_tracker;

-- -------------------------------------------------------------
-- Users  (15 rows: 1 admin + 14 regular users)
-- Passwords are plain text here; PasswordMigrationRunner
-- automatically BCrypt-hashes them on the first application
-- startup (one-time migration).
-- -------------------------------------------------------------
INSERT INTO Users (name, email, password, role) VALUES
('Admin User',    'admin@expensetrack.com', 'admin123',  'ADMIN'),
('Alice Johnson', 'alice@example.com',      'alice123',  'USER'),
('Bob Smith',     'bob@example.com',        'bob123',    'USER'),
('Carol Davis',   'carol@example.com',      'carol123',  'USER'),
('David Wilson',  'david@example.com',      'david123',  'USER'),
('Emma Brown',    'emma@example.com',       'emma123',   'USER'),
('Frank Miller',  'frank@example.com',      'frank123',  'USER'),
('Grace Taylor',  'grace@example.com',      'grace123',  'USER'),
('Henry Lee',     'henry@example.com',      'henry123',  'USER'),
('Iris Chen',     'iris@example.com',       'iris123',   'USER'),
('Jack Park',     'jack@example.com',       'jack123',   'USER'),
('Kelly Nguyen',  'kelly@example.com',      'kelly123',  'USER'),
('Liam Scott',    'liam@example.com',       'liam123',   'USER'),
('Mia Roberts',   'mia@example.com',        'mia123',    'USER'),
('Noah Hall',     'noah@example.com',       'noah123',   'USER');

-- -------------------------------------------------------------
-- Categories  (15 rows)
-- Global lookup table shared by all users.
-- category_id values are 1-15 in insertion order.
-- -------------------------------------------------------------
INSERT INTO Categories (category_name) VALUES
('Food & Dining'),       --  1
('Transportation'),      --  2
('Utilities & Bills'),   --  3
('Shopping & Retail'),   --  4
('Entertainment'),       --  5
('Healthcare'),          --  6
('Education'),           --  7
('Travel'),              --  8
('Housing & Rent'),      --  9
('Personal Care'),       -- 10
('Fitness & Wellness'),  -- 11
('Subscriptions'),       -- 12
('Insurance'),           -- 13
('Gifts & Donations'),   -- 14
('Business & Work');     -- 15

-- -------------------------------------------------------------
-- Expenses  (30 rows — spread across users 2-15)
-- user_id map:
--   2=Alice, 3=Bob,   4=Carol, 5=David, 6=Emma,
--   7=Frank, 8=Grace, 9=Henry, 10=Iris, 11=Jack,
--   12=Kelly, 13=Liam, 14=Mia, 15=Noah
-- -------------------------------------------------------------
INSERT INTO Expenses (user_id, category_id, expense_name, amount, date, payment_method, notes) VALUES
-- Alice
(2,  1, 'Whole Foods Grocery Run',     85.50, '2026-01-05', 'Debit Card',    'Weekly grocery shopping'),
(2,  2, 'Shell Gas Station',           52.00, '2026-01-06', 'Credit Card',   'Monthly gas fill-up'),
(2,  4, 'Amazon Household Order',     134.99, '2026-01-10', 'Credit Card',   'Cleaning supplies and kitchenware'),
(2,  5, 'Spotify Premium',             10.99, '2026-01-01', 'Credit Card',   'Monthly music subscription'),
-- Bob
(3,  3, 'Electric Bill - January',     98.00, '2026-01-08', 'Bank Transfer', 'PG&E monthly electric bill'),
(3,  5, 'AMC Movie Night',             28.00, '2026-01-12', 'Cash',          'Weekend movie tickets for two'),
(3,  6, 'CVS Pharmacy',                45.75, '2026-01-14', 'Debit Card',    'Monthly prescription refill'),
(3,  2, 'Clipper Card Reload',         50.00, '2026-01-03', 'Apple Pay',     'Public transit monthly top-up'),
-- Carol
(4,  7, 'Coursera Annual Plan',       199.00, '2026-01-03', 'Credit Card',   'Online learning subscription'),
(4,  8, 'Marriott Hotel Stay',        240.00, '2026-01-20', 'Credit Card',   'Two-night business trip stay'),
(4, 11, 'Planet Fitness Membership',   49.99, '2026-01-01', 'Credit Card',   'Monthly gym membership'),
-- David
(5, 12, 'Netflix Subscription',        15.99, '2026-01-01', 'Credit Card',   'Monthly streaming subscription'),
(5, 13, 'State Farm Auto Insurance',  120.00, '2026-01-02', 'Bank Transfer', 'Monthly auto insurance premium'),
(5, 14, 'Birthday Gift - Sarah',       75.00, '2026-01-18', 'Venmo',         'Friends birthday present'),
-- Emma
(6,  1, 'Nobu Restaurant Dinner',      62.00, '2026-01-15', 'Credit Card',   'Anniversary dinner outing'),
(6,  9, 'Monthly Rent Payment',      1500.00, '2026-01-01', 'Bank Transfer', 'January rent to landlord'),
-- Frank
(7, 10, 'Great Clips Haircut',         40.00, '2026-01-11', 'Cash',          'Monthly haircut appointment'),
-- Grace
(8,  5, 'Warriors Game Tickets',       85.00, '2026-01-07', 'Credit Card',   'NBA game seats section 112'),
-- Henry
(9,  6, 'Dentist Checkup',            200.00, '2026-01-09', 'Debit Card',    'Routine dental cleaning and exam'),
-- Iris
(10, 15, 'Staples Office Supplies',    67.25, '2026-01-16', 'Credit Card',   'Printer paper and desk items'),
-- Jack
(11,  2, 'Lyft Rides - Week 1',        45.00, '2026-01-05', 'Credit Card',   'Rideshare to work'),
(11,  1, 'Whole Foods Weekly',         72.30, '2026-01-08', 'Debit Card',    'Weekly groceries'),
-- Kelly
(12,  5, 'Apple Music',                10.99, '2026-01-01', 'Credit Card',   'Monthly subscription'),
(12,  4, 'Target Shopping',            88.45, '2026-01-14', 'Debit Card',    'Home essentials run'),
-- Liam
(13,  8, 'Delta Flight to NYC',       320.00, '2026-01-15', 'Credit Card',   'Round trip economy flight'),
(13,  3, 'Gas & Electric Bill',       110.00, '2026-01-03', 'Bank Transfer', 'PG&E monthly bill'),
-- Mia
(14,  6, 'Kaiser Urgent Care',        150.00, '2026-01-10', 'Credit Card',   'Walk-in medical visit'),
(14,  7, 'Udemy Course Bundle',        29.99, '2026-01-05', 'Credit Card',   'Python bootcamp'),
-- Noah
(15,  9, 'Monthly Rent',             1200.00, '2026-01-01', 'Bank Transfer', 'January rent payment'),
(15, 10, 'Supercuts Haircut',          35.00, '2026-01-17', 'Cash',          'Monthly trim');

-- -------------------------------------------------------------
-- Budgets  (20 rows)
-- Spending limits per user-category pair for January 2026.
-- The app computes spent/remaining at query time by joining
-- against the Expenses table.
-- -------------------------------------------------------------
INSERT INTO Budgets (user_id, category_id, limit_amount, start_date, end_date) VALUES
(2,   1,  400.00, '2026-01-01', '2026-01-31'),  -- Alice: Food
(2,   2,  200.00, '2026-01-01', '2026-01-31'),  -- Alice: Transportation
(2,   4,  300.00, '2026-01-01', '2026-01-31'),  -- Alice: Shopping
(2,   5,   50.00, '2026-01-01', '2026-01-31'),  -- Alice: Entertainment
(3,   3,  150.00, '2026-01-01', '2026-01-31'),  -- Bob: Utilities
(3,   5,  100.00, '2026-01-01', '2026-01-31'),  -- Bob: Entertainment
(3,   6,  100.00, '2026-01-01', '2026-01-31'),  -- Bob: Healthcare
(4,   7,  250.00, '2026-01-01', '2026-01-31'),  -- Carol: Education
(4,   8,  500.00, '2026-01-01', '2026-01-31'),  -- Carol: Travel
(4,  11,   60.00, '2026-01-01', '2026-01-31'),  -- Carol: Fitness
(5,  12,   50.00, '2026-01-01', '2026-01-31'),  -- David: Subscriptions
(5,  13,  150.00, '2026-01-01', '2026-01-31'),  -- David: Insurance
(5,  14,  100.00, '2026-01-01', '2026-01-31'),  -- David: Gifts
(6,   1,  500.00, '2026-01-01', '2026-01-31'),  -- Emma: Food
(6,   9, 1600.00, '2026-01-01', '2026-01-31'),  -- Emma: Housing
(7,  10,   60.00, '2026-01-01', '2026-01-31'),  -- Frank: Personal Care
(8,   5,  150.00, '2026-01-01', '2026-01-31'),  -- Grace: Entertainment
(9,   6,  250.00, '2026-01-01', '2026-01-31'),  -- Henry: Healthcare
(11,  1,  200.00, '2026-01-01', '2026-01-31'),  -- Jack: Food
(11,  2,  100.00, '2026-01-01', '2026-01-31');  -- Jack: Transportation
