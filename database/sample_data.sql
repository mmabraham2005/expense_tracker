USE expense_tracker;

INSERT INTO Users (name, email)
VALUES ('Demo User', 'demo@example.com');

INSERT INTO Categories (category_name)
VALUES ('Food'), ('Transportation'), ('Bills'), ('Shopping');

INSERT INTO Expenses (user_id, category_id, expense_name, amount, date, payment_method, notes)
VALUES
(1, 1, 'Lunch', 12.50, '2026-04-28', 'Credit Card', ''),
(1, 2, 'Gas', 40.00, '2026-04-28', 'Debit Card', '');

INSERT INTO Budgets (user_id, category_id, limit_amount, start_date, end_date)
VALUES
(1, 1, 200.00, '2026-04-01', '2026-04-30');