package com.hefimer.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.Question
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.X
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import com.hefimer.app.ui.theme.AppMotion
import com.hefimer.app.ui.theme.AppleGlassVariant
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.appleGlassSurface
import com.hefimer.app.ui.theme.applePressable
import com.hefimer.app.ui.theme.tr

enum class HefimerInfoDoc {
    FAQ,
    PRIVACY,
    TERMS,
}

private data class BilingualSection(
    val titleEn: String,
    val titleVi: String,
    val bodyEn: String,
    val bodyVi: String,
)

private val FAQ_ITEMS = listOf(
    BilingualSection(
        titleEn = "How do I send and receive something?",
        titleVi = "Làm sao để gửi và nhận tệp hoặc văn bản?",
        bodyEn = "Choose Send File or Send Text, set your expiry or optional password, then share the 5-digit code or QR code Hefimer creates. The recipient enters that code under Receive (or scans the QR) to open or download it.",
        bodyVi = "Chọn Gửi tệp hoặc Gửi chữ, tùy chỉnh thời hạn hoặc mật khẩu nếu cần, rồi chia sẻ mã 5 số hoặc mã QR. Người nhận chỉ cần nhập mã ở tab Nhận (hoặc quét QR) để mở hoặc tải xuống.",
    ),
    BilingualSection(
        titleEn = "When does shared content disappear?",
        titleVi = "Khi nào nội dung chia sẻ sẽ tự động biến mất?",
        bodyEn = "The 5-digit drop expires on the timer selected before sending (30s to 48h, or 1–7 days on storage.to). Hosted file bytes also follow the retention window of the chosen upload provider.",
        bodyVi = "Mã 5 số sẽ hết hạn theo đồng hồ bạn chọn trước khi gửi (từ 30 giây đến 48 giờ, hoặc 1–7 ngày trên storage.to). Tệp đính kèm cũng tuân theo thời hạn lưu trữ của máy chủ tải lên đã chọn.",
    ),
    BilingualSection(
        titleEn = "What do One-Time mode and passwords do?",
        titleVi = "Chế độ Xem 1 lần và Mật khẩu hoạt động thế nào?",
        bodyEn = "One-Time mode automatically deletes the drop immediately after its first successful retrieval. An optional password requires recipients to enter the matching passcode before Hefimer reveals any text or download link.",
        bodyVi = "Chế độ Một lần tự động xóa dữ liệu ngay sau lượt mở đầu tiên. Mật khẩu tùy chọn yêu cầu người nhận nhập đúng mã bảo vệ trước khi Hefimer hiển thị nội dung hoặc liên kết tải.",
    ),
    BilingualSection(
        titleEn = "How do Space, Chat, Board, and Hefimer Link work?",
        titleVi = "Space, Phòng chat, Bảng vẽ và Hefimer Link dùng để làm gì?",
        bodyEn = "Space gives you a shared real-time canvas for files and notes via a 5-digit code. Rooms provide ephemeral Chat and collaborative Whiteboards. Hefimer Link lets you pair your phone and browser via QR/code for 1-tap transfers.",
        bodyVi = "Space là không gian thả tệp và ghi chú theo thời gian thực qua mã 5 số. Phòng gồm Chat tạm thời và Bảng vẽ cộng tác. Hefimer Link giúp ghép đôi điện thoại với trình duyệt qua QR/mã để đẩy tệp 1 chạm.",
    ),
    BilingualSection(
        titleEn = "Do I need a Hefimer account?",
        titleVi = "Tôi có cần tạo tài khoản Hefimer không?",
        bodyEn = "No. Sending, receiving, Spaces, Chat, Boards, and device pairing require zero login, email, or account password. Adding a personal storage.to API token in Settings is completely optional for higher daily upload quotas.",
        bodyVi = "Không. Gửi, nhận, Space, Chat, Bảng vẽ và ghép nối thiết bị đều không cần đăng nhập hay email. Việc nhập API token storage.to trong Cài đặt là hoàn toàn tùy chọn nếu bạn cần gửi vượt 50 tệp/ngày.",
    ),
    BilingualSection(
        titleEn = "What should I know about privacy & security?",
        titleVi = "Tôi cần biết gì về quyền riêng tư và bảo mật?",
        bodyEn = "Hefimer uses temporary 5-digit codes and automatic expiration instead of permanent cloud libraries. Keep your 5-digit codes and Space/Room IDs private—only share them with intended recipients, and add a password for sensitive items.",
        bodyVi = "Hefimer dùng mã 5 số tạm thời và cơ chế tự hủy thay vì lưu kho vĩnh viễn. Hãy giữ kín mã 5 số, mã Space/Phòng và đặt thêm mật khẩu nếu chia sẻ nội dung quan trọng.",
    ),
)

