/**
 * =========================================================================
 * GOOGLE APPS SCRIPT - REST API FOR HUB KEDIRI FLEET & SHIPMENT MANAGEMENT
 * =========================================================================
 * 
 * CARA DEPLOY:
 * 1. Buka Google Sheets Anda atau kunjungi https://script.google.com
 * 2. Buat project baru bernama "Hub Kediri REST API".
 * 3. Hapus kode default (myFunction) dan paste seluruh kode di file ini.
 * 4. Ganti SPREADSHEETS ID di bawah ini jika ingin menggunakan spreadsheet Anda sendiri.
 * 5. Klik tombol "Deploy" di kanan atas -> Pilih "New deployment".
 * 6. Pilih jenis/type "Web app".
 * 7. Konfigurasi Deployment:
 *    - Description: "Hub Kediri REST API v1"
 *    - Execute as: "Me" (Email Anda - ini wajib agar script memiliki izin menulis ke sheet)
 *    - Who has access: "Anyone" (Siapa saja, bahkan anonim - ini wajib agar aplikasi Android/React bisa mengakses tanpa OAuth prompt)
 * 8. Klik "Deploy".
 * 9. Berikan izin akses (Authorize access) ketika diminta dengan akun Google Anda.
 * 10. Copy "Web app URL" yang dihasilkan (Format URL: https://script.google.com/macros/s/XXXXX/exec)
 * 11. Masukkan URL tersebut ke menu Pengaturan (Settings) di aplikasi Android atau React Anda.
 *
 * FITUR UTAMA:
 * - Routing Berbasis Path (contoh: /exec/pengiriman, /exec/dashboard) maupun Query Parameter (?endpoint=dashboard)
 * - Dukungan penuh CORS secara otomatis via ContentService JSON
 * - Agregasi data Real-time untuk Dashboard (/dashboard)
 * - Operasi CRUD lengkap (Create, Read, Update, Delete) untuk Log Pengiriman & Armada
 * - Integrasi Asisten AI dengan context langsung dari spreadsheet Knowledge Base
 */

// SPREADSHEETS CONFIGURATION (ID Database Spreadsheet Hub Kediri)
const SPREADSHEETS = {
  PENGIRIMAN: "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw",
  PENGIRIMAN_LOG: "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw",
  ARMADA: "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw",
  AI_DATA: "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw"
};
const DRIVE_DELIVERY_FOLDER_ID = "12NyXxBBU8MOcr6so-LCrRazCQifHeSv1";

/**
 * Handle GET Requests (Read Operations)
 * Endpoint yang didukung: /pengiriman, /armada, /aidata, /dashboard, /drivers, /ban, /logs
 */
