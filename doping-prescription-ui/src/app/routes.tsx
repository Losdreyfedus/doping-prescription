import { Navigate, type RouteObject } from 'react-router';

import { AdminDemoPage } from '@/features/admin/AdminDemoPage';
import { LoginPage } from '@/features/auth/LoginPage';
import { DopinglePage } from '@/features/dopingle/DopinglePage';
import { ExamResultPage } from '@/features/exams/ExamResultPage';
import { ExamSolvePage } from '@/features/exams/ExamSolvePage';
import { ExamsPage } from '@/features/exams/ExamsPage';
import { HomePage } from '@/features/home/HomePage';
import { ProgressPage } from '@/features/learner/ProgressPage';

import { AppShell } from './layout/AppShell';
import { RouteError } from './layout/RouteError';
import { RequireRole } from './session/RequireRole';

/** Routes of docs/arayuz.md in MVP scope; after login a student lands on Ana Sayfa and opens Dopingle from the top bar. */
export const routes: RouteObject[] = [
  { path: '/', element: <LoginPage />, errorElement: <RouteError /> },
  {
    path: '/app',
    element: (
      <RequireRole allow="STUDENT">
        <AppShell />
      </RequireRole>
    ),
    errorElement: <RouteError />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'dopingle', element: <DopinglePage /> },
      { path: 'denemeler', element: <ExamsPage /> },
      { path: 'denemeler/:attemptId', element: <ExamSolvePage /> },
      { path: 'denemeler/:attemptId/sonuc', element: <ExamResultPage /> },
      { path: 'gelisimim', element: <ProgressPage /> },
      // the screen was called Eksiklerim before the demo rename; old links keep working
      { path: 'eksiklerim', element: <Navigate to="/app/gelisimim" replace /> },
    ],
  },
  {
    path: '/admin',
    element: (
      <RequireRole allow="ADMIN">
        <AdminDemoPage />
      </RequireRole>
    ),
    errorElement: <RouteError />,
  },
  { path: '/admin/demo', element: <Navigate to="/admin" replace /> },
  { path: '*', element: <Navigate to="/" replace /> },
];
