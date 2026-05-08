# ExpenseTrack

A full-stack personal finance web application built with Spring Boot 3, MySQL, and Thymeleaf.
Users log in and track personal expenses with full CRUD operations, budget management, spending reports, and an admin dashboard.

---

## Project Overview

ExpenseTrack is a multi-user expense tracking system with role-based access control:

- **Regular users** can add, edit, delete, and search their own expenses, set per-category budgets, and view personal spending analytics.
- **Admins** can view and manage all users' data across the entire system, plus access the User Management dashboard.

Data is stored in a MySQL relational database with four normalized tables (Users, Categories, Expenses, Budgets) joined by foreign key constraints with ON DELETE CASCADE.

---

## Project Directory Structure

```
expense_tracker/
├── database/
│   ├── create_schema.sql      # Step 1: creates the database, all tables, and constraints
│   ├── initialize_data.sql    # Step 2: populates each table with 15+ sample rows
│   └── sample_data.sql        # Minimal seed data (2 users, 4 categories, 2 expenses)
│
├── src/
│   └── main/
│       ├── java/com/example/expensetracker/
│       │   ├── config/
│       │   │   └── PasswordMigrationRunner.java  # Auto-hashes plain-text passwords on startup
│       │   ├── controller/
│       │   │   └── AppController.java            # HTTP request handlers (login, CRUD, reports)
│       │   ├── model/
│       │   │   ├── User.java               # User entity (userId, name, email, password, role)
│       │   │   ├── Expense.java            # Expense entity (raw DB row)
│       │   │   ├── Budget.java             # Budget entity (raw DB row)
│       │   │   ├── Category.java           # Category entity
│       │   │   ├── ExpenseView.java        # Expense joined with user name and category name
│       │   │   ├── BudgetView.java         # Budget with computed spent/remaining amounts
│       │   │   ├── CategoryTotal.java      # Aggregated spending per category
│       │   │   └── MonthlyTotal.java       # Aggregated spending per month
│       │   └── repository/
│       │       ├── LookupRepository.java   # User auth (BCrypt) and category lookup queries
│       │       ├── ExpenseRepository.java  # Expense CRUD, search, and report queries
│       │       └── BudgetRepository.java   # Budget CRUD and spending calculation
│       └── resources/
│           ├── application.properties      # Database URL, credentials, server port
│           ├── static/
│           │   └── style.css               # Application stylesheet (dark theme)
│           └── templates/                  # Thymeleaf HTML pages
│               ├── login.html
│               ├── signup.html             # Self-registration page
│               ├── index.html              # Add expense
│               ├── view-expenses.html
│               ├── edit-expense.html
│               ├── delete-expense.html
│               ├── search-expense.html
│               ├── reports.html
│               ├── budget.html
│               └── admin-users.html        # Admin-only user management dashboard
│
├── pom.xml                                 # Maven project and dependency configuration
└── README.md
```

---

## Required Software and Dependencies

| Software       | Minimum Version | Purpose                        |
|----------------|-----------------|--------------------------------|
| Java JDK       | 17              | Runtime and compilation        |
| Apache Maven   | 3.8             | Build tool and dependency mgmt |
| MySQL Server   | 8.0             | Relational database            |

**Maven dependencies** (declared in `pom.xml`):

| Dependency                        | Purpose                         |
|-----------------------------------|---------------------------------|
| spring-boot-starter-web           | HTTP server (embedded Tomcat)   |
| spring-boot-starter-thymeleaf     | Server-side HTML templating     |
| spring-boot-starter-jdbc          | JdbcTemplate + HikariCP pool    |
| mysql-connector-j                 | MySQL JDBC driver               |
| spring-security-crypto            | BCrypt password hashing         |

---

## Setup Instructions

### Step 1 — Verify prerequisites

```bash
java -version     # must show 17 or higher
mvn -version      # must show 3.8 or higher
mysql --version   # must show 8.0 or higher
```

### Step 2 — Clone or download the project

```bash
git clone https://github.com/mmabraham2005/expense_tracker/tree/main
cd expense_tracker
```

### Step 3 — Create the database schema

Run the schema script to create the `expense_tracker` database and all four tables with constraints:

```bash
mysql -u root -pYOUR_PASSWORD < database/create_schema.sql
```

### Step 4 — Load sample data

Run the initialization script to populate each table with 15+ sample rows:

```bash
mysql -u root -pYOUR_PASSWORD < database/initialize_data.sql
```

### Step 5 — Configure the database connection

