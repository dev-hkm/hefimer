(async () => {
    const fileId = "cc8015fe-430c-463a-8118-67f250ba2d01";
    const serverName = "store-eu-par-3";
    const fileName = "test.txt";
    
    const url = `https://${serverName}.gofile.io/download/web/${fileId}/${fileName}`;
    console.log("Trying URL:", url);
    const res = await fetch(url);
    console.log("Status:", res.status);
    console.log("Headers:", res.headers.get('content-type'));
    const text = await res.text();
    console.log("Response text start:", text.substring(0, 100));
})();
