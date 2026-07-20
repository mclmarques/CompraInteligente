package com.mcldev.comprainteligente.ui.scan_screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_BASE
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.googlecode.tesseract.android.TessBaseAPI
import com.mcldev.comprainteligente.data.dao.ProductDao
import com.mcldev.comprainteligente.data.dao.SupermarketDao
import com.mcldev.comprainteligente.data.entities.Product
import com.mcldev.comprainteligente.data.entities.Supermarket
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.content.edit

/**
 * @param path: path to load the data of tesseract
 * The Viewmodel is design to handle as much of the logic as possible and to main a clean architecture
 * It is organized like this:
 * Internal global variables
 * Method to initialize and perform OCR
 * Methods to work with data using the DAOs
 * Error methods to update the UI and display the err and possible solutions
 * Internal methods which in essence are the OCR operation, OCR helper methods and post-processing OCR
 */
class ScanScreenVM(
    private val path: String,
    private val productDao: ProductDao,
    private val supermarketDao: SupermarketDao
) : ViewModel() {
    private var imageUri: Uri? = null

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    private val _products = MutableStateFlow(mutableListOf<ScannedProduct>())
    val products = _products.asStateFlow()

    private val _supermarket = MutableStateFlow<String?>(null)
    val supermarket = _supermarket.asStateFlow()

    private var engine: Engine? = null

    private suspend fun initializeLlmEngine(context: Context) {
        if (engine != null) return

        val modelFile = File(context.filesDir, "gemma_270m.litertlm")
        if (!modelFile.exists()) {
            withContext(Dispatchers.IO) {
                try {
                    context.assets.open("gemma_270m.litertlm").use { input ->
                        modelFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: IOException) {
                    Log.e("LLM", "Error copying model file", e)
                }
            }
        }

        if (modelFile.exists()) {
            withContext(Dispatchers.IO) {
                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU()
                )
                engine = Engine(config)
                engine?.initialize()
            }
        }
    }


    fun prepareScanner(): GmsDocumentScanner {
        //Scanner stuff
        //TODO: add support to scan more than 1 image simultaneously
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(SCANNER_MODE_BASE)
            .build()
        return GmsDocumentScanning.getClient(options)
    }

    /**
     * @param uri URI of the photo to perform the OCR operation on
     * These changes the State to Loading and calls the loadAndSave bitmap to load the bitmap from the URI and locally save the image.
     * Afterward it cals the performOcr and finally the postProcessOcr, that takes the String from the performOcr and
     * adjust it.
     * The method doesn't return anything because the postProcessOcr already saves the data into the appropriate
     * _products & _prices lists
     * Once complete, the State is changed to Complete
     *
     */
    fun processImage(context: Context, uri: Uri) {
        _processingState.value = ProcessingState.Loading
        if (uri.path != null) {
            imageUri = uri
            var bitmap: Bitmap? = null
            viewModelScope.launch {
                bitmap = withContext(Dispatchers.IO) {
                    loadAndSaveBitmap(
                        uri = uri,
                        context = context
                    )
                }
                if (bitmap != null) {
                    performOCR(bitmap!!)?.let {
                        Log.i("OCR", it)
                        postProcessOCRText(context, it) }
                    _processingState.value = ProcessingState.Complete
                } else {
                    ocrFault()
                }
            }
        } else storageFault()
    }

    //Data management (save, modify or edit products)
    /**
     * @param id: p
     */
    fun deleteItem(id: String) {
        _products.value = _products.value.filter { it.id != id }.toMutableList()
    }

    /**
     * Calculates the new average price for a supermarket when adding new products.
     */
    private fun calculateNewAveragePrice(
        oldAverage: Float,
        oldCount: Int,
        newPrices: List<Float>
    ): Float {
        if (oldCount == 0) return newPrices.average().toFloat()
        val totalOld = oldAverage * oldCount
        val totalNew = newPrices.sum()
        val newCount = oldCount + newPrices.size
        return ((totalOld + totalNew) / newCount)
    }

    /**
     * These methods check that a supermarket name was provided and search's for it in the DB.
     * If it is found, the average is updated, and the products get linked to that supermarket
     * Else, a new supermarket is created, and the average and products get linked to it.
     * If this process fails, an error screen is shown, indicating a storage error as the likely cause.
     */
    fun saveProducts() {
        viewModelScope.launch(Dispatchers.Default) {
            var supermarketEntity: Supermarket?
            if (supermarket.value != null) {
                //creates a list of the new prices and searches for the supermarket name in the DB.
                val newPrices = mutableListOf<Float>()
                _products.value.forEach { product ->
                    newPrices.add(product.price)
                }
                supermarketEntity =
                    supermarketDao.getSupermarketByName(supermarketName = _supermarket.value!!)
                if (supermarketEntity != null) {
                    supermarketDao.upsertSupermarket(
                        supermarketEntity.copy(
                            averagePrice = calculateNewAveragePrice(
                                supermarketEntity.averagePrice,
                                supermarketDao.getProductCount(supermarketEntity.id),
                                newPrices
                            )
                        )
                    )

                    for (item in products.value.indices) {
                        val product = Product(
                            name = products.value[item].name,
                            price = products.value[item].price,
                            supermarketId = supermarketEntity.id,
                            date = System.currentTimeMillis()
                        )
                        productDao.upsertProduct(product)
                    }
                } else {
                    supermarketDao.upsertSupermarket(
                        Supermarket(
                            name = supermarket.value!!,
                            averagePrice = calculateNewAveragePrice(
                                0F,
                                0,
                                newPrices
                            )
                        )
                    )
                    supermarketEntity = supermarketDao.getSupermarketByName(supermarket.value!!)
                    if (supermarketEntity != null) {
                        for (item in products.value.indices) {
                            val product = Product(
                                name = products.value[item].name,
                                price = products.value[item].price,
                                supermarketId = supermarketEntity.id,
                                date = System.currentTimeMillis()
                            )
                            productDao.upsertProduct(product)
                        }
                    } else storageFault()
                }
            } else ocrFault()
        }
    }

    /**
     * @param id: ID of the element to update
     * @param newProduct new product name / description. If null it won't update
     * @param newPrice new product price. If null it won't update
     * Updates the product at the given index with either the new price or the new product name.
     * Only pass either a new product name or price, not both as it won't update both
     * If you only pass the position, the method won't do anything
     */
    // In ScanScreenVM.kt
    fun updateProduct(id: String, newProduct: String? = null, newPrice: Float? = null) {
        // We map through the list. If IDs match, we update; otherwise, keep the old one.
        _products.value = _products.value.map { current ->
            if (current.id == id) {
                current.copy(
                    name = newProduct ?: current.name,
                    price = newPrice ?: current.price
                )
            } else {
                current
            }
        }.toMutableList()
    }

    fun updateSupermarket(newSupermarket: String) {
        _supermarket.value = newSupermarket
    }


    //Error methods
    fun cameraLaunchFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.CAMERA_ERROR)
    }

    /**
     * Method to trigger a storage error launch and make the UI show the appropriate err code
     */
    fun storageFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.DATA_SAVE_ERROR)
    }

    fun ocrFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.TEXT_EXTRACTION_ERROR)
    }

    //Internal helper methods of the viewmodel


    /**
     * @param uri: uri to load the bitmap
     */
    private suspend fun loadAndSaveBitmap(uri: Uri, context: Context): Bitmap? {
        return withContext(Dispatchers.IO) {
            val bitmap: Bitmap?
            val inputStream = context.contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Save the bitmap to a file
            val saveUri = context.createImageFile()
            try {
                context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            } catch (e: IOException) {
                storageFault()
            }

            bitmap // Return the loaded Bitmap
        }
    }


    /**
     * @param image: the bitmap to extract the text from
     * @return extracted text from the image
     * The method uses the tesserat API to extract the text. IT ASSUMES THE IMAGE IS ALREADY PREPROCESSED
     */
    private suspend fun performOCR(image: Bitmap): String? {
        return withContext(Dispatchers.Default) {
            // setup tessBaseApi
            val tessBaseAPI = TessBaseAPI()
            tessBaseAPI.init(path, "por") // or other languages
            tessBaseAPI.setImage(image)
            tessBaseAPI.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            val recognizedText = tessBaseAPI.utF8Text
            tessBaseAPI.stop()
            tessBaseAPI.recycle()
            recognizedText
        }
    }
    private suspend fun postProcessOCRText(context: Context, ocrText: String) {
        // The supermarket name heuristic already works fine on its own (see logs), so it
        // doesn't need to go through the LLM at all - one less thing that can fail.
        extractSupermarketName(ocrText.lines())?.let { _supermarket.value = it }

        val candidateLines = extractCandidateLines(ocrText)
        Log.i("LLM", "Candidate lines (${candidateLines.size}): ${candidateLines.joinToString(" || ")}")

        if (candidateLines.isEmpty()) {
            ocrFault()
            return
        }

        try {
            initializeLlmEngine(context)
        } catch (e: Throwable) {
            Log.e("LLM", "LLM Error (including potential LinkageErrors) while initializing", e)
        }

        if (engine == null) {
            Log.w("LLM", "Engine failed to initialize, falling back to Regex for every line")
            _products.value = candidateLines.mapNotNull { parseLineWithRegex(it) }.toMutableList()
            return
        }

        // IMPORTANT: a 270M-parameter model can't reliably extract 15+ noisy items in a single
        // shot (it needs ~17s just to prefill a whole-receipt prompt, and tends to trail off
        // instead of following the format - see "The output is:" in the logs). Asking it to
        // clean up ONE line at a time is a task it can actually do, keeps each call fast enough
        // to stay well inside a sane timeout, and means a single bad line can't take down the
        // whole scan - it just falls back to the regex parser for that one line.
        val updatedProducts = mutableListOf<ScannedProduct>()
        for (line in candidateLines) {
            val product = try {
                withContext(Dispatchers.Default) {
                    withTimeoutOrNull(10_000.milliseconds) { extractProductWithLlm(line) }
                }
            } catch (e: Throwable) {
                Log.e("LLM", "LLM error on line '$line', falling back to Regex", e)
                null
            } ?: parseLineWithRegex(line)

            if (product != null) updatedProducts.add(product)
        }

        if (updatedProducts.isNotEmpty()) {
            _products.value = updatedProducts
        } else {
            ocrFault()
        }
    }

    /**
     * Asks the LLM to turn a single, already-cleaned OCR line into "Name | Price". Kept
     * deterministic (topK = 1, temperature = 0) since this is an extraction task, not a
     * creative one - we want the model to reliably follow the pattern, not vary its answer.
     */
    private suspend fun extractProductWithLlm(line: String): ScannedProduct? {
        val currentEngine = engine ?: return null

        val prompt = """
            <start_of_turn>user
            Extraia o nome do produto e o preco unitario desta linha de um recibo de supermercado brasileiro.
            Responda em UMA linha, exatamente neste formato: NOME | PRECO
            Ignore códigos, quantidades e a palavra "kg".

            Linha: 593 PEITO PERU TEMP RECH kg 1,010KG 63,99 64,63
            NOME | PRECO: Peito de Peru Temp Rech | 63.99

            Linha: 3994082704 LASANHA SEARA 600G BOLON UN 12,90 12,90
            NOME | PRECO: Lasanha Seara 600g Bolon | 12.90

            Linha: $line
            NOME | PRECO:<end_of_turn>
            <start_of_turn>model
        """.trimIndent() + "\n"

        val response = StringBuilder()
        val conversationConfig = ConversationConfig(
            samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0)
        )
        currentEngine.createConversation(conversationConfig).use { conversation ->
            conversation.sendMessageAsync(prompt).collect { token -> response.append(token) }
        }

        val output = response.toString().replace("```", "").trim()
        Log.d("LLM", "Line: '$line' -> Output: '$output'")

        val match = Regex("""([^|\n]+)\|\s*([\d.,]+)""").find(output) ?: return null
        val name = match.groupValues[1].trim().removeSurrounding("**").trim()
        val price = match.groupValues[2].trim().replace(",", ".").toFloatOrNull() ?: return null

        if (name.length < 2 || price <= 0f) return null
        return ScannedProduct(name = name, price = price)
    }

    /**
     * True if, after stripping unit codes/digits/symbols, a line doesn't have enough real
     * letters left to be a genuine product description. Filters out OCR line-wrap continuation
     * junk such as "Aa UN 9,99 05,00" or "BIN 23,992 9 8a".
     */
    private fun looksLikeJunkName(rawName: String): Boolean {
        val stripped = rawName
            .replace(Regex("""(?i)\b(UN|KG|PC|CX|LT)\b"""), "")
            .replace(Regex("""[^\p{L}]"""), "")
        return stripped.length < 3
    }

    private fun extractSupermarketName(lines: List<String>): String? {
        return lines.take(10).find { line ->
            val trimmed = line.trim().uppercase()
            trimmed.length > 3 &&
                    trimmed.any { it.isLetter() } &&
                    !SKIP_KEYWORDS_REGEX.containsMatchIn(trimmed) &&
                    !trimmed.contains("NOTA FISCAL")
        }?.trim()
    }

    /**
     * Turns raw OCR output into a list of clean, single-line product candidates, ready to be
     * handed one at a time to the LLM (or the regex fallback). Two receipt quirks are handled
     * here so neither the LLM nor the fallback parser have to deal with them:
     *  - Weight-based items are printed across two OCR lines (description, then qty/price on
     *    the next), so a description-only line is held and merged with the following line.
     *  - Barcodes and administrative/total lines are stripped out entirely.
     */
    private fun extractCandidateLines(ocrText: String): List<String> {
        val candidates = mutableListOf<String>()
        var pendingName: String? = null

        for (rawLine in ocrText.lines()) {
            var trimmed = rawLine.trim()
            if (trimmed.isBlank()) continue

            trimmed = BARCODE_REGEX.replace(trimmed, "").trim()
            if (trimmed.isBlank()) continue

            val upper = trimmed.uppercase()
            // "PAG" also catches "PAGAMENTO"/"PAGO", not just the literal word "PAGAR"; without
            // it, "VALOR PAGO"/"FORMA DE PAGAMENTO" summary lines slip through and get treated
            // as fake products.
            if (SKIP_KEYWORDS_REGEX.containsMatchIn(upper) ||
                upper.contains("TOTAL") ||
                upper.contains("PAG") ||
                upper.contains("SUBTOTAL") ||
                upper.contains("TROCO") ||
                upper.contains("VALOR")
            ) {
                continue
            }

            val hasPrice = PRICE_REGEX.containsMatchIn(trimmed)
            val hasLetters = trimmed.any { it.isLetter() }
            // Receipt header/footer lines (timestamp, PDV/DOC numbers, operator name) have no
            // price and plenty of letters, so without this check they'd get treated as a
            // pending product description and glued onto whatever the first real item is.
            val looksLikeHeaderLine = Regex("""\d{2}:\d{2}:\d{2}|\bPDV\b|\bDOC\b|\bLJ\b""").containsMatchIn(upper)

            when {
                hasPrice && hasLetters && !looksLikeJunkName(trimmed) -> {
                    candidates.add(if (pendingName != null) "$pendingName $trimmed".trim() else trimmed)
                    pendingName = null
                }
                hasPrice && pendingName != null -> {
                    candidates.add("$pendingName $trimmed".trim())
                    pendingName = null
                }
                !hasPrice && hasLetters && trimmed.length > 3 && !looksLikeHeaderLine -> {
                    pendingName = trimmed
                }
                else -> { /* barcode/header/noise-only fragment, drop it */ }
            }
        }

        return candidates
    }

    /**
     * Picks the unit price out of all the price-shaped numbers found in a line. Brazilian
     * receipt lines are laid out as [qty/weight] [unit price] [line total], so when 2+ prices
     * are present the *second-to-last* one is the unit price and the *last* one is the total
     * paid for that line. (Previously the code always took the last one, which quietly saved
     * the total paid instead of the unit price for every weight-based item.) When only one
     * price is present - a single unit sold at qty 1, where unit price == total - that one is
     * used.
     */
    private fun extractUnitPrice(matches: List<String>): Float? {
        if (matches.isEmpty()) return null
        val chosen = if (matches.size >= 2) matches[matches.size - 2] else matches[0]
        return chosen.replace(",", ".").toFloatOrNull()
    }

    /** Regex-only fallback for a single already-cleaned candidate line. */
    private fun parseLineWithRegex(line: String): ScannedProduct? {
        val matches = PRICE_REGEX.findAll(line).map { it.value }.toList()
        val price = extractUnitPrice(matches)?.takeIf { it > 0f } ?: return null

        var name = line
        matches.forEach { name = name.replace(it, "") }
        name = name
            .replace(Regex("""(?i)\b(UN|KG|PC|CX|LT)\b"""), "")
            .replace(Regex("""\d+"""), "")
            .replace(Regex("""[^\p{L}\s]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (name.length < 3) return null
        return ScannedProduct(name = name, price = price)
    }

    companion object {
        // Matches Brazilian-formatted prices like "63,99". Deliberately rejects a match that's
        // immediately followed by another digit or by a "k"/"K" - without this, a weight token
        // like "1,010KG" gets misread as the price "1,01", polluting every weight-based line.
        private val PRICE_REGEX = Regex("""\d{1,3}[,.]\d{2}(?!\d)(?!\s?[Kk])""")
        private val BARCODE_REGEX = Regex("""\b\d{6,}\b""") // 6+ digits in a row

        // Whole-word match on purpose: checking "IE" as a bare substring (the original bug)
        // also matches inside ordinary words like "COOKIE", silently discarding that product.
        private val SKIP_KEYWORDS_REGEX = Regex(
            """(?i)\b(CNPJ|CPF|IE|UNPJ|EMITENTE|CONSUMIDOR|FISCAL|ELETRONICA)\b"""
        )
    }

    override fun onCleared() {
        engine?.close()
    }
}

/**
 * Auxiliary method to create the image file. It stores in teh shared preferences an index to keep
 * track of the pictures and avoid overwriting them
 */
fun Context.createImageFile(): Uri {
    val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var index = sharedPreferences.getInt("last_receipt_index", 0)
    index++

    val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    if (storageDir != null && !storageDir.exists()) {
        storageDir.mkdirs() // Ensure the directory exists
    }
    val newFile = File(storageDir, "receipt$index.jpg")

    // Save the new index
    sharedPreferences.edit { putInt("last_receipt_index", index) }
    return FileProvider.getUriForFile(
        this,
        "${applicationContext.packageName}.provider",
        newFile
    )
}

sealed class ProcessingState {
    data object Idle : ProcessingState()
    data object Loading : ProcessingState()
    data object Complete : ProcessingState()
    data class Error(val code: ErrorCodes) : ProcessingState()
}

data class ScannedProduct(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var price: Float
)