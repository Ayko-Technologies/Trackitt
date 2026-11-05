package com.ayko.trackitt.service.categorization

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * Local-only harness: runs the keyword dictionary over exported transactions and writes what it
 * picks. Skipped unless TRACKITT_CATEGORY_CORPUS points at a corpus file. Never commit either file.
 *
 * Corpus lines: id \t base64(merchant) \t base64(reference) \t base64(note)
 * Report lines: id \t category (OTHER when nothing matched) \t purpose named by reference or note (or -)
 */
class CategoryCorpusReport {

    @Test
    fun writeReport() = runBlocking {
        val corpus = System.getenv("TRACKITT_CATEGORY_CORPUS")
        assumeTrue(corpus != null && File(corpus).exists())
        val out = File(System.getenv("TRACKITT_CATEGORY_REPORT") ?: "$corpus.report.tsv")
        val service = DictionaryCategorizationService(userLookup = { null })
        val decoder = Base64.getDecoder()
        fun field(value: String) = String(decoder.decode(value)).ifEmpty { null }

        out.bufferedWriter().use { writer ->
            for (line in File(corpus!!).readLines()) {
                val (id, merchant, reference, note) = line.split('\t')
                val category = service.categorize(field(merchant) ?: "", field(reference), field(note))
                val purpose = service.purposeOf(field(reference), field(note))?.name ?: "-"
                writer.write("$id\t${category.name}\t$purpose")
                writer.newLine()
            }
        }
        Unit
    }
}
