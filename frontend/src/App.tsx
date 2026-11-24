import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from '@/auth/AuthContext'
import { RequireAuth } from '@/auth/RequireAuth'
import { AppLayout } from '@/components/AppLayout'
import { HomePage } from '@/pages/HomePage'

export function App() {
  const [queryClient] = useState(() => new QueryClient())
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <RequireAuth>
            <Routes>
              <Route element={<AppLayout />}>
                <Route index element={<HomePage />} />
                <Route path="*" element={<HomePage />} />
              </Route>
            </Routes>
          </RequireAuth>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
