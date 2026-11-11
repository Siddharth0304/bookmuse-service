# REST API

Base path: `/api`. `Auth` means either active `USER` or `ADMIN`; `Owner` adds ownership, `Member` adds club membership, and `Creator` adds club ownership. Public profiles and catalogue endpoints still require authentication. Send `Authorization: Bearer <accessToken>`. All list responses are `Page<T>` with the pagination envelope documented in the README. List parameters: `page=0&size=20`, maximum size 100.

## Endpoints

### Administrator visibility

Only authenticated ADMIN callers can see administrator accounts. Other roles receive lists and pagination totals with administrators excluded from readers, followers/following, and club members. Direct requests for an administrator's profile, library, followers, or following return 404; following an administrator also returns 404. ADMIN callers retain access, including their own profile and `/admin/users`.

Administrator-authored posts, comments, clubs, discussions, replies, and administrator-actor notifications are also hidden from other roles because their DTOs contain account IDs. Descendants of hidden posts/clubs/discussions are hidden too. Engagement counts, follower counts, unread counts, and page totals follow the same visibility rule. Direct access or mutation of a hidden resource returns 404. Book catalogue entries remain visible regardless of who added them. Ownership and club membership requirements still apply to ADMIN callers.

| Method | Path | Required role / rule | Request | Response |
|---|---|---|---|---|
| POST | `/auth/register` | Public | Register | 201 Tokens |
| POST | `/auth/login` | Public | Login | 200 Tokens |
| POST | `/auth/refresh` | Public | Refresh | 200 Tokens |
| POST | `/auth/logout` | Public, token possession | Refresh | 204 |
| GET | `/books` | Auth | `search?`, `genre?` (repeatable or comma-separated), page | Page<BookView> |
| GET | `/books/{id}` | Auth | — | BookView |
| POST | `/books` | ADMIN | BookInput | 201 BookView |
| PUT | `/books/{id}` | ADMIN | BookInput | BookView |
| DELETE | `/books/{id}` | ADMIN | — | 204 |
| GET | `/me/library` | Auth | page | Page<LibraryView> |
| PUT | `/me/library/{bookId}` | Auth, own library | LibraryInput | LibraryView |
| DELETE | `/me/library/{bookId}` | Auth, own library | — | 204 |
| GET | `/users/{id}/library` | Auth | page | Page<LibraryView> |
| GET | `/me/recommendations` | Auth | page | Page<BookView> |
| GET | `/me` | Auth | — | Profile |
| GET | `/me/account` | Auth | — | Account (own) |
| PUT | `/me` | Auth | ProfileInput | Profile |
| GET | `/users` | Auth | page | Page<Profile> |
| GET | `/users/{id}` | Auth | — | Profile |
| PUT | `/users/{id}/follow` | Auth, cannot follow self | — | 204 |
| DELETE | `/users/{id}/follow` | Auth | — | 204 |
| GET | `/users/{id}/followers` | Auth | page | Page<Profile> |
| GET | `/users/{id}/following` | Auth | page | Page<Profile> |
| GET | `/admin/users` | ADMIN | page | Page<Account> |
| PUT | `/admin/users/{id}/status` | ADMIN, target USER | `{ "blocked": true/false }` | Account |
| GET | `/posts` | Auth | `bookId?`, page | Page<PostView> |
| GET | `/posts/{id}` | Auth | — | PostView |
| POST | `/posts` | Auth | PostInput | 201 PostView |
| PUT | `/posts/{id}` | Owner | Body | PostView |
| DELETE | `/posts/{id}` | Owner | — | 204 |
| PUT | `/posts/{id}/like` | Auth | — | 204 |
| DELETE | `/posts/{id}/like` | Auth | — | 204 |
| POST | `/posts/{id}/shares` | Auth | — | 201 `{id}` |
| GET | `/posts/{id}/comments` | Auth | page | Page<CommentView> |
| POST | `/posts/{id}/comments` | Auth | Body | 201 CommentView |
| PUT | `/comments/{id}` | Owner | Body | CommentView |
| DELETE | `/comments/{id}` | Owner | — | 204 |
| GET | `/clubs` | Auth | page | Page<ClubView> |
| GET | `/clubs/{id}` | Auth | — | ClubView |
| POST | `/clubs` | Auth | ClubInput | 201 ClubView |
| PUT | `/clubs/{id}` | Creator | ClubInput | ClubView |
| DELETE | `/clubs/{id}` | Creator | — | 204 |
| PUT | `/clubs/{id}/membership` | Auth | — | 204 |
| DELETE | `/clubs/{id}/membership` | Auth, not creator | — | 204 |
| GET | `/clubs/{id}/members` | Auth | page | Page<Member> |
| GET | `/clubs/{id}/discussions` | Member | page | Page<DiscussionView> |
| POST | `/clubs/{id}/discussions` | Member | DiscussionInput | 201 DiscussionView |
| GET | `/discussions/{id}/replies` | Member | page | Page<ReplyView> |
| POST | `/discussions/{id}/replies` | Member | Body | 201 ReplyView |
| GET | `/notifications` | Auth, own | page | Page<NotificationView> |
| GET | `/notifications/unread-count` | Auth, own | — | `{count}` |
| PUT | `/notifications/{id}/read` | Owner | — | 204 |
| PUT | `/notifications/read-all` | Auth, own | — | 204 |

