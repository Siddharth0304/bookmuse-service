# BookMuse frontend integration guide

Base path: `/api`. `Auth` means either active `USER` or `ADMIN`; `Owner` adds ownership, `Member` adds club membership, and `Creator` adds club ownership. Public profiles and catalogue endpoints still require authentication. Send `Authorization: Bearer <accessToken>`. All list responses are `Page<T>` with the pagination envelope documented in the README. List parameters: `page=0&size=20`, maximum size 100.

## Local connection

This guide describes the implemented contract, checked against controllers, DTOs, services, security configuration, and migrations. All successful responses without an explicit status in the table use **200**. A `204` response has no body. `page` in a request column means optional query parameters, not a JSON body. Path IDs and `bookId` are UUID strings.

| Setting | Value |
|---|---|
| Backend origin | `http://localhost:8080`; backend `PORT` can override it |
| Frontend API base URL | `http://localhost:8080/api` |
| Allowed browser origin | `http://localhost:5173`; backend `FRONTEND_ORIGIN` must exactly match the frontend scheme, host, and port |
| JSON request header | `Content-Type: application/json` for requests with bodies |
| Interactive API explorer | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

The API is stateless and returns tokens in JSON; it does not set authentication cookies. Cookie credentials and CSRF tokens are unnecessary for this contract. CORS allows `Authorization` and `Content-Type` headers. Configure the backend origin setting if the frontend uses another port. CORS permission for PATCH does not mean PATCH endpoints exist.

