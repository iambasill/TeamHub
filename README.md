# TeamHub

TeamHub is an MVP backend for running the operational side of a company — not just
employee records, but the day-to-day workflows that sit around them: attendance, leave, payroll,
scheduling, assets, vendors, expenses, incidents and on-call, announcements, policy documents,
resource booking, and a private team board with chat and document sharing.

## Features

- **People & org structure** — employees, departments, roles/permissions, and profile pictures.
- **Auth** — JWT access + refresh tokens, registration, login, forgot/change password (OTP-based),
  cookie-based refresh flow.
- **Attendance & leave** — clock in/out tracking, leave requests with balances and history.
- **Payroll** — payroll run generation and history per employee.
- **Scheduling** — shift creation and assignment, plus peer-to-peer shift swap requests.
- **Assets** — asset inventory, assignment to employees, and returns.
- **Vendors** — vendor directory with status tracking.
- **Expense claims** — submission and manager/admin review.
- **Incidents & on-call** — company-wide incident tracking with severity, SLA breach detection,
  and an on-call roster.
- **Announcements** — company-wide or targeted broadcasts (by role or by board membership),
  delivered through the notification inbox.
- **Policy documents** — a searchable library of company policies.
- **Resource booking** — bookable shared resources (rooms, equipment, etc.) with a booking
  calendar.
- **Board Room** — a members-only space with real-time chat (WebSocket), document sharing, and an
  activity log, gated by a separate membership roster from ordinary roles.
- **Notifications** — a per-user inbox with Redis-backed unread counts.
- **Ops Pulse** — a single at-a-glance dashboard of live operational health (open incidents, SLA
  breaches, on-call coverage, pending leave, etc.).
- **File storage** — pluggable storage backend: local disk by default, or S3, Cloudinary, GCS, or
  Azure Blob Storage when configured.


