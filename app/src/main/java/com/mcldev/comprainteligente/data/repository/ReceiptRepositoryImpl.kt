package com.mcldev.comprainteligente.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.googlecode.tesseract.android.TessBaseAPI
import com.mcldev.comprainteligente.data.dao.ProductDao
import com.mcldev.comprainteligente.data.dao.SupermarketDao
import com.mcldev.comprainteligente.data.entities.Product
import com.mcldev.comprainteligente.data.entities.Supermarket
import com.mcldev.comprainteligente.data.model.ScannedProduct
import com.mcldev.comprainteligente.ui.scan_screen.ProcessingState
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException

class ReceiptRepositoryImpl(
    private val tessPath: String,
    private val productDao: ProductDao,
    private val supermarketDao: SupermarketDao,
    private val imageRepository: ImageRepository
) : ReceiptRepository {

    private var engine: Engine? = null
    
    private val _products = MutableStateFlow<List<ScannedProduct>>(emptyList())
    override val products: StateFlow<List<ScannedProduct>> = _products.asStateFlow()

    private val _supermarketName = MutableStateFlow<String?>(null)
    override val supermarketName: StateFlow<String?> = _supermarketName.asStateFlow()

    override fun clearResults() {
        _products.value = emptyList()
        _supermarketName.value = null
    }

    override fun updateProduct(productId: String, name: String?, price: Float?) {
        val currentList = _products.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == productId }
        if (index != -1) {
            val updated = currentList[index].copy(
                name = name ?: currentList[index].name,
                price = price ?: currentList[index].price
            )
            currentList[index] = updated
            _products.value = currentList
        }
    }

    override fun deleteProduct(productId: String) {
        val currentList = _products.value.toMutableList()
        currentList.removeAll { it.id == productId }
        _products.value = currentList
    }

    override fun updateSupermarket(name: String) {
        _supermarketName.value = name
    }

    override fun processImage(context: Context, uri: Uri): Flow<ProcessingState> = flow {
        emit(ProcessingState.Loading)
        clearResults()

        val bitmap = imageRepository.loadAndSaveBitmap(uri)
        if (bitmap == null) {
            emit(ProcessingState.Error(ErrorCodes.UNSUPPORTED_DEVICE_ERROR_2))
            return@flow
        }

        val ocrText = performOCR(bitmap)
        if (ocrText == null) {
            emit(ProcessingState.Error(ErrorCodes.TEXT_EXTRACTION_ERROR))
            return@flow
        }

        try {
            postProcessOCRText(context, ocrText)
            emit(ProcessingState.Complete)
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error post-processing OCR text", e)
            emit(ProcessingState.Error(ErrorCodes.TEXT_EXTRACTION_ERROR))
        }
    }

    private fun performOCR(bitmap: Bitmap): String? {
        val tessBaseAPI = TessBaseAPI()
        return try {
            tessBaseAPI.init(tessPath, "por")
            tessBaseAPI.setImage(bitmap)
            val text = tessBaseAPI.utF8Text
            tessBaseAPI.recycle()
            text
        } catch (e: Exception) {
            Log.e("OCR", "Error during OCR", e)
            null
        }
    }

    private suspend fun postProcessOCRText(context: Context, ocrText: String) {
        extractSupermarketName(ocrText.lines())?.let { _supermarketName.value = it }

        val candidateLines = extractCandidateLines(ocrText)
        if (candidateLines.isEmpty()) return

        val rawProducts = candidateLines.mapNotNull { parseLineWithRegex(it) }
        if (rawProducts.isEmpty()) return

        val dedupedProducts = dedupeSimilarProducts(rawProducts)
        _products.value = dedupedProducts

        try {
            initializeLlmEngine(context)
        } catch (e: Throwable) {
            Log.e("LLM", "LLM Error while initializing", e)
        }

        val currentEngine = engine ?: return

        try {
            withTimeoutOrNull(40_000) {
                withContext(Dispatchers.Default) {
                    val conversationConfig = ConversationConfig(
                        systemInstruction = Contents.of(NAME_BEAUTIFY_SYSTEM_INSTRUCTION),
                        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0)
                    )
                    currentEngine.createConversation(conversationConfig).use { conversation ->
                        val currentList = _products.value.toMutableList()
                        for (index in currentList.indices) {
                            val rawName = currentList[index].name
                            val prettyName = try {
                                withTimeoutOrNull(6_000) { beautifyName(conversation, rawName) }
                            } catch (e: Throwable) {
                                Log.e("LLM", "Beautify error for '$rawName'", e)
                                null
                            }
                            if (!prettyName.isNullOrBlank()) {
                                currentList[index] = currentList[index].copy(name = prettyName)
                                _products.value = currentList.toList()
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("LLM", "Name beautification batch failed", e)
        }
    }

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
                    backend = Backend.CPU(numOfThreads = 4)
                )
                engine = Engine(config)
                engine?.initialize()
            }
        }
    }

    private suspend fun beautifyName(conversation: Conversation, rawName: String): String? {
        val response = StringBuilder()
        conversation.sendMessageAsync("Linha: $rawName\nResposta:").collect { token -> response.append(token) }

        val output = response.toString().replace("```", "").trim()
        val firstLine = output.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return null
        val cleaned = firstLine
            .removePrefix("Resposta:")
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("**")
            .trim()

        if (cleaned.isBlank() || cleaned.length > 60 || cleaned.equals("Resposta", ignoreCase = true)) {
            return null
        }
        return cleaned
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

    private fun extractCandidateLines(ocrText: String): List<String> {
        val candidates = mutableListOf<String>()
        var pendingName: String? = null

        for (rawLine in ocrText.lines()) {
            var trimmed = rawLine.trim()
            if (trimmed.isBlank()) continue

            trimmed = BARCODE_REGEX.replace(trimmed, "").trim()
            if (trimmed.isBlank()) continue

            val upper = trimmed.uppercase()
            if (SKIP_KEYWORDS_REGEX.containsMatchIn(upper) ||
                upper.contains("TOTAL") ||
                upper.contains("PAG") ||
                upper.contains("SUBTOTAL") ||
                upper.contains("TROCO") ||
                upper.contains("VALOR")
            ) {
                pendingName = null
                continue
            }

            val hasPrice = PRICE_REGEX.containsMatchIn(trimmed)
            val hasLetters = trimmed.any { it.isLetter() }
            val looksLikeHeaderLine = Regex(
                """\d{2}:\d{2}:\d{2}|\bPDV\b|\bDOC\b|\bLJ\b|\bFONE\b|,\s*\d+\s*,"""
            ).containsMatchIn(upper)

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
                !hasPrice && looksLikeHeaderLine -> {
                    pendingName = null
                }
            }
        }
        return candidates
    }

    private fun looksLikeJunkName(rawName: String): Boolean {
        val stripped = rawName
            .replace(Regex("""(?i)\b(UN|KG|PC|CX|LT)\b"""), "")
            .replace(Regex("""[^\p{L}]"""), "")
        return stripped.length < 3
    }

    private fun parseLineWithRegex(line: String): ScannedProduct? {
        val matches = findPriceCandidates(line)
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

    private fun findPriceCandidates(line: String): List<String> {
        val strict = PRICE_REGEX.findAll(line).map { it.value }.toList()
        if (!WEIGHT_MARKER_REGEX.containsMatchIn(line) || strict.size >= 2) return strict

        val loose = LOOSE_PRICE_REGEX.findAll(line).map { it.value }.toList()
        if (loose.size < 2) return strict
        return loose.drop(1).map { normalizeToTwoDecimals(it) }
    }

    private fun normalizeToTwoDecimals(value: String): String {
        val parts = value.split(",", ".")
        if (parts.size != 2) return value
        return "${parts[0]},${parts[1].take(2)}"
    }

    private fun extractUnitPrice(matches: List<String>): Float? {
        if (matches.isEmpty()) return null
        val chosen = if (matches.size >= 2) matches[matches.size - 2] else matches[0]
        return chosen.replace(",", ".").toFloatOrNull()
    }

    private fun dedupeSimilarProducts(products: List<ScannedProduct>): List<ScannedProduct> {
        val groups = mutableListOf<MutableList<ScannedProduct>>()
        for (product in products) {
            val normalized = normalizeForComparison(product.name)
            val group = groups.find { g ->
                val repNormalized = normalizeForComparison(g.first().name)
                val nameClose = nameSimilarity(normalized, repNormalized) >= 0.5
                val priceClose = kotlin.math.abs(g.first().price - product.price) <= 0.5f
                nameClose && priceClose
            }
            if (group != null) group.add(product) else groups.add(mutableListOf(product))
        }
        return groups.map { group ->
            val modePrice = group.groupingBy { it.price }.eachCount().maxByOrNull { it.value }!!.key
            val representativeName = group.maxByOrNull { it.name.length }!!.name
            ScannedProduct(name = representativeName, price = modePrice)
        }
    }

    private fun normalizeForComparison(name: String): String =
        name.uppercase().replace(Regex("""[^\p{L}]"""), "")

    private fun nameSimilarity(a: String, b: String): Double {
        val bigramsA = charBigrams(a)
        val bigramsB = charBigrams(b)
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) return 0.0
        val intersection = bigramsA.intersect(bigramsB).size
        return (2.0 * intersection) / (bigramsA.size + bigramsB.size)
    }

    private fun charBigrams(s: String): Set<String> {
        if (s.length < 2) return setOf(s)
        return (0 until s.length - 1).map { s.substring(it, it + 2) }.toSet()
    }

    override suspend fun saveProducts(
        products: List<ScannedProduct>,
        supermarketName: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (supermarketName == null) {
            return@withContext Result.failure(Exception("Supermarket name is null"))
        }

        try {
            var supermarket = supermarketDao.getSupermarketByName(supermarketName)
            val newPrices = products.map { it.price }
            
            if (supermarket == null) {
                val newSupermarket = Supermarket(
                    name = supermarketName,
                    averagePrice = calculateNewAveragePrice(0f, 0, newPrices)
                )
                supermarketDao.upsertSupermarket(newSupermarket)
                supermarket = supermarketDao.getSupermarketByName(supermarketName)
            } else {
                val currentCount = supermarketDao.getProductCount(supermarket.id)
                val newAverage = calculateNewAveragePrice(supermarket.averagePrice, currentCount, newPrices)
                supermarketDao.upsertSupermarket(supermarket.copy(averagePrice = newAverage))
            }

            supermarket?.let { sm ->
                products.forEach { scanned ->
                    productDao.upsertProduct(
                        Product(
                            name = scanned.name,
                            price = scanned.price,
                            supermarketId = sm.id,
                            date = System.currentTimeMillis()
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateNewAveragePrice(
        currentAverage: Float,
        currentCount: Int,
        newPrices: List<Float>
    ): Float {
        val totalCurrentPrice = currentAverage * currentCount
        val totalNewPrice = newPrices.sum()
        val totalCount = currentCount + newPrices.size
        return if (totalCount > 0) (totalCurrentPrice + totalNewPrice) / totalCount else 0f
    }

    fun close() {
        engine?.close()
    }

    companion object {
        private val PRICE_REGEX = Regex("""\d{1,3}[,.]\d{2}(?!\d)(?!\s?[Kk])""")
        private val LOOSE_PRICE_REGEX = Regex("""\d{1,3}[,.]\d{2,3}(?!\d)""")
        private val WEIGHT_MARKER_REGEX = Regex("""(?i)\bk[ga6]\b""")
        private val BARCODE_REGEX = Regex("""\b\d{6,}\b""")
        private val SKIP_KEYWORDS_REGEX = Regex(
            """(?i)\b(CNPJ|CPF|IE|UNPJ|EMITENTE|CONSUMIDOR|FISCAL|ELETRONICA)\b"""
        )
        private const val NAME_BEAUTIFY_SYSTEM_INSTRUCTION = """
Você transforma a descrição de um produto de um recibo de supermercado brasileiro, com ruído de OCR e códigos, em um nome curto e legível em português. Capitalize apenas a primeira letra e nomes próprios/marcas. Responda em UMA linha, apenas com o nome, sem explicações.
"""
    }
}