For startup, follow [README.md](../README.md#run-locally). Spring does not load `.env` automatically. Database credentials, signing secrets, and bootstrap admin credentials belong only in the backend environment. A frontend variable such as `VITE_API_BASE_URL` is a suggested client convention. Optional sample books can be created through [scripts/seed.ps1](../scripts/seed.ps1).

## Endpoints

### Administrator visibility

ADMIN callers can see administrator accounts. For all other roles, administrators are excluded from reader lists, follower/following lists, and club members before pagination and counting. Administrator profile/library/follower/following URLs return 404 to other roles, as does following an administrator.

Responses containing administrator author/creator/actor IDs are filtered too: administrator-authored posts, comments, clubs, discussions, replies, and notifications from administrators are hidden. Content inside a hidden post, club, or discussion is also hidden. Direct access/mutation returns 404 for hidden resources. Engagement counts, follower counts, unread counts, and page metadata reflect only visible entries. Treat these totals as caller-specific and clear caches when switching accounts. A 404 may indicate either a missing resource or one hidden by this visibility rule.

ADMIN callers retain full administrator visibility, including `/me`, `/me/account`, and `/admin/users`, while ownership and membership restrictions still apply. Books added by administrators remain in the shared catalogue; BookView does not expose a creator account ID.

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

## Complete input validation

All required text fields must contain a non-whitespace character. Length limits count characters. Optional fields may be omitted or sent as `null`; editable optional fields are replaced, so omission does not preserve their previous value. Send all required fields on PUT; these endpoints are not partial updates.

| Input | Rules beyond the field summary above |
|---|---|
| Register | Email must be valid and at most 254 characters. Name is required, max 100. Password is 8-20 characters; there is no additional complexity rule. Email is trimmed and lowercased; name is trimmed. Duplicate email returns 409. Registration always creates USER. |
| Login | Valid nonblank email; password 8-20 characters. Email lookup is trimmed and case-insensitive. Invalid credentials or blocked account returns 401. |
| Refresh | Required nonblank `refreshToken`, max 256 characters. Same input for logout. |
| BookInput | Required title and author, max 255 each. Required genres array, 1-10 unique labels. Optional summary max 10000; optional coverUrl must be a valid URL, max 255. |
| ProfileInput | Required name max 100. Optional bio max 2000; optional avatarUrl must be a valid URL, max 255. Required favouriteGenres array, 0-10 unique labels; use `[]` to clear it. |
| Genre labels | Nonblank, max 50 each; only Unicode letters, numbers, spaces, and hyphens. Backend trims, lowercases, deduplicates, and sorts labels. No predefined genre catalogue endpoint exists. |
| LibraryInput | Required case-sensitive status: `WANT_TO_READ`, `READING`, or `READ`. Optional integer progress 0-100. READ always stores 100; other statuses store the supplied progress or null. |
| PostInput / Body | Required nonblank body, max 10000. Creating a post also requires an existing bookId. Updating a post changes only body. |
| ClubInput | Required name max 100, trimmed. Optional description max 10000. |
| DiscussionInput | Required nonblank title max 255 and body max 10000. |
| UserStatus | Required boolean `blocked`: `true` or `false`, not a string. |

## Response types for TypeScript

These describe JSON, so dates remain strings and sets become arrays. Nullable values should have UI fallbacks. `Page<T>` in the endpoint table refers to the custom `PageView<T>` envelope below, not Spring's full Page serialization.

```ts
type UUID = string;
type Instant = string; // UTC ISO-8601
type ReadingStatus = "WANT_TO_READ" | "READING" | "READ";
type Role = "USER" | "ADMIN";

interface PageView<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
interface Tokens {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number; // access token lifetime in seconds
}
interface Profile {
  id: UUID; name: string; bio: string | null; avatarUrl: string | null;
  favouriteGenres: string[]; followers: number; following: number; booksRead: number;
}
interface Account {
  id: UUID; email: string; name: string; role: Role; blocked: boolean;
}
interface Member { id: UUID; name: string; avatarUrl: string | null; }
interface BookView {
  id: UUID; title: string; author: string; genres: string[];
  summary: string | null; coverUrl: string | null;
}
interface LibraryView {
  id: UUID; bookId: UUID; status: ReadingStatus; progress: number | null;
}
interface PostView {
  id: UUID; userId: UUID; bookId: UUID; body: string; createdAt: Instant;
  likes: number; shares: number; comments: number;
}
interface CommentView {
  id: UUID; userId: UUID; postId: UUID; body: string; createdAt: Instant;
}
interface ClubView { id: UUID; creatorId: UUID; name: string; description: string | null; }
interface DiscussionView {
  id: UUID; userId: UUID; clubId: UUID; title: string; body: string; createdAt: Instant;
}
interface ReplyView {
  id: UUID; userId: UUID; discussionId: UUID; body: string; createdAt: Instant;
}
type NotificationType = "NEW_FOLLOWER" | "LIKE" | "SHARE" | "COMMENT"
  | "CLUB_JOIN" | "DISCUSSION_REPLY";
interface NotificationView {
  id: UUID; actorId: UUID; type: NotificationType; resourceId: UUID;
  read: boolean; createdAt: Instant;
}
interface Count { count: number; }
interface Id { id: UUID; }
interface ApiProblem {
  status: number;
  title?: string;
  detail?: string;
  type?: string;
  instance?: string;
  errors?: string[];
}
```

## Pagination, filters, and ordering

Use `?page=0&size=20` on any list endpoint; page numbering starts at zero, default size is 20, maximum is 100. Client `sort` is not applied. Reset page to zero when filters change. An empty list is a successful response with an empty content array. Use `number + 1 < totalPages` to decide whether another page exists.

```json
{"content":[],"number":0,"size":20,"totalElements":0,"totalPages":0}
```

| Collection | Server ordering / filtering |
|---|---|
| Books | Title ascending, then ID. Optional `search` matches a case-insensitive substring of title or author. Repeat `genre` or provide comma-separated labels to match any selected complete genre label (OR), trimmed and case-insensitive. Text search combines with the genre group using AND. Blank genre values and duplicates are ignored. |
| Library | Newest additions first, then ID; changing status does not move an entry to the top. No status filter. |
| Readers, followers, following, members | Name ascending, then ID. `/users` excludes blocked accounts; no reader search parameter. |
| Admin users | Newest accounts first, then ID. |
| Posts | Newest first, then ID; optional `bookId` filter. No author filter or followed-only feed. |
| Clubs and discussions | Newest first, then ID. No club search filter. |
| Comments and replies | Oldest first, then ID. |
| Notifications | Newest first, then ID; no unread-only filter. |
| Recommendations | Genre affinity descending, then library popularity descending, then ID. Excludes every book already in your library. Favourite genres contribute weight 2, each library book contributes weight 1 per genre. Falls back to popularity without affinities. |

Encode filters with `URLSearchParams`, for example `/api/books?search=Jane+Austen&genre=romance&page=0&size=20`.

For a multiple-selection genre control, append each selected genre separately:

```ts
const query = new URLSearchParams({ search: "harry", page: "0", size: "20" });
for (const genre of ["fantasy", "mystery"]) query.append("genre", genre);
// GET /api/books?search=harry&page=0&size=20&genre=fantasy&genre=mystery
```

This matches books satisfying the text search and either genre. `genre=fantasy,mystery` is also supported, as is a single `genre=fantasy`. With no selected genres, omit the parameter. Reset page to zero whenever the selection or search changes.

## Authentication lifecycle

1. Register or log in without a bearer header. Store both returned tokens using your application's chosen secure session design. Tokens are secrets; do not log them or embed them in URLs.
2. Load `/api/me` for public profile details and `/api/me/account` for the current ID, email, and role. Token responses do not include a user object.
3. Send the access token on protected requests. Default lifetime is 900 seconds; use the returned expiresIn rather than hardcoding it. Refresh lifetime defaults to 30 days; both lifetimes are backend-configurable.
4. Serialize refresh calls, including across tabs if they share a session. POST `/api/auth/refresh` with the current refresh token and **without an expired bearer header**. Every refresh consumes the old token and returns a replacement pair. Replace both tokens together, then retry waiting requests once. Invalid bearer headers can reject even public auth endpoints because the JWT filter runs first.
5. If refresh returns 401, clear the session and show login. Handle network/server failures separately from invalid credentials, and avoid refresh loops.
6. Logout posts the current refresh token without a bearer header, then clears client session state. Unknown or already revoked refresh tokens still produce 204. Logout revokes only that refresh token; an issued access token remains valid until its expiry.

Blocking a USER revokes their refresh tokens and invalidates existing access tokens immediately. Reactivation does not restore old tokens; the user must log in again. There is no role-assignment API. ADMIN does not bypass owner or club membership rules.

## Errors and a minimal request helper

Errors use `application/problem+json`. Validation returns an `errors` array of strings such as `name: must not be blank`; it is not a field-to-message map. Security-filter errors may contain only status and title, so detail is optional.

| Status | Frontend handling |
|---|---|
| 400 | Correct body, validation, malformed JSON, or UUID/query values. Display field errors where available. |
| 401 | Missing/invalid/expired token, blocked user, invalid login or refresh. Refresh protected calls once where appropriate. |
| 403 | Active user lacks admin, ownership, or membership permission; following a blocked reader also fails. |
| 404 | Resource does not exist; historical notification targets may have been deleted. |
| 409 | Duplicate email/data conflict, self-follow, or creator attempting to leave their club. |
| 500 | Unexpected server failure; show a retryable generic message. |

Example validation response (instance may vary):

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Request validation failed","errors":["name: must not be blank"]}
```

This helper handles JSON and empty success responses. Coordinate refresh separately using the lifecycle above; do not blindly retry non-idempotent POST requests after network failures.

```ts
const API_BASE = "http://localhost:8080/api"; // replace from frontend config

async function api<T>(path: string, init: RequestInit = {}, accessToken?: string): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body !== undefined && init.body !== null) headers.set("Content-Type", "application/json");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({})) as Partial<ApiProblem>;
    throw Object.assign(
      new Error(problem.detail ?? problem.title ?? `HTTP ${response.status}`),
      { status: response.status, problem }
    );
  }
  if (response.status === 204) return undefined as T; // use api<void> for 204 endpoints
  return response.json() as Promise<T>;
}