function doGet(e) {
  // Ambil rute endpoint dari pathInfo (misal: /pengiriman) atau query parameter (?endpoint=pengiriman)
  var route = "";
  if (e.pathInfo) {
    route = e.pathInfo.toLowerCase();
  } else {
    var paramRoute = e.parameter.endpoint || e.parameter.path || e.parameter.action || "";
    route = paramRoute.toLowerCase();
  }
  
  // Bersihkan karakter slash di awal dan akhir rute
  route = route.replace(/^\/+|\/+$/g, "");

  var customSpreadsheetId = e.parameter.spreadsheetId;

  try {
    // -----------------------------------------------------------------
    // 1. ENDPOINT: GET /pengiriman
    // Data bersumber dari Spreadsheet: 1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw
    // Mapping Kolom:
    // Kolom D (3): No Dokumen
    // Kolom E (4): Surat Jalan
    // Kolom H (7): Alamat
    // Kolom I (8): Remarks
    // Kolom L (11): Penerima
    // Kolom R (17): No Telp Customer
    // Kolom S (18): CBM
    // Kolom X (23): Armada
    // Kolom Y (24) & Z (25): Driver 1 & Driver 2
    // -----------------------------------------------------------------
    if (route === "pengiriman" || route === "getpengiriman") {
      var ssId = customSpreadsheetId || SPREADSHEETS.PENGIRIMAN;
      var ss = SpreadsheetApp.openById(ssId);
      var sheet = ss.getSheetByName("Pengiriman") || ss.getSheets()[0];
      var data = sheet.getDataRange().getValues();
      var headers = data[0];
      var list = [];
      
      for (var i = 1; i < data.length; i++) {
        var row = data[i];
        if (!row || row.length === 0) continue;

        var itemObj = {};
        for (var j = 0; j < headers.length; j++) {
          var hName = headers[j].toString().trim();
          itemObj[hName] = row[j];
        }

        var noDokumen = (row[3] !== undefined && row[3] !== null ? row[3].toString().trim() : "") || itemObj["Nomor dokumen"] || itemObj["noDokumen"] || "";
        var noSuratJalan = (row[4] !== undefined && row[4] !== null ? row[4].toString().trim() : "") || itemObj["Surat jalan"] || itemObj["noSuratJalan"] || "";
        var alamat = (row[7] !== undefined && row[7] !== null ? row[7].toString().trim() : "") || itemObj["Alamat"] || itemObj["tujuan"] || "";
        var remarks = (row[8] !== undefined && row[8] !== null ? row[8].toString().trim() : "") || itemObj["Remarks"] || itemObj["remaks"] || itemObj["catatan"] || "";
        var penerima = (row[11] !== undefined && row[11] !== null ? row[11].toString().trim() : "") || itemObj["Penerima"] || "";
        var noTelpCustomer = (row[17] !== undefined && row[17] !== null ? row[17].toString().trim() : "") || itemObj["Nomor telpon custumer"] || itemObj["noTelpCustomer"] || "";
        var cbmVal = row[18] !== undefined && row[18] !== null ? parseFloat(row[18]) : 0;
        if (isNaN(cbmVal)) cbmVal = 0.0;
        var armada = (row[23] !== undefined && row[23] !== null ? row[23].toString().trim() : "") || itemObj["Armada"] || "";
        var driver1 = (row[24] !== undefined && row[24] !== null ? row[24].toString().trim() : "") || itemObj["Driver 1"] || "";
        var driver2 = (row[25] !== undefined && row[25] !== null ? row[25].toString().trim() : "") || itemObj["Driver 2"] || "";
        
        var combinedDriver = driver1;
        if (driver2 && driver2 !== "-" && driver2 !== "") {
          combinedDriver = combinedDriver ? (combinedDriver + " / " + driver2) : driver2;
        }

        // Tanggal dari Kolom A (0) atau B (1) atau C (2) atau fallback ke format standar
        var tanggalStr = "";
        if (row[0]) tanggalStr = row[0].toString().trim();
        else if (row[1]) tanggalStr = row[1].toString().trim();
        else if (row[2]) tanggalStr = row[2].toString().trim();

        // Status dari kolom jika ada, fallback ke Belum Berangkat
        var statusStr = itemObj["Status"] || itemObj["status"] || (row[9] ? row[9].toString().trim() : "Belum Berangkat");

        if (noSuratJalan || noDokumen || alamat) {
          list.push({
            id: i,
            noDokumen: noDokumen,
            noSuratJalan: noSuratJalan,
            tanggal: tanggalStr,
            driver: combinedDriver || itemObj["driver"] || "-",
            driver1: driver1,
            driver2: driver2,
            armada: armada || itemObj["armada"] || "-",
            gudangAsal: itemObj["gudangAsal"] || "Gudang Kediri",
            tujuan: alamat,
            alamat: alamat,
            remarks: remarks,
            penerima: penerima,
            noTelpCustomer: noTelpCustomer,
            jumlahKoli: itemObj["jumlahKoli"] ? parseInt(itemObj["jumlahKoli"]) : 1,
            volumeCbm: cbmVal,
            status: statusStr,
            catatan: remarks
          });
        }
      }
      return jsonResponse({ success: true, endpoint: "/pengiriman", data: list });
    }

    // -----------------------------------------------------------------
    // 2. ENDPOINT: GET /armada
    // -----------------------------------------------------------------
    if (route === "armada" || route === "getarmada") {
      var ssId = customSpreadsheetId || SPREADSHEETS.ARMADA;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Armada") || SpreadsheetApp.openById(ssId).getSheets()[0];
      var data = sheet.getDataRange().getValues();
      var headers = data[0];
      var list = [];
      
      for (var i = 1; i < data.length; i++) {
        var row = data[i];
        var item = {};
        for (var j = 0; j < headers.length; j++) {
          var headerName = headers[j].toString().trim();
          item[headerName] = row[j];
        }
        if (item.armadaId) {
          list.push(item);
        }
      }
      return jsonResponse({ success: true, endpoint: "/armada", armada: list });
    }

    // -----------------------------------------------------------------
    // 3. ENDPOINT: GET /aidata
    // -----------------------------------------------------------------
    if (route === "aidata" || route === "getaidata" || route === "getaiknowledge") {
      var ssId = customSpreadsheetId || SPREADSHEETS.AI_DATA;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Knowledge") || SpreadsheetApp.openById(ssId).getSheets()[0];
      var data = sheet.getDataRange().getValues();
      var headers = data[0];
      var list = [];
      
      for (var i = 1; i < data.length; i++) {
        var row = data[i];
        var item = {};
        for (var j = 0; j < headers.length; j++) {
          var headerName = headers[j].toString().trim();
          item[headerName] = row[j];
        }
        list.push(item);
      }
      return jsonResponse({ success: true, endpoint: "/aidata", data: list });
    }

    // -----------------------------------------------------------------
    // 4. ENDPOINT: GET /dashboard (Agregasi multi-spreadsheet)
    // -----------------------------------------------------------------
    if (route === "dashboard" || route === "getdashboard") {
      return getDashboardData(customSpreadsheetId);
    }

    // -----------------------------------------------------------------
    // 5. ENDPOINTS LAINNYA (Drivers, Ban Armada, Logs)
    // -----------------------------------------------------------------
    if (route === "drivers" || route === "getdrivers") {
      var ssId = customSpreadsheetId || SPREADSHEETS.ARMADA;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Drivers");
      if (!sheet) {
        return jsonResponse({
          success: true,
          endpoint: "/drivers",
          drivers: [
            { id: "D01", name: "Driver HUB 1" },
            { id: "D02", name: "Driver HUB 2" }
          ]
        });
      }
      var data = sheet.getDataRange().getValues();
      var list = [];
      for (var i = 1; i < data.length; i++) {
        list.push({ id: data[i][0].toString(), name: data[i][1].toString() });
      }
      return jsonResponse({ success: true, endpoint: "/drivers", drivers: list });
    }

    if (route === "ban" || route === "getbanarmada") {
      var ssId = customSpreadsheetId || SPREADSHEETS.ARMADA;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Ban");
      if (!sheet) return jsonResponse({ success: true, endpoint: "/ban", banArmada: [] });
      var data = sheet.getDataRange().getValues();
      var headers = data[0];
      var list = [];
      for (var i = 1; i < data.length; i++) {
        var row = data[i];
        var item = {};
        for (var j = 0; j < headers.length; j++) {
          var headerName = headers[j].toString().trim();
          item[headerName] = row[j];
        }
        if (item.armadaId) list.push(item);
      }
      return jsonResponse({ success: true, endpoint: "/ban", banArmada: list });
    }

    if (route === "logs" || route === "getlogs") {
      var ssId = customSpreadsheetId || SPREADSHEETS.ARMADA;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Logs") || SpreadsheetApp.openById(ssId).getSheetByName("LogHarian");
      if (!sheet) return jsonResponse({ success: true, endpoint: "/logs", logs: [] });
      var data = sheet.getDataRange().getValues();
      var headers = data[0];
      var list = [];
      for (var i = 1; i < data.length; i++) {
        var row = data[i];
        var item = {};
        for (var j = 0; j < headers.length; j++) {
          var headerName = headers[j].toString().trim();
          item[headerName] = row[j];
        }
        list.push(item);
      }
      return jsonResponse({ success: true, endpoint: "/logs", logs: list });
    }

    // Default jika rute tidak terdefinisi
    return jsonResponse({ 
      success: false, 
      message: "Endpoint '" + route + "' tidak didukung.",
      availableEndpoints: ["/pengiriman", "/armada", "/aidata", "/dashboard", "/drivers", "/logs", "/ban"]
    });

  } catch (error) {
    return jsonResponse({ success: false, message: "GET Router Error: " + error.toString() });
  }
}