Open `src/main/resources/application.properties` and update the password to match your MySQL installation:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/expense_tracker
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.thymeleaf.cache=false
server.port=8080
```

### Step 6 — Build and run the application

```bash
mvn spring-boot:run
```

Maven will download all dependencies on the first run. On startup, `PasswordMigrationRunner` will automatically BCrypt-hash any plain-text passwords in the database — this is a one-time operation.

### Step 7 — Open in a web browser

```
http://localhost:8080
```

You will be redirected to the login page automatically.

---

## Default Login Credentials

| Role  | Email                       | Password  |
|-------|-----------------------------|-----------|
| Admin | admin@expensetrack.com      | admin123  |
| User  | alice@example.com           | alice123  |
| User  | bob@example.com             | bob123    |

> **Note:** Passwords are stored as BCrypt hashes in the database. The plain-text passwords above are for first-time login only — they are hashed automatically on the first startup.

---

## Features

| Feature              | Description                                                                         |
|----------------------|-------------------------------------------------------------------------------------|
| Sign Up              | Self-registration form; new accounts are stored with BCrypt-hashed passwords        |
| Add Expense          | Insert a new expense with category, amount, date, payment method, and notes         |
| View Expenses        | Display all expenses via a 3-table JOIN (Expenses + Users + Categories)             |
| Edit Expense         | Pre-fill a form with existing values and save changes with UPDATE                   |
| Delete Expense       | Remove a record with confirmation; verifies ownership before deleting               |
| Search & Filter      | Dynamic WHERE clause built from keyword, category, date range, and amount range     |
| Reports              | SUM/COUNT/AVG aggregations with an interactive Chart.js bar chart                   |
| Budget Tracking      | Set spending limits per category; live spent vs. limit via LEFT JOIN + SUM          |
| Budget Drill-down    | Click any budget row to expand and see the individual expenses counted toward it    |
| Delete Budget        | Remove any budget directly from the expanded detail panel                           |
| User Management      | Admin-only dashboard showing all user IDs, names, emails, and roles                 |
| Role-Based Access    | Admin sees all data; regular users are scoped to their own records only             |
| Password Hashing     | BCrypt hashing via spring-security-crypto; auto-migrated on startup                 |

---

## Database Configuration

All connection settings live in `src/main/resources/application.properties`.

| Property                              | Value                                         |
|---------------------------------------|-----------------------------------------------|
| `spring.datasource.url`              | `jdbc:mysql://localhost:3306/expense_tracker` |
| `spring.datasource.username`         | `root` (or your MySQL username)               |
| `spring.datasource.password`         | *(your MySQL password)*                       |
| `spring.datasource.driver-class-name`| `com.mysql.cj.jdbc.Driver`                    |
| `server.port`                        | `8080`                                        |

Connection pooling is handled automatically by HikariCP, bundled with `spring-boot-starter-jdbc`.

---

## Database Schema

### Entity–Relationship Summary

```
Users ──────< Expenses >────── Categories
  │                                  │
  └──────────< Budgets >─────────────┘
```

All tables are in Boyce-Codd Normal Form (BCNF). Categories are a shared global lookup table — no category name is repeated across expense rows.

### Tables

| Table        | Primary Key   | Description                                          |
|--------------|---------------|------------------------------------------------------|
| Users        | user_id       | Application accounts with role (USER or ADMIN)       |
| Categories   | category_id   | Global lookup table for expense/budget categorization|
| Expenses     | expense_id    | Individual expense transactions recorded by users    |
| Budgets      | budget_id     | Per-user, per-category spending limits with date range|

### Foreign Key Constraints

| Constraint                           | Behavior          |
|--------------------------------------|-------------------|
| Expenses.user_id → Users.user_id     | ON DELETE CASCADE |
| Expenses.category_id → Categories    | ON DELETE CASCADE |
| Budgets.user_id → Users.user_id      | ON DELETE CASCADE |
| Budgets.category_id → Categories     | ON DELETE CASCADE |

### SQL Scripts

| File                           | Purpose                                       |
|--------------------------------|-----------------------------------------------|
| `database/create_schema.sql`  | Creates the database, tables, and constraints  |
| `database/initialize_data.sql`| Inserts 15+ rows into every table             |
| `database/sample_data.sql`    | Minimal seed (2 users, 4 categories, 2 rows)  |

---

## Security

- **Password hashing** — passwords are stored as BCrypt hashes (via `spring-security-crypto`). Plain-text passwords are automatically detected and hashed on first startup by `PasswordMigrationRunner`.
- **SQL injection prevention** — all queries use JdbcTemplate parameterized placeholders (`?`). User input is never concatenated into SQL strings.
- **Session fixation prevention** — the old HTTP session is invalidated and a new session ID is issued on every successful login.
- **Ownership enforcement** — edit and delete operations verify that the expense belongs to the logged-in user before executing. A regular user cannot modify another user's records even via a crafted HTTP request.
- **Role-based query scoping** — data isolation is enforced at the SQL layer. Regular users have `WHERE user_id = ?` appended to every query; admins receive unfiltered results.
- **Admin-only routes** — `/admin/users` returns a redirect to `/` if the session role is not `ADMIN`.

