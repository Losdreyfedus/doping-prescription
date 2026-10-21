import { tr } from '@/shared/i18n/tr';

import type { ExamResult } from './api';

/** Topic analysis table; a topic with a wrong or blank answer is highlighted in light red (docs/arayuz.md). */
export function TopicAnalysis({ topics }: { topics: ExamResult['byTopic'] }) {
  return (
    <section className="rounded-card bg-surface p-6">
      <h2 className="mb-4 text-xl font-bold">{tr.result.topicAnalysis}</h2>
      <table className="w-full text-left">
        <thead>
          <tr className="text-text-muted">
            <th className="py-2">{tr.result.topic}</th>
            <th>{tr.result.questions}</th>
            <th>{tr.result.correct}</th>
            <th>{tr.result.wrong}</th>
            <th>{tr.result.blank}</th>
          </tr>
        </thead>
        <tbody>
          {topics.map((topic) => (
            <tr
              key={topic.topicId}
              data-weak={topic.correct < topic.total}
              className={topic.correct < topic.total ? 'bg-danger/10' : ''}
            >
              <td className="py-2 font-semibold">{topic.name}</td>
              <td>{topic.total}</td>
              <td>{topic.correct}</td>
              <td>{topic.wrong}</td>
              <td>{topic.blank}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