/**
 * Memproses data dashboard real-time dari gabungan spreadsheet Pengiriman & Armada
 */
function getDashboardData(customSpreadsheetId) {
  var summary = {
    pengiriman: {
      totalShipments: 0,
      selesai: 0,
      dalamPerjalanan: 0,
      belumBerangkat: 0,
      totalKoli: 0,
      totalVolumeCbm: 0
    },
    armada: {
      totalTrucks: 0,
      segeraServis: 0,
      aman: 0,
      serviceUnder1000: 0
    },
    drivers: {
      totalDrivers: 0
    }
  };

  try {
    // 1. Hitung statistik Pengiriman
    var ssIdPengiriman = customSpreadsheetId || SPREADSHEETS.PENGIRIMAN;
    var sheetPengiriman = SpreadsheetApp.openById(ssIdPengiriman).getSheetByName("Pengiriman") || SpreadsheetApp.openById(ssIdPengiriman).getSheets()[0];
    var dataPengiriman = sheetPengiriman.getDataRange().getValues();
    var headersPengiriman = dataPengiriman[0];
    
    var statusIdx = headersPengiriman.indexOf("status");
    var koliIdx = headersPengiriman.indexOf("jumlahKoli");
    var volumeIdx = headersPengiriman.indexOf("volumeCbm");
    
    if (statusIdx === -1) statusIdx = headersPengiriman.findIndex(h => h.toString().toLowerCase() === "status");
    if (koliIdx === -1) koliIdx = headersPengiriman.findIndex(h => h.toString().toLowerCase() === "jumlahkoli");
    if (volumeIdx === -1) volumeIdx = headersPengiriman.findIndex(h => h.toString().toLowerCase() === "volumecbm");

    for (var i = 1; i < dataPengiriman.length; i++) {
      var row = dataPengiriman[i];
      if (row[0] !== "" && row[0] !== undefined) {
        summary.pengiriman.totalShipments++;
        
        if (statusIdx !== -1 && row[statusIdx]) {
          var statusVal = row[statusIdx].toString().toLowerCase().trim();
          if (statusVal === "selesai") summary.pengiriman.selesai++;
          else if (statusVal === "dalam perjalanan") summary.pengiriman.dalamPerjalanan++;
          else if (statusVal === "belum berangkat") summary.pengiriman.belumBerangkat++;
        }
        
        if (koliIdx !== -1 && row[koliIdx] !== "") {
          var koliVal = parseInt(row[koliIdx]);
          if (!isNaN(koliVal)) summary.pengiriman.totalKoli += koliVal;
        }
        
        if (volumeIdx !== -1 && row[volumeIdx] !== "") {
          var volVal = parseFloat(row[volumeIdx]);
          if (!isNaN(volVal)) summary.pengiriman.totalVolumeCbm += volVal;
        }
      }
    }

    // 2. Hitung statistik Armada
    var ssIdArmada = customSpreadsheetId || SPREADSHEETS.ARMADA;
    var sheetArmada = SpreadsheetApp.openById(ssIdArmada).getSheetByName("Armada") || SpreadsheetApp.openById(ssIdArmada).getSheets()[0];
    var dataArmada = sheetArmada.getDataRange().getValues();
    var headersArmada = dataArmada[0];
    
    var statusArmadaIdx = headersArmada.indexOf("status");
    if (statusArmadaIdx === -1) statusArmadaIdx = headersArmada.findIndex(h => h.toString().toLowerCase() === "status");

    for (var i = 1; i < dataArmada.length; i++) {
      var row = dataArmada[i];
      if (row[0] !== "" && row[0] !== undefined) {
        summary.armada.totalTrucks++;
        
        if (statusArmadaIdx !== -1 && row[statusArmadaIdx]) {
          var statVal = row[statusArmadaIdx].toString().toUpperCase().trim();
          if (statVal.indexOf("SEGERA SERVIS") !== -1) summary.armada.segeraServis++;
          else if (statVal.indexOf("SERVICE <1000 KM") !== -1) summary.armada.serviceUnder1000++;
          else summary.armada.aman++;
        }
      }
    }

    // 3. Hitung statistik Driver
    var sheetDrivers = SpreadsheetApp.openById(ssIdArmada).getSheetByName("Drivers");
    if (sheetDrivers) {
      var dataDrivers = sheetDrivers.getDataRange().getValues();
      summary.drivers.totalDrivers = Math.max(0, dataDrivers.length - 1);
    } else {
      summary.drivers.totalDrivers = 2; // Default fallback
    }

    return jsonResponse({
      success: true,
      endpoint: "/dashboard",
      data: summary
    });
  } catch (err) {
    return jsonResponse({
      success: false,
      message: "Gagal menghitung agregasi dashboard: " + err.toString(),
      data: summary
    });
  }
}