---

## Error Handling

- **Database connection errors** — if MySQL is unreachable at startup, Spring Boot fails with a clear error before the server starts. During runtime, all repository methods wrap JdbcTemplate calls in `try/catch (DataAccessException e)` and rethrow with descriptive messages.
- **Missing records** — `findExpenseById` catches `EmptyResultDataAccessException` and returns `null` instead of crashing; the controller redirects gracefully.
- **Form input validation** — enforced by HTML5 attributes (`type="number"`, `required`, `min="0"`, `step="0.01"`). Browsers block invalid input before submission.
- **Authentication** — every controller endpoint calls `requireLogin()`, which checks the HTTP session and redirects to `/login` if no session exists.

---

## Permissions & Paths

### Working directory

All Maven and MySQL commands must be run from inside the `expense_tracker/` directory.
If you are one level up (e.g. inside `cs157a/`), `cd` in first:

```bash
cd /path/to/expense_tracker
# Example on macOS:
cd /Users/YOUR_USERNAME/Desktop/cs157a/expense_tracker
```

> Running `mvn spring-boot:run` from the wrong directory produces:
> `[ERROR] No plugin found for prefix 'spring-boot'`

---

### File system permissions

The project uses standard Unix permissions. Verify them with:

```bash
# From inside expense_tracker/
ls -l database/          # SQL scripts should be -rw-r--r-- (644)
ls -l src/main/resources/ # application.properties should be -rw-r--r-- (644)
```

If any file or directory has incorrect permissions, restore them:

```bash
# Directories need execute bit so they can be entered
find . -type d -exec chmod 755 {} +

# Source files need read/write for owner, read for group/others
find . -type f -exec chmod 644 {} +
```

The compiled output goes into `target/` which Maven creates and manages automatically — no manual permission changes are needed there.

---

### PATH — Java, Maven, and MySQL

All three tools must be discoverable on your `PATH`. Verify before running the app:

```bash
which java   && java -version    # must show 17 or higher
which mvn    && mvn -version     # must show 3.8 or higher
which mysql  && mysql --version  # must show 8.0 or higher
```

**macOS (Homebrew) common paths:**

| Tool  | Typical absolute path                          |
|-------|------------------------------------------------|
| Java  | `/opt/homebrew/opt/openjdk@17/bin/java`        |
| Maven | `/opt/homebrew/bin/mvn`                        |
| MySQL | `/opt/homebrew/bin/mysql`                      |

If a tool is not found, add its `bin/` directory to `~/.zshrc` (or `~/.bash_profile`):

```bash
export PATH="/opt/homebrew/bin:$PATH"
```

Then reload: `source ~/.zshrc`

---

### MySQL user privileges

The MySQL account configured in `application.properties` must hold the following
privileges on the `expense_tracker` database:

```sql
-- Run these commands as the MySQL root user:
GRANT CREATE, SELECT, INSERT, UPDATE, DELETE
    ON expense_tracker.*
    TO 'root'@'localhost';

FLUSH PRIVILEGES;
```

To verify the currently granted privileges:

```sql
SHOW GRANTS FOR 'root'@'localhost';
```

If you use a non-root MySQL account, replace `root` with that username in both
the `GRANT` statement and in `application.properties`.

---

### Database connection — absolute path reference

The JDBC connection string in `src/main/resources/application.properties` targets
the local MySQL server. The full absolute path to this file is:

```
/path/to/expense_tracker/src/main/resources/application.properties
# Example on macOS:
/Users/YOUR_USERNAME/Desktop/cs157a/expense_tracker/src/main/resources/application.properties
```

Edit it to match your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/expense_tracker
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.thymeleaf.cache=false
server.port=8080
```

> **macOS (Homebrew MySQL):** if the connection is refused, MySQL may be using a Unix
> socket instead of TCP. Append `?useSSL=false&allowPublicKeyRetrieval=true` to the URL:
> ```properties
> spring.datasource.url=jdbc:mysql://localhost:3306/expense_tracker?useSSL=false&allowPublicKeyRetrieval=true
> ```

---

### SQL script absolute paths

When running the setup scripts from a terminal opened at a different location,
use the absolute path to the script file:

```bash
# Absolute path form (replace YOUR_USERNAME and adjust if the project is elsewhere)
mysql -u root -p < /Users/YOUR_USERNAME/Desktop/cs157a/expense_tracker/database/create_schema.sql
mysql -u root -p < /Users/YOUR_USERNAME/Desktop/cs157a/expense_tracker/database/initialize_data.sql
```

---

### Port 8080

Port 8080 must be free before starting the application.

**Check if 8080 is already in use:**

```bash
# macOS / Linux
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

**If port 8080 is occupied**, change the port in `application.properties`:

```properties
server.port=8081   # or any available port
```

Then access the app at `http://localhost:8081` instead.
