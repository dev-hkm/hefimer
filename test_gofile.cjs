const fs = require('fs');
fs.writeFileSync('test.txt', 'hello world123');
(async () => {
    const sRes = await fetch('https://api.gofile.io/servers');
    const sData = await sRes.json();
    const server = sData.data.servers[0].name;

    const FormData = require('form-data');
    const form = new FormData();
    form.append('file', fs.createReadStream('test.txt'));
    const uploadRes = await fetch(`https://${server}.gofile.io/contents/uploadfile`, {
        method: 'POST',
        body: form
    });
    const upData = await uploadRes.json();
    console.log("Upload:", upData);

    const token = upData.data.guestToken;
    const contentId = upData.data.parentFolderCode; // or id
    
    const getRes = await fetch(`https://api.gofile.io/contents/${contentId}?token=${token}`);
    const getData = await getRes.json();
    console.log("Get Content:", JSON.stringify(getData, null, 2));
    
})();
