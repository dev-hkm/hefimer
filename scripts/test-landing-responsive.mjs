import assert from "node:assert/strict";
import { existsSync } from "node:fs";
import puppeteer from "puppeteer";

const baseUrl = process.env.HEFIMER_TEST_URL || "http://127.0.0.1:4175";
const browserCandidates = [
  process.env.PUPPETEER_EXECUTABLE_PATH,
  "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
  "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
].filter(Boolean);
const executablePath = browserCandidates.find((candidate) => existsSync(candidate));
const browser = await puppeteer.launch({ executablePath, headless: true, args: ["--no-sandbox"] });

try {
  const page = await browser.newPage();
  await page.evaluateOnNewDocument(() => localStorage.removeItem("hefimer_visited"));
  for (const width of [320, 360, 390, 768]) {
    await page.setViewport({ width, height: 844, deviceScaleFactor: 1 });
    await page.goto(baseUrl, { waitUntil: "networkidle0" });
    const layout = await page.evaluate(() => {
      const landing = document.querySelector(".hefimer-landing")?.getBoundingClientRect();
      const constellation = document.querySelector(".hefimer-device-constellation")?.getBoundingClientRect();
      return {
        viewport: window.innerWidth,
        documentWidth: document.documentElement.scrollWidth,
        bodyWidth: document.body.scrollWidth,
        landingLeft: landing?.left ?? -1,
        landingRight: landing?.right ?? -1,
        centerDelta: constellation ? Math.abs((constellation.left + constellation.right) / 2 - window.innerWidth / 2) : 999,
      };
    });
    assert.ok(layout.documentWidth <= width, `${width}px document overflows to ${layout.documentWidth}px`);
    assert.ok(layout.bodyWidth <= width, `${width}px body overflows to ${layout.bodyWidth}px`);
    assert.ok(layout.landingLeft >= 0 && layout.landingRight <= width, `${width}px landing escapes viewport: ${JSON.stringify(layout)}`);
    if (width <= 760) assert.ok(layout.centerDelta <= 1, `${width}px device visual is off-center by ${layout.centerDelta}px`);
  }

  if (process.env.HEFIMER_SCREENSHOTS === "1") {
    await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 1 });
    await page.goto(baseUrl, { waitUntil: "networkidle0" });
    await page.screenshot({ path: "landing-mobile-top.png" });
    await page.$eval(".hefimer-device-track", (element) => element.scrollIntoView({ block: "start" }));
    await new Promise((resolve) => setTimeout(resolve, 900));
    await page.screenshot({ path: "landing-mobile-devices.png" });
    await page.$eval(".hefimer-device-constellation", (element) => element.scrollIntoView({ block: "center" }));
    await new Promise((resolve) => setTimeout(resolve, 500));
    await page.screenshot({ path: "landing-mobile-constellation.png" });
    await page.setViewport({ width: 1440, height: 1000, deviceScaleFactor: 1 });
    await page.goto(baseUrl, { waitUntil: "networkidle0" });
    await page.$eval(".hefimer-device-track", (element) => element.scrollIntoView({ block: "start" }));
    await new Promise((resolve) => setTimeout(resolve, 900));
    await page.screenshot({ path: "landing-desktop-devices.png" });
  }
  console.log("Landing responsive test passed at 320, 360, 390, and 768px");
} finally {
  await browser.close();
}