private val PRIVACY_SECTIONS = listOf(
    BilingualSection(
        titleEn = "1. Overview",
        titleVi = "1. Tổng quan",
        bodyEn = "Hefimer is a fast file sharing, text sharing, and real-time collaboration tool built around temporary 5-digit codes. We are committed to minimizing data collection and protecting your privacy.",
        bodyVi = "Hefimer là công cụ chia sẻ tệp, văn bản và cộng tác thời gian thực thông qua mã 5 chữ số tạm thời. Chúng tôi cam kết thu thập tối thiểu dữ liệu và bảo vệ quyền riêng tư của bạn.",
    ),
    BilingualSection(
        titleEn = "2. Data We Collect",
        titleVi = "2. Dữ liệu chúng tôi xử lý",
        bodyEn = "• No Account Required: You do not need to register, provide an email, or create a Hefimer password.\n• Shared Content: Text snippets, drop metadata, Spaces, Chats, and Boards are stored temporarily in Firebase Realtime Database. Uploaded file bytes are hosted on the provider you select (e.g., storage.to, sto.care, Catbox, etc.).\n• Local Device Preferences: Settings, recent history, and optional API tokens stay strictly on your device (excluded from cloud backup).",
        bodyVi = "• Không cần tài khoản: Không cần đăng ký, email hay mật khẩu Hefimer.\n• Nội dung chia sẻ: Văn bản, thông tin phiên gửi, Space, Chat và Bảng vẽ được lưu tạm trên Firebase Realtime Database. Dữ liệu tệp được lưu tại nhà cung cấp bạn chọn (storage.to, sto.care, Catbox, v.v.).\n• Dữ liệu cục bộ: Cài đặt, lịch sử gần đây và API token tùy chọn chỉ lưu trên máy bạn (đã tắt sao lưu đám mây).",
    ),
    BilingualSection(
        titleEn = "3. How We Use Data",
        titleVi = "3. Mục đích sử dụng",
        bodyEn = "• Route files and text temporarily between your devices or recipients.\n• Synchronize real-time Spaces, Chat rooms, Whiteboards, and Hefimer Link transfers.\n• Maintain service reliability and abuse prevention.",
        bodyVi = "• Trung chuyển tệp và văn bản tạm thời giữa các thiết bị.\n• Đồng bộ thời gian thực cho Space, Phòng chat, Bảng vẽ và Hefimer Link.\n• Duy trì độ ổn định hệ thống và chống lạm dụng.",
    ),
    BilingualSection(
        titleEn = "4. Storage & Auto-Deletion",
        titleVi = "4. Lưu trữ & Tự động xóa",
        bodyEn = "• Every drop expires automatically based on the timer you pick.\n• One-Time mode deletes the drop metadata right after the first retrieval.\n• Chat rooms, Boards, and Spaces also expire automatically after their configured lifetime.",
        bodyVi = "• Mọi phiên gửi đều tự động hết hạn theo thời gian bạn chọn.\n• Chế độ Một lần xóa bản ghi ngay sau lượt nhận đầu tiên.\n• Phòng chat, Bảng vẽ và Space cũng tự động hết hạn theo thời lượng thiết lập.",
    ),
    BilingualSection(
        titleEn = "5. Third-Party Providers",
        titleVi = "5. Dịch vụ bên thứ ba",
        bodyEn = "• Firebase Realtime Database (Google): Stores temporary drop metadata, Spaces, Chat messages, and Board paths (with root listing and sensitive nodes locked down).\n• File Storage Providers: Uploaded files are transmitted directly to the selected provider (such as storage.to / Cloudflare R2, sto.care, x0.at, etc.) and are subject to that provider's retention and privacy policies.",
        bodyVi = "• Firebase Realtime Database (Google): Lưu thông tin tạm của mã gửi, Space, tin nhắn Chat và nét vẽ (đã khóa đọc danh sách toàn cục).\n• Nhà cung cấp lưu trữ tệp: Tệp được tải lên nhà cung cấp bạn chọn (storage.to / Cloudflare R2, sto.care, x0.at, v.v.) và tuân theo chính sách lưu trữ của họ.",
    ),
    BilingualSection(
        titleEn = "6. Security Practices",
        titleVi = "6. Thực hành bảo mật",
        bodyEn = "• All network traffic enforces HTTPS/TLS (cleartext traffic is disabled).\n• You can attach a passcode to sensitive drops, Spaces, or Rooms.\n• Keep your 5-digit codes private—anyone who knows an active code can open that drop.",
        bodyVi = "• Toàn bộ kết nối mạng bắt buộc dùng HTTPS/TLS (đã chặn lưu lượng không mã hóa).\n• Bạn có thể đặt mật khẩu cho tệp/đoạn mã, Space hoặc Phòng.\n• Hãy bảo mật mã 5 số của bạn—bất kỳ ai biết mã đang hoạt động đều có thể mở nội dung đó.",
    ),
    BilingualSection(
        titleEn = "7. Contact",
        titleVi = "7. Liên hệ",
        bodyEn = "If you have privacy or security questions, reach out at contact@hefimer.com.",
        bodyVi = "Nếu có thắc mắc về quyền riêng tư hoặc bảo mật, vui lòng liên hệ contact@hefimer.com.",
    ),
)

