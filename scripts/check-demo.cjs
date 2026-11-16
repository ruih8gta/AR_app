const {chromium} = require('../.tools/node_modules/playwright-core');
const path = require('node:path');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const {pathToFileURL} = require('node:url');

(async()=>{
  const browser=await chromium.launch({
    executablePath:process.env.CHROMIUM_PATH || path.join(process.env.USERPROFILE,'AppData/Local/ms-playwright/chromium-1169/chrome-win/chrome.exe'),
    headless:true,args:['--use-angle=swiftshader','--enable-unsafe-swiftshader'],
  });
  try {
    const page=await browser.newPage({viewport:{width:760,height:1000},deviceScaleFactor:1});
    const errors=[];
    page.on('pageerror',e=>errors.push(e.message));
    page.on('console',e=>{if(e.type()==='error')errors.push(e.text());});
    await page.goto(pathToFileURL(path.resolve('demo/preview.html')).href);
    const frame=page.frameLocator('iframe');
    await frame.locator('#palm-safari-demo[data-ready="true"]').waitFor({timeout:60000});
    const phone=frame.locator('.phone');
    await phone.screenshot({path:'demo/dino-screenshot.png'});
    await frame.getByRole('button',{name:'動物 · キツネ'}).click();
    assert.equal(await frame.locator('[data-name]').innerText(),'キツネ');
    await frame.getByRole('button',{name:'足ぶみ',exact:true}).click();
    assert.equal(await frame.locator('[data-status]').innerText(),'足ぶみ中');
    const canvas=frame.locator('canvas');
    const moving1=await canvas.screenshot(); await page.waitForTimeout(180);
    const moving2=await canvas.screenshot(); assert(!moving1.equals(moving2),'animation changes geometry');
    await frame.getByRole('button',{name:'停止',exact:true}).click();
    await page.waitForTimeout(100);
    const stopped1=await canvas.screenshot(); await page.waitForTimeout(180);
    const stopped2=await canvas.screenshot(); assert(stopped1.equals(stopped2),'pause holds the exact pose');
    await frame.locator('[data-size]').fill('1.5');
    assert.equal(await frame.locator('[data-size-label]').innerText(),'1.5×');
    await frame.locator('[data-angle]').fill('90');
    assert.equal(await frame.locator('[data-angle-label]').innerText(),'90°');
    const rotated=await canvas.screenshot(); assert(!rotated.equals(stopped2),'size and rotation change the model');
    await frame.locator('[data-size]').fill('1');await frame.locator('[data-angle]').fill('-30');
    await phone.screenshot({path:'demo/fox-screenshot.png'});
    await page.setViewportSize({width:320,height:1000});
    await page.waitForTimeout(100);
    const overflow=await frame.locator('#palm-safari-demo').evaluate(e=>e.scrollWidth>e.clientWidth);
    assert(!overflow,'no horizontal overflow at 320px');
    await phone.screenshot({path:'demo/narrow-screenshot.png'});
    await page.emulateMedia({colorScheme:'dark'});
    await phone.screenshot({path:'demo/dark-screenshot.png'});
    assert.deepEqual(errors,[]);
    const report={result:'PASS',checks:['3D load','model switch','animation changes image','pause freezes pose','size','rotation','320px layout','dark appearance','no browser errors']};
    fs.writeFileSync('demo/verification.json',JSON.stringify(report,null,2));
    console.log(JSON.stringify(report));
  } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
