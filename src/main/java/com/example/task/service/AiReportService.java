package com.example.task.service;

import com.example.task.config.AiConfig;
import com.example.task.dto.WeeklyStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiReportService {

    private static final Logger log = LoggerFactory.getLogger(AiReportService.class);

    @Autowired
    private AiConfig aiConfig;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            你是 TaskFlow 的周报助手。根据用户过去一周的任务数据生成周报。

            严格按照以下格式，直接输出正文，不要输出任何开头语或结尾语：

            ## 📊 本周概览
            用简洁的表格或列表总结：总任务数、已完成数、完成率、逾期数、习惯打卡天数、专注时长。
            数据来自用户提供的信息，不要编造没有的数据。

            ## ✅ 亮点
            列出 2-3 个做得好的方面，结合具体数据和任务名称，有数据支撑。

            ## 🔍 待改进
            列出 1-2 个可以提升的方面，给出具体原因，语气温和不要指责。

            ## 💡 下周建议
            给出 2-3 条具体可执行的行动建议，每条建议前用数字编号。

            ## 🌟 小结
            一句鼓励的话，要结合本周实际表现，不要泛泛而谈的鸡汤。

            规则：
            - 输出纯文本，不要使用 Markdown 的 ** 加粗、` 代码、_ 斜体等标记
            - 不要输出任何多余的开场白、结尾语、署名、生成时间
            - 语气像朋友聊天，自然平实，不要AI腔
            - 没有数据的维度直接跳过，不要硬写
            - 不要超过500字
            """;

    /**
     * Generate a weekly report using DeepSeek API (OpenAI-compatible format).
     * Falls back to a plain-text summary if API is unavailable.
     */
    public String generateReport(WeeklyStats stats) {
        String apiKey = aiConfig.getKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek API key not configured, using fallback report");
            return generateFallbackReport(stats);
        }

        try {
            String userMessage = buildStatsMessage(stats);

            // OpenAI-compatible request body
            Map<String, Object> requestBody = Map.of(
                    "model", aiConfig.getModel(),
                    "max_tokens", aiConfig.getMaxTokens(),
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userMessage)
                    )
            );

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiConfig.getUrl()))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                var choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    var message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        Object text = message.get("content");
                        if (text != null) {
                            return text.toString();
                        }
                    }
                }
                log.warn("Unexpected DeepSeek API response structure, using fallback");
            } else {
                log.error("DeepSeek API returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to call DeepSeek API: {}", e.getMessage());
        }

        return generateFallbackReport(stats);
    }

    /**
     * Build a structured message with the user's weekly statistics for the AI.
     */
    private String buildStatsMessage(WeeklyStats s) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是用户过去一周（").append(s.weekStart()).append(" 至 ").append(s.weekEnd()).append("）的任务数据：\n\n");

        sb.append("📊 任务完成情况\n");
        sb.append("- 总任务数：").append(s.totalTasks()).append("\n");
        sb.append("- 已完成：").append(s.completedTasks()).append("\n");
        sb.append(String.format("- 完成率：%.1f%%\n", s.completionRate() * 100));
        sb.append("- 逾期未完成：").append(s.overdueTasks()).append("\n\n");

        if (s.habitTasks() > 0) {
            sb.append("🔄 习惯打卡\n");
            sb.append("- 习惯任务数：").append(s.habitTasks()).append("\n");
            sb.append("- 打卡天数：").append(s.habitCheckInDays()).append("/7\n");
            sb.append(String.format("- 坚持率：%.1f%%\n", s.habitRate() * 100)).append("\n\n");
        }

        if (s.totalFocusMinutes() > 0) {
            long hours = s.totalFocusMinutes() / 60;
            long mins = s.totalFocusMinutes() % 60;
            sb.append("⏱ 专注时长\n");
            sb.append("- 本周总计：").append(hours).append(" 小时 ").append(mins).append(" 分钟\n\n");
        }

        if (s.categoryBreakdown() != null && !s.categoryBreakdown().isEmpty()) {
            sb.append("📂 分类统计\n");
            s.categoryBreakdown().forEach((cat, count) ->
                    sb.append("- ").append(cat).append("：").append(count).append(" 个任务\n"));
            sb.append("\n");
        }

        if (s.topCompleted() != null && !s.topCompleted().isEmpty()) {
            sb.append("🏆 本周完成的任务\n");
            s.topCompleted().forEach(t -> sb.append("- ").append(t).append("\n"));
        }

        sb.append("\n请根据以上数据生成周报。");
        return sb.toString();
    }

    /**
     * Generate a simple text-based report when AI is unavailable.
     */
    private String generateFallbackReport(WeeklyStats s) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 📊 本周概览\n\n");

        if (s.totalTasks() == 0) {
            sb.append("本周没有任务记录。从第一个任务开始，慢慢来。\n");
            return sb.toString();
        }

        sb.append("总任务 ").append(s.totalTasks()).append(" 个，已完成 ").append(s.completedTasks()).append(" 个");
        sb.append("，完成率 ").append(String.format("%.0f%%", s.completionRate() * 100));
        if (s.overdueTasks() > 0) {
            sb.append("，").append(s.overdueTasks()).append(" 个逾期未完成");
        }
        sb.append("\n");

        if (s.totalFocusMinutes() > 0) {
            long hours = s.totalFocusMinutes() / 60;
            long mins = s.totalFocusMinutes() % 60;
            sb.append("专注时长 ").append(hours).append(" 小时 ").append(mins).append(" 分钟\n");
        }

        if (s.habitTasks() > 0) {
            sb.append("习惯打卡 ").append(s.habitCheckInDays()).append(" 天 / 7 天\n");
        }

        sb.append("\n## ✅ 亮点\n\n");
        if (s.completionRate() >= 0.6) {
            sb.append("本周完成率不错，完成了大部分计划的任务。\n");
        }
        if (s.habitCheckInDays() >= 5) {
            sb.append("习惯坚持得很好，打卡率超过70%，继续保持。\n");
        }
        if (s.totalFocusMinutes() >= 300) {
            sb.append("专注时长很可观，深度工作时间充足。\n");
        }
        if (s.completionRate() < 0.6 && s.habitCheckInDays() < 5 && s.totalFocusMinutes() < 300) {
            sb.append("本周有所行动，万事开头难，已经迈出第一步了。\n");
        }

        sb.append("\n## 🔍 待改进\n\n");
        if (s.overdueTasks() > 0) {
            sb.append(s.overdueTasks()).append(" 个任务逾期了，建议评估一下截止时间是否合理。\n");
        }
        if (s.habitTasks() > 0 && s.habitCheckInDays() < 4) {
            sb.append("习惯打卡偏低，可以试试降低每天的任务量，先保证连续性。\n");
        }
        if (s.overdueTasks() == 0 && s.completionRate() >= 0.6) {
            sb.append("这周表现稳定，继续保持当前节奏即可。\n");
        }

        sb.append("\n## 💡 下周建议\n\n");
        int tip = 1;
        if (s.overdueTasks() > 0) {
            sb.append(tip++).append(". 优先处理逾期任务，周一就安排时间搞定它们\n");
        }
        if (s.habitCheckInDays() < 5) {
            sb.append(tip++).append(". 从每天一个小习惯开始，比一次做很多更重要\n");
        }
        if (s.completionRate() < 0.5) {
            sb.append(tip++).append(". 下周适当减少任务数量，集中精力完成最重要的3件事\n");
        }
        if (tip == 1) {
            sb.append("1. 保持本周节奏，适当给自己一点挑战\n");
        }

        sb.append("\n## 🌟 小结\n\n");
        if (s.completionRate() >= 0.8) {
            sb.append("这周效率拉满，给自己点个赞。下周继续保持这个状态就好。\n");
        } else if (s.completionRate() >= 0.5) {
            sb.append("一步一个脚印，完成比完美重要。下周继续加油。\n");
        } else if (s.totalTasks() > 0) {
            sb.append("已经开始了就不算晚，下周会更好。\n");
        }

        sb.append("\n配置 DeepSeek 后可获得更详细的 AI 分析。\n");
        return sb.toString();
    }
}
