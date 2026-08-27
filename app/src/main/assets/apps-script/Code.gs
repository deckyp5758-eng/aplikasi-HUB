/**
 * GOOGLE APPS SCRIPT BACKEND FOR FLEET MANAGEMENT & SURAT JALAN PENGIRIMAN
 * HUB KEDIRI SYSTEM
 */

// ============================================
// KONFIGURASI & KONSTANTA
// ============================================

var DEFAULT_SPREADSHEET_ID = "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw";
var GID_ARMADA = 1850941825;
var GID_AI_DATA = 888604592;
var GID_PENGAJUAN = 1517362778;
var GID_CATATAN_DRIVER = 1562754278;
var GID_LOG_HARIAN = 1263706817;
var GID_ODOMETER_KM = 1263706817;
var GID_DAFTAR_DRIVER = 479314622;
var GID_KIR_PAJAK = 2062052578;
var GID_BAN = 817527065;
var GID_AKI = 1886867333;
var GID_ARSIP_PENGIRIMAN = 1878433267;
var GID_SURAT_JALAN = 1878433267; // GID Arsip Bukti Pengiriman
var FOLDER_ID_PENGIRIMAN = "12NyXxBBU8MOcr6so-LCrRazCQifHeSv1";
var FOLDER_ID_KM = "1ZpPEGaVCz0qmu37r_Eq8H8701a3bOS76";
var FOLDER_ID_PENGAJUAN = "1Kk9f5f8_o8puwA3ZNJAKa_9cVy5TN5Lh";

function getAdminEmail() {
  return PropertiesService.getScriptProperties().getProperty("ADMIN_EMAIL") || "deckyp5758@gmail.com";
}

// ============================================
// HELPER FUNCTIONS UTAMA (INFRASTRUKTUR & OPTIMASI)
// ============================================

/**
 * Membaca seluruh sheet dalam spreadsheet SEKALI SAJA
 * Mengembalikan dictionary/map berdasarkan GID dan Nama Sheet (UPPERCASE)
 */
function getSheetMap(spreadsheet) {
  var map = {
    byGid: {},
    byName: {},
    sheets: []
  };
  if (!spreadsheet) return map;
  try {
    var sheets = spreadsheet.getSheets();
    map.sheets = sheets;
    for (var i = 0; i < sheets.length; i++) {
      var s = sheets[i];
      var gidStr = String(s.getSheetId());
      var nameUpper = s.getName().toUpperCase().replace(/\s+/g, " ").trim();
      map.byGid[gidStr] = s;
      map.byName[nameUpper] = s;
      map[gidStr] = s;
      map[nameUpper] = s;
    }
  } catch (e) {
    Logger.log("getSheetMap error: " + e.toString());
  }
  return map;
}

/**
 * Menulis 1 baris/range nilai ke sheet secara BATCH 1x RPC call setValues()
 */
function batchWriteRow(sheet, rowIndex, startCol, valuesArray) {
  if (!sheet || !valuesArray) return;
  var matrix = Array.isArray(valuesArray[0]) ? valuesArray : [valuesArray];
  if (matrix.length === 0 || matrix[0].length === 0) return;
  sheet.getRange(rowIndex, startCol, matrix.length, matrix[0].length).setValues(matrix);
}

/**
 * Standardisasi format output JSON dengan timestamp
 */
function jsonResponse(success, message, data) {
  var responseObj = {};
  if (typeof success === "object" && success !== null) {
    responseObj = success;
    if (responseObj.timestamp === undefined) {
      responseObj.timestamp = new Date().getTime();
    }
  } else {
    responseObj = {
      success: Boolean(success),
      message: message || "",
      data: data !== undefined ? data : null,
      timestamp: new Date().getTime()
    };
  }
  return ContentService.createTextOutput(JSON.stringify(responseObj)).setMimeType(ContentService.MimeType.JSON);
}

/**
 * Helper internal untuk ambil sheet berdasarkan Nama dari sheetMap/ss
 */
function getSheetByNameFromMap(ss, sheetMap, name) {
  if (!name) return null;
  var nameUpper = String(name).toUpperCase().replace(/\s+/g, " ").trim();
  if (sheetMap && sheetMap.byName && sheetMap.byName[nameUpper]) {
    return sheetMap.byName[nameUpper];
  }
  if (sheetMap && sheetMap[nameUpper] && typeof sheetMap[nameUpper].getName === "function") {
    return sheetMap[nameUpper];
  }
  if (ss && typeof ss.getSheetByName === "function") {
    return ss.getSheetByName(name);
  }
  return null;
}

// ============================================
// HANDLER UTAMA (doGet, doPost)
// ============================================

function doGet(e) {
  try {
    var ss = getSpreadsheet(e);
    var sheetMap = getSheetMap(ss);
    var action = (e && e.parameter && e.parameter.action) ? e.parameter.action : "getDrivers";

    Logger.log("doGet called. Action: " + action + ", Spreadsheet ID: " + (ss ? ss.getId() : "null"));

    if (action === "getDrivers") {
      return jsonResponse({ success: true, drivers: getDrivers(ss, sheetMap) });
    } else if (action === "getArmada" || action === "getKirPajak") {
      return jsonResponse({ success: true, armada: getArmada(ss, sheetMap) });
    } else if (action === "getLogs") {
      return jsonResponse({ success: true, logs: getLogs(ss, 100, sheetMap) });
    } else if (action === "getBanArmada") {
      return jsonResponse({ success: true, banArmada: getBanArmada(ss, sheetMap) });
    } else if (action === "getPengajuan" || action === "get_pengajuan") {
      return jsonResponse({ success: true, data: getPengajuan(ss, 100, sheetMap) });
    } else if (action === "getPengiriman") {
      return jsonResponse({ success: true, data: getPengiriman(ss, sheetMap) });
    } else if (action === "getAiKnowledge") {
      return jsonResponse({ success: true, data: getAiKnowledge(ss, sheetMap) });
    } else if (action === "checkUpdate" || action === "check_update" || action === "getAppUpdate") {
      return jsonResponse({
        success: true,
        latestVersionCode: 1,
        latestVersionName: "1.0.0",
        apkDownloadUrl: "https://github.com/deckyp5758-eng/aplikasi-HUB/releases/latest/download/app-release.apk",
        forceUpdate: true,
        changelog: "Pembaruan sistem validasi login driver dan peningkatan stabilitas aplikasi."
      });
    } else if (action === "setupAllSheets" || action === "setupSheets" || action === "setup_sheets") {
      return jsonResponse(setupAllSheets(ss));
    } else if (action === "debug" || action === "debugSheets") {
      var sheetsInfo = [];
      var allSheets = sheetMap.sheets || ss.getSheets();
      for (var s = 0; s < allSheets.length; s++) {
        var sh = allSheets[s];
        sheetsInfo.push({
          index: s,
          name: sh.getName(),
          gid: sh.getSheetId(),
          lastRow: sh.getLastRow(),
          lastColumn: sh.getLastColumn()
        });
      }

      return jsonResponse({
        success: true,
        spreadsheetId: ss.getId(),
        spreadsheetName: ss.getName(),
        allSheets: sheetsInfo
      });
    }

    // Default fallback HTML view for Web App deploy check
    return HtmlService.createHtmlOutput(
      "<html><head><style>" +
      "body { font-family: sans-serif; padding: 30px; background: #f4f6f9; color: #333; }" +
      ".card { background: white; padding: 20px; border-radius: 8px; border-top: 4px solid #0054A6; max-width: 600px; margin: 0 auto; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
      "h2 { color: #0054A6; margin-top: 0; }" +
      ".badge { background: #e3f2fd; color: #0d47a1; padding: 8px 12px; border-radius: 4px; font-weight: bold; display: inline-block; margin-bottom: 15px; }" +
      "</style></head><body>" +
      "<div class='card'>" +
      "<h2>HUB KEDIRI Apps Script Backend Active!</h2>" +
      "<div class='badge'>Status: Online & Ready</div>" +
      "<p>Web App Service ini aktif untuk menyinkronkan data Armada, Driver, Surat Jalan, dan Log Harian dengan Aplikasi Android.</p>" +
      "</div></body></html>"
    ).setTitle("HUB KEDIRI Fleet API").setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);

  } catch (err) {
    return jsonResponse(false, err.message || err.toString(), null);
  }
}

function doPost(e) {
  try {
    var contents = {};
    if (e && e.postData && e.postData.contents) {
      try {
        contents = JSON.parse(e.postData.contents);
      } catch(parseErr) {}
    }

    var ss = getSpreadsheet(e, contents);
    var sheetMap = getSheetMap(ss);
    var action = contents.action || (e && e.parameter && e.parameter.action) || "";

    Logger.log("doPost called. Action: " + action + ", Spreadsheet ID: " + (ss ? ss.getId() : "null"));

    if (action === "getDrivers") {
      return jsonResponse({ success: true, drivers: getDrivers(ss, sheetMap) });
    } else if (action === "getArmada" || action === "getKirPajak") {
      return jsonResponse({ success: true, armada: getArmada(ss, sheetMap) });
    } else if (action === "getLogs") {
      return jsonResponse({ success: true, logs: getLogs(ss, 100, sheetMap) });
    } else if (action === "getBanArmada") {
      return jsonResponse({ success: true, banArmada: getBanArmada(ss, sheetMap) });
    } else if (action === "getPengajuan" || action === "get_pengajuan") {
      return jsonResponse({ success: true, data: getPengajuan(ss, 100, sheetMap) });
    } else if (action === "getPengiriman") {
      return jsonResponse({ success: true, data: getPengiriman(ss, sheetMap) });
    } else if (action === "getAiKnowledge") {
      return jsonResponse({ success: true, data: getAiKnowledge(ss, sheetMap) });
    } else if (action === "login") {
      return jsonResponse(validateLogin(contents, ss, sheetMap));
    } else if (action === "submitLog") {
      return jsonResponse(submitLog(contents, ss, sheetMap));
    } else if (action === "submitservicelog" || action === "submit_service") {
      return jsonResponse(submitService(contents, ss, sheetMap));
    } else if (action === "update_aki" || action === "updateAki") {
      return jsonResponse(updateAki(contents, ss, sheetMap));
    } else if (action === "updateban" || action === "update_ban") {
      return jsonResponse(updateBan(contents, ss, sheetMap));
    } else if (action === "submitterkirim" || action === "submitTerkirim" || action === "submit_terkirim") {
      return jsonResponse(submitTerkirim(contents, ss, sheetMap));
    } else if (action === "updateFotoArmada" || action === "update_foto_armada") {
      return jsonResponse(updateFotoArmada(contents, ss, sheetMap));
    } else if (action === "submit_catatan_driver" || action === "submitCatatanDriver") {
      return jsonResponse(submitCatatanDriver(contents, ss, sheetMap));
    } else if (action === "clear_catatan_driver" || action === "clearCatatanDriver") {
      return jsonResponse(clearCatatanDriver(contents, ss, sheetMap));
    } else if (action === "submitPengajuan" || action === "submit_pengajuan") {
      return jsonResponse(submitPengajuan(contents, ss, sheetMap));
    } else if (action === "setupAllSheets" || action === "setupSheets" || action === "setup_sheets") {
      return jsonResponse(setupAllSheets(ss));
    } else if (action === "performOcr" || action === "ocr" || action === "extractKmFromImage") {
      var b64Ocr = contents.base64Data || contents.base64Photo || contents.base64 || contents.image || "";
      return jsonResponse(extractKmFromImage(b64Ocr));
    } else if (action === "asisten_ai") {
      return jsonResponse(handleAsistenAi(contents));
    } else {
      return jsonResponse(false, "Aksi tidak dikenal: " + action, null);
    }
  } catch (err) {
    return jsonResponse(false, err.message || err.toString(), null);
  }
}

// ============================================
// API ENDPOINTS (getDrivers, getArmada, getLogs, getBanArmada, getKirPajak, getPengiriman)
// ============================================

function getDrivers(ss, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var sheet = getSheetByGid(sheetMap, GID_DAFTAR_DRIVER) || 
              getSheetByGid(ss, GID_DAFTAR_DRIVER) || 
              getSheetByNameFromMap(ss, sheetMap, "Daftar_Driver") || 
              getSheetByNameFromMap(ss, sheetMap, "DRIVERS") || 
              getSheetByNameFromMap(ss, sheetMap, "Driver");
  var drivers = [];
  if (sheet) {
    var data = sheet.getDataRange().getValues();
    for (var i = 1; i < data.length; i++) {
      if (data[i][0] || data[i][1]) {
        drivers.push({
          id: String(data[i][0] || ("D0" + i)),
          name: String(data[i][1] || data[i][0] || "").trim()
        });
      }
    }
  }
  if (drivers.length === 0) {
    drivers = [
      { id: "D01", name: "Driver HUB 1" },
      { id: "D02", name: "Driver HUB 2" }
    ];
  }
  return drivers;
}

function getKirPajakMap(ss, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var map = {};
  var sheet = getSheetByGid(sheetMap, GID_KIR_PAJAK) || 
              getSheetByNameFromMap(ss, sheetMap, "KIR Pajak Armada") || 
              getSheetByNameFromMap(ss, sheetMap, "KIR Pajak") || 
              getSheetByNameFromMap(ss, sheetMap, "KIR_Pajak") || 
              getSheetByNameFromMap(ss, sheetMap, "KIR");
  if (!sheet) return map;

  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return map;

  var colId = 0, colPol = 1, colKir = 2, colPajakTahunan = 3, colPajak5Tahunan = 4;
  var header = data[0];
  if (header && header.length > 0) {
    for (var c = 0; c < header.length; c++) {
      var h = String(header[c] || "").trim().toLowerCase();
      if (h.indexOf("foto") > -1 || h.indexOf("gambar") > -1) continue;
      if (h.indexOf("id") > -1 || h.indexOf("armada") > -1) colId = c;
      else if (h.indexOf("polisi") > -1 || h.indexOf("nomer") > -1 || h.indexOf("no pol") > -1) colPol = c;
      else if (h.indexOf("5 tahun") > -1 || h.indexOf("5th") > -1 || h.indexOf("stnk") > -1) colPajak5Tahunan = c;
      else if (h.indexOf("pajak") > -1 || h.indexOf("tahunan") > -1) colPajakTahunan = c;
      else if (h === "kir" || h.indexOf("kir") > -1) colKir = c;
    }
  }

  for (var k = 1; k < data.length; k++) {
    var row = data[k];
    if (!row) continue;
    var kArmadaId = String(row[colId] || "").trim();
    var kNoPol = String(row[colPol] || kArmadaId).trim();
    if (!kArmadaId && !kNoPol) continue;

    var item = {
      armadaId: kArmadaId,
      noPolisi: kNoPol,
      pajakTahunan: formatDateVal(row[colPajakTahunan]),
      kir: formatDateVal(row[colKir]),
      pajak5Tahunan: formatDateVal(row[colPajak5Tahunan])
    };

    if (kArmadaId) map[kArmadaId.toUpperCase()] = item;
    if (kNoPol) map[kNoPol.toUpperCase()] = item;
  }
  return map;
}

