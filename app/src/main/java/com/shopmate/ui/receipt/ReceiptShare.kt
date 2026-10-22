package com.shopmate.ui.receipt

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.shopmate.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Renders the 1m receipt image and hands it to the system share sheet (WhatsApp, SMS, email). */
object ReceiptShare {

    suspend fun share(context: Context, receipt: ReceiptContent, receiptNumber: String) {
        val uri = withContext(Dispatchers.Default) {
            val bitmap = SharedReceiptImage.render(context, receipt)
            // Cache only: never backed up, cleared by the system when space is short.
            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(dir, "$receiptNumber.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            FileProvider.getUriForFile(context, "${context.packageName}.receipts", file)
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            // The URI also goes in ClipData: the read grant then reaches the share sheet itself
            // (for its preview), not only the app the cashier picks.
            clipData = ClipData.newRawUri(receiptNumber, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.receipt_share_title, receiptNumber)),
        )
    }
}
