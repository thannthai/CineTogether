import { createBrowserRouter } from 'react-router-dom'
import { BrowsePage, DetailPage, HomePage } from '../features/catalog'
import { AppLayout } from './AppLayout'
import { NotFoundPage, PagePlaceholder } from './PagePlaceholder'

// Screen routes follow docs/design/api.md: providerKey and movieId identify a movie.
export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'movies', element: <BrowsePage /> },
      { path: 'movies/:providerKey/:movieId', element: <DetailPage /> },
      {
        path: 'movies/:providerKey/:movieId/watch',
        element: <PagePlaceholder title="Xem phim" />,
      },
      {
        path: 'together',
        element: (
          <PagePlaceholder title="Xem chung" note="Tính năng sắp ra mắt: xem chung cùng bạn bè." />
        ),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