function getArmada(ss, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var sheet = getSheetByGid(sheetMap, GID_ARMADA) || 
              getSheetByGid(ss, GID_ARMADA) || 
              getSheetByNameFromMap(ss, sheetMap, "ARMADA") || 
              getSheetByNameFromMap(ss, sheetMap, "Armada");
  var kirMap = getKirPajakMap(ss, sheetMap);
  var armadaList = [];

  // Foto profil armada disimpan langsung pada kolom L (index 11).
  // Kolom ini dipisahkan dari FOTO KM pada kolom J.
  var fotoProfileCol = 11;

  if (sheet) {
    var data = sheet.getDataRange().getValues();
    for (var i = 1; i < data.length; i++) {
      if (data[i][0]) {
        var id = String(data[i][0]).trim().toUpperCase();
        var nopol = data[i][1] ? String(data[i][1]).trim().toUpperCase() : "";
        var extra = kirMap[id] || kirMap[nopol] || {};
        var profileFoto = (data[i][fotoProfileCol] !== undefined && data[i][fotoProfileCol] !== null)
          ? String(data[i][fotoProfileCol] || "").trim()
          : "";
        var noteVal = String(data[i][10] || data[i][9] || "");

        var kmSaatIni = Number(data[i][2]) || 0;
        var kmServiceTerakhir = Number(data[i][3]) || 0;
        var intervalService = Number(data[i][4]) || 5000;
        var kmServiceBerikutnya = Number(data[i][5]) || (kmServiceTerakhir + intervalService);
        var sisaKm = (data[i][6] !== undefined && data[i][6] !== "") ? Number(data[i][6]) : Math.max(0, kmServiceBerikutnya - kmSaatIni);

        armadaList.push({
          armadaId: String(data[i][0]),
          noPolisi: nopol,
          kmSaatIni: kmSaatIni,
          kmServiceTerakhir: kmServiceTerakhir,
          intervalService: intervalService,
          kmServiceBerikutnya: kmServiceBerikutnya,
          sisaKm: sisaKm,
          status: data[i][7] ? String(data[i][7]) : "Siap",
          flag: data[i][8] ? String(data[i][8]) : "",
          fotoKm: data[i][9] ? String(data[i][9]) : "",
          catatan: noteVal,
          catattan: noteVal,
          pajakTahunan: extra.pajakTahunan || formatDateVal(data[i][5] || ""),
          kir: extra.kir || formatDateVal(data[i][6] || ""),
          pajak5Tahunan: extra.pajak5Tahunan || formatDateVal(data[i][7] || ""),
          fotoTruck: profileFoto
        });
      }
    }
  }
  return armadaList;
}

function getLogs(ss, limit, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  limit = limit || 100;
  var sheet = getSheetByGid(sheetMap, GID_LOG_HARIAN) || 
              getSheetByGid(ss, GID_LOG_HARIAN) || 
              getSheetByNameFromMap(ss, sheetMap, "log_harian") || 
              getSheetByNameFromMap(ss, sheetMap, "LOG_HARIAN") || 
              getSheetByGid(sheetMap, GID_SURAT_JALAN);
  var logsList = [];
  if (sheet) {
    var lastRow = sheet.getLastRow();
    var lastCol = sheet.getLastColumn();
    if (lastRow >= 2 && lastCol >= 1) {
      var startRow = Math.max(2, lastRow - limit + 1);
      var numRows = lastRow - startRow + 1;
      var data = sheet.getRange(startRow, 1, numRows, lastCol).getDisplayValues();
      for (var i = 0; i < data.length; i++) {
        if (data[i][0] || data[i][1]) {
          logsList.push({
            tanggal: formatDateVal(data[i][0]),
            armadaId: String(data[i][1] || data[i][4] || ""),
            kmTerdetect: Number(data[i][2]) || 0,
            kmTerdeteksi: Number(data[i][2]) || 0,
            linkFoto: String(data[i][3] || data[i][8] || ""),
            catatan: String(data[i][4] || data[i][10] || ""),
            namaDriver: String(data[i][5] || data[i][3] || "")
          });
        }
      }
    }
  }
  return logsList;
}

function getBanArmada(ss, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var banSheet = getSheetByGid(sheetMap, GID_BAN) || 
                 getSheetByGid(ss, GID_BAN) || 
                 getSheetByNameFromMap(ss, sheetMap, "BAN ARMADA") || 
                 getSheetByNameFromMap(ss, sheetMap, "Ban armada") || 
                 getSheetByNameFromMap(ss, sheetMap, "Ban Armada") || 
                 getSheetByNameFromMap(ss, sheetMap, "BAN");
  var banList = [];
  if (banSheet) {
    var data = banSheet.getDataRange().getValues();
    if (data.length > 1) {
      var colArmada = -1, colNopol = -1, colPosisi = -1, colTahun = -1, colCode = -1, colTanggal = -1, colNoSeri = -1, colBarcode = -1;
      var colUkuran = -1, colMerk = -1, colKondisi = -1, colTekanan = -1, colKeterangan = -1;
      var header = data[0];
      if (header && header.length > 0) {
        for (var c = 0; c < header.length; c++) {
          var h = String(header[c] || "").trim().toLowerCase();
          if (!h) continue;

          if (h === "id armada" || h === "armada id" || h === "id_armada" || (h.indexOf("armada") > -1 && h.indexOf("posisi") === -1)) {
            if (colArmada === -1) colArmada = c;
          }
          if (h.indexOf("posisi") > -1) {
            if (colPosisi === -1) colPosisi = c;
          }
          if (h.indexOf("polisi") > -1 || h.indexOf("nopol") > -1) {
            if (colNopol === -1) colNopol = c;
          }
          if (h.indexOf("barcode") > -1) {
            colBarcode = c;
          }
          if (h === "tahun ban" || h === "tahun" || h.indexOf("tahun") > -1) {
            if (colTahun === -1) colTahun = c;
          }
          if (h === "code ban" || h === "kode ban" || (h.indexOf("code") > -1 && h.indexOf("ban") > -1) || h.indexOf("code") > -1 || h.indexOf("kode") > -1) {
            if (colCode === -1 && h.indexOf("barcode") === -1) colCode = c;
          }
          if (h === "tanggal update" || h.indexOf("tanggal") > -1 || h.indexOf("update") > -1) {
            if (colTanggal === -1) colTanggal = c;
          }
          if (h === "no seri" || h === "no. seri" || h.indexOf("seri") > -1) {
            if (colNoSeri === -1) colNoSeri = c;
          }
          if (h.indexOf("ukuran") > -1) {
            if (colUkuran === -1) colUkuran = c;
          }
          if (h.indexOf("merk") > -1) {
            if (colMerk === -1) colMerk = c;
          }
          if (h.indexOf("kondisi") > -1) {
            if (colKondisi === -1) colKondisi = c;
          }
          if (h.indexOf("tekanan") > -1) {
            if (colTekanan === -1) colTekanan = c;
          }
          if (h.indexOf("keterangan") > -1 || h.indexOf("catatan") > -1) {
            if (colKeterangan === -1) colKeterangan = c;
          }
        }
      }

      // Fallbacks if not detected by header name
      if (colArmada === -1) colArmada = 0;
      if (colPosisi === -1) colPosisi = 1;
      if (colTahun === -1) colTahun = 2;
      if (colCode === -1) colCode = 3;
      if (colTanggal === -1) colTanggal = 4;
      if (colNoSeri === -1) colNoSeri = 5;
      if (colBarcode === -1) colBarcode = 6; // Target column G (index 6)

      var lastArmadaId = "";
      for (var i = 1; i < data.length; i++) {
        var r = data[i];
        if (!r) continue;

        var aId = (colArmada >= 0 && colArmada < r.length) ? String(r[colArmada] || "").trim() : "";
        if (aId !== "") lastArmadaId = aId;
        var effectiveArmadaId = aId || lastArmadaId;

        var nPol = (colNopol >= 0 && colNopol < r.length) ? String(r[colNopol] || "").trim() : "";
        var pos = (colPosisi >= 0 && colPosisi < r.length) ? String(r[colPosisi] || "").trim() : "";

        if (!effectiveArmadaId && !nPol && !pos) continue;

        var bCode = (colBarcode >= 0 && colBarcode < r.length) ? String(r[colBarcode] || "").trim() : "";
        // Legacy fallback: if Barcode at colBarcode is empty, check column K (index 10)
        if (!bCode && r.length > 10) {
          var legacyVal = String(r[10] || "").trim();
          if (legacyVal) bCode = legacyVal;
        }

        var nSeri = (colNoSeri >= 0 && colNoSeri < r.length) ? String(r[colNoSeri] || "").trim() : "";
        var cCode = (colCode >= 0 && colCode < r.length) ? String(r[colCode] || "").trim() : "";
        var tTahun = (colTahun >= 0 && colTahun < r.length) ? String(r[colTahun] || "").trim() : "";
        var tTanggal = (colTanggal >= 0 && colTanggal < r.length) ? String(r[colTanggal] || "").trim() : "";

        var bestBarcode = bCode || nSeri || cCode;

        banList.push({
          armadaId: effectiveArmadaId,
          noPolisi: nPol,
          posisi: pos,
          barcode: bestBarcode,
          noSeri: nSeri || bestBarcode,
          codeBan: cCode || bestBarcode,
          ukuran: (colUkuran >= 0 && colUkuran < r.length) ? String(r[colUkuran] || "") : "",
          merk: (colMerk >= 0 && colMerk < r.length) ? String(r[colMerk] || "") : "",
          kondisi: (colKondisi >= 0 && colKondisi < r.length) ? String(r[colKondisi] || "Bagus") : "Bagus",
          tekanan: (colTekanan >= 0 && colTekanan < r.length) ? String(r[colTekanan] || "") : "",
          keterangan: (colKeterangan >= 0 && colKeterangan < r.length) ? String(r[colKeterangan] || "") : "",
          tahun: tTahun,
          tanggalUpdate: tTanggal
        });
      }
    }
  }

  // Fetch dedicated Aki sheet (GID_AKI)
  var akiSheet = getSheetByGid(ss, GID_AKI) || 
                 getSheetByGid(sheetMap, GID_AKI) || 
                 getSheetByNameFromMap(ss, sheetMap, "AKI ARMADA") || 
                 getSheetByNameFromMap(ss, sheetMap, "AKI") || 
                 getSheetByNameFromMap(ss, sheetMap, "Aki Armada");
  if (akiSheet) {
    var akiData = akiSheet.getDataRange().getDisplayValues();
    if (akiData.length > 1) {
      var headerRowIdx = 0;
      var colAId = 0, colANopol = 1, colATgl = 2, colAGanti = 3, colABarcode = 4, colAStatus = 5, colAMerk = -1;
      
      for (var hr = 0; hr < Math.min(10, akiData.length); hr++) {
        var aHeader = akiData[hr];
        if (!aHeader) continue;
        var foundMatches = 0;
        var tId = colAId, tNopol = colANopol, tTgl = colATgl, tGanti = colAGanti, tBarcode = colABarcode, tStatus = colAStatus, tMerk = colAMerk;
        
        for (var ac = 0; ac < aHeader.length; ac++) {
          var ah = String(aHeader[ac] || "").toLowerCase().trim();
          if (!ah) continue;
          if (ah.indexOf("armada") > -1 || ah.indexOf("id") > -1) { tId = ac; foundMatches++; }
          else if (ah.indexOf("polis") > -1 || ah.indexOf("nopol") > -1) { tNopol = ac; foundMatches++; }
          else if (ah.indexOf("tanggal") > -1 || ah.indexOf("pasang") > -1) { tTgl = ac; foundMatches++; }
          else if (ah.indexOf("ganti") > -1 || ah.indexOf("berikut") > -1) { tGanti = ac; foundMatches++; }
          else if (ah.indexOf("barcode") > -1 || ah.indexOf("seri") > -1) { tBarcode = ac; foundMatches++; }
          else if (ah.indexOf("status") > -1 || ah.indexOf("ket") > -1) { tStatus = ac; foundMatches++; }
          else if (ah.indexOf("merk") > -1 || ah.indexOf("tipe") > -1) { tMerk = ac; foundMatches++; }
        }
        if (foundMatches >= 2) {
          headerRowIdx = hr;
          colAId = tId; colANopol = tNopol; colATgl = tTgl; colAGanti = tGanti; colABarcode = tBarcode; colAStatus = tStatus; colAMerk = tMerk;
          break;
        }
      }

      var armadaRefMap = {};
      try {
        var aSheet = getSheetByNameFromMap(ss, sheetMap, "ARMADA") || getSheetByNameFromMap(ss, sheetMap, "Armada");
        if (aSheet) {
          var aData = aSheet.getDataRange().getValues();
          for (var arIdx = 1; arIdx < aData.length; arIdx++) {
            var rArmId = String(aData[arIdx][0] || "").trim().toUpperCase();
            var rNopol = String(aData[arIdx][1] || "").trim().toUpperCase();
            if (rArmId) armadaRefMap[cleanArmadaKey(rArmId)] = { armadaId: rArmId, noPolisi: rNopol };
            if (rNopol) armadaRefMap[cleanArmadaKey(rNopol)] = { armadaId: rArmId, noPolisi: rNopol };
          }
        }
      } catch(errRef) {}

      for (var j = headerRowIdx + 1; j < akiData.length; j++) {
        var ar = akiData[j];
        if (!ar) continue;
        var akiArmadaId = String(ar[colAId] || "").trim();
        var akiNoPol = String(ar[colANopol] || "").trim();

        if (akiArmadaId.toLowerCase().indexOf("armada") > -1 || akiNoPol.toLowerCase().indexOf("polis") > -1) continue;
        if (!akiArmadaId && !akiNoPol) continue;

        var ref = armadaRefMap[cleanArmadaKey(akiArmadaId)] || armadaRefMap[cleanArmadaKey(akiNoPol)];
        if (ref) {
          if (!akiArmadaId) akiArmadaId = ref.armadaId;
          if (!akiNoPol) akiNoPol = ref.noPolisi;
        }

        var akiTgl = String(ar[colATgl] || "").trim();
        var akiGanti = String(ar[colAGanti] || "").trim();
        var akiBarcode = String(ar[colABarcode] || "").trim();
        var akiStatus = String(ar[colAStatus] || "").trim();
        var akiMerk = (colAMerk > -1 && colAMerk < ar.length) ? String(ar[colAMerk] || "").trim() : "";
        if (!akiMerk) akiMerk = "Aki Standard";

        var calc = calculateAkiGantiDateAndStatus(akiTgl, akiStatus);
        var finalGanti = akiGanti || calc.gantiDate;
        var finalStatus = akiStatus || calc.status;
        var finalBarcode = akiBarcode || "";

        var newAkiItem = {
          armadaId: akiArmadaId,
          noPolisi: akiNoPol,
          posisi: "AKI",
          barcode: finalBarcode,
          barcodeAki: finalBarcode,
          noSeri: finalBarcode,
          ukuran: "12V",
          merk: akiMerk,
          kondisi: akiTgl,
          tanggalPasangAki: akiTgl,
          tekanan: akiMerk,
          keterangan: finalStatus,
          tahun: akiTgl ? (akiTgl.split("/").pop().split("-")[0] || "2025") : "2025"
        };

        for (var idx = banList.length - 1; idx >= 0; idx--) {
          if (String(banList[idx].posisi || "").trim().toUpperCase() === "AKI" &&
              isArmadaMatch(banList[idx].armadaId, banList[idx].noPolisi, akiArmadaId, akiNoPol)) {
            banList.splice(idx, 1);
          }
        }
        banList.push(newAkiItem);
      }
    }
  }

  return banList;
}