private val TERMS_SECTIONS = listOf(
    BilingualSection(
        titleEn = "1. Acceptance of Terms",
        titleVi = "1. Chấp nhận điều khoản",
        bodyEn = "By accessing or using Hefimer (web or Android app), you agree to comply with these Terms of Service. If you do not agree, please discontinue use of the service.",
        bodyVi = "Khi truy cập hoặc sử dụng Hefimer (bản web hoặc ứng dụng Android), bạn đồng ý tuân thủ các Điều khoản dịch vụ này. Nếu không đồng ý, vui lòng ngừng sử dụng.",
    ),
    BilingualSection(
        titleEn = "2. Intended Purpose",
        titleVi = "2. Mục đích sử dụng",
        bodyEn = "Hefimer is built for fast personal, educational, and lawful file/text sharing and collaboration. You may not use it to distribute unlawful material.",
        bodyVi = "Hefimer được thiết kế để chia sẻ tệp, văn bản và cộng tác hợp pháp cho nhu cầu cá nhân, học tập và công việc.",
    ),
    BilingualSection(
        titleEn = "3. Prohibited Activities",
        titleVi = "3. Hành vi bị nghiêm cấm",
        bodyEn = "• Sharing illegal, obscene, violent, or harmful content.\n• Distributing malware, spyware, or malicious code.\n• Spamming, abusing quotas, or launching denial-of-service attacks.\n• Infringing third-party intellectual property rights.",
        bodyVi = "• Chia sẻ nội dung vi phạm pháp luật, đồi trụy, bạo lực hoặc gây hại.\n• Phát tán mã độc, virus hoặc phần mềm gián điệp.\n• Spam, lạm dụng hạn mức hoặc tấn công từ chối dịch vụ.\n• Xâm phạm bản quyền, sở hữu trí tuệ của bên thứ ba.",
    ),
    BilingualSection(
        titleEn = "4. User Responsibility",
        titleVi = "4. Trách nhiệm người dùng",
        bodyEn = "• You are solely responsible for the content you upload or share.\n• You are responsible for keeping your 5-digit codes, Room IDs, and passcodes secure.\n• Always keep local backups of important files—Hefimer is an ephemeral transfer tool, not permanent cloud storage.",
        bodyVi = "• Bạn hoàn toàn chịu trách nhiệm về nội dung mình tải lên và chia sẻ.\n• Bạn tự chịu trách nhiệm bảo mật mã 5 số, mã phòng và mật khẩu.\n• Luôn giữ bản sao lưu trên máy với dữ liệu quan trọng—Hefimer là công cụ truyền tệp tạm thời, không phải kho lưu trữ vĩnh viễn.",
    ),
    BilingualSection(
        titleEn = "5. Third-Party Hosts",
        titleVi = "5. Máy chủ lưu trữ bên thứ ba",
        bodyEn = "File bytes are stored on external providers you select (storage.to, sto.care, Catbox, etc.). Your uploads must also comply with the chosen provider's terms of service.",
        bodyVi = "Tệp tin được lưu tại nhà cung cấp bên ngoài bạn chọn (storage.to, sto.care, Catbox, v.v.). Việc tải lên cũng phải tuân thủ điều khoản của nhà cung cấp đó.",
    ),
    BilingualSection(
        titleEn = "6. Service Availability",
        titleVi = "6. Tính khả dụng của dịch vụ",
        bodyEn = "We strive for high reliability and automatic multi-provider fallback, but do not guarantee uninterrupted 24/7 uptime and may modify or suspend features at any time.",
        bodyVi = "Chúng tôi tối ưu tính ổn định với cơ chế dự phòng đa máy chủ, nhưng không cam kết hoạt động liên tục 24/7 và có thể điều chỉnh tính năng khi cần thiết.",
    ),
    BilingualSection(
        titleEn = "7. Limitation of Liability",
        titleVi = "7. Giới hạn trách nhiệm",
        bodyEn = "The service is provided \"as-is\" without warranties of any kind. Hefimer is not liable for any direct, indirect, or incidental data loss arising from use of the service.",
        bodyVi = "Dịch vụ được cung cấp theo hiện trạng (\"as-is\"). Hefimer không chịu trách nhiệm đối với bất kỳ tổn thất dữ liệu trực tiếp hoặc gián tiếp nào phát sinh khi sử dụng.",
    ),
    BilingualSection(
        titleEn = "8. Updates to Terms",
        titleVi = "8. Thay đổi điều khoản",
        bodyEn = "We may update these Terms at any time. Continued use after updates constitutes acceptance of the revised Terms.",
        bodyVi = "Chúng tôi có thể cập nhật Điều khoản bất kỳ lúc nào. Việc tiếp tục sử dụng đồng nghĩa với việc bạn chấp nhận bản cập nhật mới.",
    ),
)

