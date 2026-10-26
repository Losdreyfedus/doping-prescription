import { useState } from 'react';
import { useNavigate } from 'react-router';

import { tr } from '@/shared/i18n/tr';
import { useSession } from '@/shared/session/sessionContext';
import { AnswerBubble } from '@/shared/ui/AnswerBubble';
import { Avatar } from '@/shared/ui/Avatar';
import { BrandLogo } from '@/shared/ui/BrandLogo';
import { LogoutIcon } from '@/shared/ui/icons';
import { PageHeader } from '@/shared/ui/PageHeader';

import { type StudentCard, useAnswerKey, useDemoActions, usePersonas, useStudents } from './api';
import { ContentEffectiveness } from './ContentEffectiveness';

/** Route /admin (docs/arayuz.md, Admin): demo control and the presenter's answer key. */
export function AdminDemoPage() {
  const { endSession } = useSession();
  const navigate = useNavigate();
  const students = useStudents();

  return (
    <div className="min-h-screen bg-surface-muted">
      <header className="flex items-center justify-between bg-surface px-8 py-4">
        <BrandLogo />
        <button
          type="button"
          onClick={() => {
            endSession();
            void navigate('/');
          }}
          className="flex items-center gap-2 rounded-button px-3 py-2 text-text-muted hover:bg-surface-muted"
        >
          <LogoutIcon />
          {tr.topBar.logout}
        </button>
      </header>
      <main className="flex flex-col gap-8 p-8">
        <PageHeader title={tr.admin.demoTitle} subtitle={tr.admin.demoSubtitle} />
        {students.isPending && <p>{tr.admin.loading}</p>}
        {students.isError && <p role="alert">{students.error.message}</p>}
        {students.isSuccess && (
          <ul aria-label={tr.admin.students} className="grid grid-cols-3 gap-4">
            {students.data.map((student) => (
              <StudentControl key={student.slug} student={student} />
            ))}
          </ul>
        )}
        <AnswerKey />
        <ContentEffectiveness />
      </main>
    </div>
  );
}

function StudentControl({ student }: { student: StudentCard }) {
  const personas = usePersonas();
  const { reset, persona } = useDemoActions();
  const [selected, setSelected] = useState('SCRIPTED_IN_PROGRESS');
  const busy = reset.isPending || persona.isPending;
  const error = reset.error ?? persona.error;

  return (
    <li className="flex flex-col gap-4 rounded-card bg-surface p-5">
      <div className="flex items-center gap-3">
        <Avatar name={student.displayName} />
        <div>
          <h2 className="font-bold">{student.displayName}</h2>
          <p className="text-xs text-text-muted">
            {student.personalizationEnabled ? tr.admin.personalizationOn : tr.admin.personalizationOff}
          </p>
        </div>
      </div>
      <dl className="grid grid-cols-2 gap-1 text-sm">
        <dt className="text-text-muted">{tr.admin.lastExam}</dt>
        <dd>{student.hasExam ? tr.admin.yes : tr.admin.no}</dd>
        <dt className="text-text-muted">{tr.admin.points}</dt>
        <dd>{student.totalPoints}</dd>
      </dl>
      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          disabled={busy}
          onClick={() => {
            reset.mutate(student.slug);
          }}
          className="rounded-button bg-danger-strong px-4 py-2 font-semibold text-surface disabled:opacity-50"
        >
          {tr.admin.reset}
        </button>
        <label className="sr-only" htmlFor={`persona-${student.slug}`}>
          {tr.admin.persona}
        </label>
        <select
          id={`persona-${student.slug}`}
          value={selected}
          onChange={(event) => {
            setSelected(event.target.value);
          }}
          className="rounded-button border border-border px-2 py-2 text-sm"
        >
          {(personas.data ?? []).map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </select>
        <button
          type="button"
          disabled={busy}
          onClick={() => {
            persona.mutate({ slug: student.slug, name: selected });
          }}
          className="rounded-button bg-ink px-4 py-2 font-semibold text-surface disabled:opacity-50"
        >
          {tr.admin.loadPersona}
        </button>
      </div>
      {(reset.isSuccess || persona.isSuccess) && !busy && (
        <p role="status" className="text-sm text-success-strong">
          {tr.admin.done}
        </p>
      )}
      {error && <p role="alert">{error.message}</p>}
    </li>
  );
}

function AnswerKey() {
  const key = useAnswerKey();
  return (
    <section className="flex flex-col gap-3 rounded-card bg-surface p-5">
      <h2 className="text-xl font-bold">{tr.admin.answerKeyTitle}</h2>
      <p className="text-sm text-text-muted">{tr.admin.answerKeyHint}</p>
      {key.isPending && <p>{tr.admin.answerKeyLoading}</p>}
      {key.isError && <p role="alert">{key.error.message}</p>}
      {key.isSuccess && key.data.length === 0 && <p>{tr.admin.answerKeyEmpty}</p>}
      {key.isSuccess && key.data.length > 0 && (
        <table className="text-left text-sm">
          <thead>
            <tr className="text-text-muted">
              <th className="py-1">{tr.admin.question}</th>
              <th>{tr.result.topic}</th>
              <th>{tr.admin.correct}</th>
              <th>{tr.admin.scenario}</th>
            </tr>
          </thead>
          <tbody>
            {key.data.map((row) => (
              <tr key={row.orderNo}>
                <td className="py-1">{row.orderNo}</td>
                <td>{row.topic}</td>
                <td>
                  <AnswerBubble letter={row.correctKey} state="correct" size="sm" />
                </td>
                <td>
                  {row.scenarioKey ? (
                    <AnswerBubble
                      letter={row.scenarioKey}
                      state={row.scenarioKey === row.correctKey ? 'correct' : 'wrong'}
                      size="sm"
                    />
                  ) : (
                    tr.result.leftBlank
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
