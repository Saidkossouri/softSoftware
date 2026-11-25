import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from '@/auth/AuthContext'
import { RequireAdmin } from '@/auth/RequireAdmin'
import { RequireAuth } from '@/auth/RequireAuth'
import { AppLayout } from '@/components/AppLayout'
import { Toaster } from '@/components/ui/toast'
import { HomePage } from '@/pages/HomePage'
import { MemberEditPage } from '@/pages/admin/MemberEditPage'
import { MembersPage } from '@/pages/admin/MembersPage'
import { FleetPage } from '@/pages/admin/FleetPage'
import { VehicleFormPage } from '@/pages/admin/VehicleFormPage'

export function App() {
  const [queryClient] = useState(() => new QueryClient())
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <Toaster>
          <BrowserRouter>
          <RequireAuth>
            <Routes>
              <Route element={<AppLayout />}>
                <Route index element={<HomePage />} />
                <Route path="admin" element={<RequireAdmin />}>
                  <Route path="membres" element={<MembersPage />} />
                  <Route path="membres/:memberId" element={<MemberEditPage />} />
                  <Route path="parc" element={<FleetPage />} />
                  <Route path="parc/nouveau" element={<VehicleFormPage />} />
                  <Route path="parc/:vehicleId" element={<VehicleFormPage />} />
                </Route>
                <Route path="*" element={<HomePage />} />
              </Route>
            </Routes>
          </RequireAuth>
          </BrowserRouter>
        </Toaster>
      </AuthProvider>
    </QueryClientProvider>
  )
}