function cleanArmadaKey(s) {
  return String(s || "").toUpperCase().replace(/[\s\-\.]+/g, "");
}

function isArmadaMatch(rowArmadaId, rowNopol, targetArmadaId, targetNoPolisi) {
  var cRowId = cleanArmadaKey(rowArmadaId);
  var cRowPol = cleanArmadaKey(rowNopol);
  var cTargetId = cleanArmadaKey(targetArmadaId);
  var cTargetPol = cleanArmadaKey(targetNoPolisi);

  if (cTargetId && cRowId && cRowId === cTargetId) return true;
  if (cTargetPol && cRowPol && cRowPol === cTargetPol) return true;
  if (cTargetId && cRowPol && cRowPol === cTargetId) return true;
  if (cTargetPol && cRowId && cRowId === cTargetPol) return true;
  return false;
}

/**
 * Hitung tanggal ganti aki berikutnya (2 tahun) dan status kelayakan aki
 */
function calculateAkiGantiDateAndStatus(tglStr, userKeterangan) {
  var userStatus = (userKeterangan && String(userKeterangan).trim().length > 0) ? String(userKeterangan).trim() : "";

  if (!tglStr) {
    return {
      gantiDate: "",
      status: userStatus || "Aman",
      isDue: false
    };
  }

  var str = String(tglStr).trim();
  var parts = str.split(/[\/\-\.]+/);
  var d = null;

  if (parts.length >= 3) {
    var p0 = parseInt(parts[0], 10);
    var p1 = parseInt(parts[1], 10);
    var p2 = parseInt(parts[2], 10);

    if (parts[0].length === 4) {
      // yyyy-mm-dd
      d = new Date(p0, p1 - 1, p2);
    } else if (parts[2].length === 4) {
      // mm/dd/yyyy or dd/mm/yyyy
      if (p0 > 12) {
        // dd/mm/yyyy
        d = new Date(p2, p1 - 1, p0);
      } else {
        // mm/dd/yyyy
        d = new Date(p2, p0 - 1, p1);
      }
    }
  }

  if (!d || isNaN(d.getTime())) {
    d = new Date(str);
  }

  if (!d || isNaN(d.getTime())) {
    return {
      gantiDate: str + " + 2 Tahun",
      status: userStatus || "Aman",
      isDue: false
    };
  }

  // Calculate 2 years date
  var gantiD = new Date(d.getTime());
  gantiD.setFullYear(gantiD.getFullYear() + 2);

  var now = new Date();
  var isDue = now.getTime() >= gantiD.getTime();

  var gMonth = gantiD.getMonth() + 1;
  var gDay = gantiD.getDate();
  var gYear = gantiD.getFullYear();
  var gantiDateFormatted = gMonth + "/" + gDay + "/" + gYear;

  var calcStatus = userStatus || (isDue ? "PERLU GANTI (SUDAH 2 TAHUN)" : "AMAN");

  return {
    gantiDate: gantiDateFormatted,
    status: calcStatus,
    isDue: isDue
  };
}

// ============================================
// AUTHENTICATION (validateLogin)
// ============================================

function validateLogin(contents, ss, sheetMap) {
    var driverIdOrName = String(contents.driverName || contents.username || contents.driverId || "").trim();
  var pin = String(contents.pin || "").trim();
  if (!driverIdOrName || !pin) {
    return { success: false, driverId: null, driverName: null, message: "ID Driver dan PIN wajib diisi." };
  }
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var sheet = getSheetByGid(sheetMap, GID_DAFTAR_DRIVER) || 
              getSheetByGid(ss, GID_DAFTAR_DRIVER) || 
              getSheetByNameFromMap(ss, sheetMap, "Daftar_Driver") || 
              getSheetByNameFromMap(ss, sheetMap, "DRIVERS") || 
              getSheetByNameFromMap(ss, sheetMap, "Driver");
  if (sheet) {
    var data = sheet.getDataRange().getValues();
    for (var i = 1; i < data.length; i++) {
      var id = String(data[i][0] || "").trim();
      var name = String(data[i][1] || "").trim();
      var storedPin = data[i][2] ? String(data[i][2]).trim() : "";

      if (id.toLowerCase() === driverIdOrName.toLowerCase() || name.toLowerCase() === driverIdOrName.toLowerCase()) {
        if (storedPin && storedPin === pin) {
          return { success: true, driverId: id || "D01", driverName: name || driverIdOrName, message: "Login Berhasil" };
        }
      }
    }
  }
  return { success: false, driverId: null, driverName: null, message: "ID Driver atau PIN salah." };
}

// ============================================
// CORE BUSINESS LOGIC (submitLog, submitService, updateBan, updateFotoArmada, submitTerkirim)
// ============================================

function submitLog(contents, ss, sheetMap) {
  var logData = contents.logData || contents;
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var armadaId = String(logData.armadaId || logData.nopol || "").trim().toUpperCase();
  var kmVal = Number(logData.kmTerdeteksi || logData.kmSaatIni || logData.kmTerdetect || logData.km || 0);
  var driverName = String(logData.driverName || logData.namaDriver || logData.driver || "").trim();
  var catatan = String(logData.catatan || logData.catattan || logData.keterangan || "").trim();

  if (!armadaId) return { success: false, message: "Validasi Gagal: ID Armada wajib diisi!" };
  if (isNaN(kmVal) || kmVal <= 0) return { success: false, message: "Validasi Gagal: Angka KM saat ini tidak valid!" };

  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);

    var targetGid = logData.sheetId || contents.sheetId || GID_ODOMETER_KM;
    var logSheet = getSheetByGid(sheetMap, targetGid)
                || getSheetByGid(sheetMap, GID_ODOMETER_KM)
                || getSheetByNameFromMap(ss, sheetMap, "log_harian")
                || getSheetByNameFromMap(ss, sheetMap, "LOG_HARIAN");
    if (!logSheet) {
      logSheet = ss.insertSheet("log_harian");
      logSheet.appendRow(["Tanggal", "Id armada", "KM terdeteksi", "link foto", "Catatan", "Nama Driver"]);
    }

    var armadaSheet = getSheetByNameFromMap(ss, sheetMap, "ARMADA") || getSheetByNameFromMap(ss, sheetMap, "Armada");

    var dateStr = Utilities.formatDate(new Date(), "GMT+7", "dd/MM/yyyy HH:mm:ss");
    var photoUrl = "";
    
    var b64Photo = logData.base64Photo || logData.fotoBase64 || logData.photoBase64 || logData.base64 || logData.image || logData.foto;
    if (b64Photo) {
      var pName = logData.photoName || logData.fileName || ("KM_" + armadaId + "_" + Date.now() + ".jpg");
      photoUrl = saveImageToDrive(b64Photo, pName, "Foto Odometer", logData.photoMimeType || "image/jpeg");
    } else if (logData.linkFoto || logData.url || logData.photoUrl || logData.fotoUrl) {
      photoUrl = logData.linkFoto || logData.url || logData.photoUrl || logData.fotoUrl;
    }

    var lastCol = logSheet.getLastColumn();
    var lastRow = logSheet.getLastRow();

    if (lastRow >= 1 && lastCol > 0) {
      var headerRow = logSheet.getRange(1, 1, 1, lastCol).getValues()[0];
      var rowData = new Array(lastCol).fill("");
      var hasHeaderMatch = false;

      for (var c = 0; c < lastCol; c++) {
        var h = String(headerRow[c] || "").toLowerCase().trim();
        if (h.indexOf("tgl") > -1 || h.indexOf("tanggal") > -1 || h.indexOf("waktu") > -1 || h.indexOf("time") > -1 || h.indexOf("date") > -1) {
          rowData[c] = dateStr; hasHeaderMatch = true;
        } else if (h.indexOf("armada") > -1 || h.indexOf("nopol") > -1 || h.indexOf("unit") > -1) {
          rowData[c] = armadaId; hasHeaderMatch = true;
        } else if (h.indexOf("km") > -1 || h.indexOf("terdeteksi") > -1) {
          rowData[c] = kmVal; hasHeaderMatch = true;
        } else if (h.indexOf("foto") > -1 || h.indexOf("bukti") > -1 || h.indexOf("link") > -1 || h.indexOf("image") > -1 || h.indexOf("photo") > -1) {
          rowData[c] = photoUrl; hasHeaderMatch = true;
        } else if (h.indexOf("catatan") > -1 || h.indexOf("keterangan") > -1 || h.indexOf("remark") > -1) {
          rowData[c] = catatan; hasHeaderMatch = true;
        } else if (h.indexOf("driver") > -1 || h.indexOf("sopir") > -1 || h.indexOf("nama") > -1) {
          rowData[c] = driverName; hasHeaderMatch = true;
        }
      }

      if (hasHeaderMatch) {
        logSheet.appendRow(rowData);
      } else {
        logSheet.appendRow([dateStr, armadaId, kmVal, photoUrl, catatan, driverName]);
      }
    } else {
      logSheet.appendRow([dateStr, armadaId, kmVal, photoUrl, catatan, driverName]);
    }

    var sisaKm = 1000;
    var serviceAlert = false;
    var threshold = 1000;

    if (armadaSheet) {
      var aRows = armadaSheet.getDataRange().getValues();
      for (var i = 1; i < aRows.length; i++) {
        var rowId = String(aRows[i][0] || "").trim().toUpperCase();
        var rowPol = String(aRows[i][1] || "").trim().toUpperCase();
        if (rowId === armadaId || rowPol === armadaId) {
          var targetRow = aRows[i];
          while (targetRow.length < 11) { targetRow.push(""); }

          targetRow[2] = kmVal; // Column C (index 2): KM SAAT INI
          
          var nextService = Number(targetRow[5]) || ((Number(targetRow[3]) || 0) + 5000);
          sisaKm = nextService - kmVal;
          targetRow[6] = sisaKm; // Column G (index 6): SISA KM

          if (photoUrl) targetRow[9] = photoUrl; // Column J (index 9)
          if (catatan) targetRow[10] = catatan; // Column K (index 10)

          var statusStr = "AMAN";
          if (sisaKm < 0) statusStr = "🚨 HARUS SERVICE";
          else if (sisaKm < threshold) statusStr = "⚠️ SERVICE <1000 KM";
          targetRow[7] = statusStr; // Column H (index 7)

          batchWriteRow(armadaSheet, i + 1, 1, targetRow);

          if (sisaKm < threshold) {
            serviceAlert = true;
            sendAdminEmail(armadaId, sisaKm, driverName, kmVal, photoUrl);
          }
          break;
        }
      }
    }

    return {
      success: true,
      linkFoto: photoUrl,
      sisaKm: sisaKm,
      serviceAlert: serviceAlert,
      message: "Log harian Odometer berhasil disimpan!"
    };
  } catch(e) {
    return { success: false, message: "Gagal menyimpan log harian: " + e.toString() };
  } finally {
    lock.releaseLock();
  }
}

function submitService(contents, ss, sheetMap) {
  var serviceData = contents.serviceData || contents;
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var armadaId = String(serviceData.armadaId || "").trim().toUpperCase();
  var kmServis = Number(serviceData.kmServis || serviceData.kmServiceTerakhir || serviceData.kmSaatIni || 0);

  if (!armadaId) return { success: false, message: "Validasi Gagal: ID Armada wajib diisi!" };
  if (isNaN(kmServis) || kmServis <= 0) return { success: false, message: "Validasi Gagal: Angka KM Service tidak valid!" };

  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);

    var armadaSheet = getSheetByNameFromMap(ss, sheetMap, "ARMADA") || getSheetByNameFromMap(ss, sheetMap, "Armada");
    if (armadaSheet) {
      var aRows = armadaSheet.getDataRange().getValues();
      for (var i = 1; i < aRows.length; i++) {
        var rowId = String(aRows[i][0] || "").trim().toUpperCase();
        var rowPol = String(aRows[i][1] || "").trim().toUpperCase();
        if (rowId === armadaId || rowPol === armadaId) {
          var intervalService = Number(aRows[i][4]) || 5000;
          var kmServiceBerikutnya = kmServis + intervalService;
          var sisaKm = kmServiceBerikutnya - kmServis;

          var serviceSlice = armadaSheet.getRange(i + 1, 3, 1, 6).getValues();
          serviceSlice[0][0] = kmServis; // Col C: KM SAAT INI
          serviceSlice[0][1] = kmServis; // Col D: KM SERVICE TERAKHIR
          serviceSlice[0][3] = kmServiceBerikutnya; // Col F: KM SERVICE BERIKUTNYA
          serviceSlice[0][4] = sisaKm; // Col G: SISA KM
          serviceSlice[0][5] = "🟢 AMAN"; // Col H: STATUS
          
          batchWriteRow(armadaSheet, i + 1, 3, serviceSlice[0]);

          if (serviceData.catatan !== undefined) {
            batchWriteRow(armadaSheet, i + 1, 11, [serviceData.catatan || ""]);
          }
          break;
        }
      }
    }

    return { success: true, message: "Data servis " + armadaId + " berhasil diperbarui. Status armada kembali AMAN!" };
  } catch(e) {
    return { success: false, message: "Gagal menyimpan service log: " + e.toString() };
  } finally {
    lock.releaseLock();
  }
}