const tokens = await api<Tokens>("/auth/login", {
  method: "POST",
  body: JSON.stringify({ email: "reader@example.com", password: "ChooseYourOwn123!" })
});
const me = await api<Account>("/me/account", {}, tokens.accessToken);
// With a real bookId from GET /books:
// await api<LibraryView>(`/me/library/${bookId}`, {
//   method: "PUT", body: JSON.stringify({ status: "READING", progress: 35 })
// }, tokens.accessToken);
```

## Frontend screen mapping and state considerations

| Screen / interaction | Calls and state updates |
|---|---|
| Catalogue and book detail | GET books / book; join with your library by bookId for reading status. Fetch posts filtered by bookId for community content. |
| My library | GET me/library, then resolve BookView per bookId; PUT changes/adds status, DELETE removes it. LibraryView.id identifies the entry; the mutation URL uses bookId. Refresh library, booksRead, and recommendations after changes. |
| Reader profile | GET users/{id}, library, followers, following. Follow/unfollow updates counts; self-follow returns 409. |
| Community feed | GET posts. Resolve userId with user profiles and bookId with book details; cache these lookups by ID. Refresh engagement after like/comment/share mutations. |
| Post editor | Create requires bookId and body; edit takes only body. Show edit/delete when post.userId equals current account ID. Same owner rule for comments. |
| Club detail | GET club and members; creatorId controls editing/deletion. Club creation automatically joins its creator. Join before loading or creating discussions/replies. |
| Notifications | GET notifications and unread-count; PUT read/read-all updates the badge and list. Poll on a suitable schedule or refetch on focus; no push channel is implemented. |
| Admin | GET admin/users and PUT user status; book create/update/delete. Use account.role for UI visibility; server enforces permissions. |

Important contract limits when designing controls:

- Post responses contain counts but no `likedByMe`, and there is no endpoint to list likes. The backend cannot restore a viewer's like toggle state after reload. Repeated likes are idempotent, so do not blindly increment the displayed count on every successful PUT; refetch it.
- Profiles contain no `isFollowing`. Determine your followed readers from all pages of `/users/{myId}/following`, or keep confirmed client state. One page is not a complete relationship set.
- Clubs contain no `isMember`, membership count, or my-clubs endpoint. Membership can be determined from all pages of the member list; discussions/replies return 403 for nonmembers. `Member.id` is the user ID, not a membership ID.
- There are no embedded author/book objects in posts, comments, discussions, replies, notifications, or library entries. Resolve referenced IDs separately and cache them. There is no batch lookup API.
- There is no single discussion GET endpoint. Opening a discussion requires locating it in its club's paginated discussion list. A DISCUSSION_REPLY notification provides the discussion ID but no club ID; navigation needs cached context or discovery through accessible clubs/discussions.
- No file upload, genre catalogue, password reset/change, email verification, account deletion, discussion/reply editing/deletion, WebSocket/SSE, or full-text reader/club search API is implemented. Avatar and cover inputs accept externally hosted URLs.
- Render body/summary/bio text safely; no HTML or Markdown rendering contract is defined. Use placeholders for missing images and nullable text.

## Notifications and mutation semantics

| Notification type | Recipient | resourceId points to |
|---|---|---|
| NEW_FOLLOWER | Followed reader | Follower's user ID |
| LIKE | Post author | Post ID |
| SHARE | Post author | Post ID |
| COMMENT | Post author | Post ID, not comment ID |
| CLUB_JOIN | Club creator | Club ID |
| DISCUSSION_REPLY | Discussion author | Discussion ID, not reply ID |

actorId identifies the acting reader. Self-actions produce no notification. Existing notifications retain historical resource IDs after deletion, so handle 404 when navigating.

Likes, follows, library additions, and club joins are unique per user/target; repeating the same action does not duplicate it. Unlike, unfollow, and library removal are idempotent. Leaving an existing club is idempotent for noncreators; a creator gets 409 and must delete the club to close it. Every POST to shares records a new event; it does not publish to an external network. Creating posts, comments, discussions, and replies is also non-idempotent.

Deleting a book cascades to library entries, posts, likes, shares, and comments. Deleting a post removes its engagement/comment records. Deleting a club removes memberships, discussions, and replies. Confirm these consequences in the frontend and invalidate related cached data after success.

## Frontend integration checklist

- Verify the configured browser origin can call the API, including CORS preflight with Authorization.
- Exercise registration, login, expired access token refresh, refresh replacement, logout, and blocked sessions.
- Handle loading, empty collections, 204, validation errors, permission errors, deleted resources, and network failures.
- Test pagination and filter reset; do not assume a single page contains all relationships.
- Check owner/admin/member controls against server errors, including creator leave conflicts.
- Verify mutation cache updates and avoid automatic retries of share/content creation after ambiguous network failures.

For backend test setup and known verification constraints, see [README.md](../README.md#verification-and-deployment) and [VERIFICATION.md](VERIFICATION.md). This file documents current behavior; live OpenAPI can be inspected against a running backend.

