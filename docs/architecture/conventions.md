# Conventions

## Frontend

Pragmatic feature-first React + TypeScript. This is the target layout: folders are created when the work reaches them,
not ahead of time.

```text
frontend/src/
├── app/                  # entry wiring: router, providers, layout shell
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
├── components/           # presentational components
├── hooks/                # feature state and data hooks
├── pages/                # route-level screens
└── index.ts              # the only public entry point of the feature
```

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
