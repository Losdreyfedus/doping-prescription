import { tr } from '@/shared/i18n/tr';

import { useContentEffectiveness } from './api';

const percent = new Intl.NumberFormat('tr-TR', { style: 'percent', maximumFractionDigits: 0 });

/** Micro-question answers and correct-answer rate per video, best first (GET /api/admin/content-effectiveness). */
export function ContentEffectiveness() {
  const videos = useContentEffectiveness();
  return (
    <section className="flex flex-col gap-3 rounded-card bg-surface p-5">
      <h2 className="text-xl font-bold">{tr.admin.effectivenessTitle}</h2>
      <p className="text-sm text-text-muted">{tr.admin.effectivenessHint}</p>
      {videos.isPending && <p>{tr.admin.effectivenessLoading}</p>}
      {videos.isError && <p role="alert">{videos.error.message}</p>}
      {videos.isSuccess && videos.data.length === 0 && <p>{tr.admin.effectivenessEmpty}</p>}
      {videos.isSuccess && videos.data.length > 0 && (
        <table aria-label={tr.admin.effectivenessTitle} className="text-left text-sm">
          <thead>
            <tr className="text-text-muted">
              <th className="py-1">{tr.admin.video}</th>
              <th>{tr.result.topic}</th>
              <th>{tr.admin.answers}</th>
              <th>{tr.admin.correctRate}</th>
            </tr>
          </thead>
          <tbody>
            {videos.data.map((video) => (
              <tr key={video.contentId}>
                <td className="py-1">{video.title}</td>
                <td>{video.topic}</td>
                <td>{video.answers}</td>
                <td className="font-semibold">{percent.format(video.correctRate)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
