package com.hefimer.app.data.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.hefimer.app.data.local.HefimerStorage
import com.hefimer.app.data.model.CustomS3Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExpireOption(val label: String, val labelVi: String = label, val value: String) {
    fun localizedLabel(): String = com.hefimer.app.ui.theme.tr(label, labelVi)
}

data class UploadProviderConfig(
    val id: String,
    val name: String,
    val maxSizeBytes: Long,
    val maxSizeLabel: String,
    val maxSizeLabelVi: String = maxSizeLabel,
    val limitDetail: String = maxSizeLabel,
    val limitDetailVi: String = maxSizeLabelVi,
    val expiry: String,
    val expiryVi: String,
    val speed: String,
    val speedVi: String,
    val downloadFlow: String = "",
    val downloadFlowVi: String = "",
    val privacyDetail: String = "",
    val privacyDetailVi: String = "",
    val bestFor: String = "",
    val bestForVi: String = "",
    val limitations: String = "",
    val limitationsVi: String = "",
    val features: String,
    val featuresVi: String,
    val isLargeFile: Boolean,
    val defaultExpire: String,
    val expireOptions: List<ExpireOption>,
) {
    fun localizedMaxSize(): String = com.hefimer.app.ui.theme.tr(maxSizeLabel, maxSizeLabelVi)
    fun localizedLimitDetail(): String = com.hefimer.app.ui.theme.tr(limitDetail, limitDetailVi)
    fun localizedExpiry(): String = com.hefimer.app.ui.theme.tr(expiry, expiryVi)
    fun localizedSpeed(): String = com.hefimer.app.ui.theme.tr(speed, speedVi)
    fun localizedDownloadFlow(): String = com.hefimer.app.ui.theme.tr(downloadFlow, downloadFlowVi)
    fun localizedPrivacyDetail(): String = com.hefimer.app.ui.theme.tr(privacyDetail, privacyDetailVi)
    fun localizedBestFor(): String = com.hefimer.app.ui.theme.tr(bestFor, bestForVi)
    fun localizedLimitations(): String = com.hefimer.app.ui.theme.tr(limitations, limitationsVi)
    fun localizedFeatures(): String = com.hefimer.app.ui.theme.tr(features, featuresVi)
}

data class FallbackUploadResult(
    val fileUrl: String,
    val providerId: String,
    val providerName: String,
)

data class LocalPickedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val tempFile: File? = null,
)

class UploadCancellationToken {
    private val cancelled = AtomicBoolean(false)
    private val activeCall = AtomicReference<Call?>(null)

    fun isCancelled(): Boolean = cancelled.get()

    fun bindCall(call: Call) {
        activeCall.set(call)
        if (cancelled.get()) call.cancel()
    }

    fun clearCall(call: Call? = null) {
        if (call == null) activeCall.set(null) else activeCall.compareAndSet(call, null)
    }

    fun cancel() {
        cancelled.set(true)
        activeCall.getAndSet(null)?.cancel()
    }
}

object UploadProviders {
    private const val MB = 1024L * 1024L
    private const val GB = 1024L * MB
    private const val WORKER_PROXY_SAFE_MAX = 95L * MB

    val STANDARD_EXPIRE_OPTIONS = listOf(
        ExpireOption("30 sec", "30 giây", "30s"),
        ExpireOption("5 min", "5 phút", "5p"),
        ExpireOption("30 min", "30 phút", "30p"),
        ExpireOption("1 hour", "1 giờ", "1h"),
        ExpireOption("12 hours", "12 giờ", "12h"),
        ExpireOption("24 hours", "24 giờ", "24h"),
        ExpireOption("48 hours", "48 giờ", "48h"),
    )