function submitCatatanDriver(contents, ss, sheetMap) {
  var data = contents.request || contents;
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var armadaId = String(data.armadaId || "").trim().toUpperCase();
  var driverName = String(data.driverName || data.driver || "").trim();
  var catatan = String(data.catatan || "").trim();
  var tanggal = String(data.tanggal || Utilities.formatDate(new Date(), "GMT+7", "dd/MM/yyyy HH:mm")).trim();

  if (!armadaId) return { success: false, message: "Validasi Gagal: ID Armada wajib diisi!" };

  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);

    var armadaSheet = getSheetByNameFromMap(ss, sheetMap, "ARMADA") || getSheetByNameFromMap(ss, sheetMap, "Armada");
    if (armadaSheet) {
      var aRows = armadaSheet.getDataRange().getValues();
      for (var i = 1; i < aRows.length; i++) {
        var rowId = String(aRows[i][0] || "").trim().toUpperCase();
        var rowPol = String(aRows[i][1] || "").trim().toUpperCase();
        if (rowId === armadaId || rowPol === armadaId) {
          batchWriteRow(armadaSheet, i + 1, 11, [catatan]);
          break;
        }
      }
    }

    var logSheet = getSheetByNameFromMap(ss, sheetMap, "CATATAN_DRIVER") || 
                   getSheetByNameFromMap(ss, sheetMap, "catatan_driver") || 
                   getSheetByNameFromMap(ss, sheetMap, "LOG_CATATAN");
    if (!logSheet) {
      logSheet = ss.insertSheet("CATATAN_DRIVER");
      logSheet.appendRow(["Tanggal", "ID Armada", "Nama Driver", "Catatan / Keluhan", "Status"]);
    }
    logSheet.appendRow([tanggal, armadaId, driverName, catatan, "Aktif"]);

    return { success: true, message: "Catatan driver armada " + armadaId + " berhasil disimpan ke Google Spreadsheet!" };
  } catch(e) {
    return { success: false, message: "Gagal menyimpan catatan driver: " + e.toString() };
  } finally {
    lock.releaseLock();
  }
}

function clearCatatanDriver(contents, ss, sheetMap) {
  var data = contents.request || contents;
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var armadaId = String(data.armadaId || "").trim().toUpperCase();
  if (!armadaId) return { success: false, message: "Validasi Gagal: ID Armada wajib diisi!" };

  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);

    var armadaSheet = getSheetByNameFromMap(ss, sheetMap, "ARMADA") || getSheetByNameFromMap(ss, sheetMap, "Armada");
    if (armadaSheet) {
      var aRows = armadaSheet.getDataRange().getValues();
      for (var i = 1; i < aRows.length; i++) {
        var rowId = String(aRows[i][0] || "").trim().toUpperCase();
        var rowPol = String(aRows[i][1] || "").trim().toUpperCase();
        if (rowId === armadaId || rowPol === armadaId) {
          batchWriteRow(armadaSheet, i + 1, 11, [""]);
          break;
        }
      }
    }

    return { success: true, message: "Catatan armada " + armadaId + " berhasil dibersihkan dari Google Spreadsheet!" };
  } catch(e) {
    return { success: false, message: "Gagal membersihkan catatan: " + e.toString() };
  } finally {
    lock.releaseLock();
  }
}

function updateAki(contents, ss, sheetMap) {
  var data = contents.akiData || contents.banData || contents;
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var targetArmadaId = String(data.armadaId || contents.armadaId || "").trim().toUpperCase();
  var targetNoPolisi = String(data.noPolisi || contents.noPolisi || data.noPol || "").trim().toUpperCase();
  var newBarcode = String(data.barcode || data.barcodeAki || contents.barcode || "").trim();
  var newTanggalPasang = String(data.tanggalPasangAki || data.kondisi || contents.kondisi || "").trim();
  var newMerk = String(data.merk || data.tekanan || contents.merk || "").trim();
  var newStatus = String(data.status || data.keterangan || contents.keterangan || "AMAN").trim();

  if (!targetArmadaId) return { success: false, message: "Validasi Gagal: ID Armada wajib diisi!" };

  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);

    var sheet = getSheetByGid(ss, GID_AKI) || 
                getSheetByGid(sheetMap, GID_AKI) || 
                getSheetByNameFromMap(ss, sheetMap, "AKI ARMADA") || 
                getSheetByNameFromMap(ss, sheetMap, "AKI") || 
                getSheetByNameFromMap(ss, sheetMap, "Aki Armada") || 
                getSheetByNameFromMap(ss, sheetMap, "Aki");

    if (!sheet) {
      sheet = ss.insertSheet("AKI ARMADA");
      sheet.appendRow(["ID ARMADA", "NO POLIS", "TANGGAL PASANG AKI", "GANTI AKI BERIKUTNYA", "BARCODE", "STATUS", "MERK"]);
    }

    var rows = sheet.getDataRange().getValues();
    var headerRowIdx = 0;
    var colArmada = 0, colNopol = 1, colTanggal = 2, colGanti = 3, colBarcode = 4, colStatus = 5, colMerk = 6;

    if (rows.length > 0) {
      for (var hr = 0; hr < Math.min(10, rows.length); hr++) {
        var header = rows[hr];
        if (!header) continue;
        var foundMatches = 0;
        var tArm = colArmada, tNop = colNopol, tTgl = colTanggal, tGanti = colGanti, tBar = colBarcode, tStat = colStatus, tMrk = colMerk;
        for (var c = 0; c < header.length; c++) {
          var h = String(header[c] || "").toLowerCase().trim();
          if (h.indexOf("armada") > -1 || h.indexOf("id") > -1) { tArm = c; foundMatches++; }
          else if (h.indexOf("polis") > -1 || h.indexOf("nopol") > -1) { tNop = c; foundMatches++; }
          else if (h.indexOf("tanggal") > -1 || h.indexOf("pasang") > -1) { tTgl = c; foundMatches++; }
          else if (h.indexOf("ganti") > -1 || h.indexOf("berikut") > -1) { tGanti = c; foundMatches++; }
          else if (h.indexOf("barcode") > -1 || h.indexOf("seri") > -1) { tBar = c; foundMatches++; }
          else if (h.indexOf("status") > -1 || h.indexOf("ket") > -1) { tStat = c; foundMatches++; }
          else if (h.indexOf("merk") > -1 || h.indexOf("tipe") > -1) { tMrk = c; foundMatches++; }
        }
        if (foundMatches >= 2) {
          headerRowIdx = hr;
          colArmada = tArm; colNopol = tNop; colTanggal = tTgl; colGanti = tGanti; colBarcode = tBar; colStatus = tStat; colMerk = tMrk;
          break;
        }
      }

      if (colMerk === -1) {
        colMerk = 6;
        sheet.getRange(headerRowIdx + 1, 7).setValue("MERK");
      }
    }

    var calc = calculateAkiGantiDateAndStatus(newTanggalPasang, newStatus);
    var gantiDateVal = calc.gantiDate;
    var statusVal = calc.status;

    var updated = false;
    if (rows.length > headerRowIdx + 1) {
      for (var i = headerRowIdx + 1; i < rows.length; i++) {
        var rowArmadaId = String(rows[i][colArmada] || "").trim().toUpperCase();
        var rowNopol = String(rows[i][colNopol] || "").trim().toUpperCase();

        if (isArmadaMatch(rowArmadaId, rowNopol, targetArmadaId, targetNoPolisi)) {
          var targetRow = rows[i];
          var maxColIndex = Math.max(5, colMerk);
          while (targetRow.length <= maxColIndex) { targetRow.push(""); }

          if (targetArmadaId) targetRow[colArmada] = targetArmadaId;
          if (targetNoPolisi) targetRow[colNopol] = targetNoPolisi;
          if (newTanggalPasang) targetRow[colTanggal] = newTanggalPasang;
          targetRow[colGanti] = gantiDateVal;
          if (newBarcode) targetRow[colBarcode] = newBarcode;
          targetRow[colStatus] = statusVal;
          if (newMerk) targetRow[colMerk] = newMerk;

          batchWriteRow(sheet, i + 1, 1, targetRow);
          updated = true;
          break;
        }
      }
    }

    if (!updated) {
      var maxColIndex = Math.max(5, colMerk);
      var newRow = [];
      for (var c = 0; c <= maxColIndex; c++) { newRow.push(""); }
      newRow[colArmada] = targetArmadaId;
      newRow[colNopol] = targetNoPolisi;
      newRow[colTanggal] = newTanggalPasang;
      newRow[colGanti] = gantiDateVal;
      newRow[colBarcode] = newBarcode;
      newRow[colStatus] = statusVal;
      newRow[colMerk] = newMerk || "Aki Standard";
      sheet.appendRow(newRow);
    }

    return {
      success: true,
      message: "Data AKI " + targetArmadaId + " berhasil disimpan ke Google Sheets (GID 1886867333)!"
    };
  } catch(e) {
    return { success: false, message: "Gagal memperbarui data aki: " + e.toString() };
  } finally {
    lock.releaseLock();
  }
}

function updateBan(contents, ss, sheetMap) {
  var data = contents.banData || contents;
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  
  var armadaId = String(data.armadaId || contents.armadaId || "").trim().toUpperCase();
  var posisi = String(data.posisi || contents.posisi || "").trim();
  var tahunBan = String(data.tahunBan || data.tahun || contents.tahunBan || contents.tahun || "").trim();
  var codeBan = String(data.codeBan || data.kodeBan || contents.codeBan || contents.kodeBan || "").trim();
  var noSeri = String(data.noSeri || contents.noSeri || "").trim();
  var barcode = String(data.barcode || contents.barcode || "").trim();

  // Cross-fill barcode/codeBan/noSeri if only one is provided
  if (barcode && !codeBan) codeBan = barcode;
  if (barcode && !noSeri) noSeri = barcode;
  if (!barcode && (codeBan || noSeri)) barcode = codeBan || noSeri;

  var sheet = getSheetByGid(sheetMap, GID_BAN) || 
              getSheetByGid(ss, GID_BAN) || 
              getSheetByNameFromMap(ss, sheetMap, "BAN ARMADA") || 
              getSheetByNameFromMap(ss, sheetMap, "Ban armada") || 
              getSheetByNameFromMap(ss, sheetMap, "Ban Armada") || 
              getSheetByNameFromMap(ss, sheetMap, "BAN");
  
  if (!sheet) {
    sheet = ss.insertSheet("BAN ARMADA");
    sheet.appendRow(["ID ARMADA", "POSISI", "TAHUN BAN", "CODE BAN", "TANGGAL UPDATE", "No seri", "Barcode"]);
  }

  var dataRange = sheet.getDataRange().getValues();
  var headers = dataRange[0] || [];
  
  // Dynamic header mapping
  var colMap = {
    armada: -1,
    posisi: -1,
    tahun: -1,
    code: -1,
    tanggal: -1,
    seri: -1,
    barcode: -1
  };

  if (headers && headers.length > 0) {
    for (var c = 0; c < headers.length; c++) {
      var h = String(headers[c] || "").trim().toLowerCase();
      if (!h) continue;

      if (h === "id armada" || h === "armada id" || h === "id_armada" || (h.indexOf("armada") > -1 && h.indexOf("posisi") === -1)) {
        if (colMap.armada === -1) colMap.armada = c;
      }
      if (h.indexOf("posisi") > -1) {
        if (colMap.posisi === -1) colMap.posisi = c;
      }
      if (h === "tahun ban" || h === "tahun" || h.indexOf("tahun") > -1) {
        if (colMap.tahun === -1) colMap.tahun = c;
      }
      if (h === "code ban" || h === "kode ban" || (h.indexOf("code") > -1 && h.indexOf("ban") > -1) || h.indexOf("code") > -1 || h.indexOf("kode") > -1) {
        if (colMap.code === -1 && h.indexOf("barcode") === -1) colMap.code = c;
      }
      if (h === "tanggal update" || h.indexOf("tanggal") > -1 || h.indexOf("update") > -1) {
        if (colMap.tanggal === -1) colMap.tanggal = c;
      }
      if (h === "no seri" || h === "no. seri" || h.indexOf("seri") > -1) {
        if (colMap.seri === -1) colMap.seri = c;
      }
      if (h.indexOf("barcode") > -1) {
        colMap.barcode = c;
      }
    }
  }

  // Fallbacks if not matched by headers
  if (colMap.armada === -1) colMap.armada = 0;
  if (colMap.posisi === -1) colMap.posisi = 1;
  if (colMap.tahun === -1) colMap.tahun = 2;
  if (colMap.code === -1) colMap.code = 3;
  if (colMap.tanggal === -1) colMap.tanggal = 4;
  if (colMap.seri === -1) colMap.seri = 5;
  if (colMap.barcode === -1) colMap.barcode = 6;

  // Ensure column G / colMap.barcode header is present if missing or sheet has fewer columns
  if (sheet.getMaxColumns() < colMap.barcode + 1) {
    sheet.insertColumnsAfter(sheet.getMaxColumns(), (colMap.barcode + 1) - sheet.getMaxColumns());
  }
  var barcodeHeaderVal = sheet.getRange(1, colMap.barcode + 1).getValue();
  if (!barcodeHeaderVal || String(barcodeHeaderVal).trim() === "") {
    sheet.getRange(1, colMap.barcode + 1).setValue("Barcode");
  }

  var targetRow = -1;
  var lastArmada = "";
  
  // Scanning data with forward-fill (handling merged cells)
  for (var i = 1; i < dataRange.length; i++) {
    var cellArmada = (colMap.armada >= 0 && colMap.armada < dataRange[i].length) ? String(dataRange[i][colMap.armada] || "").trim().toUpperCase() : "";
    if (cellArmada !== "") {
      lastArmada = cellArmada;
    }
    var currentArmada = cellArmada || lastArmada;
    var currentPosisi = (colMap.posisi >= 0 && colMap.posisi < dataRange[i].length) ? String(dataRange[i][colMap.posisi] || "").trim() : "";
    
    if (currentArmada === armadaId && currentPosisi.toLowerCase() === posisi.toLowerCase()) {
      targetRow = i + 1;
      break;
    }
  }
  
  var timestamp = Utilities.formatDate(new Date(), "GMT+7", "yyyy-MM-dd HH:mm:ss");
  
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);
    if (targetRow !== -1) {
      // UPDATE specific columns safely without overwriting other columns
      if (tahunBan) sheet.getRange(targetRow, colMap.tahun + 1).setValue(tahunBan);
      if (codeBan) sheet.getRange(targetRow, colMap.code + 1).setValue(codeBan);
      if (noSeri) sheet.getRange(targetRow, colMap.seri + 1).setValue(noSeri);
      if (barcode) sheet.getRange(targetRow, colMap.barcode + 1).setValue(barcode);
      sheet.getRange(targetRow, colMap.tanggal + 1).setValue(timestamp);

      // Legacy support: If Column K (index 10) exists and has header "Barcode", keep it updated if colMap.barcode != 10
      if (colMap.barcode !== 10 && headers.length > 10) {
        var h10 = String(headers[10] || "").trim().toLowerCase();
        if (h10.indexOf("barcode") > -1 && barcode) {
          sheet.getRange(targetRow, 11).setValue(barcode);
        }
      }
      
      return { success: true, message: "Data ban " + armadaId + " (" + posisi + ") berhasil diupdate pada baris " + targetRow };
    } else {
      // APPEND new row matching columns
      var newRowLength = Math.max(7, colMap.barcode + 1);
      var newRow = new Array(newRowLength).fill("");
      newRow[colMap.armada] = armadaId;
      newRow[colMap.posisi] = posisi;
      newRow[colMap.tahun] = tahunBan;
      newRow[colMap.code] = codeBan;
      newRow[colMap.tanggal] = timestamp;
      newRow[colMap.seri] = noSeri;
      newRow[colMap.barcode] = barcode;

      sheet.appendRow(newRow);
      return { success: true, message: "Data ban baru (" + armadaId + " - " + posisi + ") berhasil ditambahkan." };
    }
  } catch(e) {
    return { success: false, message: "Error updateBan: " + e.toString() };
  } finally {
    lock.releaseLock();
  }
}

