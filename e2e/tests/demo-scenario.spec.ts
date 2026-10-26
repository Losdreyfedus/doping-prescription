import { expect, type Page, test } from '@playwright/test';

import { type FeedPage, isFirstFeedPage, readFeedPage, readMicroQuiz } from './apiResponses';
import { correctOptionText } from './microQuestions';

/** Responses below quiz.min-response-ms earn nothing (docs/algoritma.md §7); a presenter takes longer anyway. */
const READING_TIME_MS = 2_500;
/** docs/demo-senaryosu.md, Adım 3: the topics Deniz missed in Mini Deneme 1 (seed/topics.json names). */
const MISSED_TOPICS = ['Olasılık', 'Permütasyon-Kombinasyon'];
/** prescription.feed.max-same-topic-in-row */
const MAX_SAME_TOPIC_IN_ROW = 2;
/** docs/algoritma.md §6: the reasons of the explore, popular and fallback pools; none speaks of the learner. */
const GENERIC_REASON = /^(TYT'de sık çıkan konu: .+|Bu hafta en çok izlenenlerden|Dopingle'da yeni)$/;

/**
 * The main path of docs/demo-senaryosu.md: prepare Deniz in the admin panel, look at today's feed, submit the prepared
 * exam, open the prescribed feed, answer the micro-question and see the progress on Gelişimim.
 */
