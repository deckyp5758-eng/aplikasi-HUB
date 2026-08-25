package com.example

import com.example.utils.AkiUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit tests to verify application logic including battery status calculations.
 */
class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAkiStatus_emptyDate_returnsAman() {
    val result = AkiUtils.calculateAkiStatus(
      armadaId = "ARM-001",
      noPolisi = "N 1234 AB",
      barcode = "BAR-01",
      tanggalPasangStr = "",
      userStatus = "AMAN"
    )
    assertEquals("-", result.tanggalPasang)
    assertEquals("-", result.tanggalGantiBerikutnya)
    assertNull(result.sisaHari)
    assertFalse(result.isDue)
    assertFalse(result.isExpired)
    assertEquals("AMAN", result.statusLabel)
  }

  @Test
  fun testAkiStatus_newlyInstalled_returnsAmanWithDaysRemaining() {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = Calendar.getInstance()
    val todayStr = sdf.format(today.time)

    val result = AkiUtils.calculateAkiStatus(
      armadaId = "ARM-001",
      noPolisi = "N 1234 AB",
      barcode = "BAR-01",
      tanggalPasangStr = todayStr
    )

    val expectedPasang = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(today.time)
    assertEquals(expectedPasang, result.tanggalPasang)
    assertNotNull(result.sisaHari)
    assertTrue(result.sisaHari!! > 700) // 2 years is roughly 730 days
    assertFalse(result.isExpired)
    assertFalse(result.isWarning30Days)
    assertFalse(result.isDue)
    assertEquals("AMAN", result.statusLabel)
  }

  @Test
  fun testAkiStatus_expiredThreeYearsAgo_requiresReplacement() {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val threeYearsAgo = Calendar.getInstance().apply {
      add(Calendar.YEAR, -3)
    }
    val dateStr = sdf.format(threeYearsAgo.time)

    val result = AkiUtils.calculateAkiStatus(
      armadaId = "ARM-002",
      noPolisi = "N 5678 CD",
      barcode = "BAR-02",
      tanggalPasangStr = dateStr
    )

    val expectedPasang = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(threeYearsAgo.time)
    assertEquals(expectedPasang, result.tanggalPasang)
    assertNotNull(result.sisaHari)
    assertTrue(result.sisaHari!! < 0)
    assertTrue(result.isExpired)
    assertTrue(result.isDue)
    assertEquals("GANTI (SUDAH >2 TAHUN)", result.statusLabel)
  }

  @Test
  fun testAkiStatus_warningPeriod_returnsSegeraGanti() {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    // 2 years minus 15 days ago means replacement is due in 15 days
    val warningDate = Calendar.getInstance().apply {
      add(Calendar.YEAR, -2)
      add(Calendar.DAY_OF_YEAR, 15)
    }
    val dateStr = sdf.format(warningDate.time)

    val result = AkiUtils.calculateAkiStatus(
      armadaId = "ARM-003",
      noPolisi = "N 9012 EF",
      barcode = "BAR-03",
      tanggalPasangStr = dateStr
    )

    val expectedPasang = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(warningDate.time)
    assertEquals(expectedPasang, result.tanggalPasang)
    assertNotNull(result.sisaHari)
    assertTrue(result.sisaHari!! in 1..30)
    assertFalse(result.isExpired)
    assertTrue(result.isWarning30Days)
    assertTrue(result.isDue)
    assertTrue(result.statusLabel.startsWith("SEGERA GANTI"))
  }
}

