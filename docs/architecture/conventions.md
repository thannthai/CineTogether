# Conventions

## Frontend

Pragmatic feature-first React + TypeScript. This is the target layout: folders are created when the work reaches them,
not ahead of time.

```text
frontend/src/
├── app/                  # entry wiring: router; layout/ holds the shell (header, footer, drawer, logo)
├── features/
│   ├── auth/             # Supabase client use, login/register/logout screens, session
│   ├── catalog/          # home, browse, search, movie details
│   ├── watch/            # Watch Alone page and the HLS player (hls.js)
│   └── party/            # Watch Party rooms, public discovery, WebSocket, sync, chat
├── shared/
│   ├── ui/               # generic presentational components (button, dialog, ...)
│   ├── hooks/            # generic hooks, only when one is truly feature-independent
│   ├── lib/              # API client wrapper, Supabase client, config
│   ├── types/            # types shared by several features
│   └── utils/            # small pure helpers
└── styles/               # global styles and design tokens from Figma
```

### Feature layout

A feature starts flat and grows only as needed. A larger feature may contain:

```text
features/<feature>/
├── api/                  # calls to the backend, request/response types
├── components/           # presentational components (each with its own .css next to it)
├── context/              # React context and providers (for example the auth session)
├── hooks/                # feature state and data hooks
├── lib/                  # pure functions and rules, with their tests next to them; grouped in subfolders when large
├── pages/                # route-level screens (each with its own .css next to it)
├── <feature>.css         # imports the feature's .css files in a fixed order (see "Styles")
└── index.ts              # the only public entry point of the feature
```

Tests sit next to the module they test (`lib/browse/browseView.ts` and `lib/browse/browseView.test.ts`).

### Styles

Plain CSS, one file per component or page, next to it. A feature has one aggregator file (`catalog/catalog.css`,
`app/layout/layout.css`, ...) that `@import`s its files and is imported by the screens. The aggregator fixes the order
of the files, so the cascade does not depend on which component happens to be imported first. A new component's `.css`
is added to the aggregator in the right place; rules that override another file's rules must come after it. Design
tokens and global rules live in `styles/`.

### Rules

- `app/` is the only place that knows every feature (routes, providers). Features do not import from `app/`.
- A feature imports another feature only through its `index.ts`.
- `shared/` never imports from `features/` or `app/`.
- `shared/` holds code that is generic by nature, not code that happens to be used twice.
- The HLS player lives in `features/watch/` and `features/party/` reuses it through `index.ts`; there is one player, as
  there is one Watch Party engine on the backend.
- Data access (fetch, Supabase calls, WebSocket) stays in `api/` or hooks, not inside presentational components.
- No Repository/Service/UseCase layers without a concrete need. No router, UI library, or state library is chosen yet.
- Figma decides the visuals; `docs/design/api.md` decides the data and behavior. Figma Make output is ported into this
  structure, not kept as generated.