    val ALL: List<UploadProviderConfig> = listOf(
        UploadProviderConfig(
            id = "storageto",
            name = "storage.to",
            maxSizeBytes = 25L * GB,
            maxSizeLabel = "25 GB",
            limitDetail = "25 GB/file • Anonymous: 50 files/24h & 100 GB/24h (500 GB/IP) • Free API Token: Unlimited files & bandwidth",
            limitDetailVi = "25 GB/tệp • Ẩn danh: 50 tệp/24h & 100 GB/24h (500 GB/IP) • Có API Token miễn phí: Không giới hạn số tệp & băng thông",
            expiry = "3 days default (configurable 1, 2, 3, 5, or 7 days)",
            expiryVi = "Mặc định 3 ngày (chọn linh hoạt 1, 2, 3, 5 hoặc 7 ngày)",
            speed = "Very fast direct Cloudflare R2 presigned PUT + batched multipart (>50 MB)",
            speedVi = "Rất nhanh: đẩy thẳng lên Cloudflare R2, tự chia phần đa luồng khi >50 MB",
            downloadFlow = "On-device direct stream resolution via phone IP to Cloudflare CDN (no speed cap)",
            downloadFlowVi = "Giải mã tải thẳng trên điện thoại về CDN Cloudflare (không giới hạn tốc độ)",
            privacyDetail = "No account needed; calls directly from your phone IP; supports optional Free Bearer Token & Owner Token",
            privacyDetailVi = "Không cần tài khoản; gọi API bằng IP riêng của máy; hỗ trợ gắn Free API Token trong Cài đặt",
            bestFor = "Default everyday & large-file sharing up to 25 GB (videos, archives, APKs, builds)",
            bestForVi = "Mặc định mọi nhu cầu & tệp lớn tới 25 GB (video, file nén, APK, bộ cài)",
            limitations = "Anonymous rate limits: 60 init/min, 120 parts/min per IP; files always expire after max 7 days (free tier)",
            limitationsVi = "Ẩn danh giới hạn 60 lượt tạo/phút & 50 tệp/ngày/IP; tệp tự xóa tối đa sau 7 ngày (bản miễn phí)",
            features = "Default primary host. Calls storage.to directly from your device (with Hefimer proxy backup) and auto-falls back across 17 hosts if throttled.",
            featuresVi = "Máy chủ mặc định. Gọi trực tiếp từ IP máy bạn (dự phòng qua proxy nếu bị chặn) và tự chuyển qua 17 host khác nếu đầy hạn mức.",
            isLargeFile = true,
            defaultExpire = "72h",
            expireOptions = listOf(
                ExpireOption("1 day", "1 ngày", "24h"),
                ExpireOption("2 days", "2 ngày", "48h"),
                ExpireOption("3 days (Default)", "3 ngày (Mặc định)", "72h"),
                ExpireOption("5 days", "5 ngày", "120h"),
                ExpireOption("7 days", "7 ngày", "168h"),
            ),
        ),
        UploadProviderConfig(
            id = "stocare",
            name = "sto.care",
            maxSizeBytes = 99L * MB,
            maxSizeLabel = "100 MB",
            limitDetail = "Up to 99 MB on mobile direct PUT route (no daily file count limit)",
            limitDetailVi = "Tối đa 99 MB qua luồng PUT trực tiếp trên mobile (không giới hạn số tệp/ngày)",
            expiry = "Deletes after 72 hours (fixed)",
            expiryVi = "Tự xóa sau 72 giờ (cố định)",
            speed = "Very fast direct PUT stream to trystocare.com edge",
            speedVi = "Siêu nhanh: PUT trực tiếp tới edge trystocare.com",
            downloadFlow = "Clean direct raw binary URL (instant in-app download)",
            downloadFlowVi = "Liên kết tải thẳng tệp gốc (tải ngay trong app không qua trung gian)",
            privacyDetail = "Zero signup, anonymous one-shot upload with randomized short URL",
            privacyDetailVi = "Không cần đăng ký, tải ẩn danh một chạm kèm đường dẫn ngẫu nhiên",
            bestFor = "Quick documents, photos, short clips under 100 MB needing fast direct downloads",
            bestForVi = "Tài liệu, ảnh, clip ngắn dưới 100 MB cần gửi và tải thẳng cực nhanh",
            limitations = "Fixed 72h expiry (cannot extend/shorten); 99 MB cap on Android route",
            limitationsVi = "Thời hạn cố định 72 giờ (không chỉnh được); giới hạn 99 MB trên bản di động",
            features = "Great lightweight #1 backup whenever storage.to hits a local carrier or quota limit.",
            featuresVi = "Tuyến dự phòng số 1 siêu nhẹ và ổn định khi storage.to gặp giới hạn mạng.",
            isLargeFile = false,
            defaultExpire = "72h",
            expireOptions = listOf(ExpireOption("72 hours (Fixed)", "72 giờ (Cố định)", "72h")),
        ),
        UploadProviderConfig(
            id = "x0at",
            name = "x0.at",
            maxSizeBytes = 512L * MB,
            maxSizeLabel = "512 MB",
            limitDetail = "512 MB mobile cap (service max 1 GiB) • No strict daily count cap",
            limitDetailVi = "Tối đa 512 MB trên mobile (hệ thống tối đa 1 GiB) • Không giới hạn số lượt",
            expiry = "3–100 days (size-scaled: smaller files stay up to 100d, max-size files stay 3d)",
            expiryVi = "Lưu 3–100 ngày theo dung lượng (tệp nhỏ giữ tới 100 ngày, tệp tối đa giữ 3 ngày)",
            speed = "Ultra-lightweight direct multipart POST with minimal overhead",
            speedVi = "Siêu nhẹ, phản hồi tức thì với ít chi phí kết nối",
            downloadFlow = "Direct raw file URL with zero redirect or HTML wrapper",
            downloadFlowVi = "Liên kết tải thẳng tệp gốc, không chuyển hướng hay trang chờ",
            privacyDetail = "Minimalist privacy-focused host, no account or cookies required",
            privacyDetailVi = "Tối giản bảo vệ riêng tư, không tài khoản hay cookie theo dõi",
            bestFor = "Scripts, APKs, logs, and medium files under 512 MB shared to CLI/mobile",
            bestForVi = "Tệp cài đặt, tài liệu, bản ghi dưới 512 MB cần link tải thẳng sạch sẽ",
            limitations = "Retention shrinks non-linearly as file size approaches limit; no custom expiry control",
            limitationsVi = "Thời gian lưu giảm dần theo dung lượng tệp; không tự chọn số ngày hết hạn",
            features = "Rock-solid fallback host returning clean raw URLs.",
            featuresVi = "Máy chủ dự phòng cực kỳ bền bỉ, trả về link tải thẳng sạch.",
            isLargeFile = true,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "pomf",
            name = "pomf.lain.la",
            maxSizeBytes = 1L * GB,
            maxSizeLabel = "1 GB",
            limitDetail = "Up to 1 GB per file • No account required",
            limitDetailVi = "Tối đa 1 GB mỗi tệp • Không cần tài khoản",
            expiry = "Permanent / long-term CDN retention (unless removed for abuse)",
            expiryVi = "Lưu trữ lâu dài / vĩnh viễn trên CDN (trừ nội dung vi phạm)",
            speed = "Fast direct upload & pomf2.lain.la CDN with HTTP Range resume support",
            speedVi = "Tải lên trực tiếp nhanh & CDN pomf2.lain.la hỗ trợ tiếp tục tải (Range)",
            downloadFlow = "Direct raw bytes from pomf2.lain.la CDN",
            downloadFlowVi = "Tải thẳng byte gốc từ CDN pomf2.lain.la",
            privacyDetail = "Privacy-friendly open Pomf host; anonymous uploads without login",
            privacyDetailVi = "Máy chủ Pomf thân thiện quyền riêng tư, tải ẩn danh không cần đăng nhập",
            bestFor = "Files up to 1 GB you want to stay accessible long-term without auto-expiry",
            bestForVi = "Tệp tối đa 1 GB cần giữ lâu dài mà không bị tự xóa sau vài ngày",
            limitations = "Occasional brief maintenance windows; no password or manual delete UI from Hefimer",
            limitationsVi = "Đôi khi bảo trì định kỳ ngắn; chưa hỗ trợ đặt mật khẩu hay tự xóa từ app",
            features = "Reliable 1 GB host serving direct raw bytes on pomf2.lain.la.",
            featuresVi = "Máy chủ 1 GB ổn định, tải xuống trực tiếp không qua trang trung gian.",
            isLargeFile = true,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "quax",
            name = "qu.ax",
            maxSizeBytes = 256L * MB,
            maxSizeLabel = "256 MB",
            limitDetail = "256 MB per file • Anonymous public uploads",
            limitDetailVi = "Tối đa 256 MB mỗi tệp • Tải công khai ẩn danh",
            expiry = "30 days default (inactive files subject to periodic cleanup)",
            expiryVi = "Mặc định 30 ngày (tệp không có lượt truy cập lâu ngày có thể bị dọn)",
            speed = "Fast global nginx edge with low-latency multipart upload",
            speedVi = "Edge nginx toàn cầu phản hồi nhanh, độ trễ thấp",
            downloadFlow = "On-device direct stream extraction (app resolves raw file bytes automatically)",
            downloadFlowVi = "Tự động tách luồng tải tệp gốc ngay trên điện thoại",
            privacyDetail = "No signup or tracking; generates short randomized URLs",
            privacyDetailVi = "Không cần tài khoản hay theo dõi; tạo liên kết ngắn ngẫu nhiên",
            bestFor = "Medium files up to 256 MB needing ~1 month availability",
            bestForVi = "Tệp vừa tối đa 256 MB cần lưu khoảng 1 tháng",
            limitations = "Some networks/ISPs may rate-limit public nginx edges; no custom expiry via Hefimer",
            limitationsVi = "Một số mạng có thể giới hạn tốc độ tải liên tiếp; không tùy chỉnh hạn xóa",
            features = "256 MB anonymous host with automatic on-device direct file stream extraction.",
            featuresVi = "Máy chủ 256 MB tự động tách luồng tải tệp trực tiếp ngay trên điện thoại.",
            isLargeFile = true,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "catbox",
            name = "Catbox",
            maxSizeBytes = 200L * MB,
            maxSizeLabel = "200 MB",
            limitDetail = "200 MB per file • No daily upload count cap",
            limitDetailVi = "Tối đa 200 MB mỗi tệp • Không giới hạn số lượt tải lên/ngày",
            expiry = "Permanent (hosted indefinitely unless violating ToS)",
            expiryVi = "Vĩnh viễn (lưu trữ vô thời hạn trừ khi vi phạm điều khoản)",
            speed = "Very fast global CDN via files.catbox.moe",
            speedVi = "Rất nhanh qua CDN toàn cầu files.catbox.moe",
            downloadFlow = "Instant direct raw CDN link (ideal for media & direct previews)",
            downloadFlowVi = "Link CDN tải thẳng tức thì (hỗ trợ xem trước ảnh/video cực mượt)",
            privacyDetail = "Anonymous upload with no account required",
            privacyDetailVi = "Tải ẩn danh không cần đăng ký tài khoản",
            bestFor = "Images, audio, PDFs, and videos under 200 MB that need permanent hosting",
            bestForVi = "Ảnh, âm thanh, PDF, video dưới 200 MB cần lưu trữ vĩnh viễn",
            limitations = "Explicitly blocks executable/script formats (.exe, .scr, .cpl, .jar, macro .doc)",
            limitationsVi = "Chặn các định dạng thực thi/script (.exe, .scr, .cpl, .jar, file .doc có macro)",
            features = "Dedicated 200 MB Catbox host with instant direct CDN links and high uptime.",
            featuresVi = "Máy chủ Catbox 200 MB siêu ổn định, tải thẳng qua files.catbox.moe.",
            isLargeFile = false,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "litterbox",
            name = "Litterbox",
            maxSizeBytes = 1L * GB,
            maxSizeLabel = "1 GB",
            limitDetail = "Up to 1 GB per file • Companion temporary service to Catbox",
            limitDetailVi = "Tối đa 1 GB mỗi tệp • Dịch vụ lưu tạm cùng hệ sinh thái Catbox",
            expiry = "Auto-deletes strictly after chosen 1, 12, 24, or 72 hours",
            expiryVi = "Tự hủy chính xác sau 1, 12, 24 hoặc 72 giờ tùy chọn",
            speed = "Fast direct Catbox CDN upload & delivery (proxy fallback if blocked)",
            speedVi = "Tải thẳng tốc độ cao qua CDN Catbox (có dự phòng qua proxy nếu bị chặn)",
            downloadFlow = "Instant direct raw CDN download",
            downloadFlowVi = "Tải xuống trực tiếp tệp gốc từ CDN",
            privacyDetail = "Anonymous temporary drop; auto-purged on schedule without trace",
            privacyDetailVi = "Chia sẻ tạm ẩn danh; tự động xóa sạch đúng hạn đã chọn",
            bestFor = "Temporary files up to 1 GB that you want guaranteed auto-deletion",
            bestForVi = "Tệp tạm tới 1 GB cần đảm bảo tự động xóa đúng giờ",
            limitations = "Same blocked executable types as Catbox (.exe, .scr, .cpl, .jar); max retention 72h",
            limitationsVi = "Chặn một số file thực thi giống Catbox (.exe, .scr, .cpl, .jar); giữ tối đa 72 giờ",
            features = "Temporary Catbox host up to 1 GB with configurable retention and direct downloads.",
            featuresVi = "Lưu tạm qua Catbox tối đa 1 GB, tùy chọn thời hạn xóa và tải xuống trực tiếp.",
            isLargeFile = true,
            defaultExpire = "24h",
            expireOptions = listOf(
                ExpireOption("1 hour", "1 giờ", "1h"),
                ExpireOption("12 hours", "12 giờ", "12h"),
                ExpireOption("24 hours", "24 giờ", "24h"),
                ExpireOption("72 hours", "72 giờ", "72h"),
            ),
        ),
        UploadProviderConfig(
            id = "buzzheavier",
            name = "Buzzheavier",
            maxSizeBytes = Long.MAX_VALUE,
            maxSizeLabel = "No limit",
            maxSizeLabelVi = "Không giới hạn",
            limitDetail = "No file size limit • No bandwidth or speed caps",
            limitDetailVi = "Không giới hạn dung lượng tệp • Không bóp băng thông hay tốc độ",
            expiry = "8 days initial (+2 days per download, max 60d; permanent if 30+ downloads/60d)",
            expiryVi = "8 ngày ban đầu (+2 ngày mỗi lượt tải, tối đa 60 ngày; vĩnh viễn nếu ≥30 lượt/60 ngày)",
            speed = "High-throughput raw HTTP PUT streaming upload",
            speedVi = "Tải lên HTTP PUT trực tiếp băng thông rộng",
            downloadFlow = "Opens Buzzheavier download page in browser / WebView",
            downloadFlowVi = "Khi tải sẽ mở trang Buzzheavier để lấy tệp",
            privacyDetail = "Anonymous upload without login; public link access",
            privacyDetailVi = "Tải ẩn danh không cần đăng nhập; truy cập qua liên kết chia sẻ",
            bestFor = "Huge files (>25 GB) that exceed storage.to's 25 GB ceiling",
            bestForVi = "Tệp siêu lớn (>25 GB) vượt quá giới hạn 25 GB của storage.to",
            limitations = "Requires opening Buzzheavier web page to download (not raw in-app byte stream)",
            limitationsVi = "Không tải thẳng byte trong app mà mở trang web Buzzheavier",
            features = "No-limit anonymous large-file host with fast PUT streaming.",
            featuresVi = "Lưu trữ tệp không giới hạn dung lượng, truyền tải PUT nhanh.",
            isLargeFile = true,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "fileditch",
            name = "Fileditch",
            maxSizeBytes = 150L * GB,
            maxSizeLabel = "150 GB",
            limitDetail = "Up to 150 GB per file • Anonymous friendly",
            limitDetailVi = "Tối đa 150 GB mỗi tệp • Hỗ trợ ẩn danh hoàn toàn",
            expiry = "Retained while active; files untouched for 45+ days may be cleaned up",
            expiryVi = "Giữ lâu dài khi còn lượt tải; tệp không có tương tác >45 ngày có thể bị dọn",
            speed = "High-capacity edge upload up to 150 GB",
            speedVi = "Tải lên dung lượng cực lớn tới 150 GB",
            downloadFlow = "Opens Fileditch page to download (direct stream fallback attempted where possible)",
            downloadFlowVi = "Khi tải sẽ mở trang Fileditch để tải xuống",
            privacyDetail = "No account needed; anonymous upload",
            privacyDetailVi = "Không cần tài khoản; tải lên ẩn danh",
            bestFor = "Extra-large backups, ISOs, or raw footage from 25 GB to 150 GB",
            bestForVi = "Bản sao lưu, file ISO hoặc video thô cực nặng từ 25 GB đến 150 GB",
            limitations = "Downloads go through Fileditch web page; occasional heavy load on free edges",
            limitationsVi = "Tải xuống qua trang web Fileditch; đôi lúc tải nặng vào giờ cao điểm",
            features = "High-capacity anonymous host up to 150 GB as a heavy-duty backup route.",
            featuresVi = "Máy chủ dung lượng lớn tới 150 GB làm tuyến dự phòng mạnh.",
            isLargeFile = true,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "gofile",
            name = "Gofile",
            maxSizeBytes = Long.MAX_VALUE,
            maxSizeLabel = "No limit",
            maxSizeLabelVi = "Không giới hạn",
            limitDetail = "No file size limit • Multi-server auto-selection • No speed cap",
            limitDetailVi = "Không giới hạn dung lượng • Tự chọn máy chủ tốt nhất • Không bóp tốc độ",
            expiry = "Temporary free storage: stays active while downloaded, purged after ~10+ days idle",
            expiryVi = "Lưu tạm miễn phí: giữ khi còn lượt tải, tự xóa sau ~10 ngày không có lượt truy cập",
            speed = "Fast multi-server direct upload across global Gofile nodes",
            speedVi = "Tải trực tiếp đa máy chủ tốc độ cao trên các node toàn cầu",
            downloadFlow = "Opens Gofile share folder page (supports multi-file web preview)",
            downloadFlowVi = "Mở trang thư mục Gofile (hỗ trợ xem trước nhiều định dạng trên web)",
            privacyDetail = "Anonymous guest upload; generates unique folder link",
            privacyDetailVi = "Tải lên dạng khách ẩn danh; tạo liên kết thư mục riêng",
            bestFor = "Very large files (>25 GB) or recipients comfortable downloading via Gofile web UI",
            bestForVi = "Tệp rất lớn (>25 GB) hoặc người nhận quen tải qua giao diện web Gofile",
            limitations = "Downloads open Gofile page; free tier purges inactive files quickly under high server load",
            limitationsVi = "Khi tải sẽ mở trang Gofile; tệp miễn phí không có lượt tải sẽ bị xóa sớm khi server đầy",
            features = "Large-file external route with automatic multi-server retry.",
            featuresVi = "Tuyến tệp lớn kèm tự động đổi máy chủ khi kết nối lỗi.",
            isLargeFile = true,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "vikingfile",
            name = "VikingFile",
            maxSizeBytes = Long.MAX_VALUE,
            maxSizeLabel = "No limit",
            maxSizeLabelVi = "Không giới hạn",
            limitDetail = "No stated size limit (smoothly handles multi-GB files) • No speed cap",
            limitDetailVi = "Không giới hạn dung lượng (hỗ trợ mượt tệp nhiều GB) • Không bóp tốc độ",
            expiry = "Typically ~30 days for anonymous shares (activity-based cleanup)",
            expiryVi = "Thường lưu ~30 ngày cho chia sẻ ẩn danh (dọn khi ngừng truy cập)",
            speed = "Fast S3 multipart direct chunk upload",
            speedVi = "Tải lên S3 đa phần tốc độ cao",
            downloadFlow = "Opens VikingFile download page",
            downloadFlowVi = "Khi tải sẽ mở trang VikingFile để tải tệp",
            privacyDetail = "Anonymous upload; signed S3 multipart flow via Hefimer relay",
            privacyDetailVi = "Tải ẩn danh; chia luồng S3 đa phần qua điều phối của Hefimer",
            bestFor = "Large fallback uploads when other hosts are throttled or full",
            bestForVi = "Tuyến dự phòng cho tệp lớn khi các máy chủ khác bị giới hạn",
            limitations = "Init/complete goes via Hefimer relay; downloads open VikingFile web page",
            limitationsVi = "Khởi tạo qua trung gian Hefimer; khi tải sẽ mở trang web VikingFile",
            features = "Large-file S3 multipart fallback route.",
            featuresVi = "Tuyến dự phòng S3 cho tệp lớn.",
            isLargeFile = true,
            defaultExpire = "48h",
            expireOptions = STANDARD_EXPIRE_OPTIONS,
        ),
        UploadProviderConfig(
            id = "tmpfilelink",
            name = "tfLink",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            limitDetail = "95 MB safe limit on Hefimer (host max 100 MB)",
            limitDetailVi = "Giới hạn an toàn 95 MB trên Hefimer (máy chủ gốc tối đa 100 MB)",
            expiry = "Deletes automatically after 7 days (fixed)",
            expiryVi = "Tự động xóa sau 7 ngày (cố định)",
            speed = "Global CDN upload & fast edge delivery (direct + proxy fallback)",
            speedVi = "Tải lên nhanh & phân phối CDN toàn cầu (trực tiếp + dự phòng proxy)",
            downloadFlow = "Direct CDN download",
            downloadFlowVi = "Tải xuống trực tiếp qua CDN",
            privacyDetail = "Anonymous one-step upload, auto-expires in 7 days",
            privacyDetailVi = "Tải ẩn danh một bước, tự động hết hạn sau 7 ngày",
            bestFor = "Documents & media under 95 MB needing a full 7-day window",
            bestForVi = "Tài liệu & media dưới 95 MB cần thời hạn lưu cố định 7 ngày",
            limitations = "Capped at 95 MB; 7-day expiry cannot be shortened",
            limitationsVi = "Giới hạn dưới 95 MB; không rút ngắn được thời hạn 7 ngày",
            features = "tmpfile.link temporary hosting with 7-day retention and direct CDN downloads.",
            featuresVi = "Lưu trữ tạm trên tmpfile.link trong 7 ngày và tải trực tiếp qua CDN.",
            isLargeFile = false,
            defaultExpire = "168h",
            expireOptions = listOf(ExpireOption("7 days (Fixed)", "7 ngày (Cố định)", "168h")),
        ),
        UploadProviderConfig(
            id = "tempfile",
            name = "TempFile",
            maxSizeBytes = 100L * MB,
            maxSizeLabel = "100 MB",
            limitDetail = "Up to 100 MB per file • Public API with proxy backup",
            limitDetailVi = "Tối đa 100 MB mỗi tệp • API công khai kèm dự phòng proxy",
            expiry = "Deletes after 1, 6, 24, or 48 hours (configurable)",
            expiryVi = "Tự xóa sau 1, 6, 24 hoặc 48 giờ (tùy chọn)",
            speed = "Fast direct upload & CDN edge delivery",
            speedVi = "Tải lên nhanh & phân phối qua CDN",
            downloadFlow = "Direct CDN raw download",
            downloadFlowVi = "Tải thẳng tệp gốc qua CDN",
            privacyDetail = "Anonymous upload; configurable short retention window",
            privacyDetailVi = "Tải ẩn danh; tùy chỉnh thời hạn tự xóa ngắn hạn",
            bestFor = "Files under 100 MB where you want exact 1h–48h auto-expiry",
            bestForVi = "Tệp dưới 100 MB muốn hẹn giờ tự hủy chính xác 1–48 giờ",
            limitations = "100 MB hard cap; max retention 48 hours",
            limitationsVi = "Giới hạn tối đa 100 MB; chỉ giữ tối đa 48 giờ",
            features = "Public API with direct downloads and configurable temporary retention.",
            featuresVi = "Tải xuống trực tiếp, tùy chọn thời hạn lưu trữ linh hoạt.",
            isLargeFile = false,
            defaultExpire = "24h",
            expireOptions = listOf(
                ExpireOption("1 hour", "1 giờ", "1h"),
                ExpireOption("6 hours", "6 giờ", "6h"),
                ExpireOption("24 hours", "24 giờ", "24h"),
                ExpireOption("48 hours", "48 giờ", "48h"),
            ),
        ),
        UploadProviderConfig(
            id = "filebin",
            name = "Filebin",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            limitDetail = "95 MB safe limit on Hefimer (subject to shared server pool capacity)",
            limitDetailVi = "Giới hạn an toàn 95 MB trên Hefimer (tùy dung lượng kho máy chủ)",
            expiry = "Deletes after 6 days (fixed)",
            expiryVi = "Tự xóa sau 6 ngày (cố định)",
            speed = "Standard streaming HTTP PUT into a dedicated random bin",
            speedVi = "Tốc độ truyền tải PUT ổn định vào kho riêng ngẫu nhiên",
            downloadFlow = "Verified direct file download",
            downloadFlowVi = "Tải xuống trực tiếp tệp gốc",
            privacyDetail = "Creates a unique unguessable bin ID per share; no account required",
            privacyDetailVi = "Tạo mã kho riêng khó đoán cho mỗi lần gửi; không cần tài khoản",
            bestFor = "Reliable direct file transfer under 95 MB for ~1 week",
            bestForVi = "Gửi tệp trực tiếp ổn định dưới 95 MB lưu khoảng 6 ngày",
            limitations = "95 MB cap; server may pause anonymous uploads briefly if global storage pool is full",
            limitationsVi = "Giới hạn 95 MB; máy chủ có thể tạm ngưng nhận tệp mới khi kho chung quá tải",
            features = "One private bin per share with verified direct file downloads.",
            featuresVi = "Tạo kho riêng cho mỗi lượt gửi với tải xuống trực tiếp.",
            isLargeFile = false,
            defaultExpire = "144h",
            expireOptions = listOf(ExpireOption("6 days (Fixed)", "6 ngày (Cố định)", "144h")),
        ),
        UploadProviderConfig(
            id = "tempsh",
            name = "temp.sh",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            limitDetail = "95 MB safe limit via fallback route (host native limit 4 GB)",
            limitDetailVi = "Giới hạn an toàn 95 MB qua tuyến dự phòng (máy chủ gốc tối đa 4 GB)",
            expiry = "Deletes after 3 days (fixed)",
            expiryVi = "Tự xóa sau 3 ngày (cố định)",
            speed = "Fast minimalist HTTP form upload (direct + proxy fallback)",
            speedVi = "Tải lên tối giản nhanh (trực tiếp + dự phòng proxy)",
            downloadFlow = "Direct byte download via Hefimer download resolver",
            downloadFlowVi = "Tải thẳng byte tệp qua bộ xử lý của Hefimer",
            privacyDetail = "Zero-account, no-bloat temporary file drop",
            privacyDetailVi = "Không tài khoản, tối giản không lưu thông tin dư thừa",
            bestFor = "Quick 3-day temporary backup under 95 MB",
            bestForVi = "Gửi nhanh tệp tạm dưới 95 MB lưu cố định 3 ngày",
            limitations = "Capped at 95 MB in Hefimer to keep proxy fallback reliable; fixed 3-day expiry",
            limitationsVi = "Giới hạn 95 MB trong Hefimer để đảm bảo tuyến dự phòng ổn định; cố định 3 ngày",
            features = "Minimalist temporary host with 3-day retention.",
            featuresVi = "Máy chủ tối giản lưu 3 ngày, hỗ trợ tải thẳng ổn định.",
            isLargeFile = false,
            defaultExpire = "72h",
            expireOptions = listOf(ExpireOption("3 days (Fixed)", "3 ngày (Cố định)", "72h")),
        ),
        UploadProviderConfig(
            id = "tmpfiles",
            name = "tmpfiles.org",
            maxSizeBytes = 100L * MB,
            maxSizeLabel = "100 MB",
            limitDetail = "Up to 100 MB per file (100 MiB)",
            limitDetailVi = "Tối đa 100 MB mỗi tệp (100 MiB)",
            expiry = "Auto-deletes after 60 minutes (1 hour)",
            expiryVi = "Tự động xóa chỉ sau 60 phút (1 giờ)",
            speed = "Fast direct upload with automatic /dl/ raw link conversion",
            speedVi = "Tải trực tiếp nhanh kèm tự đổi sang link gốc /dl/",
            downloadFlow = "Direct raw byte download",
            downloadFlowVi = "Tải xuống trực tiếp tệp gốc",
            privacyDetail = "Ephemeral 1-hour lifetime minimizes exposure of sensitive one-time files",
            privacyDetailVi = "Tự hủy sau đúng 1 giờ, giảm thiểu lộ lọt tệp dùng một lần",
            bestFor = "One-time sensitive docs, tickets, or quick transfers needed for under 1 hour",
            bestForVi = "Tài liệu nhạy cảm dùng một lần hoặc tệp chỉ cần nhận ngay trong 1 giờ",
            limitations = "Very short 60-minute expiry; 100 MB max size",
            limitationsVi = "Thời hạn cực ngắn chỉ 60 phút; giới hạn 100 MB",
            features = "Ultra-short-lived temporary hosting. Automatically deleted after 1 hour.",
            featuresVi = "Lưu tạm siêu ngắn hạn, tự động xóa sạch sau đúng 1 giờ.",
            isLargeFile = false,
            defaultExpire = "1h",
            expireOptions = listOf(ExpireOption("1 hour (Fixed)", "1 giờ (Cố định)", "1h")),
        ),
        UploadProviderConfig(
            id = "uguu",
            name = "Uguu.se",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            limitDetail = "95 MB safe limit on Hefimer (host max 128 MiB)",
            limitDetailVi = "Giới hạn an toàn 95 MB trên Hefimer (máy chủ gốc tối đa 128 MiB)",
            expiry = "Deletes after 3 hours (fixed)",
            expiryVi = "Tự xóa sau 3 giờ (cố định)",
            speed = "Fast lightweight Pomf-style upload",
            speedVi = "Máy chủ nhẹ kiểu Pomf, phản hồi cực nhanh",
            downloadFlow = "Direct raw file download",
            downloadFlowVi = "Tải xuống trực tiếp tệp gốc",
            privacyDetail = "Anonymous ephemeral upload, auto-purged in 3 hours",
            privacyDetailVi = "Tải ẩn danh ngắn hạn, tự động xóa sau 3 giờ",
            bestFor = "Quick screenshots, short clips, or temporary files needed for a few hours",
            bestForVi = "Ảnh chụp màn hình, clip ngắn hoặc tệp chỉ cần chia sẻ trong vài giờ",
            limitations = "Fixed 3-hour expiry; 95 MB limit",
            limitationsVi = "Tự xóa cố định sau 3 giờ; giới hạn 95 MB",
            features = "Pomf-style ephemeral file host with direct downloads.",
            featuresVi = "Máy chủ chia sẻ ngắn hạn nhẹ, tải xuống trực tiếp nhanh gọn.",
            isLargeFile = false,
            defaultExpire = "3h",
            expireOptions = listOf(ExpireOption("3 hours (Fixed)", "3 giờ (Cố định)", "3h")),
        ),
        UploadProviderConfig(
            id = "puticu",
            name = "Put.icu",
            maxSizeBytes = WORKER_PROXY_SAFE_MAX,
            maxSizeLabel = "95 MB",
            limitDetail = "95 MB safe limit (via Hefimer Linx proxy)",
            limitDetailVi = "Giới hạn an toàn 95 MB (qua proxy Linx của Hefimer)",
            expiry = "Deletes after 5 min, 1h, 12h, or 24h (via Linx-Expiry)",
            expiryVi = "Tự xóa sau 5 phút, 1 giờ, 12 giờ hoặc 24 giờ (Linx-Expiry)",
            speed = "Standard Linx PUT upload via Hefimer proxy",
            speedVi = "Tải lên Linx PUT ổn định qua proxy Hefimer",
            downloadFlow = "Direct raw file download",
            downloadFlowVi = "Tải xuống trực tiếp tệp gốc",
            privacyDetail = "Anonymous Linx drop with randomized key & short expiry",
            privacyDetailVi = "Tải ẩn danh qua Linx với khóa ngẫu nhiên và tự hủy ngắn hạn",
            bestFor = "Ultra-short 5-minute to 24-hour file drops under 95 MB",
            bestForVi = "Chia sẻ siêu ngắn hạn từ 5 phút tới 24 giờ cho tệp dưới 95 MB",
            limitations = "95 MB proxy cap; max retention 24 hours",
            limitationsVi = "Giới hạn 95 MB qua proxy; lưu tối đa 24 giờ",
            features = "Configurable short-lived retention (5m, 1h, 12h, 24h) with direct downloads.",
            featuresVi = "Tùy chọn thời hạn ngắn (5p, 1g, 12g, 24g) kèm tải trực tiếp.",
            isLargeFile = false,
            defaultExpire = "24h",
            expireOptions = listOf(
                ExpireOption("5 minutes", "5 phút", "5p"),
                ExpireOption("1 hour", "1 giờ", "1h"),
                ExpireOption("12 hours", "12 giờ", "12h"),
                ExpireOption("24 hours", "24 giờ", "24h"),
            ),
        ),
    )

