using Microsoft.EntityFrameworkCore;
using ProduceApi.Data;

namespace ProduceApi.Services;

/// <summary>
/// 背景服務：每天清晨掃描使用者目標價格，透過 FCM 發送降價通知
/// </summary>
public class PriceAlertWorker : BackgroundService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<PriceAlertWorker> _logger;

    public PriceAlertWorker(
        IServiceProvider serviceProvider,
        ILogger<PriceAlertWorker> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await CheckAndNotify(stoppingToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "價格提醒檢查失敗");
            }

            // 每 6 小時執行一次
            await Task.Delay(TimeSpan.FromHours(6), stoppingToken);
        }
    }

    private async Task CheckAndNotify(CancellationToken ct)
    {
        using var scope = _serviceProvider.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<ProduceDbContext>();

        var today = DateTime.Now.ToString("yyyy-MM-dd");

        var favorites = await db.UserFavorites.ToListAsync(ct);
        var todayPrices = await db.PriceHistories
            .Where(p => p.TransDate == today)
            .ToDictionaryAsync(p => p.CropCode, p => p.AvgPrice, ct);

        foreach (var fav in favorites)
        {
            if (!todayPrices.TryGetValue(fav.ProduceId, out var currentPrice)) continue;
            if (currentPrice > fav.TargetPrice) continue;

            // 價格已達標，發送推播通知
            var fcmToken = await db.FcmTokens
                .FirstOrDefaultAsync(f => f.UserId == fav.UserId, ct);

            if (fcmToken == null) continue;

            await SendFcmNotification(
                fcmToken.Token,
                $"🎉 {fav.CropName} 降價了！",
                $"目前均價 ${currentPrice:F1}，已達到您設定的目標價 ${fav.TargetPrice:F1}",
                fav.ProduceId);

            _logger.LogInformation("已發送降價通知：{CropName} -> {UserId}", fav.CropName, fav.UserId);
        }
    }

    private async Task SendFcmNotification(string token, string title, string body, string produceId)
    {
        // 使用 Firebase Admin SDK 發送通知
        // 實際整合時需在 DI 中註冊 FirebaseApp
        try
        {
            var message = new FirebaseAdmin.Messaging.Message
            {
                Token = token,
                Notification = new FirebaseAdmin.Messaging.Notification
                {
                    Title = title,
                    Body = body
                },
                Data = new Dictionary<string, string>
                {
                    { "produceId", produceId }
                }
            };

            await FirebaseAdmin.Messaging.FirebaseMessaging.DefaultInstance.SendAsync(message);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "FCM 推播發送失敗: token={Token}", token);
        }
    }
}
