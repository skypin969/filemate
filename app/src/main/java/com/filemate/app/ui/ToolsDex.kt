package com.filemate.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.filemate.app.R
import com.filemate.app.data.FileRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Satu kelas di dalam berkas DEX: nama yang mudah dibaca dan data aslinya. */
class DexClass(val name: String, val def: ClassDef)

/** Membaca semua kelas dari berkas APK (semua classes*.dex di dalamnya) atau satu berkas DEX. */
fun loadDexClasses(file: File): List<DexClass> {
    val container = DexFileFactory.loadDexContainer(file, Opcodes.getDefault())
    val result = ArrayList<DexClass>()
    for (entryName in container.dexEntryNames) {
        val dex = container.getEntry(entryName)?.dexFile ?: continue
        for (def in dex.classes) {
            result.add(DexClass(readableType(def.type), def))
        }
    }
    result.sortBy { it.name }
    return result
}

/** Mengubah penulisan jenis gaya DEX ("Lcom/a/B;", "[I") menjadi gaya Java ("com.a.B", "int[]"). */
fun readableType(descriptor: String): String {
    var depth = 0
    while (depth < descriptor.length && descriptor[depth] == '[') depth++
    val base = descriptor.substring(depth)
    val name = when (base) {
        "V" -> "void"
        "Z" -> "boolean"
        "B" -> "byte"
        "S" -> "short"
        "C" -> "char"
        "I" -> "int"
        "J" -> "long"
        "F" -> "float"
        "D" -> "double"
        else -> base.removePrefix("L").removeSuffix(";").replace('/', '.')
    }
    return name + "[]".repeat(depth)
}

/** Kata sifat (public, static, final, ...) dari angka penanda akses. */
private fun flagWords(flags: Int, forMethod: Boolean): String {
    val words = ArrayList<String>()
    if ((flags and 0x1) != 0) words.add("public")
    if ((flags and 0x2) != 0) words.add("private")
    if ((flags and 0x4) != 0) words.add("protected")
    if ((flags and 0x8) != 0) words.add("static")
    if ((flags and 0x10) != 0) words.add("final")
    if (forMethod && (flags and 0x20) != 0) words.add("synchronized")
    if (forMethod && (flags and 0x100) != 0) words.add("native")
    if ((flags and 0x400) != 0) words.add("abstract")
    return if (words.isEmpty()) "" else words.joinToString(" ") + " "
}

/** Ringkasan satu kelas: induk, antarmuka, variabel, dan fungsi beserta jenisnya. */
fun describeClass(def: ClassDef): String {
    val text = StringBuilder()
    val kind = if ((def.accessFlags and 0x200) != 0) "interface" else "class"
    text.append(flagWords(def.accessFlags and 0x400.inv(), false)).append(kind).append(' ').append(readableType(def.type)).append('\n')
    def.superclass?.let { text.append("  extends ").append(readableType(it)).append('\n') }
    if (def.interfaces.isNotEmpty()) {
        text.append("  implements ").append(def.interfaces.joinToString(", ") { readableType(it) }).append('\n')
    }
    text.append('\n')
    for (field in def.fields) {
        text.append(flagWords(field.accessFlags, false)).append(readableType(field.type)).append(' ').append(field.name).append('\n')
    }
    text.append('\n')
    for (method in def.methods) {
        val params = method.parameterTypes.joinToString(", ") { readableType(it.toString()) }
        text.append(flagWords(method.accessFlags, true))
            .append(readableType(method.returnType)).append(' ')
            .append(method.name).append('(').append(params).append(")\n")
    }
    return text.toString().trimEnd()
}

/**
 * Analisis kelas: menampilkan daftar semua kelas di sebuah APK atau DEX. Ketuk satu kelas
 * untuk melihat induk, variabel, dan fungsinya.
 */
@Composable
fun ClassAnalysisScreen(path: String, onBack: () -> Unit) {
    var classes by remember { mutableStateOf<List<DexClass>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var opened by remember { mutableStateOf<DexClass?>(null) }

    LaunchedEffect(path) {
        val loaded = withContext(Dispatchers.IO) {
            try {
                loadDexClasses(File(path))
            } catch (e: Throwable) {
                null
            }
        }
        failed = loaded == null
        classes = loaded ?: emptyList()
    }

    val all = classes
    val shown = remember(all, query) {
        all.orEmpty().filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }

    ToolScaffold(stringResource(R.string.tool_class_analysis), onBack) {
        Text(
            text = FileRepository.nameOf(path) + if (all != null) "  (${shown.size}/${all.size})" else "",
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        if (all == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Ink)
        }
        if (failed) {
            Text(
                text = stringResource(R.string.dex_failed),
                color = Danger,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f).navigationBarsPadding()) {
            items(shown) { item ->
                Text(
                    text = item.name,
                    color = Ink,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { opened = item }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    val current = opened
    if (current != null) {
        val detail = remember(current) {
            try {
                describeClass(current.def)
            } catch (e: Throwable) {
                "?"
            }
        }
        AlertDialog(
            onDismissRequest = { opened = null },
            containerColor = Color.White,
            title = { Text(current.name.substringAfterLast('.'), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    SelectionContainer {
                        Text(
                            text = detail,
                            color = Ink,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { opened = null }) {
                    Text(stringResource(R.string.mcp_close), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}