    val FALLBACK_PRIORITY = listOf(
        "storageto",
        "stocare",
        "x0at",
        "pomf",
        "quax",
        "catbox",
        "tmpfilelink",
        "tempfile",
        "uguu",
        "tempsh",
        "filebin",
        "puticu",
        "tmpfiles",
        "litterbox",
        "buzzheavier",
        "fileditch",
        "gofile",
        "vikingfile",
    )

    /** Providers suited for direct inline image & SVG previews (direct CDN or working /api/download proxy). */
    val IMAGE_PREVIEW_PRIORITY = listOf(
        "pomf",
        "x0at",
        "stocare",
        "tmpfiles",
        "storageto",
        "uguu",
        "tmpfilelink",
        "puticu",
    )

    fun toConfig(custom: CustomS3Provider): UploadProviderConfig = UploadProviderConfig(
        id = custom.id,
        name = custom.name.ifBlank { custom.bucket },
        maxSizeBytes = 5L * GB,
        maxSizeLabel = "5 GB",
        limitDetail = "5 GB direct PUT to your bucket (${custom.bucket})",
        limitDetailVi = "Tối đa 5 GB tải thẳng lên bucket của bạn (${custom.bucket})",
        expiry = "Controlled by your bucket lifecycle (presigned links up to 7d)",
        expiryVi = "Theo cấu hình bucket của bạn (link ký sẵn tối đa 7 ngày)",
        speed = "Direct AWS SigV4 presigned PUT from phone to ${custom.endpoint.removePrefix("https://")}",
        speedVi = "PUT trực tiếp có chữ ký AWS SigV4 từ máy tới ${custom.endpoint.removePrefix("https://")}",
        downloadFlow = if (custom.publicBaseUrl.isNotBlank()) {
            "Direct download via your custom domain"
        } else {
            "Direct S3/R2 download via presigned GET URL"
        },
        downloadFlowVi = if (custom.publicBaseUrl.isNotBlank()) {
            "Tải thẳng qua tên miền CDN riêng của bạn"
        } else {
            "Tải thẳng từ S3/R2 qua liên kết GET đã ký sẵn"
        },
        privacyDetail = "100% user-owned storage; credentials stay strictly on this device",
        privacyDetailVi = "Kho lưu trữ riêng 100%; khóa truy cập chỉ lưu cục bộ trên máy này",
        bestFor = "Private or team files stored in your own Cloudflare R2 / S3 bucket",
        bestForVi = "Tệp riêng tư hoặc công việc lưu thẳng vào R2 / S3 của chính bạn",
        limitations = "Requires valid CORS / endpoint credentials on your bucket",
        limitationsVi = "Yêu cầu endpoint và khóa S3/R2 hợp lệ trên bucket của bạn",
        features = "Custom user-configured S3/R2 provider (${custom.region.ifBlank { "auto" }}).",
        featuresVi = "Máy chủ R2 / S3 tùy chỉnh do bạn tự cấu hình (${custom.region.ifBlank { "auto" }}).",
        isLargeFile = true,
        defaultExpire = "72h",
        expireOptions = listOf(
            ExpireOption("1 day", "1 ngày", "24h"),
            ExpireOption("2 days", "2 ngày", "48h"),
            ExpireOption("3 days (Default)", "3 ngày (Mặc định)", "72h"),
            ExpireOption("5 days", "5 ngày", "120h"),
            ExpireOption("7 days", "7 ngày", "168h"),
        ),
    )