@Composable
fun HefimerInfoDocSheetContent(
    doc: HefimerInfoDoc,
    onClose: () -> Unit,
) {
    val icon: ImageVector = when (doc) {
        HefimerInfoDoc.FAQ -> PhosphorIcons.Regular.Question
        HefimerInfoDoc.PRIVACY -> PhosphorIcons.Regular.ShieldCheck
        HefimerInfoDoc.TERMS -> PhosphorIcons.Regular.FileText
    }
    val kicker = when (doc) {
        HefimerInfoDoc.FAQ -> tr("HELP & GUIDE", "HỖ TRỢ & HƯỚNG DẪN")
        HefimerInfoDoc.PRIVACY -> tr("SECURITY & PRIVACY", "BẢO MẬT & RIÊNG TƯ")
        HefimerInfoDoc.TERMS -> tr("LEGAL", "PHÁP LÝ")
    }
    val title = when (doc) {
        HefimerInfoDoc.FAQ -> tr("Frequently Asked Questions", "Câu hỏi thường gặp (FAQ)")
        HefimerInfoDoc.PRIVACY -> tr("Privacy & Security Policy", "Chính sách Bảo mật & Riêng tư")
        HefimerInfoDoc.TERMS -> tr("Terms of Service", "Điều khoản Dịch vụ")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.84f)
            .appleGlassSurface(
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                variant = AppleGlassVariant.Elevated,
                elevation = 24.dp,
            )
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(HefimerColors.White20),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .appleGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    ),
            ) {
                Icon(icon, null, tint = HefimerColors.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = kicker,
                    color = HefimerColors.White45,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                Text(
                    text = title,
                    color = HefimerColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .appleGlassSurface(
                        shape = CircleShape,
                        variant = AppleGlassVariant.Clear,
                        elevation = 0.dp,
                    )
                    .applePressable(pressedScale = 0.89f, onClick = onClose),
            ) {
                Icon(PhosphorIcons.Regular.X, tr("Close", "Đóng"), tint = HefimerColors.White70, modifier = Modifier.size(16.dp))
            }
        }

        Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (doc == HefimerInfoDoc.FAQ) {
                var expandedIndex by remember { mutableIntStateOf(0) }
                FAQ_ITEMS.forEachIndexed { idx, item ->
                    val expanded = expandedIndex == idx
                    val rot by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = AppMotion.snappySpring(),
                        label = "faq_rot_$idx",
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(22.dp),
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .applePressable(pressedScale = 0.985f) {
                                expandedIndex = if (expanded) -1 else idx
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "0${idx + 1}",
                                color = HefimerColors.White40,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = tr(item.titleEn, item.titleVi),
                                color = HefimerColors.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                PhosphorIcons.Regular.CaretDown,
                                null,
                                tint = HefimerColors.White60,
                                modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rot },
                            )
                        }
                        AnimatedVisibility(
                            visible = expanded,
                            enter = AppMotion.softExpandEnter,
                            exit = AppMotion.softExpandExit,
                        ) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.fillMaxWidth().height(0.5.dp).background(HefimerColors.White10))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = tr(item.bodyEn, item.bodyVi),
                                    color = HefimerColors.White70,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = tr("Last Updated: November 2026", "Cập nhật gần nhất: Tháng 11/2026"),
                    color = HefimerColors.White45,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                val sections = if (doc == HefimerInfoDoc.PRIVACY) PRIVACY_SECTIONS else TERMS_SECTIONS
                sections.forEach { sec ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassSurface(
                                shape = RoundedCornerShape(22.dp),
                                variant = AppleGlassVariant.Clear,
                                elevation = 0.dp,
                            )
                            .padding(14.dp),
                    ) {
                        Text(
                            text = tr(sec.titleEn, sec.titleVi),
                            color = HefimerColors.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = tr(sec.bodyEn, sec.bodyVi),
                            color = HefimerColors.White70,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
