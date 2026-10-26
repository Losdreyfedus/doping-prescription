import { fireEvent, render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';

import type { FeedCard } from './api';
import { VideoCard } from './VideoCard';

const card: FeedCard = {
  impressionId: '00000000-0000-4000-8000-000000000010',
  position: 0,
  content: {
    contentId: 34,
    title: 'Zar ve para atışında olasılık, 3 örnek',
    teacherName: 'Burak Öğretmen',
    videoUrl: '/videos/olasilik-1.webm',
    durationSec: 36,
    contentKind: 'SOLVED_EXAMPLE',
  },
  topic: { topicId: 9, code: 'TYT.MAT.OLAS', name: 'Olasılık' },
  pool: 'WEAK_TOPIC',
  reasonText: 'Son denemende Olasılık: 3 soruda 0 doğru',
  hasMicroQuiz: true,
  pointsAvailable: 15,
  saved: false,
};

const props = { card, progress: 0, elapsedSec: 0, playing: true, onToggle: vi.fn() };

describe('VideoCard', () => {
  it('plays the real file and reports a missing file so the player can fall back', () => {
    const onVideoError = vi.fn();
    const { container } = render(
      <VideoCard {...props} videoRef={createRef<HTMLVideoElement>()} onVideoError={onVideoError} />,
    );

    const video = container.querySelector('video');
    expect(video).toHaveAttribute('src', '/videos/olasilik-1.webm');
    expect(screen.queryByText('Olasılık', { selector: '.text-sun' })).not.toBeInTheDocument();

    if (!video) {
      throw new Error('no video element');
    }
    fireEvent.error(video);
    expect(onVideoError).toHaveBeenCalledOnce();
  });

  it('draws the simulated scene and subtitle without a video', () => {
    const { container } = render(<VideoCard {...props} />);

    expect(container.querySelector('video')).not.toBeInTheDocument();
    expect(screen.getByText('Zar ve para atışında olasılık, 3 örnek')).toBeInTheDocument();
    expect(screen.getByText('Son denemende Olasılık: 3 soruda 0 doğru')).toBeInTheDocument();
  });

  it('describes the play button with the topic and the reason', () => {
    render(<VideoCard {...props} />);

    expect(screen.getByRole('button', { name: 'Duraklat' })).toHaveAccessibleDescription(
      'Olasılık Son denemende Olasılık: 3 soruda 0 doğru',
    );
  });
});
