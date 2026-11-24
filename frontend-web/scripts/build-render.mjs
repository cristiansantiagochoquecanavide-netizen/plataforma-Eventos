import { writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const backendUrl = process.env.BACKEND_API_URL;
if (!backendUrl) {
  throw new Error('BACKEND_API_URL must point to the Render API service.');
}

const apiUrl = new URL('/api', backendUrl).toString().replace(/\/$/, '');
const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const environmentFile = resolve(scriptDirectory, '../src/environments/environment.prod.ts');

await writeFile(
  environmentFile,
  `export const environment = { apiUrl: ${JSON.stringify(apiUrl)} };\n`,
  'utf8',
);