function updateFotoArmada(contents, ss, sheetMap) {
  var armadaId = String(contents.armadaId || "").trim().toUpperCase();
  var base64Photo = contents.base64Photo || contents.fotoBase64 || "";
  var photoMimeType = contents.photoMimeType || "image/jpeg";
  var fotoProfileCol = 12; // Kolom L pada sheet Armada.

  if (!armadaId) return { success: false, message: "Armada ID wajib diisi." };
  if (!base64Photo) return { success: false, message: "Tidak ada data foto yang dikirim." };

  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);

  try {
    var sheet = getSheetByGid(sheetMap, GID_ARMADA) ||
                getSheetByGid(ss, GID_ARMADA) ||
                getSheetByNameFromMap(ss, sheetMap, "ARMADA") ||
                getSheetByNameFromMap(ss, sheetMap, "Armada");
    if (!sheet) return { success: false, message: "Sheet 'Armada' tidak ditemukan." };

    if (sheet.getMaxColumns() < fotoProfileCol) {
      sheet.insertColumnsAfter(sheet.getMaxColumns(), fotoProfileCol - sheet.getMaxColumns());
    }
    var headerCell = sheet.getRange(1, fotoProfileCol).getValue();
    if (!headerCell || String(headerCell).trim() === "") {
      sheet.getRange(1, fotoProfileCol).setValue("FOTO PROFIL ARMADA");
    }

    var linkFoto = saveImageToDrive(
      base64Photo,
      "PROFILE_ARMADA_" + armadaId + ".jpg",
      "foto profil armada",
      photoMimeType
    );
    if (!linkFoto) return { success: false, message: "Foto gagal disimpan ke Google Drive." };

    var data = sheet.getDataRange().getValues();
    var updatedCount = 0;
    for (var i = 1; i < data.length; i++) {
      var cellVal = data[i][0] ? String(data[i][0]).trim().toUpperCase() : "";
      if (cellVal === armadaId) {
        sheet.getRange(i + 1, fotoProfileCol).setValue(linkFoto);
        updatedCount++;
      }
    }

    if (updatedCount === 0) {
      return { success: false, message: "Armada ID " + armadaId + " tidak ditemukan pada sheet Armada." };
    }

    return {
      success: true,
      linkFoto: linkFoto,
      message: "Foto profil armada " + armadaId + " berhasil disimpan pada kolom FOTO PROFIL ARMADA."
    };
  } catch(err) {
    return { success: false, message: "Gagal update foto profil: " + err.toString() };
  }
}

function submitTerkirim(contents, ss, sheetMap) {
  var p = contents.request || contents;
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);

    var targetGid = p.sheetId || contents.sheetId || GID_SURAT_JALAN;
    var logSheet = getSheetByGid(sheetMap, targetGid) 
                || getSheetByGid(sheetMap, GID_SURAT_JALAN)
                || getSheetByNameFromMap(ss, sheetMap, "ARSIP PENGIRIMAN") 
                || getSheetByNameFromMap(ss, sheetMap, "PENCATATAN SURAT JALAN") 
                || getSheetByNameFromMap(ss, sheetMap, "REKAP BUKTI PENGIRIMAN");

    if (!logSheet) {
      logSheet = ss.insertSheet("ARSIP PENGIRIMAN");
      logSheet.appendRow(["Tanggal & Waktu", "Driver", "No Dokumen", "No Surat Jalan", "Penerima / Alamat", "Catatan Driver", "Foto 1 (Depan)", "Foto 2 (Belakang)", "Foto 3 (Kiri)", "Foto 4 (Kanan)", "Foto 5 / Video", "Status"]);
    }
    
    var orderNo = p.noSuratJalan || p.noDokumen || ("ORDER_" + Date.now());
    var folderName = "Bukti_Pengiriman_" + orderNo;
    
    var fileLinks = [];
    if (p.files && p.files.length > 0) {
      for (var i = 0; i < p.files.length; i++) {
        var f = p.files[i];
        if (f.base64) {
          var url = saveImageToDrive(f.base64, f.fileName || ("bukti_" + (i+1) + ".jpg"), folderName, f.mimeType || "image/jpeg");
          if (url) fileLinks.push(url);
        } else if (f.url || f.link) {
          fileLinks.push(f.url || f.link);
        }
      }
    }

    var nowFormatted = Utilities.formatDate(new Date(), "GMT+7", "dd/MM/yyyy HH:mm:ss");
    var driverId = p.driver || p.driverId || p.username || "Driver";
    var noDoc = p.noDokumen || p.noSuratJalan || "";
    var noSj = p.noSuratJalan || p.noDokumen || "";
    var penerima = p.penerima || p.alamat || "";
    var catatan = p.catatan || "";

    var lastCol = logSheet.getLastColumn();
    var lastRow = logSheet.getLastRow();

    if (lastRow >= 1 && lastCol > 0) {
      var headerRow = logSheet.getRange(1, 1, 1, lastCol).getValues()[0];
      var rowData = new Array(lastCol).fill("");
      var fileLinkIndex = 0;
      var hasHeaderMatch = false;

      for (var c = 0; c < lastCol; c++) {
        var h = String(headerRow[c] || "").toLowerCase().trim();
        if (h.indexOf("tgl") > -1 || h.indexOf("tanggal") > -1 || h.indexOf("waktu") > -1 || h.indexOf("time") > -1) {
          rowData[c] = nowFormatted; hasHeaderMatch = true;
        } else if (h.indexOf("driver") > -1 || h.indexOf("sopir") > -1) {
          rowData[c] = driverId; hasHeaderMatch = true;
        } else if (h.indexOf("doc") > -1 || h.indexOf("dokumen") > -1 || h.indexOf("order") > -1) {
          rowData[c] = noDoc; hasHeaderMatch = true;
        } else if (h.indexOf("surat jalan") > -1 || h.indexOf("sj") > -1 || h.indexOf("receive") > -1) {
          rowData[c] = noSj; hasHeaderMatch = true;
        } else if (h.indexOf("penerima") > -1 || h.indexOf("alamat") > -1 || h.indexOf("customer") > -1 || h.indexOf("toko") > -1) {
          rowData[c] = penerima; hasHeaderMatch = true;
        } else if (h.indexOf("catatan") > -1 || h.indexOf("keterangan") > -1 || h.indexOf("alasan") > -1 || h.indexOf("remark") > -1) {
          rowData[c] = catatan; hasHeaderMatch = true;
        } else if (h.indexOf("foto") > -1 || h.indexOf("bukti") > -1 || h.indexOf("image") > -1 || h.indexOf("file") > -1 || h.indexOf("link") > -1 || h.indexOf("video") > -1) {
          if (fileLinkIndex < fileLinks.length) {
            rowData[c] = fileLinks[fileLinkIndex++];
          } else if (fileLinks.length > 0 && fileLinkIndex === 0) {
            rowData[c] = fileLinks.join("\n");
          }
          hasHeaderMatch = true;
        } else if (h.indexOf("status") > -1) {
          rowData[c] = "TERKIRIM"; hasHeaderMatch = true;
        }
      }

      if (hasHeaderMatch) {
        logSheet.appendRow(rowData);
      } else {
        logSheet.appendRow([
          nowFormatted, driverId, noDoc, noSj, penerima, catatan,
          fileLinks[0] || "", fileLinks[1] || "", fileLinks[2] || "", fileLinks[3] || "", fileLinks[4] || "",
          "TERKIRIM"
        ]);
      }
    } else {
      logSheet.appendRow([
        nowFormatted, driverId, noDoc, noSj, penerima, catatan,
        fileLinks[0] || "", fileLinks[1] || "", fileLinks[2] || "", fileLinks[3] || "", fileLinks[4] || "",
        "TERKIRIM"
      ]);
    }

    try {
      var allSheets = sheetMap.sheets || ss.getSheets();
      var targetKeyDoc = noDoc.toLowerCase().trim();
      var targetKeySj = noSj.toLowerCase().trim();

      if (targetKeyDoc || targetKeySj) {
        for (var s = 0; s < allSheets.length; s++) {
          var curSheet = allSheets[s];
          var sName = curSheet.getName().toUpperCase();
          if (sName.indexOf("LOG") > -1 || sName.indexOf("ARSIP") > -1 || sName.indexOf("DRIVER") > -1 || sName.indexOf("ARMADA") > -1) continue;

          var sData = curSheet.getDataRange().getValues();
          for (var r = 0; r < sData.length; r++) {
            var rowStr = sData[r].join(" ").toLowerCase();
            if ((targetKeyDoc && rowStr.indexOf(targetKeyDoc) > -1) || (targetKeySj && rowStr.indexOf(targetKeySj) > -1)) {
              var rowChanged = false;
              for (var c = 0; c < sData[r].length; c++) {
                var cellVal = String(sData[r][c] || "").trim().toUpperCase();
                if (cellVal === "BELUM TERKIRIM" || cellVal === "PROSES" || cellVal === "PENDING" || cellVal === "DALAM PERJALANAN") {
                  sData[r][c] = "TERKIRIM";
                  rowChanged = true;
                }
              }
              if (rowChanged) {
                batchWriteRow(curSheet, r + 1, 1, sData[r]);
              }
            }
          }
        }
      }
    } catch(errUpdate) {
      Logger.log("Update status di sheet utama error: " + errUpdate.toString());
    }

    return { success: true, message: "Bukti terkirim & foto berhasil disimpan ke Google Spreadsheet GID " + logSheet.getSheetId() + "!" };
  } catch(e) {
    return { success: false, message: "Gagal menyimpan bukti terkirim: " + e.toString() };
  } finally {
    lock.releaseLock();
  }
}

// ============================================
// UTILITIES & HELPERS (getSpreadsheet, getOrCreateFolder, saveImageToDrive, sendAdminEmail, getKirPajakMap, getTerkirimSet, parseSheetData)
// ============================================

function getSpreadsheet(e, postData) {
  var id = "";
  if (e && e.parameter && e.parameter.spreadsheetId) id = e.parameter.spreadsheetId;
  if (!id && postData && postData.spreadsheetId) id = postData.spreadsheetId;
  if (!id) {
    try {
      id = PropertiesService.getScriptProperties().getProperty("SPREADSHEET_ID");
    } catch (err) {}
  }
  if (!id) id = DEFAULT_SPREADSHEET_ID;

  if (id) {
    try {
      var opened = SpreadsheetApp.openById(id);
      if (opened) return opened;
    } catch (err) {
      Logger.log("openById failed for ID [" + id + "]: " + err.toString());
    }
  }
  try {
    var active = SpreadsheetApp.getActiveSpreadsheet();
    if (active) return active;
  } catch (err) {}

  throw new Error("Gagal membuka Spreadsheet dengan ID: " + id);
}

function getSheetByGid(ssOrMap, gid) {
  if (!gid) return null;
  var gidStr = String(gid);
  
  if (ssOrMap) {
    if (ssOrMap.byGid && ssOrMap.byGid[gidStr]) {
      return ssOrMap.byGid[gidStr];
    }
    if (ssOrMap[gidStr] && typeof ssOrMap[gidStr].getName === "function") {
      return ssOrMap[gidStr];
    }
    if (typeof ssOrMap.getSheets === "function") {
      try {
        var sheets = ssOrMap.getSheets();
        for (var i = 0; i < sheets.length; i++) {
          if (String(sheets[i].getSheetId()) === gidStr) return sheets[i];
        }
      } catch(e) {}
    }
  }
  return null;
}

function getOrCreateFolder(folderName) {
  var parentId = FOLDER_ID_PENGIRIMAN;
  var fn = (folderName || "").toLowerCase();
  
  if (fn.indexOf("km") !== -1 || fn.indexOf("odometer") !== -1) {
    parentId = FOLDER_ID_KM;
  }
  
  var parentFolder = null;
  try {
    parentFolder = DriveApp.getFolderById(parentId);
  } catch(errParent) {
    try {
      parentFolder = DriveApp.getFolderById(FOLDER_ID_PENGIRIMAN);
    } catch(e2) {
      try { parentFolder = DriveApp.getRootFolder(); } catch(eRoot) {}
    }
  }

  if (!folderName || !parentFolder) {
    return parentFolder;
  }

  try {
    var subFolders = parentFolder.getFoldersByName(folderName);
    if (subFolders.hasNext()) {
      return subFolders.next();
    } else {
      return parentFolder.createFolder(folderName);
    }
  } catch(e) {
    Logger.log("getOrCreateFolder subfolder error: " + e.toString());
    return parentFolder;
  }
}

