const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:5173/public-invite/83d2dd9e-85a9-4992-9925-560242325938');
  await page.waitForTimeout(2000);
  const content = await page.content();
  if (content.includes('Minecraft Username')) {
    console.log('SUCCESS: Form rendered properly!');
  } else {
    console.log('FAILED: Form not rendered. Content:');
    console.log(content);
  }
  await browser.close();
})();
