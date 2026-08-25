
/**
 * Fungsi untuk update data ban di sheet "Ban"
 * 
 * Perbaikan:
 * 1. Menangani Forward-fill ID Armada (handle merge cells).
 * 2. Update spesifik kolom saja, tidak overwrite baris.
 * 3. Menambahkan mapping tahunBan, kodeBan, noSeri, tanggalUpdate.
 * 4. Append baris baru jika Armada ID + Posisi tidak ditemukan.
 * 5. Timestamp GMT+7.
 */
function updateBan(e) {
  var postData = JSON.parse(e.postData.contents);
  var armadaId = postData.armadaId;
  var posisi = postData.posisi;
  var tahunBan = postData.tahunBan;
  var codeBan = postData.codeBan; // barcode
  var noSeri = postData.noSeri; // seri
  
  var ssId = SPREADSHEETS.ARMADA;
  var sheet = SpreadsheetApp.openById(ssId).getSheetByName("Ban");
  
  if (!sheet) {
    return jsonResponse({ success: false, message: "Sheet 'Ban' tidak ditemukan." });
  }

  var data = sheet.getDataRange().getValues();
  var headers = data[0];
  
  // Mapping kolom berdasarkan header atau fallback (Indeks 0-based)
  // Kolom A: 0, B: 1, C: 2, D: 3, E: 4, F: 5
  var colMap = {
    armada: headers.indexOf("ID ARMADA") !== -1 ? headers.indexOf("ID ARMADA") : 0,
    posisi: headers.indexOf("POSISI") !== -1 ? headers.indexOf("POSISI") : 1,
    tahun: headers.indexOf("TAHUN BAN") !== -1 ? headers.indexOf("TAHUN BAN") : 2,
    code: headers.indexOf("CODE BAN") !== -1 ? headers.indexOf("CODE BAN") : 3,
    tanggal: headers.indexOf("TANGGAL UPDATE") !== -1 ? headers.indexOf("TANGGAL UPDATE") : 4,
    seri: headers.indexOf("No seri") !== -1 ? headers.indexOf("No seri") : 5
  };
  
  var targetRow = -1;
  var lastArmada = "";
  
  // Scanning data
  for (var i = 1; i < data.length; i++) {
    // Forward-fill Armada ID
    if (data[i][colMap.armada] !== "") {
      lastArmada = data[i][colMap.armada].toString().trim();
    }
    
    var currentPosisi = data[i][colMap.posisi] ? data[i][colMap.posisi].toString().trim() : "";
    
    if (lastArmada === armadaId.toString().trim() && currentPosisi === posisi.toString().trim()) {
      targetRow = i + 1;
      break;
    }
  }
  
  var timestamp = Utilities.formatDate(new Date(), "GMT+7", "yyyy-MM-dd HH:mm:ss");
  
  if (targetRow !== -1) {
    // UPDATE
    if (tahunBan) sheet.getRange(targetRow, colMap.tahun + 1).setValue(tahunBan);
    if (codeBan) sheet.getRange(targetRow, colMap.code + 1).setValue(codeBan);
    if (noSeri) sheet.getRange(targetRow, colMap.seri + 1).setValue(noSeri);
    sheet.getRange(targetRow, colMap.tanggal + 1).setValue(timestamp);
    
    return jsonResponse({ success: true, message: "Data ban berhasil diupdate pada baris " + targetRow });
  } else {
    // APPEND
    sheet.appendRow([
      armadaId, 
      posisi, 
      tahunBan, 
      codeBan, 
      timestamp, 
      noSeri
    ]);
    
    return jsonResponse({ success: true, message: "Data ban baru berhasil ditambahkan." });
  }
}