function saveImageToDrive(base64Str, filename, folderName, mimeType) {
  try {
    if (!base64Str) return "";
    var cleanB64 = String(base64Str).replace(/^data:image\/[a-z]+;base64,/, "").replace(/\s/g, "");
    var folder = getOrCreateFolder(folderName);

    var blob = Utilities.newBlob(Utilities.base64Decode(cleanB64), mimeType || "image/jpeg", filename || "bukti.jpg");
    var file = folder ? folder.createFile(blob) : DriveApp.createFile(blob);
    try {
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    } catch(errShare) {}
    return file.getUrl();
  } catch(e) {
    Logger.log("saveImageToDrive error: " + e.toString());
    return "";
  }
}

function sendAdminEmail(armadaId, sisaKm, driverName, kmTerdeteksi, fotoLink) {
  var adminEmail = getAdminEmail();
  var subject = "⚠️ PENGINGAT SERVIS: Sisa KM Armada " + armadaId + " Kritis!";
  var htmlBody = "<div style='font-family: Arial; padding: 20px; border: 1px solid #ddd; border-radius: 8px;'>" +
                 "<h2>⚠️ PENGINGAT SERVIS ARMADA</h2>" +
                 "<p>Halo Admin,</p>" +
                 "<p>Armada <strong>" + armadaId + "</strong> telah mendekati/melewati batas kilometer servis.</p>" +
                 "<ul>" +
                 "<li><strong>ID Armada:</strong> " + armadaId + "</li>" +
                 "<li><strong>KM Terdeteksi:</strong> " + kmTerdeteksi + " KM</li>" +
                 "<li><strong>Sisa Jarak KM:</strong> <span style='color:red;font-weight:bold;'>" + sisaKm + " KM</span></li>" +
                 "<li><strong>Driver:</strong> " + driverName + "</li>" +
                 "</ul>";
  if (fotoLink) {
    htmlBody += "<p><a href='" + fotoLink + "' target='_blank'>Lihat Foto Odometer</a></p>";
  }
  htmlBody += "</div>";

  try {
    GmailApp.sendEmail(adminEmail, subject, "", { htmlBody: htmlBody });
  } catch(err) {
    Logger.log("Failed send admin email: " + err.toString());
  }
}

function formatDateVal(val) {
  if (!val) return "";
  if (val instanceof Date) return Utilities.formatDate(val, "GMT+7", "dd/MM/yyyy HH:mm");
  var s = String(val).trim();
  if (s.indexOf("GMT") > -1) {
    try {
      var d = new Date(s);
      if (!isNaN(d.getTime())) return Utilities.formatDate(d, "GMT+7", "dd/MM/yyyy HH:mm");
    } catch(e) {}
  }
  return s;
}

function getTerkirimSet(ss, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var terkirimMap = {};
  try {
    var logSheet = getSheetByNameFromMap(ss, sheetMap, "ARSIP PENGIRIMAN") || 
                   getSheetByNameFromMap(ss, sheetMap, "PENCATATAN SURAT JALAN") || 
                   getSheetByNameFromMap(ss, sheetMap, "REKAP BUKTI PENGIRIMAN") || 
                   getSheetByGid(sheetMap, GID_SURAT_JALAN);
    if (logSheet) {
      var lRows = logSheet.getDataRange().getValues();
      for (var j = 1; j < lRows.length; j++) {
        var rNoDoc = String(lRows[j][2] || lRows[j][1] || "").trim().toLowerCase();
        var rSj = String(lRows[j][3] || lRows[j][2] || "").trim().toLowerCase();
        if (rNoDoc) terkirimMap[rNoDoc] = true;
        if (rSj) terkirimMap[rSj] = true;
      }
    }
  } catch(e) {}
  return terkirimMap;
}

function parseSheetData(ss, sheet, tanggalTag, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheet) return [];
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var rows = sheet.getDataRange().getValues();
  if (rows.length <= 1) return [];
  var list = [];
  var terkirimSet = getTerkirimSet(ss, sheetMap);

  var colOrder = 0, colReceive = 1, colAddress = 2, colReason = 3, colShipTo = 4, colTelp = 5, colCbm = 6, colArmada = 7, colDriver = 8, colKenek = 9, colStatus = -1;

  var maxHeaderMatches = 0;
  for (var hIdx = 0; hIdx < Math.min(25, rows.length); hIdx++) {
    var hRow = rows[hIdx];
    if (!hRow) continue;
    var matches = 0;
    var tOrder = colOrder, tReceive = colReceive, tAddress = colAddress, tReason = colReason;
    var tShipTo = colShipTo, tTelp = colTelp, tCbm = colCbm, tArmada = colArmada, tDriver = colDriver, tKenek = colKenek, tStatus = colStatus;

    for (var c = 0; c < hRow.length; c++) {
      var cellVal = String(hRow[c] || "").trim().toLowerCase();
      if (!cellVal) continue;

      if (cellVal.indexOf("order") > -1 || cellVal.indexOf("no doc") > -1 || cellVal.indexOf("dokumen") > -1) { tOrder = c; matches++; }
      else if (cellVal.indexOf("receive") > -1 || cellVal.indexOf("surat jalan") > -1 || cellVal.indexOf("sj") > -1) { tReceive = c; matches++; }
      else if (cellVal.indexOf("alamat") > -1 || cellVal.indexOf("address") > -1 || cellVal.indexOf("tujuan") > -1) { tAddress = c; matches++; }
      else if (cellVal.indexOf("alasan") > -1 || cellVal.indexOf("catatan") > -1 || cellVal.indexOf("keterangan") > -1 || cellVal.indexOf("reason") > -1 || cellVal.indexOf("remark") > -1) { tReason = c; matches++; }
      else if (cellVal.indexOf("ship to") > -1 || cellVal.indexOf("penerima") > -1 || cellVal.indexOf("customer") > -1 || cellVal.indexOf("toko") > -1) { tShipTo = c; matches++; }
      else if (cellVal.indexOf("telp") > -1 || cellVal.indexOf("hp") > -1 || cellVal.indexOf("phone") > -1) { tTelp = c; matches++; }
      else if (cellVal.indexOf("cbm") > -1 || cellVal.indexOf("vol") > -1) { tCbm = c; matches++; }
      else if (cellVal.indexOf("armada") > -1 || cellVal.indexOf("nopol") > -1 || cellVal.indexOf("unit") > -1) { tArmada = c; matches++; }
      else if (cellVal.indexOf("driver") > -1 || cellVal.indexOf("sopir") > -1) { tDriver = c; matches++; }
      else if (cellVal.indexOf("kenek") > -1 || cellVal.indexOf("helper") > -1) { tKenek = c; matches++; }
      else if (cellVal.indexOf("status") > -1) { tStatus = c; matches++; }
    }

    if (matches > maxHeaderMatches) {
      maxHeaderMatches = matches;
      colOrder = tOrder;
      colReceive = tReceive;
      colAddress = tAddress;
      colReason = tReason;
      colShipTo = tShipTo;
      colTelp = tTelp;
      colCbm = tCbm;
      colArmada = tArmada;
      colDriver = tDriver;
      colKenek = tKenek;
      colStatus = tStatus;
    }
  }

  var currentArmada = "";
  var currentJalur = "";
  var currentDriver = "";
  var currentKenek = "";

  var commonCities = [
    "MADIUN", "JOMBANG", "NGANJUK", "KEDIRI", "BLITAR", "TRENGGALEK", "TULUNGAGUNG",
    "MALANG", "SURABAYA", "PONOROGO", "MAGETAN", "NGAWI", "PACITAN", "TUBAN",
    "LAMONGAN", "BOJONEGORO", "GRESIK", "MOJOKERTO", "PASURUAN", "PROBOLINGGO",
    "SIDOARJO", "GUDANG", "BANYUWANGI", "JEMBER", "LUMAJANG"
  ];

  for (var i = 0; i < rows.length; i++) {
    var r = rows[i];
    if (!r) continue;

    var combinedRowStr = r.join(" ").toUpperCase().trim();
    if (!combinedRowStr) continue;

    var col0 = (colOrder >= 0 && colOrder < r.length) ? String(r[colOrder] || "").trim() : "";
    var col1 = (colReceive >= 0 && colReceive < r.length) ? String(r[colReceive] || "").trim() : "";
    var c0Upper = col0.toUpperCase();
    var c1Upper = col1.toUpperCase();

    if (c0Upper.indexOf("NO ORDER") !== -1 || c0Upper.indexOf("NO DOC") !== -1 || c0Upper === "NO" ||
        c1Upper.indexOf("NO RECEIVE") !== -1 || c1Upper.indexOf("NO SURAT JALAN") !== -1 ||
        c0Upper === "ADDRESS" || c1Upper === "ADDRESS" || combinedRowStr.indexOf("NO ORDER NO RECEIVE") !== -1 ||
        (combinedRowStr.indexOf("NO ORDER") !== -1 && combinedRowStr.indexOf("NO RECEIVE") !== -1) ||
        combinedRowStr.indexOf("GRAND TOTAL") !== -1 || combinedRowStr.indexOf("TOTAL ORDER") !== -1) {
      continue;
    }

    var hasDocNumber = isDataRow(col0, col1, r);

    if (!hasDocNumber) {
      var hkMatch = combinedRowStr.match(/HK\s*\d+/i) || combinedRowStr.match(/\b[A-Z]{1,2}\s*\d{3,4}\s*[A-Z]{1,3}\b/i);
      if (hkMatch) {
        currentArmada = hkMatch[0].replace(/\s+/g, "").toUpperCase();
      }

      if (combinedRowStr.indexOf("DRIVER") > -1) {
        var drvParts = combinedRowStr.split("DRIVER");
        if (drvParts.length > 1) {
          var drvText = drvParts[1].replace(/[:=]/g, "").trim().split(/\s{2,}|HK|\+|\-/)[0];
          if (drvText) currentDriver = drvText;
        }
      }

      if (combinedRowStr.indexOf("KENEK") > -1 || combinedRowStr.indexOf("HELPER") > -1) {
        var knkParts = combinedRowStr.split(/KENEK|HELPER/);
        if (knkParts.length > 1) {
          var knkText = knkParts[1].replace(/[:=]/g, "").trim().split(/\s{2,}|HK|\+|\-/)[0];
          if (knkText) currentKenek = knkText;
        }
      }

      var foundCities = [];
      for (var cIdx = 0; cIdx < commonCities.length; cIdx++) {
        if (combinedRowStr.indexOf(commonCities[cIdx]) > -1) {
          foundCities.push(commonCities[cIdx]);
        }
      }

      if (foundCities.length > 0) {
        currentJalur = foundCities.join(" + ");
      } else {
        var cleanBanner = combinedRowStr
          .replace(/HK\s*\d+/gi, "")
          .replace(/DRIVER\s*:?\s*[A-Z\s]+/gi, "")
          .replace(/KENEK\s*:?\s*[A-Z\s]+/gi, "")
          .trim();
        if (cleanBanner.length > 2 && cleanBanner !== "NO RECEIVE" && cleanBanner !== "ADDRESS" && cleanBanner !== "REASON") {
          currentJalur = cleanBanner;
        }
      }

      continue;
    }

    var address = (colAddress >= 0 && colAddress < r.length) ? String(r[colAddress] || "").trim() : "";
    var reason = (colReason >= 0 && colReason < r.length) ? String(r[colReason] || "").trim() : "";
    var shipto = (colShipTo >= 0 && colShipTo < r.length) ? String(r[colShipTo] || "").trim() : "";
    var telp = (colTelp >= 0 && colTelp < r.length) ? String(r[colTelp] || "").trim() : "";
    var cbmVal = (colCbm >= 0 && colCbm < r.length) ? (parseFloat(String(r[colCbm] || "0").replace(",", ".")) || 0.0) : 0.0;
    
    var armadaVal = (colArmada >= 0 && colArmada < r.length && String(r[colArmada]).trim()) ? String(r[colArmada]).trim() : currentArmada;
    var driverVal = (colDriver >= 0 && colDriver < r.length && String(r[colDriver]).trim()) ? String(r[colDriver]).trim() : currentDriver;
    var kenekVal = (colKenek >= 0 && colKenek < r.length && String(r[colKenek]).trim()) ? String(r[colKenek]).trim() : currentKenek;
    
    var tujuanVal = currentJalur || address;

    var rowStatus = "Belum Berangkat";
    if (colStatus >= 0 && colStatus < r.length && r[colStatus]) {
      rowStatus = String(r[colStatus]).trim();
    }

    var docNum = col0 || col1;
    var sjNum = col1 || col0;

    if (!docNum) {
      for (var cellIdx = 0; cellIdx < r.length; cellIdx++) {
        var cellContent = String(r[cellIdx] || "").trim();
        if (cellContent && cellIdx !== colAddress && cellIdx !== colShipTo && cellContent.length >= 2) {
          docNum = cellContent;
          sjNum = cellContent;
          break;
        }
      }
    }
    if (!docNum) docNum = "ORD-" + (list.length + 1);
    if (!sjNum) sjNum = docNum;

    var docKey1 = docNum.toLowerCase();
    var docKey2 = sjNum.toLowerCase();
    if ((docKey1 && terkirimSet[docKey1]) || (docKey2 && terkirimSet[docKey2])) {
      rowStatus = "TERKIRIM";
    }

    list.push({
      id: list.length + 1,
      noDokumen: docNum,
      noSuratJalan: sjNum,
      tanggal: tanggalTag,
      driver: driverVal,
      driver1: driverVal,
      driver2: kenekVal,
      armada: armadaVal,
      gudangAsal: "",
      tujuan: tujuanVal,
      alamat: address,
      penerima: shipto,
      noTelpCustomer: telp,
      jumlahKoli: 1,
      volumeCbm: cbmVal,
      status: rowStatus,
      catatan: reason,
      remarks: reason
    });
  }

  return list;
}