    fun allProviders(customProviders: List<CustomS3Provider> = emptyList()): List<UploadProviderConfig> =
        customProviders.map { toConfig(it) } + ALL

    fun getConfig(id: String, customProviders: List<CustomS3Provider> = emptyList()): UploadProviderConfig =
        customProviders.firstOrNull { it.id == id }?.let { toConfig(it) }
            ?: ALL.firstOrNull { it.id == id }
            ?: ALL.first()

    fun normalizeExpire(
        providerId: String,
        currentExpire: String,
        customProviders: List<CustomS3Provider> = emptyList(),
    ): String {
        val cfg = getConfig(providerId, customProviders)
        return if (cfg.expireOptions.any { it.value == currentExpire }) currentExpire else cfg.defaultExpire
    }

    fun getProviderNameFromUrl(url: String, hasObjectKey: Boolean = false): String {
        if (url.isBlank()) return "File"
        if (hasObjectKey) return "Cloudflare R2"
        return when {
            url.contains("storage.to") -> "storage.to"
            url.contains("trystocare.com") || url.contains("sto.care") -> "sto.care"
            url.contains("x0.at") -> "x0.at"
            url.contains("pomf2.lain.la") || url.contains("pomf.lain.la") -> "pomf.lain.la"
            url.contains("qu.ax") -> "qu.ax"
            url.contains("files.catbox.moe") -> "Catbox"
            url.contains("catbox.moe") -> "Litterbox"
            url.contains("buzzheavier.com") -> "Buzzheavier"
            url.contains("fileditch") || url.contains("yescatvideos.com") -> "Fileditch"
            url.contains("gofile.io") -> "Gofile"
            url.contains("vikingfile.com") -> "VikingFile"
            url.contains("pixeldrain.com") -> "Pixeldrain"
            url.contains("tmpfile.link") -> "tfLink"
            url.contains("tmpfiles.org") -> "tmpfiles.org"
            url.contains("filebin.net") -> "Filebin"
            url.contains("tempfile.org") -> "TempFile"
            url.contains("temp.sh") -> "temp.sh"
            url.contains("uguu.se") -> "Uguu.se"
            url.contains("put.icu") -> "Put.icu"
            else -> runCatching {
                java.net.URI(url).host.orEmpty().removePrefix("www.").substringBefore(".")
                    .replaceFirstChar { it.uppercase() }
            }.getOrDefault("Host")
        }
    }
}

