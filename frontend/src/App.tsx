import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { Toaster } from 'sonner'
import { AuthProvider } from '@/context/auth-context'
import { AdminAuthProvider } from '@/context/admin-auth-context'
import { RequireAuth } from '@/components/require-auth'
import { RequireAdmin } from '@/components/admin/require-admin'
import { AdminShell } from '@/components/admin/admin-shell'
import { HomePage } from '@/pages/home-page'
import { AuthPage } from '@/pages/auth-page'
import { ProposalsPage } from '@/pages/proposals-page'
import { VotePage } from '@/pages/vote-page'
import { ResultPage } from '@/pages/result-page'
import { AdminLoginPage } from '@/pages/admin/admin-login-page'
import { AdminDashboardPage } from '@/pages/admin/admin-dashboard-page'
import { AdminProposalsPage } from '@/pages/admin/admin-proposals-page'
import { AdminProposalFormPage } from '@/pages/admin/admin-proposal-form-page'
import { AdminResultsPage } from '@/pages/admin/admin-results-page'
import { AdminResultDetailPage } from '@/pages/admin/admin-result-detail-page'
import { AdminOwnersPage } from '@/pages/admin/admin-owners-page'
import { AdminOwnersPrintQrPage } from '@/pages/admin/admin-owners-print-qr-page'
import { AdminUnitsImportPage } from '@/pages/admin/admin-units-import-page'
import { AdminSystemPage } from '@/pages/admin/admin-system-page'

function LegacyAuthRedirect() {
  const { search } = useLocation()
  return <Navigate to={`/vote${search}`} replace />
}

export function App() {
  return (
    <AuthProvider>
      <AdminAuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/auth" element={<LegacyAuthRedirect />} />
            <Route path="/vote" element={<AuthPage />} />
            <Route
              path="/proposals"
              element={
                <RequireAuth>
                  <ProposalsPage />
                </RequireAuth>
              }
            />
            <Route
              path="/proposals/:id/vote"
              element={
                <RequireAuth>
                  <VotePage />
                </RequireAuth>
              }
            />
            <Route
              path="/proposals/:id/result"
              element={
                <RequireAuth>
                  <ResultPage />
                </RequireAuth>
              }
            />

            <Route path="/admin/login" element={<AdminLoginPage />} />
            <Route
              path="/admin/owners/print-qr"
              element={
                <RequireAdmin>
                  <AdminOwnersPrintQrPage />
                </RequireAdmin>
              }
            />
            <Route
              path="/admin"
              element={
                <RequireAdmin>
                  <AdminShell />
                </RequireAdmin>
              }
            >
              <Route index element={<AdminDashboardPage />} />
              <Route path="proposals" element={<AdminProposalsPage />} />
              <Route path="proposals/new" element={<AdminProposalFormPage mode="new" />} />
              <Route path="proposals/:id/edit" element={<AdminProposalFormPage mode="edit" />} />
              <Route path="results" element={<AdminResultsPage />} />
              <Route path="results/:id" element={<AdminResultDetailPage />} />
              <Route path="owners" element={<AdminOwnersPage />} />
              <Route path="units/import" element={<AdminUnitsImportPage />} />
              <Route path="system" element={<AdminSystemPage />} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
          <Toaster richColors position="top-center" />
        </BrowserRouter>
      </AdminAuthProvider>
    </AuthProvider>
  )
}
