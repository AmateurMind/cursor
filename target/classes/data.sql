-- Seed users
INSERT INTO users (email, name) VALUES
('alice@example.com', 'Alice'),
('bob@example.com', 'Bob');

-- Seed accounts (per user)
INSERT INTO accounts (user_id, name, type, currency_code) VALUES
(1, 'Cash', 'cash', 'USD'),
(1, 'Bank', 'bank', 'USD'),
(2, 'Cash', 'cash', 'USD');

-- Seed categories (global)
INSERT INTO categories (name, type, color) VALUES
('grocery', 'expense', '#2ecc71'),
('electronics', 'expense', '#3498db'),
('utilities', 'expense', '#9b59b6'),
('treat', 'expense', '#e67e22'),
('transit', 'expense', '#1abc9c'),
('health', 'expense', '#e74c3c'),
('entertainment', 'expense', '#f1c40f'),
('shopping', 'expense', '#34495e'),
('education', 'expense', '#8e44ad'),
('rent', 'expense', '#95a5a6'),
('travel', 'expense', '#16a085');

-- Seed expenses for Alice (user_id=1)
INSERT INTO expenses (title, amount, category, expense_date, notes, user_id, account_id) VALUES
('Weekly groceries', 150.50, 'grocery', '2025-01-10', 'Vegetables & fruits', 1, 1),
('Electricity bill', 2200.00, 'utilities', '2025-01-15', 'Jan bill', 1, 2),
('Laptop charger', 999.00, 'electronics', '2025-01-18', 'USBC 65W', 1, 2),
('Bus pass', 300.00, 'transit', '2025-01-02', 'Monthly pass', 1, 1),
('Chocolate', 120.00, 'treat', '2025-01-05', 'Dark 70%', 1, 1);

-- Seed expenses for Bob (user_id=2)
INSERT INTO expenses (title, amount, category, expense_date, notes, user_id, account_id) VALUES
('Internet', 799.00, 'utilities', '2025-01-05', 'Monthly plan', 2, 3),
('Pumpkin', 80.00, 'grocery', '2025-01-06', '1 kg', 2, 3),
('Movie night', 450.00, 'entertainment', '2025-01-12', 'Cinema', 2, 3);
