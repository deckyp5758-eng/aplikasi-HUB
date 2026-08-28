package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppUpdateResponse
import com.example.data.ArmadaApiItem
import com.example.data.ArmadaEntity
import com.example.utils.UpdateUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("H033", appName)
  }

  @Test
  fun `test AppUpdateResponse model parsing and defaults`() {
    val response = AppUpdateResponse(
      success = true,
      latestVersionCode = 6,
      latestVersionName = "1.3.0",
      apkDownloadUrl = "https://example.com/app-debug.apk",
      forceUpdate = false,
      changelog = "Pembaruan fitur foto service"
    )

    assertTrue(response.success)
    assertEquals(6, response.latestVersionCode)
    assertEquals("1.3.0", response.latestVersionName)
    assertEquals("https://example.com/app-debug.apk", response.apkDownloadUrl)
    assertEquals(false, response.forceUpdate)
    assertEquals("Pembaruan fitur foto service", response.changelog)
  }

  @Test
  fun `test version comparison logic for update availability`() {
    val currentVersionCode = 5

    // Server has newer version
    val updateResponseNewer = AppUpdateResponse(
      success = true,
      latestVersionCode = 6,
      latestVersionName = "1.3.0"
    )
    val hasUpdate = updateResponseNewer.success &&
        updateResponseNewer.latestVersionCode != null &&
        updateResponseNewer.latestVersionCode > currentVersionCode

    assertTrue("Harus mendeteksi update ketika versi server lebih tinggi", hasUpdate)

    // Server has same version
    val updateResponseSame = AppUpdateResponse(
      success = true,
      latestVersionCode = 5,
      latestVersionName = "1.2.1"
    )
    val hasNoUpdate = updateResponseSame.success &&
        updateResponseSame.latestVersionCode != null &&
        updateResponseSame.latestVersionCode > currentVersionCode

    assertFalse("Tidak boleh memicu update jika versi server sama atau lebih rendah", hasNoUpdate)
  }

  @Test
  fun `test UpdateUiState state transitions`() {
    var state: UpdateUiState = UpdateUiState.Idle
    assertEquals(UpdateUiState.Idle, state)

    state = UpdateUiState.Checking
    assertEquals(UpdateUiState.Checking, state)

    val info = AppUpdateResponse(
      success = true,
      latestVersionCode = 6,
      latestVersionName = "1.3.0"
    )
    state = UpdateUiState.UpdateAvailable(info)
    assertTrue(state is UpdateUiState.UpdateAvailable)
    assertEquals(6, (state as UpdateUiState.UpdateAvailable).info.latestVersionCode)

    state = UpdateUiState.Downloading(progressPercent = 50, downloadedBytes = 5000L, totalBytes = 10000L)
    assertTrue(state is UpdateUiState.Downloading)
    assertEquals(50, (state as UpdateUiState.Downloading).progressPercent)
  }

  @Test
  fun `test ArmadaEntity mapping with fotoService`() {
    val item = ArmadaApiItem(
      armadaId = "L 1234 AB",
      noPolisi = "L 1234 AB",
      kmSaatIni = 150000,
      kmServiceTerakhir = 145000,
      intervalService = 5000,
      kmServiceBerikutnya = 150000,
      sisaKm = 0,
      status = "Waktunya Service",
      flag = "MERAH",
      fotoKm = "https://example.com/km.jpg",
      catattan = "Segera ganti oli",
      fotoTruck = "https://example.com/truck.jpg",
      fotoService = "https://example.com/service.jpg"
    )

    val entity = ArmadaEntity(
      armadaId = item.armadaId,
      noPolisi = item.noPolisi,
      kmSaatIni = item.kmSaatIni,
      kmServiceTerakhir = item.kmServiceTerakhir,
      intervalService = item.intervalService,
      kmServiceBerikutnya = item.kmServiceBerikutnya,
      sisaKm = item.sisaKm,
      status = item.status,
      flag = item.flag,
      fotoKm = item.fotoKm,
      catattan = item.catattan,
      fotoTruck = item.fotoTruck,
      fotoService = item.fotoService
    )

    assertNotNull(entity)
    assertEquals("L 1234 AB", entity.armadaId)
    assertEquals("https://example.com/truck.jpg", entity.fotoTruck)
    assertEquals("https://example.com/service.jpg", entity.fotoService)
  }
}

