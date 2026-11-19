package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.core.debug.Sc61860InstructionSet
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDialect
import com.digihori.pgp.core.source.basic.BasicCodeMeaning
import java.io.InputStream
import java.util.Locale
import kotlinx.serialization.json.*

/** Installed, read-only knowledge. Never reads a project, ROM, user file, or network. */
internal class PocketKnowledgeService(
    private val openResource: (String) -> InputStream? = PocketKnowledgeService::class.java::getResourceAsStream,
) {
    private data class Entry(
        val id: String, val topic: String, val title: String, val summary: String,
        val keywords: List<String>, val article: String?, val sources: List<String>,
        val basis: String = "implementationDerived", val exampleId: String? = null,
        val machineIds: List<String> = listOf("pc-1245"),
    )

    private val articles = listOf(
        Entry(GENERATION_GUIDE_ID, "generation", "Program generation and pre-build review / 生成と自己確認",
            "Source structure, machine-specific bounds and timing, state restoration, and pre-build review.",
            listOf("generation", "self-check", "pre-build", "生成", "自己確認"), "PROGRAM_GENERATION.md",
            listOf("docs/guides/AI_PROGRAM_GENERATION.md"), basis = "documentedGuidance", machineIds = MACHINES),
        Entry("sc61860.buzzer", "audio", "SC61860 buzzer and melodies / ブザー・音階・メロディー",
            "Port C sound modes, software square waves, and pitch/duration guidance by machine clock.",
            listOf("buzzer", "sound", "tone", "melody", "ブザー", "発音", "音階", "メロディー"), "SC61860_BUZZER.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245Buzzer.kt",
                "test-data/assembly/pc1350-coffee-break/src/demo.asm"), machineIds = MACHINES),
        Entry("pc1245.basic", "basic", "PC-1245 BASIC and character codes / BASIC・文字コード",
            "OLD dialect, line numbers, tokens, characters, text escapes and CALL syntax.",
            listOf("BASIC", "PRINT", "INPUT", "IF", "文字", "構文", "トークン", "PI"), "PC1245_BASIC.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245BasicDialect.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245BasicTokenizer.kt",
                "docs/reference/BASIC_TEXT_FORMAT.md")),
        Entry("pc1245.memory", "memory", "PC-1245 memory / メモリ配置と共存",
            "Catalog memory regions, BASIC pointers, endianness and example placement constraints.",
            listOf("RAM", "ROM", "メモリ", "C500", "C600", "ポインター", "共存"), "PC1245_MEMORY.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245MemoryBus.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245BasicDialect.kt",
                "docs/reference/machines/RAM_1245_1261.md")),
        Entry("pc1245.display", "display", "PC-1245 LCD direct drawing / LCD直接描画",
            "80x7 pixels, column addressing, LCD enable and firmware redraw limits.",
            listOf("LCD", "表示", "画面", "描画", "F800", "OUTC"), "PC1245_DISPLAY.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245Display.kt",
                "desktopApp/src/main/kotlin/com/digihori/pgp/desktop/application/development/DevelopmentReadService.kt",
                "core/src/commonTest/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245DisplayTest.kt")),
        Entry("sc61860.assembler", "assembler", "Built-in assembler / 内蔵assembler構文",
            "ORG, DB, DW, EQU, expressions, relative branches and unsupported directives.",
            listOf("アセンブラ", "構文", "ORG", "DB", "DW", "EQU", "LP", "CAL", "TEST"), "SC61860_ASSEMBLER.md",
            listOf("docs/guides/SC61860_ASSEMBLY.md",
                "core/src/commonTest/kotlin/com/digihori/pgp/core/debug/Sc61860AssemblerTest.kt"), machineIds = MACHINES),
        Entry("pc1245.execution", "execution", "PC-1245 launch and observation / 起動・観測と未提供情報",
            "Load versus Continue versus RUN/CALL, manual keys and unknown ROM routine ABI.",
            listOf("起動", "実行", "キー", "入力", "RUN", "CALL", "ABI", "ROMルーチン", "フラグ"), "PC1245_EXECUTION.md",
            listOf("docs/reference/AI_INTEGRATION.md", "test-data/assembly/pc1245-hello/README.md")),
        Entry("pc1245.example.hello-world", "example", "PC-1245 HELLO WORLD example / 作例",
            "Retrieve the existing BASIC and assembly project with pgp_get_example.",
            listOf("サンプル", "作例", "HELLO", "表示", "CALL", "RTN"), "PC1245_HELLO_EXAMPLE.md",
            listOf("test-data/assembly/pc1245-hello/README.md",
                "desktopApp/src/test/kotlin/com/digihori/pgp/desktop/project/Pc1245HelloWorldTest.kt"),
            basis = "documentedExample", exampleId = "pc1245-hello-world"),
        Entry("pc1245.example.numeric-labels", "example", "PC-1245 numeric labels / 数値入力と分岐の作例",
            "G01: 1→ONE, 2→TWO, other→OTHER, 0→END. Observed with bounded input and LCD on one local ROM.",
            listOf("G01", "INPUT", "IF", "THEN", "数値", "入力", "ONE", "TWO", "OTHER", "生成"), "PC1245_NUMERIC_LABELS.md",
            listOf("test-data/basic/ai/pc1245-numeric-labels/README.md", "docs/development/AI_PROGRAM_GENERATION_VERIFICATION.md"),
            basis = "emulatorObservedExample", exampleId = "pc1245-numeric-labels"),
        Entry("pc1251.basic", "basic", "PC-1251 BASIC / BASIC・文字コード",
            "PGP OLD dialect code table and limits; ROM editor input uses PC-1251 key mapping.",
            listOf("BASIC", "文字", "トークン", "INPUT", "CALL"), "PC1251_SCOPE.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/source/basic/BasicProgramCodec.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1245/Pc1245BasicDialect.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1251/Pc1251RomBasicInput.kt",
                "desktopApp/src/main/kotlin/com/digihori/pgp/desktop/basic/DesktopBasicLoader.kt"), machineIds = listOf("pc-1251")),
        Entry("pc1251.memory", "memory", "PC-1251 memory / RAM構成とメモリ配置",
            "Catalog defaults and HARDWARE/UNRESTRICTED distinction. Ask machine spec for active session layout.",
            listOf("RAM", "メモリ", "HARDWARE", "UNRESTRICTED", "B800", "A000"), "PC1251_SCOPE.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/api/MachineMemoryLayout.kt",
                "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1251/Pc1251Family.kt"), machineIds = listOf("pc-1251")),
        Entry("pc1251.display", "display", "PC-1251 LCD / 表示の対応範囲",
            "24 character columns; direct drawing and MCP display capture are not exposed for this model.",
            listOf("LCD", "表示", "画面"), "PC1251_SCOPE.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1251/Pc1251Display.kt"), machineIds = listOf("pc-1251")),
        Entry("pc1251.execution", "execution", "PC-1251 AI workflow scope / 起動・生成の対応範囲",
            "Project generation, bounded input and stopped LCD capture are supported; real-ROM workflow remains unverified.",
            listOf("RUN", "CALL", "起動", "実行", "生成", "未対応"), "PC1251_SCOPE.md",
            listOf("docs/guides/AI_PROGRAM_GENERATION.md"), machineIds = listOf("pc-1251")),
        Entry("pc1251.example.numeric-labels", "example", "PC-1251 numeric labels / 数値入力と分岐の作例",
            "1→ONE, 2→TWO, other→OTHER, 0→END. Observed with MCP input and LCD on one local ROM.",
            listOf("G01", "INPUT", "IF", "数値", "入力", "ONE", "TWO", "OTHER", "作例"), "PC1251_NUMERIC_LABELS.md",
            listOf("test-data/basic/ai/pc1251-numeric-labels/README.md", "docs/development/AI_MACHINE_ROLLOUT_VERIFICATION.md"),
            basis = "emulatorObservedExample", exampleId = "pc1251-numeric-labels", machineIds = listOf("pc-1251")),
        Entry("pc1261.basic", "basic", "PC-1261 BASIC / BASIC・文字コード",
            "S1 BASIC, decimal line numbers, character escapes and Studio build/load workflow.",
            listOf("BASIC", "S1", "INPUT", "PRINT", "CALL", "文字", "行番号"), "PC1261_SCOPE.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1261/Pc1261BasicTokenizer.kt",
                "docs/reference/BASIC_TEXT_FORMAT.md"), machineIds = listOf("pc-1261")),
        Entry("pc1261.memory", "memory", "PC-1261 memory / RAMと解析範囲",
            "Hardware RAM 0x4000–0x67FF and the Studio unrestricted configuration.",
            listOf("RAM", "メモリ", "4000", "67FF", "解析"), "PC1261_SCOPE.md",
            listOf("docs/reference/machines/RAM_1245_1261.md"), machineIds = listOf("pc-1261")),
        Entry("pc1261.display", "display", "PC-1261 LCD / LCD表示",
            "Stopped screen pixels and PNG; direct drawing conventions are not included.",
            listOf("LCD", "表示", "画面"), "PC1261_SCOPE.md",
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/machine/pc1261/Pc1261Display.kt"),
            machineIds = listOf("pc-1261")),
        Entry("pc1261.execution", "execution", "PC-1261 execution / 実行",
            "Approval-backed execution and LCD observations with a matching PC-1261 project.",
            listOf("RUN", "CALL", "起動", "実行", "キー"), "PC1261_SCOPE.md",
            listOf("docs/guides/AI_PROGRAM_GENERATION.md"), machineIds = listOf("pc-1261")),
        Entry("pc1261.example.numeric-labels", "example", "PC-1261 numeric labels / 数値入力と分岐の作例",
            "1→ONE, 2→TWO, other→OTHER, 0→END. Observed with bounded input and LCD on one local ROM.",
            listOf("INPUT", "IF", "THEN", "数値", "ONE", "TWO", "OTHER", "作例"), "PC1261_SCOPE.md",
            listOf("test-data/basic/ai/pc1261-numeric-labels/README.md"),
            basis = "emulatorObservedExample", exampleId = "pc1261-numeric-labels", machineIds = listOf("pc-1261")),
        Entry("pc1350.basic", "basic", "PC-1350 BASIC",
            "PC-1350 S1 BASIC and text escapes.", listOf("BASIC", "S1", "PRINT", "INPUT", "文字"), "PC1350_SCOPE.md",
            listOf("docs/reference/machines/PC-1350.md"), machineIds = listOf("pc-1350")),
        Entry("pc1350.memory", "memory", "PC-1350 memory / RAMと解析範囲",
            "RAM 0x2000–0x7FFF excluding LCD addresses.", listOf("RAM", "メモリ", "2000", "解析"), "PC1350_SCOPE.md",
            listOf("docs/reference/machines/PC-1350.md"), machineIds = listOf("pc-1350")),
        Entry("pc1350.display", "display", "PC-1350 LCD / LCD表示",
            "25 characters by four rows; 150 by 32 pixels.", listOf("LCD", "表示", "画面"), "PC1350_SCOPE.md",
            listOf("docs/reference/machines/PC-1350.md"), machineIds = listOf("pc-1350")),
        Entry("pc1350.execution", "execution", "PC-1350 execution / 実行",
            "Firmware MODE key, RUN indicator and bounded execution.", listOf("RUN", "CALL", "MODE", "実行"), "PC1350_SCOPE.md",
            listOf("docs/reference/machines/PC-1350.md"), machineIds = listOf("pc-1350")),
        Entry("pc1350.example.lcd-strip", "example", "PC-1350 LCD strip / VRAM境界をまたぐ短い描画",
            "A data-driven sprite, shared VRAM copy, one-column movement, timed frames and register restoration.",
            listOf("LCD", "VRAM", "描画", "画像", "座標", "待機", "animation", "sprite", "作例"), "PC1350_LCD_STRIP.md",
            listOf("test-data/assembly/pc1350-lcd-strip/src/strip.asm",
                "desktopApp/src/test/kotlin/com/digihori/pgp/desktop/mcp/Pc1350LcdStripTest.kt"),
            basis = "emulatorObservedExample", exampleId = "pc1350-lcd-strip", machineIds = listOf("pc-1350")),
        Entry("pc1350.example.numeric-labels", "example", "PC-1350 numeric labels / 数値入力と分岐の作例",
            "Numeric branching and LCD prompts on PC-1350.", listOf("INPUT", "IF", "LCD", "作例"), "PC1350_SCOPE.md",
            listOf("test-data/basic/ai/pc1350-numeric-labels/README.md"),
            basis = "emulatorObservedExample", exampleId = "pc1350-numeric-labels", machineIds = listOf("pc-1350")),
        Entry("pc1401.basic", "basic", "PC-1401 BASIC",
            "PC-1401 S1 BASIC and text escapes.", listOf("BASIC", "S1", "PRINT", "INPUT", "文字"), "PC1401_SCOPE.md",
            listOf("docs/reference/machines/PC-1401.md"), machineIds = listOf("pc-1401")),
        Entry("pc1401.memory", "memory", "PC-1401 memory / RAMと解析範囲",
            "Hardware RAM 0x3800–0x47FF and the Studio unrestricted configuration.", listOf("RAM", "メモリ", "3800", "解析"), "PC1401_SCOPE.md",
            listOf("docs/reference/machines/PC-1401.md"), machineIds = listOf("pc-1401")),
        Entry("pc1401.display", "display", "PC-1401 LCD / LCD表示",
            "16 characters on one row; 80 by 7 logical pixels.", listOf("LCD", "表示", "画面"), "PC1401_SCOPE.md",
            listOf("docs/reference/machines/PC-1401.md"), machineIds = listOf("pc-1401")),
        Entry("pc1401.execution", "execution", "PC-1401 execution / 実行",
            "RUN mode, approval-backed execution and stopped LCD.", listOf("RUN", "CALL", "MODE", "実行"), "PC1401_SCOPE.md",
            listOf("docs/reference/machines/PC-1401.md"), machineIds = listOf("pc-1401")),
        Entry("pc1401.example.numeric-labels", "example", "PC-1401 numeric labels / 数値入力と分岐の作例",
            "Numeric branching and LCD prompts on PC-1401, observed on one local ROM.", listOf("INPUT", "IF", "LCD", "作例"), "PC1401_SCOPE.md",
            listOf("test-data/basic/ai/pc1401-numeric-labels/README.md"),
            basis = "emulatorObservedExample", exampleId = "pc1401-numeric-labels", machineIds = listOf("pc-1401")),
        Entry("pc1450.basic", "basic", "PC-1450 BASIC",
            "PC-1450 S1 BASIC and text escapes.", listOf("BASIC", "S1", "PRINT", "INPUT", "文字"), "PC1450_SCOPE.md",
            listOf("docs/reference/machines/PC-1450.md"), machineIds = listOf("pc-1450")),
        Entry("pc1450.memory", "memory", "PC-1450 memory / RAMと解析範囲",
            "RAM 0x2000–0x5FFF; the higher control window is not RAM.", listOf("RAM", "メモリ", "2000", "解析"), "PC1450_SCOPE.md",
            listOf("docs/reference/machines/PC-1450.md"), machineIds = listOf("pc-1450")),
        Entry("pc1450.display", "display", "PC-1450 LCD / LCD表示",
            "16 characters on one row; 80 by 7 logical pixels.", listOf("LCD", "表示", "画面"), "PC1450_SCOPE.md",
            listOf("docs/reference/machines/PC-1450.md"), machineIds = listOf("pc-1450")),
        Entry("pc1450.execution", "execution", "PC-1450 execution / 実行",
            "RUN mode, approval-backed execution and stopped LCD.", listOf("RUN", "CALL", "MODE", "実行"), "PC1450_SCOPE.md",
            listOf("docs/reference/machines/PC-1450.md"), machineIds = listOf("pc-1450")),
        Entry("pc1450.example.numeric-labels", "example", "PC-1450 numeric labels / 数値入力と分岐の作例",
            "Numeric branching and LCD prompts on PC-1450, observed on one local ROM.", listOf("INPUT", "IF", "LCD", "作例"), "PC1450_SCOPE.md",
            listOf("test-data/basic/ai/pc1450-numeric-labels/README.md"),
            basis = "emulatorObservedExample", exampleId = "pc1450-numeric-labels", machineIds = listOf("pc-1450")),
        Entry("pc1360.basic", "basic", "PC-1360 BASIC",
            "PC-1360 S2 BASIC and text escapes.", listOf("BASIC", "S2", "PRINT", "INPUT", "文字"), "PC1360_SCOPE.md",
            listOf("docs/reference/machines/PC-1360.md"), machineIds = listOf("pc-1360")),
        Entry("pc1360.memory", "memory", "PC-1360 memory / RAMと解析範囲",
            "RAM 0x8000–0xFFFF; 0x4000–0x7FFF is the banked ROM window.", listOf("RAM", "メモリ", "8000", "バンク", "解析"), "PC1360_SCOPE.md",
            listOf("docs/reference/machines/PC-1360.md"), machineIds = listOf("pc-1360")),
        Entry("pc1360.display", "display", "PC-1360 LCD / LCD表示",
            "25 characters by four rows; 150 by 32 pixels.", listOf("LCD", "表示", "画面"), "PC1360_SCOPE.md",
            listOf("docs/reference/machines/PC-1360.md"), machineIds = listOf("pc-1360")),
        Entry("pc1360.execution", "execution", "PC-1360 execution / 実行",
            "Firmware MODE key, RUN indicator and bounded execution.", listOf("RUN", "CALL", "MODE", "実行"), "PC1360_SCOPE.md",
            listOf("docs/reference/machines/PC-1360.md"), machineIds = listOf("pc-1360")),
        Entry("pc1360.example.numeric-labels", "example", "PC-1360 numeric labels / 数値入力と分岐の作例",
            "Numeric branching and LCD prompts on PC-1360, observed on one local ROM.", listOf("INPUT", "IF", "LCD", "作例"), "PC1360_SCOPE.md",
            listOf("test-data/basic/ai/pc1360-numeric-labels/README.md"),
            basis = "emulatorObservedExample", exampleId = "pc1360-numeric-labels", machineIds = listOf("pc-1360")),
        Entry("pc1360k.basic", "basic", "PC-1360K BASIC",
            "PC-1360K S2 BASIC and text escapes.", listOf("BASIC", "S2", "PRINT", "INPUT", "文字"), "PC1360K_SCOPE.md",
            listOf("docs/reference/machines/PC-1360K.md"), machineIds = listOf("pc-1360k")),
        Entry("pc1360k.memory", "memory", "PC-1360K memory / RAMと解析範囲",
            "Main RAM 0x8000–0xFFFF and Kanji work RAM 0x2000–0x27FF.", listOf("RAM", "メモリ", "8000", "バンク", "解析"), "PC1360K_SCOPE.md",
            listOf("docs/reference/machines/PC-1360K.md"), machineIds = listOf("pc-1360k")),
        Entry("pc1360k.display", "display", "PC-1360K LCD / LCD表示",
            "25 characters by four rows; 150 by 32 pixels.", listOf("LCD", "表示", "画面"), "PC1360K_SCOPE.md",
            listOf("docs/reference/machines/PC-1360K.md"), machineIds = listOf("pc-1360k")),
        Entry("pc1360k.execution", "execution", "PC-1360K execution / 実行",
            "Firmware MODE key, RUN indicator and bounded execution.", listOf("RUN", "CALL", "MODE", "実行"), "PC1360K_SCOPE.md",
            listOf("docs/reference/machines/PC-1360K.md"), machineIds = listOf("pc-1360k")),
        Entry("pc1360k.example.numeric-labels", "example", "PC-1360K numeric labels / 数値入力と分岐の作例",
            "Numeric branching and LCD prompts on PC-1360K, observed on one local ROM.", listOf("INPUT", "IF", "LCD", "作例"), "PC1360K_SCOPE.md",
            listOf("test-data/basic/ai/pc1360k-numeric-labels/README.md"),
            basis = "emulatorObservedExample", exampleId = "pc1360k-numeric-labels", machineIds = listOf("pc-1360k")),
        Entry("pc1460.basic", "basic", "PC-1460 BASIC",
            "PC-1460 S1 BASIC and text escapes.", listOf("BASIC", "S1", "PRINT", "INPUT", "文字"), "PC1460_SCOPE.md",
            listOf("docs/reference/machines/PC-1460.md"), machineIds = listOf("pc-1460")),
        Entry("pc1460.memory", "memory", "PC-1460 memory / RAMと解析範囲",
            "RAM 0x8000–0xFFFF and four ROM banks.", listOf("RAM", "メモリ", "8000", "バンク", "解析"), "PC1460_SCOPE.md",
            listOf("docs/reference/machines/PC-1460.md"), machineIds = listOf("pc-1460")),
        Entry("pc1460.display", "display", "PC-1460 LCD / LCD表示",
            "24 characters on one row; 120 by 7 pixels.", listOf("LCD", "表示", "画面"), "PC1460_SCOPE.md",
            listOf("docs/reference/machines/PC-1460.md"), machineIds = listOf("pc-1460")),
        Entry("pc1460.execution", "execution", "PC-1460 execution / 実行",
            "Firmware MODE key, RUN indicator and bounded execution.", listOf("RUN", "CALL", "MODE", "実行"), "PC1460_SCOPE.md",
            listOf("docs/reference/machines/PC-1460.md"), machineIds = listOf("pc-1460")),
        Entry("pc1460.example.numeric-labels", "example", "PC-1460 numeric labels / 数値入力と分岐の作例",
            "Numeric branching and LCD prompts on PC-1460, observed on one local ROM.", listOf("INPUT", "IF", "LCD", "作例"), "PC1460_SCOPE.md",
            listOf("test-data/basic/ai/pc1460-numeric-labels/README.md"),
            basis = "emulatorObservedExample", exampleId = "pc1460-numeric-labels", machineIds = listOf("pc-1460")),
        Entry("pc1470u.basic", "basic", "PC-1470U BASIC",
            "PC-1470U S2 BASIC and text escapes.", listOf("BASIC", "S2", "PRINT", "INPUT", "文字"), "PC1470U_SCOPE.md",
            listOf("docs/reference/machines/PC-1470U.md"), machineIds = listOf("pc-1470u")),
        Entry("pc1470u.memory", "memory", "PC-1470U memory / RAMと解析範囲",
            "Studio RAM window 0x8000–0xFFFF and eight ROM banks.", listOf("RAM", "メモリ", "8000", "バンク", "解析"), "PC1470U_SCOPE.md",
            listOf("docs/reference/machines/PC-1470U.md"), machineIds = listOf("pc-1470u")),
        Entry("pc1470u.display", "display", "PC-1470U LCD / LCD表示",
            "24 characters by two rows; 120 by 14 pixels.", listOf("LCD", "表示", "画面"), "PC1470U_SCOPE.md",
            listOf("docs/reference/machines/PC-1470U.md"), machineIds = listOf("pc-1470u")),
        Entry("pc1470u.execution", "execution", "PC-1470U execution / 実行",
            "Firmware MODE key, RUN indicator and bounded execution.", listOf("RUN", "CALL", "MODE", "実行"), "PC1470U_SCOPE.md",
            listOf("docs/reference/machines/PC-1470U.md"), machineIds = listOf("pc-1470u")),
        Entry("pc1470u.example.numeric-labels", "example", "PC-1470U numeric labels / 数値入力と分岐の作例",
            "Numeric branching and LCD prompts on PC-1470U, observed on one local ROM.", listOf("INPUT", "IF", "LCD", "作例"), "PC1470U_SCOPE.md",
            listOf("test-data/basic/ai/pc1470u-numeric-labels/README.md"),
            basis = "emulatorObservedExample", exampleId = "pc1470u-numeric-labels", machineIds = listOf("pc-1470u")),
    )
    private val instructions = Sc61860InstructionSet.allDefined().groupBy { it.mnemonic }.toSortedMap()
    private val entries = articles + instructions.map { (mnemonic, _) ->
        val hasDetails = Sc61860InstructionKnowledge.contains(mnemonic)
        Entry("sc61860.instruction.${mnemonic.lowercase(Locale.ROOT)}", "instruction", "SC61860 $mnemonic",
            if (hasDetails) "Core instruction effects, flags, and cycle counts." else "Core opcode, operand, and control-flow metadata.",
            if (hasDetails) listOf(mnemonic, "命令", "opcode", "cycles", "timing", "サイクル") else listOf(mnemonic, "命令", "opcode"),
            null,
            listOf("core/src/commonMain/kotlin/com/digihori/pgp/core/debug/Sc61860InstructionSet.kt") +
                listOfNotNull(Sc61860InstructionKnowledge.SOURCE.takeIf { hasDetails }), machineIds = MACHINES)
    }
    private val bodies by lazy { articles.associate { it.id to resource("articles/${it.article}") } }

    fun search(
        machineId: String, query: String, topic: String?, offset: Int, limit: Int,
        includeContent: Boolean = false, ids: List<String>? = null,
    ): JsonObject {
        machine(machineId)
        if (query.length > 256 || topic != null && topic !in TOPICS || offset !in 0..1_000_000 || limit !in 1..16)
            throw DevelopmentReadError("INVALID_ARGUMENTS")
        if (ids != null && (query.isNotBlank() || topic != null || ids.size !in 1..16 ||
                ids.distinct().size != ids.size || ids.any { it.isBlank() || it.length > 128 })) {
            throw DevelopmentReadError("INVALID_ARGUMENTS")
        }
        val words = query.lowercase(Locale.ROOT).trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        val found = ids?.map { id -> findEntry(machineId, id) } ?: entries.filter { entry ->
            machineId in entry.machineIds && (topic == null || entry.topic == topic) && words.all { word ->
                (listOf(entry.id, entry.title, entry.summary) + entry.keywords + bodies[entry.id].orEmpty())
                    .any { it.lowercase(Locale.ROOT).contains(word) }
            }
        }.sortedWith(compareByDescending<Entry> { entry ->
            words.count { it == entry.id || entry.keywords.any { keyword -> keyword.equals(it, ignoreCase = true) } }
        }.thenBy { it.id })
        return response(machineId, "pgp-knowledge-search") {
            put("machineId", machineId); put("query", query); put("topic", topic?.let(::JsonPrimitive) ?: JsonNull)
            ids?.let { put("ids", JsonArray(it.map(::JsonPrimitive))) }
            put("offset", offset); put("limit", limit); put("total", found.size)
            put("nextOffset", if (offset + limit < found.size) JsonPrimitive(offset + limit) else JsonNull)
            put("items", buildJsonArray {
                found.drop(offset).take(limit).forEach { entry ->
                    add(if (includeContent) buildJsonObject { putContent(entry, machineId) } else metadata(entry))
                }
            })
        }
    }

    fun get(machineId: String, id: String): JsonObject {
        machine(machineId)
        val entry = findEntry(machineId, id)
        return response(machineId, "pgp-knowledge-item") {
            put("machineId", machineId)
            putContent(entry, machineId)
        }
    }

    private fun findEntry(machineId: String, id: String): Entry =
        entries.find { it.id == id && machineId in it.machineIds } ?: throw DevelopmentReadError("KNOWLEDGE_NOT_FOUND")

    private fun JsonObjectBuilder.putContent(entry: Entry, machineId: String) {
        put("item", metadata(entry))
        put("body", bodies[entry.id] ?: if (Sc61860InstructionKnowledge.contains(entry.title.removePrefix("SC61860 "))) {
            "Core instruction effects, flags and timing are in data.coreBehavior. See sc61860.assembler for syntax."
        } else "Core instruction metadata. See sc61860.assembler for syntax. Full semantics, flags and cycle counts are not provided.")
        put("data", data(entry, machineId))
    }

    fun example(machineId: String, exampleId: String): JsonObject {
        machine(machineId)
        val entry = articles.find { it.exampleId == exampleId && machineId in it.machineIds } ?: throw DevelopmentReadError("EXAMPLE_NOT_FOUND")
        return response(machineId, "pgp-knowledge-example") {
            put("machineId", machineId); put("exampleId", exampleId); put("item", metadata(entry))
            put("guidance", bodies.getValue(entry.id)); put("createsFiles", false)
            put("files", buildJsonArray {
                val paths = when (exampleId) {
                    "pc1245-hello-world" -> listOf("pgp-project.json", "src/main.bas", "src/hello.asm")
                    "pc1350-lcd-strip" -> listOf("pgp-project.json", "src/main.bas", "src/strip.asm")
                    else -> listOf("pgp-project.json", "src/main.bas")
                }
                paths.forEach { path ->
                    add(buildJsonObject { put("path", path); put("text", resource("examples/$exampleId/$path")); put("encoding", "UTF-8") })
                }
            })
        }
    }

    private fun data(entry: Entry, machineId: String): JsonObject = buildJsonObject {
        when (entry.id) {
            "sc61860.buzzer" -> put("cyclesPerSecond", catalogSpec(machineId).getValue("cyclesPerSecond"))
            "pc1245.basic", "pc1251.basic" -> {
                val dialect = Pc1245BasicDialect.definition
                put("dialectId", dialect.id.value); put("maximumLineNumber", dialect.programFormat.maximumLineNumber)
                put("startMarker", dialect.programFormat.startMarker); put("endMarker", dialect.programFormat.endMarker)
                put("lineTerminator", dialect.programFormat.lineTerminator); put("lineNumberEncoding", dialect.programFormat.lineNumberEncoding.name)
                put("codeTable", buildJsonArray { (0..255).forEach { code -> dialect.meaningOf(code)?.let { meaning -> add(buildJsonObject {
                    put("code", code)
                    when (meaning) {
                        is BasicCodeMeaning.Keyword -> { put("kind", "keyword"); put("text", meaning.text) }
                        is BasicCodeMeaning.Character -> { put("kind", "character"); put("text", meaning.value.toString()) }
                        is BasicCodeMeaning.Special -> { put("kind", "special"); put("symbol", meaning.symbol.name) }
                    }
                }) } } })
            }
            "pc1261.basic", "pc1350.basic", "pc1401.basic", "pc1450.basic", "pc1360.basic", "pc1360k.basic", "pc1460.basic", "pc1470u.basic" -> put("dialectId", catalogSpec(machineId).getValue("basicDialect"))
            "pc1245.memory" -> {
                val spec = catalogSpec()
                put("memoryRegions", spec.getValue("memoryRegions")); put("memoryLayout", spec.getValue("memoryLayout"))
                put("basicTextStart", Pc1245BasicDialect.PROGRAM_TEXT_START)
                put("basicStartPointerLow", Pc1245BasicDialect.PROGRAM_START_POINTER_LOW)
                put("basicEndPointerLow", Pc1245BasicDialect.PROGRAM_END_POINTER_LOW)
                put("basicStorageEndExclusive", Pc1245BasicDialect.PROGRAM_STORAGE_END_EXCLUSIVE)
            }
            "pc1245.display", "pc1251.display", "pc1261.display", "pc1350.display", "pc1401.display", "pc1450.display", "pc1360.display", "pc1360k.display", "pc1460.display", "pc1470u.display" -> put("display", catalogSpec(machineId).getValue("display"))
            "pc1251.memory", "pc1261.memory", "pc1350.memory", "pc1401.memory", "pc1450.memory", "pc1360.memory", "pc1360k.memory", "pc1460.memory", "pc1470u.memory" -> {
                val spec = catalogSpec(machineId)
                put("memoryRegions", spec.getValue("memoryRegions")); put("memoryLayout", spec.getValue("memoryLayout"))
            }
            "pc1251.execution", "pc1261.execution", "pc1350.execution", "pc1401.execution", "pc1450.execution", "pc1360.execution", "pc1360k.execution", "pc1460.execution", "pc1470u.execution" -> {
                put("keyboard", catalogSpec(machineId).getValue("keyboard"))
                put("boundedExecutionSupported", true); put("displayCaptureSupported", true); put("projectCreationSupported", true)
            }
            "pc1245.execution" -> put("keyboard", catalogSpec().getValue("keyboard"))
            else -> if (entry.topic == "instruction") {
                put("variants", buildJsonArray { instructions.getValue(entry.title.removePrefix("SC61860 ")).forEach { instruction -> add(buildJsonObject {
                    put("opcode", instruction.opcode); put("mnemonic", instruction.mnemonic); put("length", instruction.length)
                    put("operandEncoding", instruction.operandEncoding.name); put("flow", instruction.flow.name); put("encodable", instruction.encodable)
                }) } })
                val behavior = Sc61860InstructionKnowledge.details(entry.title.removePrefix("SC61860 "))
                behavior?.let { put("coreBehavior", it) }
                put("notProvided", buildJsonArray {
                    add("fullSemantics")
                    if (behavior == null) { add("flagEffects"); add("cycleCounts") }
                    else add("hardwareVerification")
                })
            }
        }
    }

    private fun metadata(entry: Entry) = buildJsonObject {
        put("id", entry.id); put("topic", entry.topic); put("title", entry.title); put("summary", entry.summary)
        put("machineIds", buildJsonArray { entry.machineIds.forEach { add(it) } }); put("keywords", buildJsonArray { entry.keywords.forEach { add(it) } })
        entry.exampleId?.let { put("exampleId", it) }
        put("sources", buildJsonArray {
            (entry.sources + listOfNotNull(entry.article?.let { "docs/reference/knowledge/$it" })).forEach { path ->
                add(buildJsonObject { put("path", path); put("scope", "PGP repository reference; bundled data does not require a checkout") })
            }
        })
        putJsonObject("verification") {
            put("basis", entry.basis); put("realHardware", "notVerified")
            put("romApplicability", "PGP implementation for the listed machineIds; arbitrary ROM revisions and ROM routine ABI are not verified")
            put("evidencePolicy", "Source/test references are provenance, not proof that real-ROM tests ran in this session")
        }
    }

    private fun catalogSpec(machineId: String = "pc-1245") = DesktopDevelopmentReadService { DevelopmentReadState() }.machineSpec(machineId)
    private fun machine(machineId: String) {
        if (machineId !in MACHINES) throw DevelopmentReadError("UNSUPPORTED_KNOWLEDGE_MACHINE")
    }
    private fun resource(path: String): String = checkNotNull(openResource("/knowledge/$path"))
        .use { stream ->
            val bytes = stream.readNBytes(MAX_BYTES + 1)
            check(bytes.size <= MAX_BYTES)
            bytes.decodeToString(throwOnInvalidSequence = true)
        }
    private fun response(machineId: String, format: String, body: JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject {
        put("format", format); put("schemaVersion", 1); put("catalogVersion", when (machineId) {
            "pc-1245" -> "pc1245-v2"
            "pc-1251" -> "pc1251-v3"
            "pc-1261" -> "pc1261-v1"
            "pc-1350" -> "pc1350-v1"
            "pc-1401" -> "pc1401-v1"
            "pc-1450" -> "pc1450-v1"
            else -> "${machineId.replace("-", "")}-v1"
        }); body()
    }.also { if (it.toString().toByteArray(Charsets.UTF_8).size > MAX_BYTES) throw DevelopmentReadError("RESPONSE_TOO_LARGE") }

    companion object {
        val MACHINES = GeneratedProjectFiles.MACHINES
        const val GENERATION_GUIDE_ID = "pgp.program-generation"
        val TOPICS = listOf("generation", "basic", "memory", "display", "audio", "assembler", "execution", "example", "instruction")
        const val MAX_BYTES = 64 * 1024
    }
}
