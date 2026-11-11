# Book sample data

`books.csv` contains **1,000 distinct real books**, each with a public HTTPS cover image URL. The original 40 records retain their IDs, creation timestamps, titles, authors, genres, and original short summaries; their covers and update timestamps have been filled in. Another 960 records were added. It matches the PostgreSQL `books` table column order:

```text
id,created_at,updated_at,title,author,genres,summary,cover_url
```

UUIDs and UTC timestamps are included because direct SQL imports bypass JPA's automatic ID and timestamp generation. Genres use the backend's lowercase, pipe-separated storage format. All cover URLs and summaries are populated; images remain hosted by their public providers. The original 40 summaries are preserved. Of the 960 added summaries, 957 are cleaned plain-text excerpts from the source catalogue's XML descriptions, limited to 1,500 characters and ending at a sentence where possible; three are original short summaries for books without source descriptions. Descriptions have HTML tags and leading edition notices removed. They are catalogue blurbs, not uniformly styled or independently reviewed plot synopses.

## Sources and validation

Added titles and credited author/contributor names come from [goodbooks-10k](https://github.com/zygmuntz/goodbooks-10k) by Zygmunt Zajac (GitHub `zygmuntz`). Source files: [books.csv](https://github.com/zygmuntz/goodbooks-10k/blob/master/books.csv), [tags.csv](https://github.com/zygmuntz/goodbooks-10k/blob/master/tags.csv), and [book_tags.csv](https://github.com/zygmuntz/goodbooks-10k/blob/master/book_tags.csv). English-language records were selected, duplicate normalized titles excluded, and up to five recognized genre labels derived from the strongest reader tags. These tags are community classifications, not an authoritative taxonomy. Source author fields can include translators or other credited contributors.

Cover URLs use the dataset's Goodreads image URLs. Five original records without a source cover use ISBN-matched [Open Library covers](https://openlibrary.org/dev/docs/api/covers), with `default=false` so missing covers do not silently return a placeholder. Dataset placeholder URLs were excluded. All 1,000 selected cover URLs returned HTTP 200 with image content, recognizable image bytes, and a body of at least 1,000 bytes during generation. This verifies availability at generation time; external providers can change or remove images later. Use an image error fallback in the frontend.

- `books-sources.csv` maps every local book ID to its source book page and dataset ID, and records the summary source and whether it is an original summary or a description excerpt. Description excerpts come from the matching book ID in [books_xml.zip](https://github.com/zygmuntz/goodbooks-10k/blob/master/books_xml/books_xml.zip). This is a provenance file, not a table-import file.
- `books-validation.json` records the cover check time and each selected image's response type and size, plus excluded image failures. Its separate summary validation records completeness and length checks. It contains metadata only, not downloaded image files.
- Source metadata is licensed under [Creative Commons Attribution-ShareAlike 4.0](https://creativecommons.org/licenses/by-sa/4.0/); the upstream license notice is preserved in `BOOKS_SOURCE_LICENSE.txt`. The adapted catalogue metadata is distributed under the same license. Changes consist of selection, normalization, genre mapping, local identifiers/timestamps, cover completion, and description cleaning/excerpting. Cover artwork remains subject to its respective rights; metadata licensing does not grant ownership of the images.

## Import with psql

Connect to your BookMuse database using psql. Then run this single-line command; adjust the path if the project moves:

```sql
\copy public.books (id, created_at, updated_at, title, author, genres, summary, cover_url) FROM 'C:/Users/User/Desktop/bookmuse/data/books.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8', FORCE_NULL (summary, cover_url))
```

This reads the CSV from the machine running psql. If psql runs inside Docker, copy the CSV into that container and use its container path instead.

For a GUI such as pgAdmin or DBeaver, select the `books` table's CSV import action, enable the header, use comma as delimiter and double quotes as quote/escape character, and map the nine columns by name. Configure empty `summary` values as NULL if supported.

Import once: reimporting the same CSV will conflict on its primary keys. Titles are not unique, so books with matching titles already in your database are not automatically skipped. This file does not alter or remove existing records.

If you already imported an earlier version of this CSV, import through a staging table and upsert by ID so cover URLs and summaries are updated and any missing records are inserted. Run this in one psql session; `\copy` must stay on one line:

```sql
BEGIN;
CREATE TEMP TABLE books_import (LIKE public.books INCLUDING DEFAULTS) ON COMMIT DROP;
\copy books_import (id, created_at, updated_at, title, author, genres, summary, cover_url) FROM 'C:/Users/User/Desktop/bookmuse/data/books.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8', FORCE_NULL (summary, cover_url))
INSERT INTO public.books (id, created_at, updated_at, title, author, genres, summary, cover_url)
SELECT id, created_at, updated_at, title, author, genres, summary, cover_url FROM books_import
ON CONFLICT (id) DO UPDATE
SET cover_url = EXCLUDED.cover_url, summary = EXCLUDED.summary, updated_at = CURRENT_TIMESTAMP;
COMMIT;
```

This updates only cover URLs, summaries, and update timestamps for existing IDs; other stored values are retained. It does not deduplicate separately created books with different IDs.

After import, log in and call `GET /api/books?page=0&size=100` to see the first page of the catalogue; at 1,000 records there are ten pages of 100. Responses expose genres as JSON arrays even though the database stores them as pipe-separated text. Do not use the raw CSV rows as API request bodies: `POST /api/books` expects the BookInput JSON format and an admin token.