## DTO fields

### Multiple genre filters

`GET /api/books` accepts any number of selected genre labels using repeated `genre` parameters or comma-separated values:

```text
/api/books?genre=fantasy&genre=mystery&page=0&size=20
/api/books?genre=fantasy,mystery&search=harry&page=0&size=20
```

Selected genres match with **OR** (any selected genre); text search combines with the genre group using **AND**. Matching uses complete labels, case-insensitively, with whitespace trimmed and blanks/duplicates ignored. Omit `genre` or send only blanks to leave genres unrestricted. A single `genre=romance` still works. Search, pagination, and title/ID ordering are unchanged. URL-encode labels containing spaces, for example `genre=science%20fiction`.

`id` fields are UUIDs; timestamps are UTC ISO-8601. `?` denotes optional/nullable. Request bodies are JSON; PUT replaces the indicated editable fields.

| Request | Fields |
|---|---|
| Register | `email`, `password` (8–20), `name` (1–100) |
| Login | `email`, `password` |
| Refresh | `refreshToken` |
| BookInput | `title`, `author` (max 255 each), `genres` (1–10 labels, max 50 each), `summary?` (max 10000), `coverUrl?` (URL, max 255) |
| LibraryInput | `status`: WANT_TO_READ / READING / READ, `progress?`: 0–100 |
| PostInput | `bookId`, `body` (1–10000) |
| Body | `body` (1–10000) |
| ProfileInput | `name`, `bio?` (max 2000), `avatarUrl?`, `favouriteGenres` (0–10 labels) |
| ClubInput | `name` (1–100), `description?` (max 10000) |
| DiscussionInput | `title` (1–255), `body` (1–10000) |

| Response | Fields |
|---|---|
| Tokens | `accessToken`, `refreshToken`, `tokenType` (Bearer), `expiresIn` (seconds) |
| Profile | `id`, `name`, `bio`, `avatarUrl`, `favouriteGenres`, `followers`, `following`, `booksRead` |
| Account | `id`, `email`, `name`, `role`, `blocked` |
| Member | `id`, `name`, `avatarUrl` |
| BookView | `id`, `title`, `author`, `genres`, `summary`, `coverUrl` |
| LibraryView | `id`, `bookId`, `status`, `progress` |
| PostView | `id`, `userId`, `bookId`, `body`, `createdAt`, `likes`, `shares`, `comments` |
| CommentView | `id`, `userId`, `postId`, `body`, `createdAt` |
| ClubView | `id`, `creatorId`, `name`, `description` |
| DiscussionView | `id`, `userId`, `clubId`, `title`, `body`, `createdAt` |
| ReplyView | `id`, `userId`, `discussionId`, `body`, `createdAt` |
| NotificationView | `id`, `actorId`, `type`, `resourceId`, `read`, `createdAt` |

## Examples

Register:

```json
{"email":"reader@example.com","password":"ChooseYourOwn123!","name":"Reader"}
```

Admin creates a book:

```json
{"title":"Pride and Prejudice","author":"Jane Austen","genres":["classics","romance"],"summary":"A novel about manners and marriage."}
```

Then `PUT /api/me/library/{bookId}` with `{"status":"READING","progress":35}`.

Clients should serialize refresh attempts: replace both stored tokens after a successful refresh, and clear authentication when refresh returns 401. Treat response tokens as secrets and send requests over HTTPS in production.
