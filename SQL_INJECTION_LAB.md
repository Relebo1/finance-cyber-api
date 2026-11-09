# SQL Injection Practice Endpoints

All exercises are for this application running on `127.0.0.1:8080` against the disposable `finance_sqli_lab` MySQL database. The examples below document query shapes only; they do not include or execute attack payloads. Observe responses in Postman and SQL/query errors in the local app logs.

The unsafe repository classes use JDBC `Connection.createStatement()` and `Statement.executeQuery(...)`. Request text is concatenated directly into SQL without escaping or parameter binding.

## 1. Transaction description search

- **Request:** `GET /api/transactions/search?q=market`
- **Input:** query parameter `q`
- **Vulnerable construction:**

  ```sql
  SELECT ... FROM financial_transactions t ... WHERE t.description LIKE '%<q>%'
  ```

- **Why vulnerable:** `q` is inserted into the quoted `LIKE` expression as SQL source text.
- **Observe:** ordinary text returns matching descriptions. SQL syntax in this value can alter the predicate or cause a database syntax error.

The same vulnerable description predicate is available as the optional `q` parameter on `GET /api/transactions` and can be combined with dates and pagination.

## 2. Transaction type filter

- **Request:** `GET /api/transactions?type=EXPENSE`
- **Input:** query parameter `type`
- **Vulnerable construction:**

  ```sql
  ... WHERE t.type = '<type>'
  ```

- **Why vulnerable:** the controller accepts a raw string and the repository concatenates it into the SQL condition rather than mapping it to the enum or binding it as a parameter.
- **Observe:** `INCOME` and `EXPENSE` return their corresponding records; modified SQL input can change the filter result or trigger a SQL error.

The type filter can be combined with `startDate`, `endDate`, `q`, `page`, and `size`.

## 3. Category name search

- **Request:** `GET /api/categories/search?name=Gro`
- **Input:** query parameter `name`
- **Vulnerable construction:**

  ```sql
  SELECT ... FROM category WHERE name LIKE '%<name>%'
  ```

- **Why vulnerable:** the search value is concatenated into SQL and executed with `Statement`.
- **Observe:** ordinary partial names return matching categories; SQL syntax can change the predicate or cause a database error.

## 4. User lookup by username

- **Request:** `GET /api/users/lookup?username=labuser`
- **Input:** query parameter `username`
- **Vulnerable construction:**

  ```sql
  SELECT ... FROM app_users WHERE username='<username>'
  ```

- **Why vulnerable:** raw username text becomes part of the SQL predicate.
- **Observe:** a matching username returns its public profile fields. Altered SQL input can change which row(s) match or trigger an error.

## 5. Login credential query

- **Request:** `POST /api/auth/login`
- **Body:**

  ```json
  {"username":"labuser","password":"finance123"}
  ```

- **Inputs:** `username` and `password` JSON fields
- **Vulnerable construction:**

  ```sql
  SELECT ... FROM app_users WHERE username='<username>' AND password='<password>'
  ```

- **Why vulnerable:** both credentials are concatenated directly into the query. Passwords are also stored as plain text in this practice version.
- **Observe:** the seeded credentials return a user profile; invalid values return `401`. Altered SQL input can change the query result or produce a SQL error.

This endpoint does not issue a session or protect other API endpoints. It exists to demonstrate a vulnerable login query, not to provide application authentication.

## 6. Transaction retrieval by ID

- **Request:** `GET /api/transactions/{id}` (for example, `/api/transactions/1`)
- **Input:** path variable `id`
- **Vulnerable construction:**

  ```sql
  SELECT ... FROM financial_transactions t ... WHERE t.id = <id>
  ```

- **Why vulnerable:** the path variable is treated as a raw SQL fragment instead of being parsed as a number and bound.
- **Observe:** a numeric ID returns a transaction; malformed or SQL-modified input can produce a database error or change the result set.

## Ordinary database paths for comparison

Category create/list/detail/update/delete, transaction create/list-without-`type`-or-`q`/update/delete, account CRUD, user registration, and financial summary use Spring Data JPA repositories. These paths do not concatenate request text into SQL. Category and account deletion return `409 Conflict` when existing transactions reference them.

## Safety boundary

Keep the server bound to localhost and use the dedicated fictional database only. Do not use real credentials or personal financial records, and do not expose the service publicly.
