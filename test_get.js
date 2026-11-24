(async () => {
    const parentFolder = "bf210503-2bfe-4771-bd8a-25b8614a2693";
    const token = "eDR8litvAwDbfxoe83hauFJs2e1URlxA";
    
    const url = `https://api.gofile.io/contents/${parentFolder}?token=${token}`;
    const res = await fetch(url);
    const data = await res.json();
    console.log(JSON.stringify(data, null, 2));
})();
