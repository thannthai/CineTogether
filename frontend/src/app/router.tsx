import { createBrowserRouter } from 'react-router-dom'
import { BrowsePage, DetailPage, HomePage } from '../features/catalog'
import { WatchTogetherPage } from '../features/party'
import { WatchPage } from '../features/watch'
import { AppLayout } from './layout/AppLayout'
import { NotFoundPage } from './layout/PagePlaceholder'

// Screen routes follow docs/design/api.md: providerKey and movieId identify a movie.
export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'movies', element: <BrowsePage /> },
      { path: 'movies/:providerKey/:movieId', element: <DetailPage /> },
      { path: 'movies/:providerKey/:movieId/watch', element: <WatchPage /> },
      { path: 'together', element: <WatchTogetherPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
