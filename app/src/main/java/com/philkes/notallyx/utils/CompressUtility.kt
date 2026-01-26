package com.philkes.notallyx.utils

import android.util.Log
import com.github.luben.zstd.Zstd
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.data.model.SpanRepresentation
import org.json.JSONObject

/**
 * Compression utilities for large text payloads to decrease memory usage by leveraging ZSTD to
 * de-/compress text + spans (JSON)
 */
object CompressUtility {

    private const val TAG = "CompressUtility"
    private const val TEXT_FIELD = "text"
    private const val SPANS_FIELD = "spans"

    // Threshold in characters for when to compress text (approximately 10KB)
    const val COMPRESSION_THRESHOLD: Int = 10_000

    /** Compresses text and spans using GZIP compression into a ByteArray. */
    fun compressTextAndSpans(text: String, spans: List<SpanRepresentation>): ByteArray {
        val jsonObject = JSONObject()
        jsonObject.put(TEXT_FIELD, text)
        jsonObject.put(SPANS_FIELD, Converters.spansToJSONArray(spans))
        val bytes = jsonObject.toString().toByteArray(Charsets.UTF_8)
        return Zstd.compress(bytes, 4)
    }

    /** Decompresses text and spans that were compressed with GZIP. */
    fun decompressTextAndSpans(compressedData: ByteArray): Pair<String, List<SpanRepresentation>> {
        val decompressedSize = Zstd.getFrameContentSize(compressedData)
        if (decompressedSize <= 0) {
            Log.e(
                TAG,
                "Invalid compressed data (frameContentSize: $decompressedSize), returning empty",
            )
            return Pair("", emptyList())
        } else {
            val result = ByteArray(decompressedSize.toInt())
            Zstd.decompress(result, compressedData)
            val jsonString = result.toString(Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)
            val text = jsonObject.getString(TEXT_FIELD)
            val spansArray = jsonObject.getJSONArray(SPANS_FIELD)
            val spans = Converters.jsonToSpans(spansArray)
            return Pair(text, spans)
        }
    }
}
