import { writeFileSync } from 'fs';

writeFileSync('test.txt', 'hello world123');
(async () => {
    try {
        const sRes = await fetch('https://api.gofile.io/servers');
        const sData = await sRes.json();
        const server = sData.data.servers[0].name;

        // Create a blob
        const fileContent = "hello world123";
        const blob = new Blob([fileContent], { type: 'text/plain' });
        
        const form = new FormData();
        form.append('file', blob, 'test.txt');
        
        const uploadRes = await fetch(`https://${server}.gofile.io/contents/uploadfile`, {
            method: 'POST',
            body: form
        });
        const upData = await uploadRes.json();
        console.log("Upload response:", JSON.stringify(upData, null, 2));
    } catch (e) {
        console.error(e);
    }
})();