/**
 * Handle POST Requests (Write Operations)
 * Endpoint & Aksi yang didukung: 
 * - /pengiriman (untuk add)
 * - /updatepengiriman, /deletepengiriman
 * - /login, /submitlog, /submitservicelog
 * - /asisten_ai
 */
function doPost(e) {
  var route = "";
  if (e && e.pathInfo) {
    route = e.pathInfo.toLowerCase();
  } else if (e && e.parameter) {
    var paramRoute = e.parameter.endpoint || e.parameter.path || e.parameter.action || "";
    route = paramRoute.toLowerCase();
  }
  route = route.replace(/^\/+|\/+$/g, "");

  try {
    var postData = {};
    if (e && e.postData && e.postData.contents) {
      postData = JSON.parse(e.postData.contents);
    }
    var customSpreadsheetId = e && e.parameter ? e.parameter.spreadsheetId : null;

    if (!route && postData) {
      route = (postData.action || postData.route || postData.endpoint || "").toLowerCase().replace(/^\/+|\/+$/g, "");
    }

    // -----------------------------------------------------------------
    // POST /pengiriman ATAU action=addPengiriman
    // -----------------------------------------------------------------
    if (route === "addpengiriman" || route === "pengiriman") {
      var ssId = customSpreadsheetId || SPREADSHEETS.PENGIRIMAN;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Pengiriman") || SpreadsheetApp.openById(ssId).getSheets()[0];
      
      var lastRow = sheet.getLastRow();
      var newId = 1;
      if (lastRow > 1) {
        newId = parseInt(sheet.getRange(lastRow, 1).getValue()) + 1;
        if (isNaN(newId)) newId = lastRow;
      }

      sheet.appendRow([
        newId,
        postData.noSuratJalan || "",
        postData.tanggal || "",
        postData.driver || "",
        postData.armada || "",
        postData.gudangAsal || "",
        postData.tujuan || "",
        postData.jumlahKoli || 0,
        postData.volumeCbm || 0.0,
        postData.status || "Belum Berangkat",
        postData.catatan || ""
      ]);

      return jsonResponse({ success: true, message: "Pengiriman berhasil ditambahkan!", newId: newId });
    }

    // -----------------------------------------------------------------
    // POST /updatepengiriman
    // -----------------------------------------------------------------
    if (route === "updatepengiriman") {
      var ssId = customSpreadsheetId || SPREADSHEETS.PENGIRIMAN;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Pengiriman") || SpreadsheetApp.openById(ssId).getSheets()[0];
      var data = sheet.getDataRange().getValues();
      var targetId = parseInt(postData.id);
      
      for (var i = 1; i < data.length; i++) {
        if (parseInt(data[i][0]) === targetId) {
          var rowNum = i + 1;
          if (postData.noSuratJalan !== undefined) sheet.getRange(rowNum, 2).setValue(postData.noSuratJalan);
          if (postData.tanggal !== undefined) sheet.getRange(rowNum, 3).setValue(postData.tanggal);
          if (postData.driver !== undefined) sheet.getRange(rowNum, 4).setValue(postData.driver);
          if (postData.armada !== undefined) sheet.getRange(rowNum, 5).setValue(postData.armada);
          if (postData.gudangAsal !== undefined) sheet.getRange(rowNum, 6).setValue(postData.gudangAsal);
          if (postData.tujuan !== undefined) sheet.getRange(rowNum, 7).setValue(postData.tujuan);
          if (postData.jumlahKoli !== undefined) sheet.getRange(rowNum, 8).setValue(postData.jumlahKoli);
          if (postData.volumeCbm !== undefined) sheet.getRange(rowNum, 9).setValue(postData.volumeCbm);
          if (postData.status !== undefined) sheet.getRange(rowNum, 10).setValue(postData.status);
          if (postData.catatan !== undefined) sheet.getRange(rowNum, 11).setValue(postData.catatan);

          return jsonResponse({ success: true, message: "Pengiriman berhasil diupdate!" });
        }
      }
      return jsonResponse({ success: false, message: "Data pengiriman tidak ditemukan dengan ID: " + targetId });
    }

    // -----------------------------------------------------------------
    // POST /submitterkirim ATAU action=submitterkirim
    // Simpan foto/video ke Google Drive folder ID: 1EarofgXOvxNsKVGc5XeTfOkYYV6ceGwK
    // Catat log ke Spreadsheet ID: 1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw
    // -----------------------------------------------------------------
    if (route === "submitterkirim" || route === "terkirimpengiriman" || route === "terkirim") {
      var deliveryDate = postData.tanggal || Utilities.formatDate(new Date(), "GMT+7", "yyyy-MM-dd");
      var orderNo = postData.noSuratJalan || postData.noDokumen || "";
      var folderName = orderNo ? "Bukti_Pengiriman_" + orderNo : deliveryDate.toString().replace(/\//g, "-").trim();
      if (!folderName) folderName = Utilities.formatDate(new Date(), "GMT+7", "yyyy-MM-dd");

      var uploadedUrls = [];
      if (postData.files && postData.files.length > 0) {
        try {
          var parentFolder = DriveApp.getFolderById(DRIVE_DELIVERY_FOLDER_ID);
          var subFolders = parentFolder.getFoldersByName(folderName);
          var targetFolder = subFolders.hasNext() ? subFolders.next() : parentFolder.createFolder(folderName);

          for (var f = 0; f < postData.files.length; f++) {
            var fileObj = postData.files[f];
            if (fileObj && fileObj.base64) {
              var fileBytes = Utilities.base64Decode(fileObj.base64);
              var mimeType = fileObj.mimeType || "image/jpeg";
              var isVid = mimeType.indexOf("video") >= 0;
              var ext = isVid ? ".mp4" : ".jpg";
              var fileName = fileObj.fileName || ("Bukti_" + (postData.noSuratJalan || "SJ") + "_" + (f + 1) + ext);
              var blob = Utilities.newBlob(fileBytes, mimeType, fileName);
              var driveFile = targetFolder.createFile(blob);
              try {
                driveFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
              } catch (eDrive) {}
              uploadedUrls.push(driveFile.getUrl());
            }
          }
        } catch (eFolder) {
          Logger.log("Drive Upload Error: " + eFolder.message);
        }
      }

      var fileLinksJoined = uploadedUrls.join("\n");

      // Catat ke Spreadsheet Log Pengiriman: 1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw
      var logSsId = SPREADSHEETS.PENGIRIMAN_LOG;
      var logSs = SpreadsheetApp.openById(logSsId);
      var logSheet = logSs.getSheetByName("Pengiriman_Log") || logSs.getSheetByName("LogPengiriman") || logSs.getSheetByName("Pengiriman");
      if (!logSheet) {
        logSheet = logSs.insertSheet("Pengiriman_Log");
        logSheet.appendRow([
          "Waktu Terkirim", "Tanggal Pengiriman", "No Dokumen", "No Surat Jalan", 
          "Driver", "Armada", "Alamat / Tujuan", "Penerima", "No Telp Customer", 
          "CBM", "Status", "Bukti Drive (Foto/Video)", "Catatan Driver"
        ]);
      }

      var nowTimestamp = Utilities.formatDate(new Date(), "GMT+7", "yyyy-MM-dd HH:mm:ss");
      logSheet.appendRow([
        nowTimestamp,
        postData.tanggal || "",
        postData.noDokumen || "",
        postData.noSuratJalan || "",
        postData.driver || "",
        postData.armada || "",
        postData.alamat || "",
        postData.penerima || "",
        postData.noTelpCustomer || "",
        postData.volumeCbm || 0.0,
        "TERKIRIM",
        fileLinksJoined,
        postData.catatan || ""
      ]);

      // Update status di Sheet Asal (1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw)
      try {
        var srcSsId = SPREADSHEETS.PENGIRIMAN;
        var srcSs = SpreadsheetApp.openById(srcSsId);
        var srcSheet = srcSs.getSheetByName("Pengiriman") || srcSs.getSheets()[0];
        var srcData = srcSheet.getDataRange().getValues();
        var targetRowIndex = -1;
        if (postData.id && parseInt(postData.id) > 0 && parseInt(postData.id) < srcData.length) {
          targetRowIndex = parseInt(postData.id);
        } else {
          for (var r = 1; r < srcData.length; r++) {
            var rowSJ = (srcData[r][4] || "").toString().trim();
            var rowDok = (srcData[r][3] || "").toString().trim();
            if ((postData.noSuratJalan && rowSJ === postData.noSuratJalan.trim()) || (postData.noDokumen && rowDok === postData.noDokumen.trim())) {
              targetRowIndex = r;
              break;
            }
          }
        }
        if (targetRowIndex > 0) {
          srcSheet.getRange(targetRowIndex + 1, 10).setValue("TERKIRIM");
        }
      } catch (eSrc) {
        Logger.log("Update status source error: " + eSrc.message);
      }

      return jsonResponse({
        success: true,
        message: "Pengiriman terkirim! Bukti berhasil disimpan ke Google Drive dan Spreadsheet.",
        driveUrls: uploadedUrls
      });
    }

    // -----------------------------------------------------------------
    // POST /deletepengiriman
    // -----------------------------------------------------------------
    if (route === "deletepengiriman") {
      var ssId = customSpreadsheetId || SPREADSHEETS.PENGIRIMAN;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Pengiriman") || SpreadsheetApp.openById(ssId).getSheets()[0];
      var data = sheet.getDataRange().getValues();
      var targetId = parseInt(postData.id);

      for (var i = 1; i < data.length; i++) {
        if (parseInt(data[i][0]) === targetId) {
          sheet.deleteRow(i + 1);
          return jsonResponse({ success: true, message: "Pengiriman berhasil dihapus!" });
        }
      }
      return jsonResponse({ success: false, message: "Data pengiriman tidak ditemukan dengan ID: " + targetId });
    }

    // -----------------------------------------------------------------
    // POST /login (Validasi Pin Driver)
    // -----------------------------------------------------------------
    if (route === "login") {
      var ssId = customSpreadsheetId || SPREADSHEETS.ARMADA;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Drivers") || SpreadsheetApp.openById(ssId).getSheetByName("Daftar_Driver");
      var inputDriver = (postData.driverName || "").toString().trim().toLowerCase();
      var inputPin = (postData.pin || "").toString().trim();

      if (!sheet) {
        return jsonResponse({ success: false, message: "Tab 'Drivers' atau 'Daftar_Driver' tidak ditemukan di Spreadsheet." });
      }
      var data = sheet.getDataRange().getValues();
      for (var i = 1; i < data.length; i++) {
        var driverIdOnSheet = data[i][0] ? data[i][0].toString().trim() : "";
        var driverNameOnSheet = data[i][1] ? data[i][1].toString().trim() : "";
        var pinOnSheet = data[i][2] ? data[i][2].toString().trim() : "";

        if (driverNameOnSheet.toLowerCase() === inputDriver || driverIdOnSheet.toLowerCase() === inputDriver) {
          if (pinOnSheet === inputPin) {
            return jsonResponse({ success: true, driverId: driverIdOnSheet || "D01", driverName: driverNameOnSheet || postData.driverName });
          }
        }
      }
      return jsonResponse({ success: false, message: "ID Driver / Nama atau PIN salah." });
    }

    // -----------------------------------------------------------------
    // POST /submitlog (Kirim Log Harian Driver & Update KM Armada)
    // -----------------------------------------------------------------
    if (route === "submitlog") {
      var ssId = customSpreadsheetId || SPREADSHEETS.ARMADA;
      var logsSheet = SpreadsheetApp.openById(ssId).getSheetByName("Logs") || SpreadsheetApp.openById(ssId).getSheetByName("LogHarian");
      var logData = postData.logData;
      
      // Catat log harian baru
      var now = new Date();
      var formattedDate = Utilities.formatDate(now, "GMT+7", "dd MMMM yyyy HH:mm") + " WIB";
      logsSheet.appendRow([
        formattedDate,
        logData.armadaId,
        logData.kmTerdeteksi,
        logData.base64Photo ? "ImageUploaded" : "", 
        logData.catatan || "",
        logData.driverName
      ]);

      // Update parameter "KM Saat Ini" di daftar Armada
      var armadaSheet = SpreadsheetApp.openById(ssId).getSheetByName("Armada");
      var armadaData = armadaSheet.getDataRange().getValues();
      var sisaKm = 5000;
      var serviceAlert = false;

      for (var i = 1; i < armadaData.length; i++) {
        if (armadaData[i][0].toString().trim() === logData.armadaId) {
          var rowNum = i + 1;
          armadaSheet.getRange(rowNum, 3).setValue(logData.kmTerdeteksi); // kmSaatIni
          
          var kmServis = parseInt(armadaData[i][3]); // kmServiceTerakhir
          var interval = parseInt(armadaData[i][4]); // intervalService
          var nextServis = kmServis + interval;
          sisaKm = nextServis - logData.kmTerdeteksi;
          
          armadaSheet.getRange(rowNum, 6).setValue(nextServis);
          armadaSheet.getRange(rowNum, 7).setValue(sisaKm);
          
          var status = sisaKm <= 0 ? "⚠️ SEGERA SERVIS" : (sisaKm < 1000 ? "⚠️ SERVICE <1000 KM" : "AMAN");
          armadaSheet.getRange(rowNum, 8).setValue(status);
          serviceAlert = sisaKm < 1000;
        }
      }

      return jsonResponse({
        success: true,
        message: "Log harian armada berhasil terekam!",
        sisaKm: sisaKm,
        serviceAlert: serviceAlert,
        linkFoto: ""
      });
    }

    // -----------------------------------------------------------------
    // POST /submitservicelog (Selesai Servis Rutin Armada)
    // -----------------------------------------------------------------
    if (route === "submitservicelog") {
      var ssId = customSpreadsheetId || SPREADSHEETS.ARMADA;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Armada");
      var data = sheet.getDataRange().getValues();
      var armadaId = postData.armadaId;
      var kmServis = parseInt(postData.kmServis);
      var catatan = postData.catatan || "";

      for (var i = 1; i < data.length; i++) {
        if (data[i][0].toString().trim() === armadaId) {
          var rowNum = i + 1;
          sheet.getRange(rowNum, 4).setValue(kmServis); // kmServiceTerakhir
          
          var interval = parseInt(data[i][4]); // intervalService
          var nextServis = kmServis + interval;
          var kmSaatIni = parseInt(data[i][2]); // kmSaatIni
          var sisaKm = nextServis - kmSaatIni;
          
          sheet.getRange(rowNum, 6).setValue(nextServis);
          sheet.getRange(rowNum, 7).setValue(sisaKm);
          
          var status = sisaKm <= 0 ? "⚠️ SEGERA SERVIS" : (sisaKm < 1000 ? "⚠️ SERVICE <1000 KM" : "AMAN");
          sheet.getRange(rowNum, 8).setValue(status);
          if (catatan) {
            sheet.getRange(rowNum, 11).setValue(catatan);
          }

          // Catat di log harian sebagai log servis
          var logsSheet = SpreadsheetApp.openById(ssId).getSheetByName("Logs") || SpreadsheetApp.openById(ssId).getSheetByName("LogHarian");
          if (logsSheet) {
            var now = new Date();
            var formattedDate = Utilities.formatDate(now, "GMT+7", "dd MMMM yyyy HH:mm") + " WIB";
            logsSheet.appendRow([
              formattedDate,
              armadaId,
              kmSaatIni,
              "",
              "⚙️ SELESAI SERVIS RUTIN (KM " + kmServis + "). Catatan: " + catatan,
              "Sistem Servis"
            ]);
          }

          return jsonResponse({ success: true, message: "Servis armada berhasil terekam dan parameter KM direset!" });
        }
      }
      return jsonResponse({ success: false, message: "ArmadaId tidak ditemukan!" });
    }

    // -----------------------------------------------------------------
    // POST /updateban ATAU /update_ban (Update Barcode & Informasi Ban Armada)
    // CATATAN: Barcode Ban ditempatkan di KOLOM D (Kolom 4) pada sheet "Ban"
    // -----------------------------------------------------------------
    if (route === "updateban" || route === "update_ban") {
      var ssId = customSpreadsheetId || SPREADSHEETS.ARMADA;
      var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Ban");
      if (!sheet) {
        return jsonResponse({ success: false, message: "Sheet Ban tidak ditemukan." });
      }

      var banData = postData.banData || postData;
      var targetArmadaId = banData.armadaId;
      var targetPosisi = banData.posisi;

      var data = sheet.getDataRange().getValues();
      var headers = data[0].map(function(h) { return h.toString().trim().toLowerCase(); });

      // Indeks kolom dinamis berdasar header dengan fallback ke standar:
      // A (1): armadaId, B (2): noPolisi, C (3): posisi, D (4): barcode, E (5): noSeri, F (6): ukuran, G (7): merk, H (8): kondisi, I (9): tekanan, J (10): keterangan, K (11): tahun
      var colArmada = headers.indexOf("armadaid") !== -1 ? headers.indexOf("armadaid") + 1 : 1;
      var colPosisi = headers.indexOf("posisi") !== -1 ? headers.indexOf("posisi") + 1 : 3;
      var colBarcode = headers.indexOf("barcode") !== -1 ? headers.indexOf("barcode") + 1 : 4; // Kolom D
      var colTahun = headers.indexOf("tahun") !== -1 ? headers.indexOf("tahun") + 1 : 11;
      var colKondisi = headers.indexOf("kondisi") !== -1 ? headers.indexOf("kondisi") + 1 : 8;
      var colTekanan = headers.indexOf("tekanan") !== -1 ? headers.indexOf("tekanan") + 1 : 9;
      var colKeterangan = headers.indexOf("keterangan") !== -1 ? headers.indexOf("keterangan") + 1 : 10;

      for (var i = 1; i < data.length; i++) {
        var rowArmada = data[i][colArmada - 1] ? data[i][colArmada - 1].toString().trim() : "";
        var rowPosisi = data[i][colPosisi - 1] ? data[i][colPosisi - 1].toString().trim() : "";

        if (rowArmada.toLowerCase() === targetArmadaId.toString().trim().toLowerCase() &&
            rowPosisi.toLowerCase() === targetPosisi.toString().trim().toLowerCase()) {
          var rowNum = i + 1;

          if (banData.barcode !== undefined) sheet.getRange(rowNum, colBarcode).setValue(banData.barcode);
          if (banData.tahun !== undefined) sheet.getRange(rowNum, colTahun).setValue(banData.tahun);
          if (banData.kondisi !== undefined) sheet.getRange(rowNum, colKondisi).setValue(banData.kondisi);
          if (banData.tekanan !== undefined) sheet.getRange(rowNum, colTekanan).setValue(banData.tekanan);
          if (banData.keterangan !== undefined) sheet.getRange(rowNum, colKeterangan).setValue(banData.keterangan);

          return jsonResponse({
            success: true,
            message: "Data Ban berhasil diupdate! Barcode disimpan di Kolom D.",
            updatedData: banData
          });
        }
      }
      return jsonResponse({ success: false, message: "Data ban tidak ditemukan untuk armada " + targetArmadaId + " posisi " + targetPosisi });
    }

    // -----------------------------------------------------------------
    // POST /asisten_ai (Integrasi Asisten Gemini API dengan Konteks Spreadsheet)
    // -----------------------------------------------------------------
    if (route === "asisten_ai" || route === "asisten-ai") {
      var res = callGeminiWithContext(
        postData.chatMessage || "",
        customSpreadsheetId || SPREADSHEETS.ARMADA,
        postData.apiKey
      );
      return jsonResponse(res);
    }

    return jsonResponse({ success: false, message: "Endpoint POST '" + route + "' tidak didukung." });
  } catch (error) {
    return jsonResponse({ success: false, message: "POST Router Error: " + error.toString() });
  }
}

/**
 * ============================================
 * GEMINI AI INTEGRATION - GOOGLE APPS SCRIPT
 * ============================================
 */

var GEMINI_API_KEY = PropertiesService.getScriptProperties().getProperty("GEMINI_API_KEY") || "";

/**
 * Fungsi utama untuk memanggil Gemini API dengan konteks data armada
 */
function callGeminiWithContext(chatMessage, spreadsheetId, passedApiKey) {
  var apiKey = passedApiKey || GEMINI_API_KEY;
  
  if (!apiKey || apiKey === "" || apiKey === "MY_GEMINI_API_KEY") {
    return { 
      success: false, 
      message: "API Key Gemini belum dikonfigurasi. Silakan isi API Key di Pengaturan aplikasi." 
    };
  }

  try {
    // 1. Ambil semua data konteks dari spreadsheet
    var contextData = buildArmadaContext(spreadsheetId);
    
    // 2. Buat prompt lengkap dengan konteks
    var fullPrompt = buildGeminiPrompt(chatMessage, contextData);
    
    // 3. Panggil Gemini API
    var result = callGeminiAPI(fullPrompt, apiKey);
    
    return { success: true, message: result };
    
  } catch (e) {
    if (typeof Log === "function") {
      Log("Gemini Error: " + e.toString());
    }
    return { success: false, message: "Gemini Apps Script Error: " + e.toString() };
  }
}

/**
 * Mengumpulkan data armada dari semua sheet untuk konteks AI
 */
function buildArmadaContext(spreadsheetId) {
  var ss = SpreadsheetApp.openById(spreadsheetId || SPREADSHEETS.ARMADA);
  var context = [];
  
  // Ambil data Armada
  try {
    var armadaSheet = ss.getSheetByName("Armada") || ss.getSheets()[0];
    if (armadaSheet) {
      var armadaData = armadaSheet.getDataRange().getValues();
      context.push("=== DATA ARMADA ===");
      for (var i = 1; i < armadaData.length && i <= 20; i++) {
        if (armadaData[i][0]) {
          context.push(
            "ID: " + armadaData[i][0] + 
            " | Nopol: " + (armadaData[i][1] || "-") +
            " | KM: " + (armadaData[i][2] || "0") +
            " | Sisa Servis: " + (armadaData[i][6] || "0") + " km" +
            " | Status: " + (armadaData[i][7] || "-")
          );
        }
      }
    }
  } catch(e) {}
  
  // Ambil data Driver
  try {
    var driverSheet = ss.getSheetByName("Drivers") || ss.getSheetByName("Daftar_Driver");
    if (driverSheet) {
      var driverData = driverSheet.getDataRange().getValues();
      context.push("\n=== DATA DRIVER ===");
      for (var i = 1; i < driverData.length && i <= 20; i++) {
        if (driverData[i][1]) {
          context.push("ID: " + driverData[i][0] + " | Nama: " + driverData[i][1]);
        }
      }
    }
  } catch(e) {}
  
  // Ambil data Log Harian (7 hari terakhir)
  try {
    var logSheet = ss.getSheetByName("Logs") || ss.getSheetByName("log_harian") || ss.getSheetByName("LogHarian");
    if (logSheet) {
      var logData = logSheet.getDataRange().getValues();
      context.push("\n=== LOG HARIAN TERAKHIR ===");
      var startIdx = Math.max(1, logData.length - 10);
      for (var i = startIdx; i < logData.length; i++) {
        if (logData[i][1]) {
          context.push(
            "Tanggal: " + logData[i][0] + 
            " | Armada: " + logData[i][1] +
            " | KM: " + logData[i][2] +
            " | Driver: " + (logData[i][5] || "-")
          );
        }
      }
    }
  } catch(e) {}
  
  return context.join("\n");
}

/**
 * Membangun prompt lengkap untuk Gemini
 */
function buildGeminiPrompt(userMessage, contextData) {
  var systemPrompt = [
    "Kamu adalah JONI, asisten manajemen armada kendaraan komersial HUB Kediri.",
    "",
    "KARAKTER:",
    "- Tegas, profesional, tidak basa-basi",
    "- Bahasa Indonesia padat dan jelas",
    "- Gunakan istilah teknis otomotif yang tepat",
    "- Selalu akhiri dengan rekomendasi tindakan konkret",
    "",
    "KEAHLIAN KENDARAAN:",
    "HINO PICKUP: Oli & filter tiap 10.000-15.000 km, fuel filter 20.000-30.000 km, air filter 15.000 km, transmisi 40.000-60.000 km, coolant 60.000 km, rem 10.000 km, ban 15.000-20.000 km.",
    "DAIHATSU PICKUP: Oli tiap 3.000-5.000 km, coolant bulanan, rem 15.000 km, ban 8.000 km, aki 3-4 tahun, filter udara rutin, busi tiap service besar, belt tiap tahun.",
    "",
    "ATURAN:",
    "1. JANGAN pernah buat data palsu",
    "2. JANGAN beri saran mekanik berbahaya",
    "3. Format tanggal: DD/MM/YYYY",
    "4. Format uang: Rp X.XXX.XXX",
    "5. Jika data tidak tersedia, katakan 'Data tidak tersedia di sistem.'",
    ""
  ].join("\n");
  
  return systemPrompt + "\n" + contextData + "\n\n=== PERTANYAAN USER ===\n" + userMessage;
}

/**
 * Memanggil Gemini API langsung
 */
function callGeminiAPI(prompt, apiKey) {
  var url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;
  
  var payload = {
    contents: [{
      parts: [{ text: prompt }]
    }],
    generationConfig: {
      temperature: 0.7,
      maxOutputTokens: 2048,
      topP: 0.95
    }
  };
  
  var options = {
    method: "post",
    contentType: "application/json",
    payload: JSON.stringify(payload),
    muteHttpExceptions: true
  };
  
  var response = UrlFetchApp.fetch(url, options);
  var responseCode = response.getResponseCode();
  var responseText = response.getContentText();
  
  if (responseCode !== 200) {
    throw new Error("Gemini API Error " + responseCode + ": " + responseText);
  }
  
  var result = JSON.parse(responseText);
  
  // Handle safety filter
  if (result.promptFeedback && result.promptFeedback.blockReason) {
    return "Maaf, saya tidak dapat menjawab pertanyaan tersebut karena alasan keamanan (" + result.promptFeedback.blockReason + ").";
  }
  
  if (!result.candidates || result.candidates.length === 0) {
    throw new Error("Respons Gemini kosong");
  }
  
  var candidate = result.candidates[0];
  
  // Handle finish reason
  if (candidate.finishReason && candidate.finishReason !== "STOP") {
    if (typeof Log === "function") {
      Log("Gemini finish reason: " + candidate.finishReason);
    }
  }
  
  return candidate.content.parts[0].text;
}

/**
 * Format standard output JSON response dengan dukungan CORS secara default
 */
function jsonResponse(obj) {
  var output = ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
    
  // Google Apps Script Web Apps secara default menyertakan header CORS 
  // (Access-Control-Allow-Origin: *) pada semua respons JSON dari ContentService.
  return output;
}
