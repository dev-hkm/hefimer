// Script to convert og-image.svg to og-image.png using sharp
// Run: node scripts/gen-og.mjs

import { createRequire } from 'module';
import { readFileSync, writeFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const svgPath = join(__dirname, '../public/og-image.svg');
const pngPath = join(__dirname, '../public/og-image.png');

const svgContent = readFileSync(svgPath);

try {
  const sharp = (await import('sharp')).default;
  await sharp(svgContent)
    .png({ quality: 100, compressionLevel: 6 })
    .resize(1200, 630)
    .toFile(pngPath);
  console.log('✅ og-image.png generated at', pngPath);
} catch (e) {
  console.error('sharp not available, trying alternative...');
  // Fallback: try puppeteer-core or skip
  console.log('Install sharp: npm install sharp --save-dev');
}
