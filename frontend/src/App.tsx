import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from '@/auth/AuthContext'
import { RequireAdmin } from '@/auth/RequireAdmin'
import { RequireAuth } from '@/auth/RequireAuth'
import { AppLayout } from '@/components/AppLayout'
import { Toaster } from '@/components/ui/toast'
import { MaintenanceRecordFormPage } from '@/pages/MaintenanceRecordFormPage'
import { MaintenanceRecordPage } from '@/pages/MaintenanceRecordPage'
import { VehicleDetailPage } from '@/pages/VehicleDetailPage'
import { VehiclePlanPage } from '@/pages/VehiclePlanPage'
import { VehiclesPage } from '@/pages/VehiclesPage'
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
                <Route index element={<VehiclesPage />} />
                <Route path="vehicules/:vehicleId" element={<VehicleDetailPage />} />
                <Route path="vehicules/:vehicleId/entretiens/nouveau" element={<MaintenanceRecordFormPage />} />
                <Route path="vehicules/:vehicleId/plan" element={<VehiclePlanPage />} />
                <Route path="entretiens/:recordId" element={<MaintenanceRecordPage />} />
                <Route path="entretiens/:recordId/modifier" element={<MaintenanceRecordFormPage />} />
                <Route path="admin" element={<RequireAdmin />}>
                  <Route path="membres" element={<MembersPage />} />
                  <Route path="membres/:memberId" element={<MemberEditPage />} />
                  <Route path="parc" element={<FleetPage />} />
                  <Route path="parc/nouveau" element={<VehicleFormPage />} />
                  <Route path="parc/:vehicleId" element={<VehicleFormPage />} />
                </Route>
                <Route path="*" element={<VehiclesPage />} />
              </Route>
            </Routes>
          </RequireAuth>
          </BrowserRouter>
        </Toaster>
      </AuthProvider>
    </QueryClientProvider>
  )
}
