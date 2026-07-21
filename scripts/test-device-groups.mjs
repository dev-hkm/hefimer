import puppeteer from "puppeteer";
import { existsSync } from "node:fs";
import assert from "node:assert/strict";

const baseUrl = process.env.HEFIMER_TEST_URL || "http://127.0.0.1:8788";

async function clickButton(page, text) {
  await page.waitForFunction((label) => Array.from(document.querySelectorAll("button")).some((button) => {
    return button.textContent?.replace(/\s+/g, " ").trim().includes(label);
  }), { timeout: 20_000 }, text);
  const clicked = await page.evaluate((label) => {
    const button = Array.from(document.querySelectorAll("button")).find((candidate) => {
      return candidate.textContent?.replace(/\s+/g, " ").trim().includes(label);
    });
    if (!button) return false;
    button.scrollIntoView({ block: "center" });
    button.click();
    return true;
  }, text);
  if (!clicked) throw new Error(`Button not found: ${text}`);
}

async function waitForText(page, text) {
  await page.waitForFunction((label) => document.body.textContent?.includes(label), { timeout: 20_000 }, text);
}

function observePage(page, name) {
  page.on("pageerror", (error) => console.error(`[${name}] page error:`, error.message));
  page.on("console", (message) => {
    if (message.type() === "error") console.error(`[${name}] console error:`, message.text());
  });
}

const browserCandidates = [
  process.env.PUPPETEER_EXECUTABLE_PATH,
  "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
  "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
].filter(Boolean);
const executablePath = browserCandidates.find((candidate) => existsSync(candidate));
const browser = await puppeteer.launch({ executablePath, headless: true, args: ["--no-sandbox"] });
try {
  const contextA = await browser.createBrowserContext();
  await contextA.overridePermissions(new URL(baseUrl).origin, ["clipboard-read", "clipboard-write"]);
  const pageA = await contextA.newPage();
  let registrationRequests = 0;
  pageA.on("request", (request) => {
    if (request.url().includes("/api/devices/register")) registrationRequests += 1;
  });
  observePage(pageA, "sender");
  await pageA.evaluateOnNewDocument(() => localStorage.setItem("hefimer_visited", "1"));
  await pageA.goto(baseUrl, { waitUntil: "networkidle0" });
  assert.equal(registrationRequests, 0, "Device identity must stay lazy until the Hub is opened");
  await pageA.waitForSelector('button[title="Paired devices"]', { timeout: 20_000 });
  await pageA.evaluate(() => document.querySelector('button[title="Paired devices"]')?.click());
  try {
    await waitForText(pageA, "Create secure group");
  } catch (error) {
    await pageA.screenshot({ path: "device-hub-e2e-failure.png", fullPage: true });
    throw error;
  }
  await clickButton(pageA, "Create secure group");
  await waitForText(pageA, "Pair another device");
  const token = await pageA.waitForFunction(() => {
    const values = Array.from(document.querySelectorAll("span")).map((element) => element.textContent?.trim() || "");
    return values.find((value) => /^hfm-[A-Za-z0-9]{36}$/.test(value)) || false;
  }, { timeout: 20_000 }).then((handle) => handle.jsonValue());
  if (typeof token !== "string") throw new Error("Pairing token was not rendered");
  await pageA.click('button[aria-label="Copy pairing token"]');
  await waitForText(pageA, "Copied");
  assert.equal(await pageA.evaluate(() => navigator.clipboard.readText()), token, "Copy token must write the complete token");
  if (process.env.HEFIMER_SCREENSHOTS === "1") {
    await pageA.setViewport({ width: 1440, height: 1000, deviceScaleFactor: 1 });
    await pageA.screenshot({ path: "device-hub-desktop.png", fullPage: false });
    await pageA.setViewport({ width: 390, height: 844, deviceScaleFactor: 1 });
    await pageA.screenshot({ path: "device-hub-mobile.png", fullPage: false });
  }

  const contextB = await browser.createBrowserContext();
  const pageB = await contextB.newPage();
  observePage(pageB, "receiver");
  await pageB.evaluateOnNewDocument(() => localStorage.setItem("hefimer_visited", "1"));
  await pageB.goto(`${baseUrl}/?pair=${encodeURIComponent(token)}`, { waitUntil: "networkidle0" });
  await waitForText(pageB, "Pair this device");
  await clickButton(pageB, "Pair this device");
  await waitForText(pageB, "Connected devices");

  await pageA.evaluate(() => {
    window.dispatchEvent(new CustomEvent("hefimer:drop-created", {
      detail: {
        kind: "file",
        name: "device-group-smoke-test.txt",
        provider: "storage.to",
        sizeBytes: 128,
        dropCode: "54321",
        expiresAt: Date.now() + 60 * 60 * 1000,
        payload: { type: "file", fileName: "device-group-smoke-test.txt", fileUrl: "https://storage.to/example" },
      },
    }));
  });

  await new Promise((resolve) => setTimeout(resolve, 5_000));
  await clickButton(pageB, "Activity");
  await waitForText(pageB, "device-group-smoke-test.txt");
  await clickButton(pageB, "Approve");
  await new Promise((resolve) => setTimeout(resolve, 2_000));

  await clickButton(pageA, "Delete group for everyone");
  await waitForText(pageA, "Create secure group");

  console.log("Device group smoke test passed: create -> invite -> pair -> offer -> approve");
} finally {
  await browser.close();
}
