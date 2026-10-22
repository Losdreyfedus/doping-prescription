import { expect, type Page, test } from '@playwright/test';

import { correctOptionText } from './microQuestions';

/** Responses below quiz.min-response-ms earn nothing (docs/algoritma.md §7); a presenter takes longer anyway. */
const READING_TIME_MS = 2_500;

/**
 * The main path of docs/demo-senaryosu.md: prepare Deniz in the admin panel, submit the prepared exam, open the
 * prescribed feed, answer the micro-question and see the progress on Gelişimim.
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

  await test.step('Adım 2: Deniz hazır denemeyi gönderir', async () => {
    await page.getByRole('button', { name: /Deniz/ }).click();
    await page.getByRole('link', { name: 'Testler ve Denemeler' }).click();
    await page.getByRole('button', { name: /Devam|Başla/ }).first().click();
    await expect(page.getByText('11 Soru Çözüldü')).toBeVisible();
    await page.getByRole('button', { name: /Sınavı Bitir|Testi Bitir/ }).first().click();
    await page.getByRole('dialog').getByRole('button', { name: 'Bitir' }).click();
    await expect(page.getByText('Dopingle akışın bu sonuca göre güncellendi.')).toBeVisible();
  });

  await test.step('Adım 3: reçete, ilk kart Olasılık', async () => {
    await page.getByRole('link', { name: 'Reçeteni gör' }).click();
    await expect(page.getByTestId('reason').first()).toHaveText('Son denemende Olasılık: 3 soruda 0 doğru');
    await expect(page.getByText('+15 puan').first()).toBeVisible();
  });

  await test.step('Adım 4: video biter, mikro-soru doğru cevaplanır', async () => {
    const quizResponse = page.waitForResponse((response) => /\/api\/feed\/cards\/.+\/quiz$/.test(response.url()), {
      timeout: 40_000,
    });
    const quiz = (await (await quizResponse).json()) as { microQuestionId: number };
    const panel = page.getByRole('region', { name: 'Mikro-soru' });
    await expect(panel).toBeVisible();
    await page.waitForTimeout(READING_TIME_MS);
    await answer(page, correctOptionText(quiz.microQuestionId));

    await expect(panel.getByText('Doğru!')).toBeVisible();
    await expect(panel.getByText('+15 puan')).toBeVisible();
    await expect(panel.getByText('Olasılık ustalığın %17 → %27')).toBeVisible();
    await panel.getByRole('button', { name: 'Devam' }).click();
  });

  await test.step('Adım 5: Gelişimim, Olasılık kırmızı ve yukarı ok', async () => {
    await page.getByRole('link', { name: 'Gelişimim' }).click();
    const first = page.getByRole('list', { name: 'Konu ustalıkları' }).getByRole('listitem').first();
    await expect(first).toHaveAttribute('data-band', 'WEAK');
    await expect(first).toContainText('Olasılık');
    await expect(first).toContainText('%27');
    await expect(first.getByLabel('Yükseliyor')).toBeVisible();
  });
});

async function answer(page: Page, optionText: string) {
  const escaped = optionText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  await page
    .getByRole('region', { name: 'Mikro-soru' })
    .getByRole('button', { name: new RegExp(`^[A-D] ${escaped}$`) })
    .click();
}
