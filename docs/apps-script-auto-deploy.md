# Sinkronisasi otomatis Code.gs H033 ke Apps Script

## Cara kerja

File canonical backend aplikasi adalah `app/src/main/assets/apps-script/Code.gs` pada branch `main` repository GitHub. Workflow `.github/workflows/sync-apps-script.yml` hanya berjalan ketika file tersebut berubah atau ketika dijalankan manual dari GitHub Actions.

Setiap eksekusi workflow:

1. Mengambil source branch `main`.
2. Menarik isi project Apps Script H033 yang sudah ada.
3. Mengganti hanya file `Code.gs` dengan source canonical dari repository.
4. Menolak proses apabila project target mengandung `BBM.gs`.
5. Mengunggah source ke project Apps Script.
6. Membuat immutable version baru berdasarkan SHA commit GitHub.
7. Memperbarui deployment Web App yang sama memakai `clasp redeploy`, sehingga URL aplikasi tetap sama.
8. Menguji endpoint `debug` pada URL Web App aktif.

`BBM.gs`, spreadsheet legacy, tab BBM, dan dashboard legacy tidak menjadi bagian dari workflow ini.

## Setup satu kali

Workflow membutuhkan satu repository secret bernama `CLASP_CREDENTIALS_JSON`. Secret ini harus berisi isi file kredensial clasp milik akun Google yang mempunyai akses editor ke project Apps Script H033.

Jangan menuliskan credential tersebut di chat, commit, issue, log, atau file repository. Tambahkan melalui GitHub:

1. Buka repository `deckyp5758-eng/aplikasi-HUB`.
2. Pilih **Settings**.
3. Buka **Secrets and variables → Actions**.
4. Pilih **New repository secret**.
5. Isi nama secret: `CLASP_CREDENTIALS_JSON`.
6. Tempel isi JSON credential clasp secara langsung pada kolom Secret.
7. Simpan dengan **Add secret**.

Workflow membaca secret melalui environment variable dan tidak mencetak nilainya.

## Perilaku setelah setup

Perubahan pada `Code.gs` di branch `main` akan memicu workflow otomatis. Perubahan pada file Android lain tidak memicu deployment Apps Script. Jika workflow gagal karena secret belum tersedia, source GitHub tidak berubah dan deployment Apps Script tidak disentuh.

Versi baru dibuat pada deployment yang sama. Pengguna aplikasi tetap memakai URL Web App aktif yang sudah dikonfigurasi; tidak dibuat URL baru pada setiap commit.

## Pengujian aman

Sebelum secret ditambahkan, workflow dapat diperiksa melalui lint/diff lokal, tetapi deploy nyata belum dapat dijalankan. Setelah secret ditambahkan, jalankan workflow pertama kali dengan **Actions → Sync H033 Apps Script → Run workflow**. Pastikan hasil berakhir hijau dan langkah **Verify the same Web App URL** berhasil.

Jika ingin menguji perubahan, ubah hanya file canonical `app/src/main/assets/apps-script/Code.gs`, commit ke `main`, lalu lihat log workflow. Jangan menggunakan **Restore** pada checkpoint lama AI Studio sebelum source dibandingkan dengan GitHub.

## Pengamanan yang sudah diterapkan

Workflow menggunakan `permissions: contents: read`, concurrency satu jalur agar dua deployment tidak berjalan bersamaan, timeout 10 menit, dan pemeriksaan keberadaan `BBM.gs`. Deployment menggunakan `clasp redeploy` terhadap deployment ID yang sudah ada, bukan membuat deployment baru setiap kali.

## Referensi resmi

- [Google Apps Script: Use clasp](https://developers.google.com/apps-script/guides/clasp)
- [Google Apps Script API: Manage deployments](https://developers.google.com/apps-script/api/how-tos/manage-deployments)
- [GitHub Docs: Using secrets in GitHub Actions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)
