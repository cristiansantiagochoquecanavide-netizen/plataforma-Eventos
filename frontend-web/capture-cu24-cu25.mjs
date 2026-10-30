import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const output = fileURLToPath(new URL('../documentacion/capturas/', import.meta.url));
await mkdir(output, { recursive: true });

const browser = await chromium.launch({
  headless: true,
  executablePath: 'C:/Users/ASUS/AppData/Local/ms-playwright/chromium-1187/chrome-win/chrome.exe',
});
const page = await browser.newPage({ viewport: { width: 1600, height: 1200 }, deviceScaleFactor: 1 });
page.on('console', message => { if (message.type() === 'error') console.error('browser:', message.text()); });
page.on('pageerror', error => console.error('page:', error.message));

try {
  await page.goto('http://localhost:4200/iniciar-sesion', { waitUntil: 'networkidle' });
  await page.locator('input[type="email"]').fill('OrganizadorEventos@gmail.com');
  await page.locator('input[type="password"]').fill('Organizador123');
  await page.getByRole('button', { name: 'Entrar al panel' }).click();
  await page.waitForURL(url => !url.pathname.includes('iniciar-sesion'), { timeout: 20000 });

  for (const [route, filename] of [['aforo', 'CU24-aforo.png'], ['notificaciones', 'CU25-notificaciones.png']]) {
    await page.goto(`http://localhost:4200/${route}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    await page.screenshot({ path: join(output, filename), fullPage: true });
    console.log(`${filename}: ${page.url()}`);
  }
} finally {
  await browser.close();
}