function isDataRow(col0, col1, rowArray) {
  var s0 = col0 ? String(col0).trim() : "";
  var s1 = col1 ? String(col1).trim() : "";

  function checkDoc(str) {
    if (!str) return false;
    var clean = str.replace(/\s+/g, "");
    if (clean.length < 2) return false;
    if (/^HK\s*\d{1,2}$/i.test(clean)) return false;
    var digits = clean.replace(/\D/g, "");
    if (digits.length >= 1) return true;
    if (/^[A-Z0-9\-\/]{3,}$/i.test(clean)) return true;
    return false;
  }

  if (checkDoc(s0) || checkDoc(s1)) return true;

  var nonCount = 0;
  for (var k = 0; k < Math.min(10, rowArray.length); k++) {
    var cellText = String(rowArray[k] || "").trim();
    if (!cellText) continue;
    nonCount++;
    if (checkDoc(cellText) && cellText.toUpperCase().indexOf("HEADER") === -1 && cellText.toUpperCase().indexOf("NO ") === -1 && cellText.toUpperCase().indexOf("DRIVER") === -1) {
      return true;
    }
  }

  if (nonCount >= 2) {
    var combined = rowArray.join(" ").toUpperCase();
    if (combined.indexOf("NO ORDER") === -1 && combined.indexOf("NO RECEIVE") === -1 && combined.indexOf("DRIVER:") === -1 && combined.indexOf("KENEK:") === -1) {
      return true;
    }
  }

  return false;
}

/**
 * Memindai foto odometer menggunakan Gemini API (OCR)
 * Mengembalikan objek respon dengan properti success dan km
 */
function extractKmFromImage(base64Data) {
  try {
    if (!base64Data) {
      return { success: false, message: "Tidak ada data foto yang diterima." };
    }

    var cleanB64 = String(base64Data).replace(/^data:image\/[a-z]+;base64,/, "").replace(/\s/g, "");
    var apiKey = PropertiesService.getScriptProperties().getProperty("GEMINI_API_KEY") 
                 || "AQ.Ab8RN6KlZ_4E0UXZKnsr5e-ex9rV4ISxkDCrNiEjOo9KQfdlAQ";

    if (!apiKey || apiKey === "AQ.Ab8RN6KlZ_4E0UXZKnsr5e-ex9rV4ISxkDCrNiEjOo9KQfdlAQ") {
      return { 
        success: false, 
        message: "API Key Gemini belum dikonfigurasi di Script Properties SCRIPT_PROPERTIES. Silakan atur GEMINI_API_KEY." 
      };
    }

    var models = ["gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro"];
    var lastError = "";

    for (var i = 0; i < models.length; i++) {
      var model = models[i];
      var url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

      var payload = {
        "contents": [
          {
            "parts": [
              { "text": "Tolong baca angka odometer (kilometer saat ini) dari foto ini. Hanya kembalikan angkanya saja dalam format integer murni tanpa teks/tambahan/simbol apa pun (contoh: 124530). Jika angka tidak terbaca, kembalikan 'null'." },
              {
                "inlineData": {
                  "mimeType": "image/jpeg",
                  "data": cleanB64
                }
              }
            ]
          }
        ],
        "generationConfig": {
          "responseMimeType": "text/plain"
        }
      };

      var options = {
        "method": "post",
        "contentType": "application/json",
        "payload": JSON.stringify(payload),
        "muteHttpExceptions": true
      };

      try {
        var response = UrlFetchApp.fetch(url, options);
        var responseCode = response.getResponseCode();
        var responseText = response.getContentText();

        if (responseCode === 200) {
          var result = JSON.parse(responseText);
          if (result.candidates && result.candidates.length > 0) {
            var text = result.candidates[0].content.parts[0].text.trim();
            var digits = text.replace(/\D/g, "");
            var km = parseInt(digits, 10);
            if (!isNaN(km)) {
              return { success: true, km: km };
            }
          }
        } else {
          lastError = "Response Code " + responseCode + ": " + responseText;
        }
      } catch (e) {
        lastError = e.toString();
      }
    }

    return { success: false, message: "Gagal membaca odometer: " + lastError };
  } catch (err) {
    return { success: false, message: "Error OCR: " + err.toString() };
  }
}

// ============================================
// FITUR PENGAJUAN (BAN & AKSESORIS - GID 1517362778)
// ============================================

function submitPengajuan(contents, ss, sheetMap) {
  var p = contents.request || contents;
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);

    var targetGid = p.sheetId || contents.sheetId || GID_PENGAJUAN;
    var sheet = getSheetByGid(sheetMap, targetGid) 
             || getSheetByGid(ss, targetGid)
             || getSheetByNameFromMap(ss, sheetMap, "PENGAJUAN")
             || getSheetByNameFromMap(ss, sheetMap, "PENGAJUAN BARANG");

    if (!sheet) {
      sheet = ss.insertSheet("PENGAJUAN");
    }

    if (sheet.getLastRow() < 1 || sheet.getLastColumn() < 1) {
      sheet.appendRow([
        "Tanggal & Waktu", "No Pengajuan", "Driver", "ID Armada", "No Polisi", 
        "Kategori", "Detail Barang/Ban", "Catatan Driver", 
        "Foto 1 (Tahun/Utama)", "Foto 2 (Barcode)", "Foto 3 (View 1)", "Foto 4 (View 2)", "Foto Lainnya", "Status"
      ]);
    }

    var nowFormatted = Utilities.formatDate(new Date(), "GMT+7", "dd/MM/yyyy HH:mm:ss");
    var todayStr = Utilities.formatDate(new Date(), "GMT+7", "dd/MM/yyyy");
    var subfolderName = "PENGAJUAN" + todayStr;

    var parentFolderId = FOLDER_ID_PENGAJUAN;
    var subFolder = getOrCreateSubFolderById(parentFolderId, subfolderName);

    var fileLinks = [];
    if (p.files && p.files.length > 0) {
      for (var i = 0; i < p.files.length; i++) {
        var f = p.files[i];
        if (f.base64) {
          var tag = f.fileTag ? ("_" + String(f.fileTag).replace(/[^a-zA-Z0-9]/g, "_")) : ("_foto_" + (i+1));
          var fileName = (p.armadaId || "ARMADA") + tag + "_" + Date.now() + ".jpg";
          var url = saveImageToSubFolder(f.base64, fileName, subFolder, f.mimeType || "image/jpeg");
          if (url) fileLinks.push(url);
        } else if (f.url || f.link) {
          fileLinks.push(f.url || f.link);
        }
      }
    }

    var noPengajuan = "PGJ-" + Date.now();
    var driver = p.driver || p.driverName || "Driver";
    var armadaId = p.armadaId || "";
    var noPolisi = p.noPolisi || "";
    var kategori = p.kategori || "Aksesoris";
    var detail = p.detail || "";
    var catatan = p.catatan || p.keterangan || "";

    var foto1 = fileLinks[0] || "";
    var foto2 = fileLinks[1] || "";
    var foto3 = fileLinks[2] || "";
    var foto4 = fileLinks[3] || "";
    var fotoLainnya = (fileLinks.length > 4) ? fileLinks.slice(4).join("\n") : "";

    sheet.appendRow([
      nowFormatted, noPengajuan, driver, armadaId, noPolisi,
      kategori, detail, catatan,
      foto1, foto2, foto3, foto4, fotoLainnya, "PENDING"
    ]);

    return { 
      success: true, 
      noPengajuan: noPengajuan, 
      message: "Pengajuan " + kategori + " berhasil dikirim & disimpan ke Google Sheets (GID " + GID_PENGAJUAN + ")!" 
    };
  } catch(e) {
    return { success: false, message: "Gagal memproses pengajuan: " + e.toString() };
  } finally {
    lock.releaseLock();
  }
}

function getPengajuan(ss, limit, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  limit = limit || 100;

  var sheet = getSheetByGid(sheetMap, GID_PENGAJUAN) 
           || getSheetByGid(ss, GID_PENGAJUAN)
           || getSheetByNameFromMap(ss, sheetMap, "PENGAJUAN");

  var list = [];
  if (sheet) {
    var data = sheet.getDataRange().getDisplayValues();
    if (data.length > 1) {
      for (var i = 1; i < data.length; i++) {
        var row = data[i];
        if (row[0] || row[1] || row[3]) {
          list.push({
            id: i,
            tanggal: row[0] || "",
            noPengajuan: row[1] || "",
            driver: row[2] || "",
            armadaId: row[3] || "",
            noPolisi: row[4] || "",
            kategori: row[5] || "",
            detail: row[6] || "",
            catatan: row[7] || "",
            foto1Url: row[8] || "",
            foto2Url: row[9] || "",
            foto3Url: row[10] || "",
            foto4Url: row[11] || "",
            fotoLainnyaUrls: row[12] || "",
            status: row[13] || "PENDING"
          });
        }
      }
    }
  }
  return list;
}

function getOrCreateSubFolderById(parentFolderId, subfolderName) {
  var parentFolder = null;
  try {
    parentFolder = DriveApp.getFolderById(parentFolderId);
  } catch(err) {
    try { parentFolder = DriveApp.getFolderById(FOLDER_ID_PENGAJUAN); } catch(e2) {}
  }
  if (!parentFolder) {
    try { parentFolder = DriveApp.getRootFolder(); } catch(eRoot) {}
  }

  try {
    var subFolders = parentFolder.getFoldersByName(subfolderName);
    if (subFolders.hasNext()) {
      return subFolders.next();
    } else {
      return parentFolder.createFolder(subfolderName);
    }
  } catch(e) {
    Logger.log("getOrCreateSubFolderById error: " + e.toString());
    return parentFolder;
  }
}

function saveImageToSubFolder(base64Str, filename, folderObj, mimeType) {
  try {
    if (!base64Str) return "";
    var cleanB64 = String(base64Str).replace(/^data:image\/[a-z]+;base64,/, "").replace(/\s/g, "");
    var blob = Utilities.newBlob(Utilities.base64Decode(cleanB64), mimeType || "image/jpeg", filename || "foto.jpg");
    var file = folderObj ? folderObj.createFile(blob) : DriveApp.createFile(blob);
    try {
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    } catch(errShare) {}
    return file.getUrl();
  } catch(e) {
    Logger.log("saveImageToSubFolder error: " + e.toString());
    return "";
  }
}

// ============================================
// FITUR PENGIRIMAN & AI (getPengiriman, getAiKnowledge, handleAsistenAi)
// ============================================

function getPengiriman(ss, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);
  var allSheets = sheetMap.sheets || ss.getSheets();
  var pengirimanList = [];

  for (var i = 0; i < allSheets.length; i++) {
    var sheet = allSheets[i];
    var sName = sheet.getName().trim();
    var sNameUpper = sName.toUpperCase();

    // Ignore non-delivery system sheets
    if (sNameUpper.indexOf("LOG") > -1 || 
        sNameUpper.indexOf("ARMADA") > -1 || 
        sNameUpper.indexOf("DRIVER") > -1 || 
        sNameUpper.indexOf("BAN") > -1 || 
        sNameUpper.indexOf("AKI") > -1 || 
        sNameUpper.indexOf("KIR") > -1 || 
        sNameUpper.indexOf("PENGAJUAN") > -1 || 
        sNameUpper.indexOf("ARSIP") > -1 || 
        sNameUpper.indexOf("REKAP") > -1 ||
        sNameUpper.indexOf("CATATAN") > -1) {
      continue;
    }

    var items = parseSheetData(ss, sheet, sName, sheetMap);
    for (var j = 0; j < items.length; j++) {
      items[j].id = pengirimanList.length + 1;
      pengirimanList.push(items[j]);
    }
  }
  return pengirimanList;
}

function getAiKnowledge(ss, sheetMap) {
  if (!ss) ss = getSpreadsheet();
  if (!sheetMap) sheetMap = getSheetMap(ss);

  var knowledgeList = [];
  try {
    var sheet = getSheetByNameFromMap(ss, sheetMap, "AI_KNOWLEDGE") || 
                getSheetByNameFromMap(ss, sheetMap, "KNOWLEDGE") || 
                getSheetByNameFromMap(ss, sheetMap, "FAQ");

    if (sheet) {
      var data = sheet.getDataRange().getDisplayValues();
      for (var i = 1; i < data.length; i++) {
        if (data[i][0] || data[i][1]) {
          knowledgeList.push({
            id: String(data[i][0] || ("K" + i)),
            kategori: String(data[i][1] || "Umum"),
            pertanyaan: String(data[i][2] || data[i][0] || ""),
            jawaban: String(data[i][3] || data[i][1] || "")
          });
        }
      }
    }
  } catch (e) {}

  if (knowledgeList.length === 0) {
    knowledgeList = [
      {
        id: "K01",
        kategori: "Perawatan Armada",
        pertanyaan: "Kapan jadwal service berkala armada truk?",
        jawaban: "Service berkala dilakukan setiap kelipatan 5.000 KM. Pastikan melakukan input KM Odometer harian melalui aplikasi agar status service terpantau."
      },
      {
        id: "K02",
        kategori: "Pengiriman Surat Jalan",
        pertanyaan: "Bagaimana prosedur konfirmasi pengiriman terkirim?",
        jawaban: "Driver wajib mengambil foto bukti serah terima (foto barang & surat jalan bertanda tangan) lalu mengunggahnya melalui menu Rekap Surat Jalan."
      },
      {
        id: "K03",
        kategori: "Pemeriksaan Ban & Aki",
        pertanyaan: "Kapan aki dan ban harus dicek / diganti?",
        jawaban: "Pemeriksaan tekanan ban dilakukan setiap hari sebelum berangkat. Penggantian aki direkomendasikan setiap 2 tahun sekali."
      }
    ];
  }

  return knowledgeList;
}