test('demo scenario: exam → prescription → micro-question → progress', async ({ page }) => {
  await test.step('Hazırlık: admin sıfırlar ve SCRIPTED_IN_PROGRESS personasını yükler', async () => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Admin' }).click();
    const deniz = page.getByRole('listitem').filter({ hasText: 'Deniz' });
    await deniz.getByRole('button', { name: 'Sıfırla' }).click();
    await expect(deniz.getByText('Tamamlandı.')).toBeVisible();
    await deniz.getByLabel('Persona').selectOption('SCRIPTED_IN_PROGRESS');
    await deniz.getByRole('button', { name: 'Persona yükle' }).click();
    await expect(deniz.getByText('Tamamlandı.')).toBeVisible();
    await page.getByRole('button', { name: 'Çıkış Yap' }).click();
  });

  await test.step('Adım 1: kişiselleştirilmemiş akış, genel etiketler', async () => {
    await page.getByRole('button', { name: /Deniz/ }).click();
    const feed = await openFeed(page, () => page.getByRole('link', { name: 'Dopingle', exact: true }).click());

    // Deniz has no signals yet: the pinned demo videos still open the feed, but with their pools' generic reasons.
    const [first] = feed.cards;
    if (!first) {
      throw new Error('the feed before the exam is empty');
    }
    for (const card of feed.cards) {
      expect(card.reasonText).toMatch(GENERIC_REASON);
    }
    await expect(page.getByTestId('reason')).toHaveText(first.reasonText);
    await page.getByRole('button', { name: "Dopingle'ı kapat" }).click();
    await expect(page.getByTestId('reason')).toBeHidden();
  });

  await test.step('Adım 2: Deniz hazır denemeyi gönderir', async () => {
    await page.getByRole('link', { name: 'Testler ve Denemeler' }).click();
    await page
      .getByRole('listitem')
      .filter({ has: page.getByText('TYT Matematik Mini Deneme 1', { exact: true }) })
      .getByRole('button', { name: 'Devam Et' })
      .click();
    await expect(page.getByText('11 Soru Çözüldü')).toBeVisible();
    await page.getByRole('button', { name: /Sınavı Bitir|Testi Bitir/ }).first().click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('1 soru boş.');
    await dialog.getByRole('button', { name: 'Bitir' }).click();

    await expectStat(page, 'Doğru', 8);
    await expectStat(page, 'Yanlış', 3);
    await expectStat(page, 'Boş', 1);
    await expect(page.getByText('7,25 net')).toBeVisible();
    for (const topic of MISSED_TOPICS) {
      await expect(page.getByRole('row').filter({ hasText: topic })).toHaveAttribute('data-weak', 'true');
    }
    await expect(page.getByText('Dopingle akışın bu sonuca göre güncellendi.')).toBeVisible();
  });

  await test.step('Adım 3: reçete, ilk kart Olasılık', async () => {
    // The topics come from the feed response, so the first card stays on screen untouched for step 4.
    const feed = await openFeed(page, () => page.getByRole('link', { name: 'Reçeteni gör' }).click());
    const topics = feed.cards.slice(0, 10).map((card) => card.topic.name);

    expect(topics).toHaveLength(10);
    expect(topics[0]).toBe('Olasılık');
    expect(topics.slice(0, 5).filter((topic) => MISSED_TOPICS.includes(topic)).length).toBeGreaterThanOrEqual(3);
    expect(longestSameTopicRun(topics)).toBeLessThanOrEqual(MAX_SAME_TOPIC_IN_ROW);
    expect(feed.cards.slice(0, 10).map((card) => card.reasonText)).toContain("TYT'de sık çıkan konu: Fonksiyonlar");
    await expect(page.getByTestId('reason')).toHaveText('Son denemende Olasılık: 3 soruda 0 doğru');
    await expect(page.getByText('+15 puan').first()).toBeVisible();
  });

  await test.step('Adım 4: video biter, mikro-soru doğru cevaplanır', async () => {
    const quizResponse = page.waitForResponse((response) => /\/api\/feed\/cards\/.+\/quiz$/.test(response.url()), {
      timeout: 60_000, // a real demo video plays at normal speed (up to ~36 s); CI uses the simulated player
    });
    const quiz = await readMicroQuiz(await quizResponse);
    const panel = page.getByRole('region', { name: 'Mikro-soru' });
    await expect(panel).toBeVisible();
    // The server ignores answers faster than quiz.min-response-ms and nothing on screen marks that moment.
    await page.waitForTimeout(READING_TIME_MS);
    await answer(page, correctOptionText(quiz.microQuestionId));

    await expect(panel.getByText('Doğru!')).toBeVisible();
    await expect(panel.getByText('+15 puan')).toBeVisible();
    await expect(panel.getByText('Olasılık ustalığın %17 → %27')).toBeVisible();
    await panel.getByRole('button', { name: 'Devam' }).click();
  });

  await test.step('Adım 5: Gelişimim, Olasılık kırmızı ve yukarı ok', async () => {
    await page.getByRole('link', { name: 'Gelişimim' }).click();
    const rows = page.getByRole('list', { name: 'Konu ustalıkları' }).getByRole('listitem');
    const first = rows.first();
    await expect(first).toHaveAttribute('data-band', 'WEAK');
    await expect(first).toContainText('Olasılık');
    await expect(first).toContainText('%27');
    // the arrow is decorative; its meaning is screen-reader text next to it
    await expect(first).toContainText('↑');
    await expect(first.getByText('Yükseliyor')).toBeAttached();
    await expect(first).toContainText(/Bu konudan \d+ video hazır/);
    await expect(first.getByRole('link', { name: 'Olasılık çalış' })).toHaveText('Çalış');
    // every topic offers its videos and a Çalış button
    await expect(rows.getByRole('link', { name: / çalış$/ })).toHaveCount(await rows.count());
  });
});

/** One of the result screen's count cards (Doğru, Yanlış, Boş). */
async function expectStat(page: Page, label: string, count: number) {
  const card = page.getByRole('group', { name: label, exact: true });
  await expect(card.getByText(String(count), { exact: true })).toBeVisible();
}

/** Opens Dopingle with the given action and returns the first page of the new feed session. */
async function openFeed(page: Page, open: () => Promise<void>): Promise<FeedPage> {
  const response = page.waitForResponse(isFirstFeedPage);
  await open();
  return readFeedPage(await response);
}

function longestSameTopicRun(topics: string[]): number {
  let longest = 0;
  let run = 0;
  topics.forEach((topic, index) => {
    run = index > 0 && topics[index - 1] === topic ? run + 1 : 1;
    longest = Math.max(longest, run);
  });
  return longest;
}

async function answer(page: Page, optionText: string) {
  const escaped = optionText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  await page
    .getByRole('region', { name: 'Mikro-soru' })
    .getByRole('button', { name: new RegExp(`^[A-D] ${escaped}$`) })
    .click();
}
