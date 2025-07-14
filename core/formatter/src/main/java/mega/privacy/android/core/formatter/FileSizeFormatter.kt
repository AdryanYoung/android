package mega.privacy.android.core.formatter

import android.annotation.SuppressLint
import android.content.Context
import java.text.DecimalFormat

@SuppressLint("StringFormatInvalid")
fun formatFileSize(size: Long, context: Context): String {
    val format = DecimalFormat("#.##")
    val kilobyte = 1024f
    val megabyte = kilobyte * 1024
    val gigabyte = megabyte * 1024
    val terabyte = gigabyte * 1024
    val petabyte = terabyte * 1024
    val exabyte = petabyte * 1024
    return if (size < kilobyte) {
        context.getString(
            R.string.label_file_size_byte,
            size.toString()
        )
    } else if (size < megabyte) {
        context.getString(
            R.string.label_file_size_kilo_byte,
            format.format((size / kilobyte).toDouble())
        )
    } else if (size < gigabyte) {
        context.getString(
            R.string.label_file_size_mega_byte,
            format.format((size / megabyte).toDouble())
        )
    } else if (size < terabyte) {
        context.getString(
            R.string.label_file_size_giga_byte,
            format.format((size / gigabyte).toDouble())
        )
    } else if (size < petabyte) {
        context.getString(
            R.string.label_file_size_tera_byte,
            format.format((size / terabyte).toDouble())
        )
    } else if (size < exabyte) {
        context.getString(
            R.string.label_file_size_peta_byte,
            format.format((size / petabyte).toDouble())
        )
    } else {
        context.getString(
            R.string.label_file_size_exa_byte,
            format.format((size / exabyte).toDouble())
        )
    }
}