function handleAsistenAi(contents) {
  try {
    var chatMsg = String(contents.chatMessage || contents.prompt || contents.message || "").trim();
    var b64 = contents.base64Data || contents.base64Photo || "";
    var userApiKey = contents.apiKey || "";

    if (!chatMsg && !b64) {
      return { success: false, message: "Pesan atau foto tidak boleh kosong." };
    }

    var apiKey = userApiKey || PropertiesService.getScriptProperties().getProperty("GEMINI_API_KEY") 
                 || "AQ.Ab8RN6KlZ_4E0UXZKnsr5e-ex9rV4ISxkDCrNiEjOo9KQfdlAQ";

    var systemInstruction = "Anda adalah Asisten AI Operasional Armada HUB Kediri (INFORMA / Kawan Lama Group). " +
      "Tugas Anda membantu Driver dan Tim Operasional terkait panduan pengiriman surat jalan, perawatan armada truk (oli, ban, aki, service), " +
      "pencatatan odometer KM, dan prosedur K3 pengiriman. Jawab dengan singkat, sopan, profesional, dan akurat.";

    var models = ["gemini-2.0-flash", "gemini-1.5-flash"];
    var promptText = systemInstruction + "\n\nPertanyaan User: " + (chatMsg || "Analisis foto ini terkait armada.");

    for (var i = 0; i < models.length; i++) {
      var model = models[i];
      var url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

      var parts = [{ "text": promptText }];
      if (b64) {
        var cleanB64 = String(b64).replace(/^data:image\/[a-z]+;base64,/, "").replace(/\s/g, "");
        parts.push({
          "inlineData": {
            "mimeType": contents.mimeType || "image/jpeg",
            "data": cleanB64
          }
        });
      }

      var payload = {
        "contents": [{ "parts": parts }],
        "generationConfig": { "responseMimeType": "text/plain" }
      };

      var options = {
        "method": "post",
        "contentType": "application/json",
        "payload": JSON.stringify(payload),
        "muteHttpExceptions": true
      };

      try {
        var response = UrlFetchApp.fetch(url, options);
        if (response.getResponseCode() === 200) {
          var resJson = JSON.parse(response.getContentText());
          if (resJson.candidates && resJson.candidates.length > 0) {
            var answer = resJson.candidates[0].content.parts[0].text.trim();
            return { success: true, message: answer };
          }
        }
      } catch (e) {}
    }

    return { 
      success: true, 
      message: "Sistem Asisten AI HUB Kediri: Silakan pastikan kilometer odometer dicatat secara rutin setiap hari, cek kondisi ban & aki sebelum berangkat, dan simpan foto bukti surat jalan saat barang terkirim." 
    };
  } catch (err) {
    return { success: false, message: "Gagal memproses Asisten AI: " + err.toString() };
  }
}

// ============================================
// STRUCTURAL SETUP & SHEET FORMATTING (setupAllSheets)
// ============================================

/**
 * Master Setup: Merapikan & Membangun SELURUH Sheet Aplikasi sekaligus!
 * Versi Batch Super Cepat & Bebas Error.
 */
function setupAllSheets(ss) {
  if (!ss) ss = getSpreadsheet();
  
  try {
    setupSheetArmada(ss);
    setupSheetPengiriman(ss);
    setupSheetLogKM(ss);
    setupSheetDriver(ss);
    setupSheetBan(ss);
    setupSheetAki(ss);
    setupSheetSparepart(ss);
    
    try {
      SpreadsheetApp.getUi().alert("🎉 SUCCESS! Seluruh Sheet (Armada, Pengiriman, LogKM, Driver, Ban, Aki, Sparepart) telah berhasil dirapikan secara instan!");
    } catch(uiErr) {
      Logger.log("Setup All Sheets Completed!");
    }
    
    return { success: true, message: "Seluruh Sheet (Armada, Pengiriman, LogKM, Driver, Ban, Aki, Sparepart) telah berhasil dirapikan secara instan!" };
  } catch(e) {
    return { success: false, message: "Gagal memproses setupAllSheets: " + e.toString() };
  }
}

// -------------------------------------------------------------
// 1. SHEET ARMADA
// -------------------------------------------------------------
function setupSheetArmada(ss) {
  var sheet = ss.getSheetByName("Armada") || ss.insertSheet("Armada");
  var headers = [["Armada ID", "No Polisi", "KM Saat Ini", "KM Service Terakhir", "Interval Service", "KM Service Berikutnya", "Sisa KM", "Status", "Flag", "Foto KM", "Catatan", "Pajak Tahunan", "KIR Date", "Pajak 5 Tahunan", "Foto Truck"]];
  
  applyHeaderStyle(sheet, headers, "#0A2540");
  
  var maxRows = Math.max(sheet.getLastRow(), 50);
  var numRows = maxRows - 1;

  sheet.getRange(2, 3, numRows, 5).setNumberFormat("#,##0");
  sheet.getRange(2, 12, numRows, 3).setNumberFormat("yyyy-mm-dd");
  sheet.getRange(2, 1, numRows, 2).setHorizontalAlignment("center");
  sheet.getRange(2, 8, numRows, 2).setHorizontalAlignment("center");
  sheet.getRange(2, 12, numRows, 3).setHorizontalAlignment("center");

  // Batch Formula (Diisi Sekaligus Tanpa Loop Lambat)
  var formulas = [];
  for (var r = 2; r <= maxRows; r++) {
    formulas.push([
      '=IF(D' + r + '="","", D' + r + ' + E' + r + ')',
      '=IF(F' + r + '="","", F' + r + ' - C' + r + ')',
      '=IF(G' + r + '="","",' +
        'IF(G' + r + '<=0, "SERVIS SEKARANG",' +
        'IF(G' + r + '<=1000, "⚠️ SERVICE <1000 KM", "AMAN")))'
    ]);
  }
  sheet.getRange(2, 6, numRows, 3).setFormulas(formulas);

  // Conditional Formatting
  var statusRange = sheet.getRange(2, 8, numRows, 1);
  var ruleRed = SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("SERVIS SEKARANG").setBackground("#FEE2E2").setFontColor("#991B1B").setBold(true).setRanges([statusRange]).build();
  var ruleYellow = SpreadsheetApp.newConditionalFormatRule().whenTextContains("⚠️ SERVICE <1000 KM").setBackground("#FEF3C7").setFontColor("#92400E").setBold(true).setRanges([statusRange]).build();
  var ruleGreen = SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("AMAN").setBackground("#DCFCE7").setFontColor("#166534").setBold(true).setRanges([statusRange]).build();
  sheet.setConditionalFormatRules([ruleRed, ruleYellow, ruleGreen]);
  
  applyGridAndResize(sheet, headers[0].length, maxRows);
}

// -------------------------------------------------------------
// 2. SHEET PENGIRIMAN
// -------------------------------------------------------------
function setupSheetPengiriman(ss) {
  var sheet = ss.getSheetByName("Pengiriman") || ss.getSheetByName("SuratJalan") || ss.insertSheet("Pengiriman");
  var headers = [["No Dokumen", "No Surat Jalan", "Tanggal", "Driver", "Armada", "Gudang Asal", "Tujuan", "Alamat", "Penerima", "No Telp Customer", "Jumlah Koli", "Volume CBM", "Status", "Catatan"]];
  
  applyHeaderStyle(sheet, headers, "#1E3A8A");
  var maxRows = Math.max(sheet.getLastRow(), 50);
  var numRows = maxRows - 1;
  
  sheet.getRange(2, 3, numRows, 1).setNumberFormat("yyyy-mm-dd");
  sheet.getRange(2, 11, numRows, 1).setNumberFormat("#,##0");
  sheet.getRange(2, 12, numRows, 1).setNumberFormat("0.00");
  sheet.getRange(2, 1, numRows, 5).setHorizontalAlignment("center");
  sheet.getRange(2, 13, numRows, 1).setHorizontalAlignment("center");

  var statusRange = sheet.getRange(2, 13, numRows, 1);
  var ruleTerkirim = SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("TERKIRIM").setBackground("#DCFCE7").setFontColor("#166534").setBold(true).setRanges([statusRange]).build();
  var ruleJalan = SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("Dalam Perjalanan").setBackground("#E0F2FE").setFontColor("#075985").setBold(true).setRanges([statusRange]).build();
  var rulePending = SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("Belum Berangkat").setBackground("#F1F5F9").setFontColor("#475569").setRanges([statusRange]).build();
  sheet.setConditionalFormatRules([ruleTerkirim, ruleJalan, rulePending]);

  applyGridAndResize(sheet, headers[0].length, maxRows);
}

// -------------------------------------------------------------
// 3. SHEET LOG KM
// -------------------------------------------------------------
function setupSheetLogKM(ss) {
  var sheet = ss.getSheetByName("LogKM") || ss.insertSheet("LogKM");
  var headers = [["Timestamp", "Armada ID", "Driver", "KM Odometer", "Foto KM", "Status", "Catatan"]];
  
  applyHeaderStyle(sheet, headers, "#0D9488");
  var maxRows = Math.max(sheet.getLastRow(), 50);
  var numRows = maxRows - 1;
  
  sheet.getRange(2, 1, numRows, 1).setNumberFormat("yyyy-mm-dd hh:mm:ss");
  sheet.getRange(2, 4, numRows, 1).setNumberFormat("#,##0");
  sheet.getRange(2, 1, numRows, 3).setHorizontalAlignment("center");
  sheet.getRange(2, 6, numRows, 1).setHorizontalAlignment("center");

  applyGridAndResize(sheet, headers[0].length, maxRows);
}

// -------------------------------------------------------------
// 4. SHEET DRIVER
// -------------------------------------------------------------
function setupSheetDriver(ss) {
  var sheet = ss.getSheetByName("Driver") || ss.insertSheet("Driver");
  var headers = [["Driver ID", "Nama Driver", "No HP", "Status", "Foto Profile"]];
  
  applyHeaderStyle(sheet, headers, "#312E81");
  var maxRows = Math.max(sheet.getLastRow(), 30);
  var numRows = maxRows - 1;
  
  sheet.getRange(2, 1, numRows, 1).setHorizontalAlignment("center");
  sheet.getRange(2, 3, numRows, 2).setHorizontalAlignment("center");

  applyGridAndResize(sheet, headers[0].length, maxRows);
}

// -------------------------------------------------------------
// 5. SHEET BAN
// -------------------------------------------------------------
function setupSheetBan(ss) {
  var sheet = ss.getSheetByName("Ban") || ss.insertSheet("Ban");
  var headers = [["Armada ID", "Position", "Brand", "Serial Number", "Tread Depth (mm)", "Status", "Last Inspection Date"]];
  
  applyHeaderStyle(sheet, headers, "#7C2D12");
  var maxRows = Math.max(sheet.getLastRow(), 50);
  var numRows = maxRows - 1;
  
  sheet.getRange(2, 5, numRows, 1).setNumberFormat("0.0");
  sheet.getRange(2, 7, numRows, 1).setNumberFormat("yyyy-mm-dd");
  sheet.getRange(2, 1, numRows, 2).setHorizontalAlignment("center");
  sheet.getRange(2, 6, numRows, 2).setHorizontalAlignment("center");

  applyGridAndResize(sheet, headers[0].length, maxRows);
}

// -------------------------------------------------------------
// 6. SHEET AKI
// -------------------------------------------------------------
function setupSheetAki(ss) {
  var sheet = ss.getSheetByName("Aki") || ss.insertSheet("Aki");
  var headers = [["Armada ID", "No Polisi", "Brand", "Serial Number", "Tanggal Pemasangan", "Usia Aki (Hari)", "Status"]];
  
  applyHeaderStyle(sheet, headers, "#991B1B");
  var maxRows = Math.max(sheet.getLastRow(), 50);
  var numRows = maxRows - 1;
  
  sheet.getRange(2, 5, numRows, 1).setNumberFormat("yyyy-mm-dd");
  sheet.getRange(2, 6, numRows, 1).setNumberFormat("#,##0");
  sheet.getRange(2, 1, numRows, 2).setHorizontalAlignment("center");
  sheet.getRange(2, 5, numRows, 3).setHorizontalAlignment("center");

  // Batch Formula Aki
  var formulas = [];
  for (var r = 2; r <= maxRows; r++) {
    formulas.push([
      '=IF(E' + r + '="","", INT(TODAY() - E' + r + '))',
      '=IF(F' + r + '="","",' +
        'IF(F' + r + '>=730, "🚨 GANTI AKI (>2 THN)",' +
        'IF(F' + r + '>=660, "⚠️ CEK AKI (<2 BLN)", "NORMAL")))'
    ]);
  }
  sheet.getRange(2, 6, numRows, 2).setFormulas(formulas);

  var statusRange = sheet.getRange(2, 7, numRows, 1);
  var ruleRed = SpreadsheetApp.newConditionalFormatRule().whenTextContains("GANTI AKI").setBackground("#FEE2E2").setFontColor("#991B1B").setBold(true).setRanges([statusRange]).build();
  var ruleYellow = SpreadsheetApp.newConditionalFormatRule().whenTextContains("CEK AKI").setBackground("#FEF3C7").setFontColor("#92400E").setBold(true).setRanges([statusRange]).build();
  var ruleGreen = SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("NORMAL").setBackground("#DCFCE7").setFontColor("#166534").setBold(true).setRanges([statusRange]).build();
  sheet.setConditionalFormatRules([ruleRed, ruleYellow, ruleGreen]);

  applyGridAndResize(sheet, headers[0].length, maxRows);
}

// -------------------------------------------------------------
// 7. SHEET SPAREPART
// -------------------------------------------------------------
function setupSheetSparepart(ss) {
  var sheet = ss.getSheetByName("Sparepart") || ss.insertSheet("Sparepart");
  var headers = [["Item ID", "Nama Item", "Kategori", "Stok", "Batas Minimum", "Unit", "Status"]];
  
  applyHeaderStyle(sheet, headers, "#065F46");
  var maxRows = Math.max(sheet.getLastRow(), 50);
  var numRows = maxRows - 1;
  
  sheet.getRange(2, 4, numRows, 2).setNumberFormat("#,##0");
  sheet.getRange(2, 1, numRows, 1).setHorizontalAlignment("center");
  sheet.getRange(2, 6, numRows, 2).setHorizontalAlignment("center");

  // Batch Formula Sparepart
  var formulas = [];
  for (var r = 2; r <= maxRows; r++) {
    formulas.push([
      '=IF(D' + r + '="","",' +
        'IF(D' + r + '<=E' + r + ', "⚠️ STOK MENIPIS/HABIS", "TERSEDIA"))'
    ]);
  }
  sheet.getRange(2, 7, numRows, 1).setFormulas(formulas);

  applyGridAndResize(sheet, headers[0].length, maxRows);
}

// -------------------------------------------------------------
// UTILITY FUNCTIONS FOR SETUP
// -------------------------------------------------------------
function applyHeaderStyle(sheet, headers, bgColor) {
  sheet.getRange(1, 1, 1, headers[0].length).setValues(headers);
  var headerRange = sheet.getRange(1, 1, 1, headers[0].length);
  headerRange
    .setBackground(bgColor)
    .setFontColor("#FFFFFF")
    .setFontWeight("bold")
    .setHorizontalAlignment("center")
    .setVerticalAlignment("middle");
  sheet.setRowHeight(1, 40);
  sheet.setFrozenRows(1);
}

function applyGridAndResize(sheet, colCount, maxRows) {
  var dataRange = sheet.getRange(1, 1, maxRows, colCount);
  dataRange.setBorder(true, true, true, true, true, true, "#CBD5E1", SpreadsheetApp.BorderStyle.SOLID);
}