class FileUploadEngine(
    private val context: Context,
    private val storage: HefimerStorage,
) {
    private val client = HefimerApiConfig.uploadClient
    private val json = HefimerApiConfig.json
    private val origin = HefimerApiConfig.SITE_ORIGIN
    private var cachedGofileServer: String? = null

    fun resolvePickedFile(uri: Uri): LocalPickedFile {
        var name = "file_${System.currentTimeMillis()}"
        var size = -1L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { name = it }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        if (size < 0L) {
            size = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            }.getOrDefault(0L)
        }
        return LocalPickedFile(uri = uri, name = name, size = size.coerceAtLeast(0L), mimeType = mime)
    }

    suspend fun zipFolderTree(
        treeUri: Uri,
        onProgress: (Int) -> Unit,
        cancelToken: UploadCancellationToken,
    ): LocalPickedFile = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException(
                com.hefimer.app.ui.theme.tr(
                    "Could not open selected folder",
                    "Không thể mở thư mục đã chọn",
                ),
            )
        val folderName = (root.name?.takeIf { it.isNotBlank() } ?: "folder").removeSuffix(".zip")
        val files = mutableListOf<Pair<String, DocumentFile>>()

        fun walk(doc: DocumentFile, prefix: String) {
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            for (child in doc.listFiles()) {
                val childName = child.name ?: continue
                val rel = if (prefix.isEmpty()) childName else "$prefix/$childName"
                if (child.isDirectory) walk(child, rel) else files.add(rel to child)
            }
        }
        walk(root, "")
        if (files.isEmpty()) {
            throw IOException(com.hefimer.app.ui.theme.tr("Selected folder is empty", "Thư mục đã chọn đang trống"))
        }

        val outFile = File(context.cacheDir, "${folderName}_${System.currentTimeMillis()}.zip")
        val totalBytes = files.sumOf { it.second.length().coerceAtLeast(1L) }.coerceAtLeast(1L)
        var writtenBytes = 0L

        ZipOutputStream(FileOutputStream(outFile)).use { zos ->
            val buf = ByteArray(16 * 1024)
            for ((relPath, doc) in files) {
                if (cancelToken.isCancelled()) {
                    outFile.delete()
                    throw IOException("UPLOAD_CANCELED")
                }
                zos.putNextEntry(ZipEntry(relPath))
                context.contentResolver.openInputStream(doc.uri)?.use { input ->
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        zos.write(buf, 0, read)
                        writtenBytes += read
                        onProgress(((writtenBytes * 100) / totalBytes).toInt().coerceIn(0, 100))
                    }
                }
                zos.closeEntry()
            }
        }

        LocalPickedFile(
            uri = Uri.fromFile(outFile),
            name = "$folderName.zip",
            size = outFile.length(),
            mimeType = "application/zip",
            tempFile = outFile,
        )
    }

    private fun asciiSafeWireName(raw: String): String {
        val ext = raw.substringAfterLast('.', "")
            .filter { it.isLetterOrDigit() }
            .take(12)
        val base = raw.substringBeforeLast('.', raw)
        val decomposed = java.text.Normalizer.normalize(base, java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .replace(Regex("""[^A-Za-z0-9._-]+"""), "_")
            .trim('_', '.')
            .ifBlank { "file_${System.currentTimeMillis()}" }
            .take(80)
        return if (ext.isNotEmpty() && raw.contains('.')) "$decomposed.$ext" else decomposed
    }

    suspend fun uploadWithFallback(
        primaryProviderId: String,
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (loadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
        onStatus: (String) -> Unit,
        onFallback: (String, String) -> Unit,
    ): FallbackUploadResult = withContext(Dispatchers.IO) {
        if (picked.size <= 0L) {
            throw IOException(
                com.hefimer.app.ui.theme.tr(
                    "Cannot upload an empty 0-byte file (${picked.name})",
                    "Không thể tải lên tệp rỗng 0 byte (${picked.name})",
                ),
            )
        }
        val customProviders = storage.loadCustomS3Providers()
        val orderedIds = listOf(primaryProviderId) +
            UploadProviders.FALLBACK_PRIORITY.filter { it != primaryProviderId }
        val eligibleIds = orderedIds.filter { id ->
            picked.size <= UploadProviders.getConfig(id, customProviders).maxSizeBytes
        }
        if (eligibleIds.isEmpty()) {
            val primary = UploadProviders.getConfig(primaryProviderId, customProviders)
            throw IOException(
                com.hefimer.app.ui.theme.tr(
                    "File size exceeds limit of ${primary.maxSizeLabel} for ${primary.name}",
                    "Kích thước tệp vượt giới hạn ${primary.localizedMaxSize()} của ${primary.name}",
                ),
            )
        }

        var lastError: Throwable? = null
        for (i in eligibleIds.indices) {
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            val providerId = eligibleIds[i]
            val cfg = UploadProviders.getConfig(providerId, customProviders)
            if (i > 0) {
                val prevCfg = UploadProviders.getConfig(eligibleIds[i - 1], customProviders)
                onFallback(prevCfg.name, cfg.name)
                onProgress(0)
                onBytesProgress(0L, picked.size)
            }
            try {
                val effectiveExpire = if (i == 0) expire else cfg.defaultExpire
                val url = uploadSingle(
                    providerId = providerId,
                    picked = picked,
                    expire = effectiveExpire,
                    cancelToken = cancelToken,
                    onProgress = onProgress,
                    onBytesProgress = onBytesProgress,
                    onStatus = onStatus,
                )
                return@withContext FallbackUploadResult(
                    fileUrl = url,
                    providerId = providerId,
                    providerName = cfg.name,
                )
            } catch (e: Throwable) {
                if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") {
                    throw IOException("UPLOAD_CANCELED")
                }
                lastError = e
            }
        }
        throw lastError ?: IOException(
            com.hefimer.app.ui.theme.tr("All upload providers failed", "Tất cả máy chủ tải lên đều thất bại"),
        )
    }

    /**
     * Uploads an image/SVG to preview-friendly hosts and verifies with a fast Range GET
     * that the resulting direct/proxied URL actually serves image bytes (not 404 or HTML).
     */
    suspend fun uploadVerifiedImage(
        preferredProviderId: String,
        picked: LocalPickedFile,
        expire: String = "48h",
        cancelToken: UploadCancellationToken = UploadCancellationToken(),
    ): FallbackUploadResult = withContext(Dispatchers.IO) {
        if (picked.size <= 0L) {
            throw IOException(
                com.hefimer.app.ui.theme.tr(
                    "Cannot upload an empty 0-byte image (${picked.name})",
                    "Không thể tải lên ảnh rỗng 0 byte (${picked.name})",
                ),
            )
        }
        val customProviders = storage.loadCustomS3Providers()
        val orderedIds = buildList {
            if (preferredProviderId in UploadProviders.IMAGE_PREVIEW_PRIORITY) {
                add(preferredProviderId)
            }
            UploadProviders.IMAGE_PREVIEW_PRIORITY.forEach { if (it !in this) add(it) }
        }.filter { id -> picked.size <= UploadProviders.getConfig(id, customProviders).maxSizeBytes }

        var lastError: Throwable? = null
        for (providerId in orderedIds) {
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            val cfg = UploadProviders.getConfig(providerId, customProviders)
            try {
                val url = uploadSingle(
                    providerId = providerId,
                    picked = picked,
                    expire = expire,
                    cancelToken = cancelToken,
                    onProgress = {},
                    onBytesProgress = { _, _ -> },
                    onStatus = {},
                )
                verifyImageUrlReachable(url, picked.name)
                return@withContext FallbackUploadResult(
                    fileUrl = url,
                    providerId = providerId,
                    providerName = cfg.name,
                )
            } catch (e: Throwable) {
                if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") {
                    throw IOException("UPLOAD_CANCELED")
                }
                lastError = e
            }
        }
        throw lastError ?: IOException(
            com.hefimer.app.ui.theme.tr("Image upload failed across all hosts", "Tải ảnh thất bại trên tất cả máy chủ"),
        )
    }

    private fun verifyImageUrlReachable(rawUrl: String, fileName: String) {
        val probeUrl = DownloadHelper.resolveDirectImageUrl(rawUrl, fileName) ?: rawUrl
        val req = Request.Builder()
            .url(probeUrl)
            .header("Range", "bytes=0-511")
            .get()
            .build()
        HefimerApiConfig.apiClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful && resp.code != 206) {
                throw IOException("Image host returned HTTP ${resp.code}")
            }
            val ct = resp.header("Content-Type")?.lowercase().orEmpty()
            if (ct.contains("text/html") || ct.contains("application/xhtml")) {
                throw IOException("Image host returned an HTML page instead of image bytes")
            }
            val headBytes = resp.body?.source()?.let { src ->
                val buf = okio.Buffer()
                src.read(buf, 128L)
                buf.readUtf8()
            }.orEmpty().trimStart().lowercase()
            if (headBytes.startsWith("<!doctype html") || headBytes.startsWith("<html")) {
                throw IOException("Image host returned HTML content")
            }
        }
    }

    internal suspend fun uploadForProvider(
        providerId: String,
        picked: LocalPickedFile,
        expire: String = UploadProviders.getConfig(providerId).defaultExpire,
        cancelToken: UploadCancellationToken = UploadCancellationToken(),
    ): String = withContext(Dispatchers.IO) {
        uploadSingle(
            providerId = providerId,
            picked = picked,
            expire = expire,
            cancelToken = cancelToken,
            onProgress = {},
            onBytesProgress = { _, _ -> },
            onStatus = {},
        )
    }

    private fun uploadSingle(
        providerId: String,
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String = when (providerId) {
        "storageto" -> uploadStorageTo(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        "stocare" -> uploadStoCare(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "x0at" -> uploadX0At(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "pomf" -> uploadPomf(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "quax" -> uploadQuAx(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "catbox" -> uploadCatbox(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "buzzheavier" -> uploadBuzzheavier(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "fileditch" -> uploadFileditch(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "gofile" -> uploadGofile(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        "litterbox" -> uploadLitterbox(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        "vikingfile" -> uploadVikingFile(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "tmpfilelink" -> uploadTmpfileLink(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "tempfile" -> uploadTempFile(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        "filebin" -> uploadFilebin(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "tempsh" -> uploadTempSh(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "tmpfiles" -> uploadTmpfiles(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "uguu" -> uploadUguu(picked, cancelToken, onProgress, onBytesProgress, onStatus)
        "puticu" -> uploadPutIcu(picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
        else -> {
            val custom = storage.loadCustomS3Providers().firstOrNull { it.id == providerId }
            if (custom != null) {
                uploadCustomS3(custom, picked, expire, cancelToken, onProgress, onBytesProgress, onStatus)
            } else {
                throw IOException(
                    com.hefimer.app.ui.theme.tr(
                        "Unsupported upload provider: $providerId",
                        "Máy chủ tải lên không được hỗ trợ: $providerId",
                    ),
                )
            }
        }
    }

    suspend fun testCustomS3Connection(rawProvider: CustomS3Provider): Result<CustomS3Provider> =
        withContext(Dispatchers.IO) {
            runCatching {
                val norm = S3SigV4Signer.normalizeProvider(rawProvider)
                if (norm.bucket.isBlank()) {
                    throw IOException(com.hefimer.app.ui.theme.tr("Bucket name is required", "Vui lòng nhập tên bucket"))
                }
                if (norm.accessKeyId.isBlank() || norm.secretAccessKey.isBlank()) {
                    throw IOException(com.hefimer.app.ui.theme.tr("Access Key & Secret Key are required", "Vui lòng nhập Access Key và Secret Key"))
                }
                val probeKey = "hefimer/.hefimer-probe-${UUID.randomUUID().toString().take(8)}.txt"
                val putUrl = S3SigV4Signer.presignUrl(
                    provider = norm,
                    method = "PUT",
                    objectKey = probeKey,
                    expiresSeconds = 300L,
                )
                val probeBody = "ok".toRequestBody(null)
                val putReq = Request.Builder().url(putUrl).put(probeBody).build()
                HefimerApiConfig.apiClient.newCall(putReq).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        val s3Err = S3SigV4Signer.parseS3ErrorXml(text)
                        throw IOException(
                            s3Err ?: com.hefimer.app.ui.theme.tr(
                                "Connection failed (HTTP ${resp.code})",
                                "Kết nối thất bại (HTTP ${resp.code})",
                            ),
                        )
                    }
                }
                // Best-effort cleanup of the tiny probe file
                runCatching {
                    val delUrl = S3SigV4Signer.presignUrl(
                        provider = norm,
                        method = "DELETE",
                        objectKey = probeKey,
                        expiresSeconds = 300L,
                    )
                    HefimerApiConfig.apiClient.newCall(Request.Builder().url(delUrl).delete().build()).execute().close()
                }
                norm
            }
        }

    private fun uploadCustomS3(
        provider: CustomS3Provider,
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading to ${provider.name}...", "Đang tải lên ${provider.name}..."))
        val safeName = asciiSafeWireName(picked.name)
        val objectKey = "hefimer/${UUID.randomUUID().toString().take(12)}/$safeName"
        val putUrl = S3SigV4Signer.presignUrl(
            provider = provider,
            method = "PUT",
            objectKey = objectKey,
            expiresSeconds = 3600L,
        )
        val putReq = Request.Builder()
            .url(putUrl)
            .put(streamingBody(picked, onProgress, cancelToken, onBytesProgress, omitContentType = true))
            .build()
        execute(putReq, cancelToken)

        val getExpireSec = when (expire) {
            "24h" -> 86_400L
            "48h" -> 172_800L
            "72h" -> 259_200L
            "120h" -> 432_000L
            else -> 604_800L
        }
        return S3SigV4Signer.buildPublicUrl(provider, objectKey)
            ?: S3SigV4Signer.presignUrl(
                provider = provider,
                method = "GET",
                objectKey = objectKey,
                expiresSeconds = getExpireSec,
            )
    }

    private fun <T> withPartRetry(cancelToken: UploadCancellationToken, block: () -> T): T {
        var last: Throwable? = null
        for (attempt in 0..1) {
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            try {
                return block()
            } catch (e: Throwable) {
                if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
                last = e
                if (attempt == 0) runCatching { Thread.sleep(600L) }
            }
        }
        throw last ?: IOException("Multipart part upload failed")
    }

    private fun openStream(picked: LocalPickedFile): InputStream {
        if (picked.tempFile != null && picked.tempFile.exists()) {
            return picked.tempFile.inputStream()
        }
        return context.contentResolver.openInputStream(picked.uri)
            ?: throw IOException(com.hefimer.app.ui.theme.tr("Cannot read selected file", "Không thể đọc tệp đã chọn"))
    }

    private fun streamingBody(
        picked: LocalPickedFile,
        onProgress: (Int) -> Unit,
        cancelToken: UploadCancellationToken,
        onBytesProgress: (Long, Long) -> Unit = { _, _ -> },
        rangeStart: Long = 0L,
        rangeLength: Long = picked.size,
        overrideMediaType: MediaType? = picked.mimeType.substringBefore(';').trim().toMediaTypeOrNull(),
        omitContentType: Boolean = false,
    ): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = if (omitContentType) null else overrideMediaType
        override fun contentLength(): Long = if (rangeLength > 0L) rangeLength else -1L

        override fun writeTo(sink: BufferedSink) {
            val total = rangeLength.coerceAtLeast(1L)
            var uploaded = 0L
            var lastEmitNs = 0L
            openStream(picked).use { input ->
                val buf = ByteArray(64 * 1024)
                if (rangeStart > 0L) {
                    var skipped = 0L
                    while (skipped < rangeStart) {
                        if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
                        val remainingSkip = rangeStart - skipped
                        val s = input.skip(remainingSkip)
                        if (s > 0L) {
                            skipped += s
                        } else {
                            // InputStream.skip() can return 0 on ContentResolver pipes before EOF;
                            // read & discard explicitly until exact rangeStart is reached.
                            val discarded = input.read(buf, 0, minOf(buf.size.toLong(), remainingSkip).toInt())
                            if (discarded <= 0) break
                            skipped += discarded
                        }
                    }
                }
                var remaining = if (rangeLength > 0L) rangeLength else Long.MAX_VALUE
                while (remaining > 0L) {
                    if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
                    val toRead = minOf(buf.size.toLong(), remaining).toInt()
                    val read = input.read(buf, 0, toRead)
                    if (read <= 0) break
                    sink.write(buf, 0, read)
                    uploaded += read
                    if (rangeLength > 0L) remaining -= read
                    val now = System.nanoTime()
                    if (now - lastEmitNs >= 32_000_000L || uploaded >= total) {
                        lastEmitNs = now
                        val pct = ((uploaded * 100L) / total).toInt().coerceIn(0, 100)
                        onProgress(pct)
                        onBytesProgress(rangeStart + uploaded, picked.size.coerceAtLeast(1L))
                    }
                }
            }
            onProgress(100)
            onBytesProgress((rangeStart + uploaded).coerceAtMost(picked.size), picked.size.coerceAtLeast(1L))
        }
    }

    private fun execute(
        request: Request,
        cancelToken: UploadCancellationToken,
    ): Triple<Int, String, okhttp3.Headers> {
        if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
        val call = client.newCall(request)
        cancelToken.bindCall(call)
        try {
            call.execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val parsedMsg = runCatching {
                        val obj = json.parseToJsonElement(text).jsonObject
                        obj["error"]?.jsonPrimitive?.contentOrNull
                            ?: obj["message"]?.jsonPrimitive?.contentOrNull
                    }.getOrNull() ?: S3SigV4Signer.parseS3ErrorXml(text)
                    throw IOException(
                        parsedMsg ?: com.hefimer.app.ui.theme.tr(
                            "Upload failed with status ${resp.code}",
                            "Tải lên thất bại với mã lỗi ${resp.code}",
                        ),
                    )
                }
                return Triple(resp.code, text, resp.headers)
            }
        } finally {
            cancelToken.clearCall(call)
        }
    }

    private fun encodePathSeg(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun uploadStoCare(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        if (picked.size > 99L * 1024L * 1024L) {
            throw IOException(
                com.hefimer.app.ui.theme.tr("sto.care API limit is 100 MB", "Giới hạn API của sto.care là 100 MB"),
            )
        }
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via sto.care...", "Đang tải lên qua sto.care..."))
        val safeWireName = asciiSafeWireName(picked.name)
        val req = Request.Builder()
            .url("https://ul.sto.care/${encodePathSeg(safeWireName)}")
            .put(streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val (_, text, _) = execute(req, cancelToken)
        val obj = json.parseToJsonElement(text).jsonObject
        return obj["url"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException(
                obj["error"]?.jsonPrimitive?.contentOrNull
                    ?: com.hefimer.app.ui.theme.tr("sto.care did not return a URL", "sto.care không trả về liên kết"),
            )
    }

    private fun uploadStorageTo(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Initializing upload with storage.to...", "Đang khởi tạo với storage.to..."))
        val visitorToken = storage.getVisitorToken()
        val apiToken = storage.getStorageToApiToken()
        val jsonMedia = "application/json; charset=utf-8".toMediaTypeOrNull()
        val cleanMime = picked.mimeType.substringBefore(';').trim().ifBlank { "application/octet-stream" }
        val directBase = "https://storage.to/api"
        val proxyBase = "$origin/api/proxy/storageto"

        fun Request.Builder.withStorageToAuth(ownerToken: String? = null): Request.Builder {
            if (apiToken.isNotBlank()) {
                header("Authorization", "Bearer $apiToken")
                if (!ownerToken.isNullOrBlank()) {
                    header("X-Owner-Token", ownerToken)
                }
            } else {
                header("X-Visitor-Token", visitorToken)
                if (!ownerToken.isNullOrBlank()) {
                    header("Authorization", "Owner $ownerToken")
                }
            }
            return this
        }

        val initPayload = buildJsonObject {
            put("filename", JsonPrimitive(picked.name))
            put("content_type", JsonPrimitive(cleanMime))
            put("size", JsonPrimitive(picked.size))
        }.toString()

        var activeBase = directBase
        val initText = try {
            val directReq = Request.Builder()
                .url("$directBase/upload/init")
                .withStorageToAuth()
                .post(initPayload.toRequestBody(jsonMedia))
                .build()
            execute(directReq, cancelToken).second
        } catch (directErr: Throwable) {
            if (cancelToken.isCancelled() || directErr.message == "UPLOAD_CANCELED") throw directErr
            // Don't burn shared proxy IP quota if user's own IP already hit 429 rate/quota limit
            if (directErr.message?.contains("(429)") == true) throw directErr
            activeBase = proxyBase
            val proxyReq = Request.Builder()
                .url("$proxyBase/upload/init")
                .withStorageToAuth()
                .post(initPayload.toRequestBody(jsonMedia))
                .build()
            execute(proxyReq, cancelToken).second
        }

        val initObj = json.parseToJsonElement(initText).jsonObject
        if (initObj["success"]?.jsonPrimitive?.content != "true") {
            throw IOException(
                initObj["error"]?.jsonPrimitive?.contentOrNull
                    ?: com.hefimer.app.ui.theme.tr("storage.to init failed", "Khởi tạo storage.to thất bại"),
            )
        }

        val r2Key = initObj["r2_key"]?.jsonPrimitive?.contentOrNull.orEmpty()
        var ownerToken = initObj["owner_token"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val uploadType = initObj["type"]?.jsonPrimitive?.contentOrNull.orEmpty()

        if (uploadType == "single") {
            val uploadUrl = initObj["upload_url"]?.jsonPrimitive?.contentOrNull
                ?: throw IOException(com.hefimer.app.ui.theme.tr("Missing storage.to upload URL", "Thiếu liên kết tải lên storage.to"))
            onStatus(com.hefimer.app.ui.theme.tr("Uploading bytes to storage.to...", "Đang tải lên storage.to..."))
            val putReq = Request.Builder()
                .url(uploadUrl)
                .put(streamingBody(picked, onProgress, cancelToken, onBytesProgress, overrideMediaType = cleanMime.toMediaTypeOrNull()))
                .build()
            execute(putReq, cancelToken)
        } else if (uploadType == "multipart") {
            val uploadId = initObj["upload_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val partSize = initObj["part_size"]?.jsonPrimitive?.longOrNull ?: (10L * 1024L * 1024L)
            val totalParts = initObj["total_parts"]?.jsonPrimitive?.intOrNull ?: 1
            val partUrlMap = mutableMapOf<Int, String>()
            (initObj["initial_urls"] as? JsonObject)?.forEach { (key, elem) ->
                val num = key.toIntOrNull()
                val url = elem.jsonPrimitive.contentOrNull
                if (num != null && !url.isNullOrBlank()) {
                    partUrlMap[num] = url
                }
            }
            onStatus(com.hefimer.app.ui.theme.tr("Uploading $totalParts parts to storage.to...", "Đang tải $totalParts phần lên storage.to..."))
            val completedParts = mutableListOf<Pair<Int, String>>()

            for (partNum in 1..totalParts) {
                if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
                if (partUrlMap[partNum].isNullOrBlank()) {
                    val batchNumbers = (partNum..minOf(totalParts, partNum + 24)).filter { partUrlMap[it].isNullOrBlank() }
                    val partReqBody = buildJsonObject {
                        put("upload_id", JsonPrimitive(uploadId))
                        put("part_numbers", buildJsonArray { batchNumbers.forEach { add(JsonPrimitive(it)) } })
                    }.toString()
                    val partUrlReq = Request.Builder()
                        .url("$activeBase/upload/parts")
                        .withStorageToAuth(ownerToken)
                        .post(partReqBody.toRequestBody(jsonMedia))
                        .build()
                    val (_, partUrlText, _) = execute(partUrlReq, cancelToken)
                    val arr = json.parseToJsonElement(partUrlText).jsonObject["part_urls"]?.jsonArray
                    arr?.forEach { item ->
                        val obj = item.jsonObject
                        val num = obj["partNumber"]?.jsonPrimitive?.intOrNull
                        val url = obj["url"]?.jsonPrimitive?.contentOrNull
                        if (num != null && !url.isNullOrBlank()) {
                            partUrlMap[num] = url
                        }
                    }
                }
                val partUrl = partUrlMap[partNum]
                if (partUrl.isNullOrBlank()) {
                    throw IOException(com.hefimer.app.ui.theme.tr("Missing URL for part $partNum", "Thiếu liên kết cho phần $partNum"))
                }

                val start = (partNum - 1L) * partSize
                val len = minOf(partSize, picked.size - start).coerceAtLeast(0L)
                val (_, _, headers) = withPartRetry(cancelToken) {
                    val putPartReq = Request.Builder()
                        .url(partUrl)
                        .put(
                            streamingBody(
                                picked = picked,
                                onProgress = { pct ->
                                    val overall = (((partNum - 1.0) / totalParts) * 100.0 + pct.toDouble() / totalParts).toInt()
                                    onProgress(overall.coerceIn(0, 100))
                                },
                                cancelToken = cancelToken,
                                onBytesProgress = onBytesProgress,
                                rangeStart = start,
                                rangeLength = len,
                                omitContentType = true,
                            ),
                        )
                        .build()
                    execute(putPartReq, cancelToken)
                }
                val etag = headers["ETag"] ?: headers["etag"]
                    ?: throw IOException(com.hefimer.app.ui.theme.tr("Missing ETag for part $partNum", "Thiếu ETag cho phần $partNum"))
                completedParts.add(partNum to etag)
            }

            onStatus(com.hefimer.app.ui.theme.tr("Assembling parts...", "Đang ghép các phần..."))
            val completeBody = buildJsonObject {
                put("upload_id", JsonPrimitive(uploadId))
                put(
                    "parts",
                    JsonArray(
                        completedParts.map { (num, etag) ->
                            buildJsonObject {
                                put("partNumber", JsonPrimitive(num))
                                put("etag", JsonPrimitive(etag))
                            }
                        },
                    ),
                )
            }.toString()
            val completeReq = Request.Builder()
                .url("$activeBase/upload/complete-multipart")
                .withStorageToAuth(ownerToken)
                .post(completeBody.toRequestBody(jsonMedia))
                .build()
            execute(completeReq, cancelToken)
        } else {
            throw IOException(
                com.hefimer.app.ui.theme.tr(
                    "Unsupported storage.to upload type: $uploadType",
                    "Kiểu tải lên storage.to không được hỗ trợ: $uploadType",
                ),
            )
        }

        onStatus(com.hefimer.app.ui.theme.tr("Finalizing upload with storage.to...", "Đang hoàn tất với storage.to..."))
        val confirmBody = buildJsonObject {
            put("filename", JsonPrimitive(picked.name))
            put("size", JsonPrimitive(picked.size))
            put("content_type", JsonPrimitive(cleanMime))
            put("r2_key", JsonPrimitive(r2Key))
        }.toString()
        val confirmReq = Request.Builder()
            .url("$activeBase/upload/confirm")
            .withStorageToAuth()
            .post(confirmBody.toRequestBody(jsonMedia))
            .build()
        val (_, confirmText, _) = execute(confirmReq, cancelToken)
        val confirmObj = json.parseToJsonElement(confirmText).jsonObject
        val fileObj = confirmObj["file"]?.jsonObject
        val fileUrl = fileObj?.get("raw_url")?.jsonPrimitive?.contentOrNull
            ?: fileObj?.get("url")?.jsonPrimitive?.contentOrNull
            ?: throw IOException(
                confirmObj["error"]?.jsonPrimitive?.contentOrNull
                    ?: com.hefimer.app.ui.theme.tr("storage.to confirm failed", "Xác nhận storage.to thất bại"),
            )

        val fileId = fileObj?.get("id")?.jsonPrimitive?.contentOrNull
        ownerToken = confirmObj["owner_token"]?.jsonPrimitive?.contentOrNull ?: ownerToken
        val days = when (expire) {
            "24h" -> 1
            "48h" -> 2
            "72h" -> 3
            "120h" -> 5
            "168h" -> 7
            else -> 3
        }
        if (!fileId.isNullOrBlank() && ownerToken.isNotBlank() && days != 3) {
            runCatching {
                val expReq = Request.Builder()
                    .url("$activeBase/file/$fileId/expiry")
                    .withStorageToAuth(ownerToken)
                    .post("""{"days":$days}""".toRequestBody(jsonMedia))
                    .build()
                HefimerApiConfig.apiClient.newCall(expReq).execute().close()
            }
        }
        return fileUrl
    }

    private fun uploadGofile(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Getting Gofile server...", "Đang tìm máy chủ Gofile..."))
        val servers = mutableListOf<String>()
        runCatching {
            val req = Request.Builder().url("https://api.gofile.io/servers").get().build()
            HefimerApiConfig.apiClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val obj = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
                    val list = obj["data"]?.jsonObject?.get("servers")?.jsonArray
                    list?.forEach { elem ->
                        elem.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.let { servers.add(it) }
                    }
                }
            }
        }
        cachedGofileServer?.let { if (!servers.contains(it)) servers.add(0, it) }
        listOf("store-eu-par-3", "store1", "store3").forEach { if (!servers.contains(it)) servers.add(it) }

        onStatus(com.hefimer.app.ui.theme.tr("Uploading via Gofile.io...", "Đang tải lên qua Gofile.io..."))
        var lastErr: Throwable? = null
        for (server in servers.take(3)) {
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", picked.name, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
                    .build()
                val req = Request.Builder()
                    .url("https://$server.gofile.io/contents/uploadfile")
                    .post(body)
                    .build()
                val (_, text, _) = execute(req, cancelToken)
                val obj = json.parseToJsonElement(text).jsonObject
                if (obj["status"]?.jsonPrimitive?.contentOrNull != "ok") {
                    throw IOException(com.hefimer.app.ui.theme.tr("Gofile upload failed", "Tải lên Gofile thất bại"))
                }
                cachedGofileServer = server
                val data = obj["data"]?.jsonObject
                val code = data?.get("parentFolderCode")?.jsonPrimitive?.contentOrNull
                    ?: data?.get("id")?.jsonPrimitive?.contentOrNull
                    ?: throw IOException(com.hefimer.app.ui.theme.tr("Gofile missing folder code", "Gofile thiếu mã thư mục"))
                val guestToken = data?.get("guestToken")?.jsonPrimitive?.contentOrNull
                if (!guestToken.isNullOrBlank()) {
                    val expSec = (System.currentTimeMillis() + com.hefimer.app.data.model.parseExpireDurationMs(expire)) / 1000L
                    runCatching {
                        val expBody = buildJsonObject {
                            put("contentId", JsonPrimitive(code))
                            put("attribute", JsonPrimitive("expire"))
                            put("attributeValue", JsonPrimitive(expSec))
                        }.toString().toRequestBody("application/json".toMediaTypeOrNull())
                        val expReq = Request.Builder()
                            .url("https://api.gofile.io/contents")
                            .header("Authorization", "Bearer $guestToken")
                            .put(expBody)
                            .build()
                        HefimerApiConfig.apiClient.newCall(expReq).execute().close()
                    }
                }
                return "https://gofile.io/d/$code"
            } catch (e: Throwable) {
                if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
                lastErr = e
            }
        }
        throw lastErr ?: IOException(com.hefimer.app.ui.theme.tr("Gofile upload failed", "Tải lên Gofile thất bại"))
    }

    private fun uploadLitterbox(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via Litterbox...", "Đang tải lên qua Litterbox..."))
        val allowed = setOf("1h", "12h", "24h", "72h")
        val timeVal = if (expire in allowed) expire else "24h"
        val wireName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("time", timeVal)
            .addFormDataPart("fileToUpload", wireName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()

        val text = try {
            val req = Request.Builder()
                .url("https://litterbox.catbox.moe/resources/internals/api.php")
                .header("Accept", "*/*")
                .header("Origin", "https://litterbox.catbox.moe")
                .header("Referer", "https://litterbox.catbox.moe/")
                .post(makeBody())
                .build()
            execute(req, cancelToken).second.trim()
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED" || picked.size > 95L * 1024L * 1024L) {
                throw e
            }
            val proxyReq = Request.Builder()
                .url("$origin/api/proxy/litterbox")
                .post(makeBody())
                .build()
            execute(proxyReq, cancelToken).second.trim()
        }
        if (!text.startsWith("https://")) {
            throw IOException(text.ifBlank { com.hefimer.app.ui.theme.tr("Litterbox upload failed", "Tải lên Litterbox thất bại") })
        }
        return text
    }

    private fun uploadVikingFile(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Initializing upload with VikingFile...", "Đang khởi tạo với VikingFile..."))
        val initReq = Request.Builder()
            .url("$origin/api/proxy/vikingfile/get-upload-url")
            .post(FormBody.Builder().add("size", picked.size.toString()).build())
            .build()
        val (_, initText, _) = execute(initReq, cancelToken)
        val initObj = json.parseToJsonElement(initText).jsonObject
        val uploadId = initObj["uploadId"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val key = initObj["key"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val partSize = initObj["partSize"]?.jsonPrimitive?.longOrNull ?: picked.size
        val urls = initObj["urls"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        if (uploadId.isBlank() || key.isBlank() || urls.isEmpty()) {
            throw IOException(
                com.hefimer.app.ui.theme.tr(
                    "VikingFile returned invalid multipart metadata",
                    "VikingFile trả về siêu dữ liệu tải lên không hợp lệ",
                ),
            )
        }

        onStatus(
            if (urls.size > 1) {
                com.hefimer.app.ui.theme.tr("Uploading ${urls.size} parts to VikingFile...", "Đang tải ${urls.size} phần lên VikingFile...")
            } else {
                com.hefimer.app.ui.theme.tr("Uploading via VikingFile...", "Đang tải lên qua VikingFile...")
            }
        )
        val parts = mutableListOf<Pair<Int, String>>()
        urls.forEachIndexed { idx, partUrl ->
            if (cancelToken.isCancelled()) throw IOException("UPLOAD_CANCELED")
            val partNum = idx + 1
            val start = idx.toLong() * partSize
            val len = minOf(partSize, picked.size - start).coerceAtLeast(0L)
            val (_, _, headers) = withPartRetry(cancelToken) {
                val putReq = Request.Builder()
                    .url(partUrl)
                    .put(
                        streamingBody(
                            picked = picked,
                            onProgress = { pct ->
                                val overall = (((idx.toDouble()) / urls.size) * 100.0 + pct.toDouble() / urls.size).toInt()
                                onProgress(overall.coerceIn(0, 100))
                            },
                            cancelToken = cancelToken,
                            onBytesProgress = onBytesProgress,
                            rangeStart = start,
                            rangeLength = len,
                            omitContentType = true,
                        ),
                    )
                    .build()
                execute(putReq, cancelToken)
            }
            val etag = (headers["ETag"] ?: headers["etag"])?.replace("\"", "")
                ?: throw IOException(
                    com.hefimer.app.ui.theme.tr(
                        "VikingFile part $partNum missing ETag",
                        "Phần $partNum của VikingFile thiếu ETag",
                    ),
                )
            parts.add(partNum to etag)
        }

        onStatus(com.hefimer.app.ui.theme.tr("Finalizing VikingFile upload...", "Đang hoàn tất tải lên VikingFile..."))
        val form = FormBody.Builder()
            .add("key", key)
            .add("uploadId", uploadId)
            .add("name", picked.name)
            .add("user", "")
        parts.forEachIndexed { idx, (num, etag) ->
            form.add("parts[$idx][PartNumber]", num.toString())
            form.add("parts[$idx][ETag]", etag)
        }
        val completeReq = Request.Builder()
            .url("$origin/api/proxy/vikingfile/complete-upload")
            .post(form.build())
            .build()
        val (_, completeText, _) = execute(completeReq, cancelToken)
        val obj = json.parseToJsonElement(completeText).jsonObject
        return obj["url"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException(
                obj["error"]?.jsonPrimitive?.contentOrNull
                    ?: com.hefimer.app.ui.theme.tr("VikingFile complete failed", "Hoàn tất VikingFile thất bại"),
            )
    }

    private fun uploadTmpfileLink(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via tfLink...", "Đang tải lên qua tfLink..."))
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val text = try {
            execute(Request.Builder().url("https://tmpfile.link/api/upload").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/tmpfilelink").post(makeBody()).build(), cancelToken).second
        }
        val obj = json.parseToJsonElement(text).jsonObject
        return obj["downloadLink"]?.jsonPrimitive?.contentOrNull
            ?: obj["downloadLinkEncoded"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException(com.hefimer.app.ui.theme.tr("tfLink upload failed", "Tải lên tfLink thất bại"))
    }

    private fun uploadTempFile(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via TempFile...", "Đang tải lên qua TempFile..."))
        val hours = Regex("""\d+""").find(expire)?.value?.toIntOrNull() ?: 24
        val validHours = if (hours in listOf(1, 6, 24, 48)) hours else 24
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("files", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .addFormDataPart("expiryHours", validHours.toString())
            .build()

        val text = try {
            execute(Request.Builder().url("https://tempfile.org/api/upload/local").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/tempfile").post(makeBody()).build(), cancelToken).second
        }
        val obj = json.parseToJsonElement(text).jsonObject
        val url = obj["files"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        if (obj["success"]?.jsonPrimitive?.content != "true" || url.isNullOrBlank()) {
            throw IOException(
                obj["message"]?.jsonPrimitive?.contentOrNull
                    ?: com.hefimer.app.ui.theme.tr("TempFile upload failed", "Tải lên TempFile thất bại"),
            )
        }
        return "${url}download"
    }

    private fun uploadFilebin(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via Filebin...", "Đang tải lên qua Filebin..."))
        val bin = "hefimer-${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val safeWireName = asciiSafeWireName(picked.name)
        val encodedName = encodePathSeg(safeWireName)
        val text = try {
            val directReq = Request.Builder()
                .url("https://filebin.net/$bin/$encodedName")
                .header("Accept", "application/json")
                .post(streamingBody(picked, onProgress, cancelToken, onBytesProgress))
                .build()
            execute(directReq, cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            val proxyReq = Request.Builder()
                .url("$origin/api/proxy/filebin/$bin/$encodedName")
                .post(streamingBody(picked, onProgress, cancelToken, onBytesProgress))
                .build()
            execute(proxyReq, cancelToken).second
        }
        val returnedName = runCatching {
            json.parseToJsonElement(text).jsonObject["file"]?.jsonObject?.get("filename")?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: safeWireName
        return "https://filebin.net/$bin/${encodePathSeg(returnedName)}"
    }

    private fun uploadTempSh(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via temp.sh...", "Đang tải lên qua temp.sh..."))
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val text = try {
            execute(Request.Builder().url("https://temp.sh/upload").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/tempsh").post(makeBody()).build(), cancelToken).second
        }
        val trimmed = text.trim()
        if (!trimmed.startsWith("https://temp.sh/")) {
            throw IOException(com.hefimer.app.ui.theme.tr("temp.sh upload failed", "Tải lên temp.sh thất bại"))
        }
        return trimmed
    }

    private fun uploadTmpfiles(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via tmpfiles.org...", "Đang tải lên qua tmpfiles.org..."))
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val text = try {
            execute(Request.Builder().url("https://tmpfiles.org/api/v1/upload").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/tmpfiles").post(makeBody()).build(), cancelToken).second
        }
        val obj = json.parseToJsonElement(text).jsonObject
        val url = obj["data"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        if (obj["status"]?.jsonPrimitive?.contentOrNull == "success" && !url.isNullOrBlank()) return url
        throw IOException(
            obj["error"]?.jsonPrimitive?.contentOrNull
                ?: com.hefimer.app.ui.theme.tr("tmpfiles upload failed", "Tải lên tmpfiles thất bại"),
        )
    }

    private fun uploadUguu(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via Uguu.se...", "Đang tải lên qua Uguu.se..."))
        val safeName = asciiSafeWireName(picked.name)
        fun makeBody() = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("files[]", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val text = try {
            execute(Request.Builder().url("https://uguu.se/upload").post(makeBody()).build(), cancelToken).second
        } catch (e: Throwable) {
            if (cancelToken.isCancelled() || e.message == "UPLOAD_CANCELED") throw e
            execute(Request.Builder().url("$origin/api/proxy/uguu").post(makeBody()).build(), cancelToken).second
        }
        val obj = json.parseToJsonElement(text).jsonObject
        val url = obj["files"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        if (obj["success"]?.jsonPrimitive?.content == "true" && !url.isNullOrBlank()) return url
        throw IOException(com.hefimer.app.ui.theme.tr("Uguu.se upload failed", "Tải lên Uguu.se thất bại"))
    }

    private fun uploadPutIcu(
        picked: LocalPickedFile,
        expire: String,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via Put.icu...", "Đang tải lên qua Put.icu..."))
        val sec = (com.hefimer.app.data.model.parseExpireDurationMs(expire) / 1000L).coerceIn(60L, 86400L)
        val req = Request.Builder()
            .url("$origin/api/proxy/puticu/${encodePathSeg(asciiSafeWireName(picked.name))}")
            .header("Accept", "application/json")
            .header("Linx-Expiry", sec.toString())
            .put(streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val (_, text, _) = execute(req, cancelToken)
        val obj = json.parseToJsonElement(text).jsonObject
        return obj["direct_url"]?.jsonPrimitive?.contentOrNull
            ?: obj["url"]?.jsonPrimitive?.contentOrNull
            ?: throw IOException(com.hefimer.app.ui.theme.tr("Put.icu upload failed", "Tải lên Put.icu thất bại"))
    }

    private fun uploadX0At(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via x0.at...", "Đang tải lên qua x0.at..."))
        val safeName = asciiSafeWireName(picked.name)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val req = Request.Builder().url("https://x0.at").post(body).build()
        val url = execute(req, cancelToken).second.trim()
        if (!url.startsWith("https://")) {
            throw IOException(url.ifBlank { com.hefimer.app.ui.theme.tr("x0.at upload failed", "Tải lên x0.at thất bại") })
        }
        return url
    }

    private fun uploadPomf(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via pomf.lain.la...", "Đang tải lên qua pomf.lain.la..."))
        val safeName = asciiSafeWireName(picked.name)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("files[]", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val req = Request.Builder().url("https://pomf.lain.la/upload.php").post(body).build()
        val text = execute(req, cancelToken).second
        val obj = json.parseToJsonElement(text).jsonObject
        val fileUrl = obj["files"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        if (obj["success"]?.jsonPrimitive?.content == "true" && !fileUrl.isNullOrBlank()) {
            return fileUrl
        }
        throw IOException(
            obj["description"]?.jsonPrimitive?.contentOrNull
                ?: com.hefimer.app.ui.theme.tr("pomf.lain.la upload failed", "Tải lên pomf.lain.la thất bại"),
        )
    }

    private fun uploadQuAx(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via qu.ax...", "Đang tải lên qua qu.ax..."))
        val safeName = asciiSafeWireName(picked.name)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("files[]", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val req = Request.Builder().url("https://qu.ax/upload.php").post(body).build()
        val text = execute(req, cancelToken).second
        val obj = json.parseToJsonElement(text).jsonObject
        val fileUrl = obj["files"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        if (obj["success"]?.jsonPrimitive?.content == "true" && !fileUrl.isNullOrBlank()) {
            return fileUrl
        }
        throw IOException(com.hefimer.app.ui.theme.tr("qu.ax upload failed", "Tải lên qu.ax thất bại"))
    }

    private fun uploadCatbox(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via Catbox...", "Đang tải lên qua Catbox..."))
        val safeName = asciiSafeWireName(picked.name)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("fileToUpload", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val req = Request.Builder()
            .url("https://catbox.moe/user/api.php")
            .header("Accept", "*/*")
            .post(body)
            .build()
        val url = execute(req, cancelToken).second.trim()
        if (!url.startsWith("https://")) {
            throw IOException(url.ifBlank { com.hefimer.app.ui.theme.tr("Catbox upload failed", "Tải lên Catbox thất bại") })
        }
        return url
    }

    private fun uploadBuzzheavier(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via Buzzheavier...", "Đang tải lên qua Buzzheavier..."))
        val safeWireName = asciiSafeWireName(picked.name)
        val req = Request.Builder()
            .url("https://w.buzzheavier.com/${encodePathSeg(safeWireName)}")
            .put(streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val text = execute(req, cancelToken).second
        val obj = json.parseToJsonElement(text).jsonObject
        val id = obj["data"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
        if (!id.isNullOrBlank()) {
            return "https://buzzheavier.com/$id"
        }
        throw IOException(com.hefimer.app.ui.theme.tr("Buzzheavier upload failed", "Tải lên Buzzheavier thất bại"))
    }

    private fun uploadFileditch(
        picked: LocalPickedFile,
        cancelToken: UploadCancellationToken,
        onProgress: (Int) -> Unit,
        onBytesProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit,
    ): String {
        onStatus(com.hefimer.app.ui.theme.tr("Uploading via Fileditch...", "Đang tải lên qua Fileditch..."))
        val safeName = asciiSafeWireName(picked.name)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", safeName, streamingBody(picked, onProgress, cancelToken, onBytesProgress))
            .build()
        val req = Request.Builder()
            .url("https://new.fileditch.com/upload.php")
            .post(body)
            .build()
        val text = execute(req, cancelToken).second
        val obj = json.parseToJsonElement(text).jsonObject
        val fileUrl = obj["url"]?.jsonPrimitive?.contentOrNull
        if (obj["success"]?.jsonPrimitive?.content == "true" && !fileUrl.isNullOrBlank()) {
            return fileUrl
        }
        throw IOException(
            obj["message"]?.jsonPrimitive?.contentOrNull
                ?: com.hefimer.app.ui.theme.tr("Fileditch upload failed", "Tải lên Fileditch thất bại"),
        )
    }
}
