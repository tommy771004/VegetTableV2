using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using ProduceApi.Data;
using ProduceApi.Models;

namespace ProduceApi.Services;

/// <summary>
/// 背景託管服務：每天定時從農業部 API 抓取最新價格並寫入資料庫
/// </summary>
public class ProduceSyncWorker : BackgroundService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ILogger<ProduceSyncWorker> _logger;

    public ProduceSyncWorker(
        IServiceProvider serviceProvider,
        IHttpClientFactory httpClientFactory,
        ILogger<ProduceSyncWorker> logger)
    {
        _serviceProvider = serviceProvider;
        _httpClientFactory = httpClientFactory;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await SyncPricesFromMoa(stoppingToken);
                _logger.LogInformation("農產品價格同步完成：{Time}", DateTime.Now);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "農產品價格同步失敗");
            }

            // 每 24 小時執行一次
            await Task.Delay(TimeSpan.FromHours(24), stoppingToken);
        }
    }

    private async Task SyncPricesFromMoa(CancellationToken ct)
    {
        var client = _httpClientFactory.CreateClient("MoaApi");
        var apiUrl = "Service/OpenData/FromM/FarmTransData.aspx";

        var response = await client.GetAsync(apiUrl, ct);
        response.EnsureSuccessStatusCode();

        var json = await response.Content.ReadAsStringAsync(ct);
        var moaData = JsonSerializer.Deserialize<List<MoaProduceDto>>(json);

        if (moaData == null || moaData.Count == 0)
        {
            _logger.LogWarning("MOA API 回傳空資料");
            return;
        }

        using var scope = _serviceProvider.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<ProduceDbContext>();

        var today = DateTime.Now.ToString("yyyy-MM-dd");

        // 檢查今日是否已同步
        var alreadySynced = await db.PriceHistories.AnyAsync(p => p.TransDate == today, ct);
        if (alreadySynced)
        {
            _logger.LogInformation("今日資料已同步，跳過");
            return;
        }

        var histories = moaData.Select(m => new PriceHistory
        {
            CropCode = m.CropCode,
            CropName = m.CropName,
            MarketName = m.MarketName,
            AvgPrice = m.AvgPrice,
            TransVolume = m.TransVolume,
            TransDate = m.TransDate,
            SyncedAt = DateTime.UtcNow
        }).ToList();

        db.PriceHistories.AddRange(histories);
        await db.SaveChangesAsync(ct);

        _logger.LogInformation("已同步 {Count} 筆農產品價格資料", histories.Count);
    }
}
