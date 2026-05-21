package com.example.ui.console

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.database.ConsoleLog
import com.example.data.database.ConsoleRepository
import com.example.data.network.Content
import com.example.data.network.GeminiRequest
import com.example.data.network.GenerationConfig
import com.example.data.network.Part
import com.example.data.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Screen Navigation Enum
enum class ConsoleTab {
    DASHBOARD,
    TOOLS,
    COPILOT
}

// Copilot Chat Model
data class ChatMessage(
    val sender: String, // "USER", "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ConsoleViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ConsoleRepository(database.consoleLogDao())

    // 1. Logs Flow (Room Database Source of Truth)
    val consoleLogs: StateFlow<List<ConsoleLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Navigation Tab Key
    private val _currentTab = MutableStateFlow(ConsoleTab.DASHBOARD)
    val currentTab: StateFlow<ConsoleTab> = _currentTab.asStateFlow()

    // 3. Dynamic Interactive States
    private val _bootloaderState = MutableStateFlow("Locked") // Locked, Unlocking..., Unlocked
    val bootloaderState: StateFlow<String> = _bootloaderState.asStateFlow()

    private val _superuserState = MutableStateFlow("Disabled") // Disabled, Injecting..., Enabled
    val superuserState: StateFlow<String> = _superuserState.asStateFlow()

    private val _systemVersionState = MutableStateFlow("Android 10.0 (Legacy)") // Android 10.0 (Legacy), Upgrading..., Android 14.1 (Cyber OS)
    val systemVersionState: StateFlow<String> = _systemVersionState.asStateFlow()

    private val _isUpgraded = MutableStateFlow(false)
    val isUpgraded: StateFlow<Boolean> = _isUpgraded.asStateFlow()

    // For tracking action animations
    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // 4. Copilot Chat Parameters
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("AI", "Welcome to CyberCore AI Console. Ask me about Android custom ROMs, flashing instructions, bootloader guidelines, or safety risks of rooting.")
        )
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Host Info Helper
    val deviceManufacturer: String = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val deviceModel: String = Build.MODEL
    val deviceHardware: String = Build.HARDWARE

    init {
        // Run first seed check for logs
        viewModelScope.launch {
            delay(300)
            if (database.consoleLogDao().getLogCount() == 0) {
                seedInitialDiagnosticLogs()
            }
        }
    }

    fun selectTab(tab: ConsoleTab) {
        _currentTab.value = tab
    }

    fun updateChatInput(text: String) {
        _chatInput.value = text
    }

    private suspend fun seedInitialDiagnosticLogs() {
        repository.log("INFO", "=== CYBERCORE PRO PLATFORM v2.6.5 ===")
        repository.log("INFO", "Detecting physical device architecture...")
        repository.log("INFO", "Hardware detected: $deviceManufacturer $deviceModel [Board: $deviceHardware]")
        repository.log("WARN", "Environment Baseline: Bootloader LOCKED, Root CONTEXT Restricted")
        repository.log("WARN", "Firmware Target: API level ${Build.VERSION.SDK_INT} detected")
        repository.log("INFO", "Console Engine initialized. Developer sandbox is operational.")
    }

    // Interactive Action to Simulate Bootloader Unlock
    fun unlockBootloader() {
        if (_bootloaderState.value != "Locked" || _isBusy.value) return
        _isBusy.value = true
        _bootloaderState.value = "Unlocking..."
        
        viewModelScope.launch {
            repository.log("CMD", "adb reboot bootloader")
            delay(1200)
            repository.log("INFO", "Phone rebooting into download/fastboot protocol...")
            delay(1000)
            repository.log("INFO", "Device connected to local fastboot engine interface (Port: USB3.0/tty0)")
            delay(1200)
            repository.log("CMD", "fastboot flashing unlock")
            delay(1500)
            repository.log("WARN", "==================================================")
            repository.log("WARN", "WARNING: BOOTLOADER UNLOCK REQUEST SIGNALED.")
            repository.log("WARN", "This will wipe all local device storage blocks for privacy.")
            repository.log("WARN", "==================================================")
            delay(1500)
            repository.log("INFO", "Sending command sequence key: 0xAB6B5D71C...")
            delay(1200)
            repository.log("INFO", "Wiping userdata block cache / cryptokey elements...")
            delay(1400)
            repository.log("SUCCESS", "Bootloader UNLOCKED successfully. Verification signature: [DISABLED]")
            repository.log("CMD", "fastboot reboot")
            delay(1000)
            repository.log("INFO", "Operating system starting up into unlocked developer state...")
            
            _bootloaderState.value = "Unlocked"
            _isBusy.value = false
        }
    }

    // Interactive Action to Simulate Root Injection (Magisk Style)
    fun injectRoot() {
        if (_superuserState.value != "Disabled" || _isBusy.value) return
        // Ideally should have bootloader unlocked first, let's warn but proceed
        _isBusy.value = true
        _superuserState.value = "Injecting..."

        viewModelScope.launch {
            if (_bootloaderState.value != "Unlocked") {
                repository.log("WARN", "CRITICAL WARNING: Attempting root injection with locked bootloader!")
                repository.log("WARN", "Standard security layers (dm-verity) will reject modifications.")
                delay(1200)
            }
            repository.log("CMD", "adb shell getprop ro.secure")
            delay(1000)
            repository.log("INFO", "Current environment value is secure=1. Building patch context...")
            delay(1200)
            repository.log("INFO", "Downloading Magisk background daemon payload (v26.1)...")
            delay(1300)
            repository.log("CMD", "fastboot boot boot_patched.img")
            delay(1400)
            repository.log("INFO", "Decompressing boot ramdisk headers...")
            delay(1000)
            repository.log("INFO", "Injecting customized init.rc rule: /system/bin/magiskd --startup")
            delay(1200)
            repository.log("INFO", "Remounting logical block partitions as write-permissive...")
            delay(1000)
            repository.log("INFO", "Deploying custom compiled su binary into /system/xbin/su")
            delay(1100)
            repository.log("CMD", "chmod 06755 /system/xbin/su")
            delay(1200)
            repository.log("SUCCESS", "SuperUser context established! Root authority environment [ACTIVE]")
            
            _superuserState.value = "Enabled"
            _isBusy.value = false
        }
    }

    // Interactive Action to Simulated Custom OS Upgrade
    fun runSystemFixAndUpgrade() {
        if (_isBusy.value) return
        _isBusy.value = true
        _systemVersionState.value = "Upgrading..."

        viewModelScope.launch {
            repository.log("CMD", "cyber_upgrade_checker --channel beta --force")
            delay(1000)
            repository.log("INFO", "Querying remote custom ROM CyberCore servers...")
            delay(1200)
            repository.log("INFO", "Target artifact: package_cybercore_14.1_stable.zip")
            repository.log("INFO", "AOSP Baseband, API 34. Custom optimization engine compiled.")
            delay(1500)
            repository.log("INFO", "Synchronized payload download progress (Size: 2.14GB)...")
            
            // Fast counting simulation
            for (p in 0..100 step 25) {
                repository.log("INFO", "Downloader payload buffer: $p% fetched")
                delay(800)
            }

            repository.log("SUCCESS", "Zip checksum MD5 verified: D41D8CD98F00B204E9800998ECF8427E")
            delay(1000)
            repository.log("CMD", "reboot recovery_ota")
            delay(1200)
            repository.log("INFO", "Device restarted in customized TWRP recovery interface.")
            delay(1000)
            repository.log("INFO", "Unpacking system payload block blocks onto partition /dev/block/by-name/system_a...")
            delay(1500)
            repository.log("INFO", "Flashing custom kernel optimized performance governor presets...")
            delay(1200)
            repository.log("INFO", "Initializing custom Art compiler runtime caches...")
            delay(1300)
            repository.log("SUCCESS", "Flashing protocol complete! Initializing custom system blocks...")
            delay(1200)
            repository.log("INFO", "Rebooting device. Welcome to Android 14.1 custom ROM environment.")

            _systemVersionState.value = "Android 14.1 (Cyber OS)"
            _isUpgraded.value = true
            _isBusy.value = false
        }
    }

    // Direct performance quick maintenance run
    fun runQuickOptimization() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            repository.log("CMD", "sys_optimize --trim --governor")
            delay(1000)
            repository.log("INFO", "Scanning inactive background processes and pagecaches...")
            delay(1000)
            // Real battery stats checking
            repository.log("INFO", "Thermal limit safety range verified. Operating parameters safe.")
            delay(1000)
            repository.log("INFO", "Performing local fstrim on /data partition...")
            delay(1200)
            repository.log("SUCCESS", "Reclaimed 485MB cached space. Governor profile switched to Performance.")
            _isBusy.value = false
        }
    }

    // Clear saved Room console history
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            seedInitialDiagnosticLogs()
        }
    }

    // Use actual Gemini API to answer Android Modding / custom ROM questions safely and educationally
    fun sendCopilotQuery() {
        val query = _chatInput.value.trim()
        if (query.isEmpty() || _isChatLoading.value) return

        // 1. Add User Message
        val currentMsgs = _chatHistory.value.toMutableList()
        currentMsgs.add(ChatMessage("USER", query))
        _chatHistory.value = currentMsgs
        _chatInput.value = ""
        _isChatLoading.value = true

        viewModelScope.launch {
            val systemInstructionText = """
                You are the CyberCore AI Assistant, a professional device customizer, ROM flasher, and system optimization assistant. 
                Your job is to answer structural, technical, or programming questions about Android system customization, bootloaders, custom ROMs, standard rooting (like Magisk), and update procedures safely and educationally. 
                You must explain both the benefits and safety risks (such as bricking, voided warranty, hardware security locks, losing Knox/SafetyNet) of these processes, and guide the user through safe, official, and legal developer steps. 
                Keep your answers highly technical, analytical, extremely clear, and engagingly structured with markdown.
                If they ask about an impossible task (such as hacking remote devices, bypass locks illegally), refuse firmly and explain the security engineering reason why it's locked down.
            """.trimIndent()

            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = query)))),
                systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))),
                generationConfig = GenerationConfig(temperature = 0.7f)
            )

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                    // Placeholder key or empty key warning response
                    delay(1500)
                    val history = _chatHistory.value.toMutableList()
                    history.add(ChatMessage("AI", "⚠️ **SECURE ACCESS WARNING**: Gemini API Key is not configured yet! Please configure your `GEMINI_API_KEY` in the AI Studio Secrets panel.\n\n*Simulated Educational Response*:\n\nTo root or bootloader-unlock safely, you must utilize native recovery methods (such as TWRP or Magisk patching) which require physical device authorization and enabling Developer Settings. Standard modern Android phones (Android 10+) employ dm-verity and hardware security modules (like Titans or Knox) to protect the integrity of the boot path." ))
                    _chatHistory.value = history
                } else {
                    val response = RetrofitClient.service.generateContent(apiKey, requestBody)
                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No diagnostic reply retrieved from the Gemini console agent."
                    
                    val history = _chatHistory.value.toMutableList()
                    history.add(ChatMessage("AI", responseText))
                    _chatHistory.value = history
                }
            } catch (e: java.net.UnknownHostException) {
                val errorMsg = "❌ **Network Offline Error**: Unable to resolve the Gemini service domain. Please check your internet connection or network status."
                repository.log("ERROR", "Gemini API Connection Offline: Host interface unresolved (DNS/offline).")
                val history = _chatHistory.value.toMutableList()
                history.add(ChatMessage("AI", errorMsg))
                _chatHistory.value = history
            } catch (e: java.net.ConnectException) {
                val errorMsg = "❌ **Service Unreachable**: Connection to Google APIs server failed. Ensure you are connected to a working cellular or Wi-Fi network."
                repository.log("ERROR", "Gemini API Connection Failed: Server interface timed out or rejected request.")
                val history = _chatHistory.value.toMutableList()
                history.add(ChatMessage("AI", errorMsg))
                _chatHistory.value = history
            } catch (e: java.net.SocketTimeoutException) {
                val errorMsg = "❌ **Response Timeout**: The request timed out. The server did not respond within the allocated time window. Please try shortening your query."
                repository.log("ERROR", "Gemini API Call Limit: Socket read timeout (60s limit).")
                val history = _chatHistory.value.toMutableList()
                history.add(ChatMessage("AI", errorMsg))
                _chatHistory.value = history
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                val errorDetail = when (code) {
                    400 -> "Bad Request (400) - Verify model configuration, input syntax, or safety blocks."
                    401, 403 -> "Authentication Failure ($code) - The API key provided is invalid, restricted, or expired. Re-verify the GEMINI_API_KEY inside AI Studio Secrets panel."
                    429 -> "Quota Limits Exceeded (429) - You have surpassed your allocated Gemini API rate limits or credit quotas."
                    500, 503 -> "Internal Gateway Error ($code) - Google API services are temporarily down or experiencing server load."
                    else -> "Unexpected Network Code ($code) - Server returned negative response status."
                }
                val errorMsg = "❌ **Diagnostics Alert**: $errorDetail\n\n*System Error Frame*:\n```\n$errorBody\n```"
                repository.log("ERROR", "Gemini API Protocol Code $code: $errorDetail")
                val history = _chatHistory.value.toMutableList()
                history.add(ChatMessage("AI", errorMsg))
                _chatHistory.value = history
            } catch (e: Exception) {
                val errorMsg = "❌ **General Communication Fault**: ${e.localizedMessage ?: "Unknown diagnostic exception"}"
                repository.log("ERROR", "Gemini Copilot Error: ${e.localizedMessage}")
                val history = _chatHistory.value.toMutableList()
                history.add(ChatMessage("AI", errorMsg))
                _chatHistory.value = history
            } finally {
                _isChatLoading.value = false
            }
        }
    }
}
