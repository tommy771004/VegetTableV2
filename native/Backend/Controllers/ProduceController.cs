using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using System.Xml.Linq;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using ProduceApi.Data;
using ProduceApi.Models;

namespace ProduceApi.Controllers;

[ApiController]
[Route("api/produce")]
public class ProduceController : ControllerBase
{
    private readonly ProduceDbContext _db;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly IConfiguration _configuration;
    private readonly ILogger<ProduceController> _logger;

    public ProduceController(
        ProduceDbContext db,
        IHttpClientFactory httpClientFactory,
        IConfiguration configuration,
        ILogger<ProduceController> logger)
    {
        _db = db;
        _httpClientFactory = httpClientFactory;
        _configuration = configuration;
        _logger = logger;
    }

    // ==================== 認證 ====================

    /// <summary>
    /// POST /api/produce/auth/token - 以 deviceId 換取 JWT Token
    /// </summary>
    [HttpPost("auth/token")]
    public IActionResult GetToken([FromBody] AuthTokenRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.DeviceId))
            return BadRequest("deviceId is required");

        var secretKey = _configuration["Jwt:SecretKey"]!;
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secretKey));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var claims = new[]
        {
            new Claim("deviceId", request.DeviceId),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
        };

        var token = new JwtSecurityToken(
            claims: claims,
            expires: DateTime.UtcNow.AddDays(365),
            signingCredentials: credentials);

        var tokenString = new JwtSecurityTokenHandler().WriteToken(token);

        return Ok(new AuthTokenResponse { Token = tokenString });
    }

    // ==================== 今日菜價 ====================

    /// <summary>
    /// GET /api/produce/daily-prices?keyword=高麗菜&page=1
    /// </summary>
    [HttpGet("daily-prices")]
    public async Task<ActionResult<PaginatedResult<ProduceDto>>> GetDailyPrices(
        [FromQuery] string? keyword = null,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20)
    {
        var today = DateTime.Now.ToString("yyyy-MM-dd");
        var query = _db.PriceHistories.Where(p => p.TransDate == today);

        if (!string.IsNullOrWhiteSpace(keyword))
        {
            query = query.Where(p => p.CropName.Contains(keyword));
        }

        var totalCount = await query.CountAsync();
        var items = await query
            .OrderBy(p => p.CropName)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(p => new ProduceDto
            {
                CropCode = p.CropCode,
                CropName = p.CropName,
                MarketName = p.MarketName,
                AvgPrice = p.AvgPrice,
                TransVolume = p.TransVolume,
                TransDate = p.TransDate
            })
            .ToListAsync();

        return Ok(new PaginatedResult<ProduceDto>
        {
            Items = items,
            TotalCount = totalCount,
            Page = page,
            PageSize = pageSize
        });
    }

    // ==================== 歷史價格 ====================

    /// <summary>
    /// GET /api/produce/history/{produceId}
    /// </summary>
    [HttpGet("history/{produceId}")]
    public async Task<ActionResult<List<HistoricalPriceDto>>> GetPriceHistory(string produceId)
    {
        var history = await _db.PriceHistories
            .Where(p => p.CropCode == produceId)
            .OrderByDescending(p => p.TransDate)
            .Take(30)
            .Select(p => new HistoricalPriceDto
            {
                Date = p.TransDate,
                AvgPrice = p.AvgPrice,
                TransVolume = p.TransVolume
            })
            .ToListAsync();

        return Ok(history);
    }

    // ==================== 熱門交易 ====================

    /// <summary>
    /// GET /api/produce/top-volume - 今日交易量前 10 名
    /// </summary>
    [HttpGet("top-volume")]
    public async Task<ActionResult<List<ProduceDto>>> GetTopVolume()
    {
        var today = DateTime.Now.ToString("yyyy-MM-dd");
        var items = await _db.PriceHistories
            .Where(p => p.TransDate == today)
            .OrderByDescending(p => p.TransVolume)
            .Take(10)
            .Select(p => new ProduceDto
            {
                CropCode = p.CropCode,
                CropName = p.CropName,
                MarketName = p.MarketName,
                AvgPrice = p.AvgPrice,
                TransVolume = p.TransVolume,
                TransDate = p.TransDate
            })
            .ToListAsync();

        return Ok(items);
    }

    // ==================== 價格異常 ====================

    /// <summary>
    /// GET /api/produce/anomalies - 單日漲幅超過 50% 的異常警告
    /// </summary>
    [HttpGet("anomalies")]
    public async Task<ActionResult<List<PriceAnomalyDto>>> GetAnomalies()
    {
        var today = DateTime.Now.ToString("yyyy-MM-dd");
        var yesterday = DateTime.Now.AddDays(-1).ToString("yyyy-MM-dd");

        var todayPrices = await _db.PriceHistories
            .Where(p => p.TransDate == today)
            .ToListAsync();

        var yesterdayPrices = await _db.PriceHistories
            .Where(p => p.TransDate == yesterday)
            .ToDictionaryAsync(p => p.CropCode, p => p.AvgPrice);

        var anomalies = todayPrices
            .Where(t => yesterdayPrices.ContainsKey(t.CropCode) && yesterdayPrices[t.CropCode] > 0)
            .Select(t =>
            {
                var prevPrice = yesterdayPrices[t.CropCode];
                var change = (t.AvgPrice - prevPrice) / prevPrice * 100;
                return new PriceAnomalyDto
                {
                    CropCode = t.CropCode,
                    CropName = t.CropName,
                    PreviousPrice = prevPrice,
                    CurrentPrice = t.AvgPrice,
                    ChangePercent = Math.Round(change, 2),
                    TransDate = t.TransDate
                };
            })
            .Where(a => Math.Abs(a.ChangePercent) >= 50)
            .OrderByDescending(a => a.ChangePercent)
            .ToList();

        return Ok(anomalies);
    }

    // ==================== 價格預測 ====================

    /// <summary>
    /// GET /api/produce/forecast/{produceId} - 7 日移動平均預測
    /// </summary>
    [HttpGet("forecast/{produceId}")]
    public async Task<ActionResult<PricePredictionDto>> GetForecast(string produceId)
    {
        var history = await _db.PriceHistories
            .Where(p => p.CropCode == produceId)
            .OrderByDescending(p => p.TransDate)
            .Take(14)
            .ToListAsync();

        if (history.Count < 7)
            return NotFound("歷史資料不足，無法預測");

        var recent7 = history.Take(7).Select(p => p.AvgPrice).ToList();
        var previous7 = history.Skip(7).Take(7).Select(p => p.AvgPrice).ToList();

        var ma7 = recent7.Average();
        var maPrev = previous7.Count > 0 ? previous7.Average() : ma7;

        string trend;
        if (ma7 > maPrev * 1.05) trend = "上漲";
        else if (ma7 < maPrev * 0.95) trend = "下跌";
        else trend = "持平";

        return Ok(new PricePredictionDto
        {
            PredictedPrice = Math.Round(ma7, 2),
            Trend = trend,
            MovingAverage7Day = Math.Round(ma7, 2)
        });
    }

    // ==================== 當季盛產 ====================

    /// <summary>
    /// GET /api/produce/seasonal - 當季盛產農作物清單
    /// </summary>
    [HttpGet("seasonal")]
    public IActionResult GetSeasonalCrops()
    {
        var currentMonth = DateTime.Now.Month;

        // 台灣常見當季蔬果對照表
        var seasonalData = new List<SeasonalCropDto>
        {
            new() { CropCode = "LC1", CropName = "高麗菜", SeasonMonths = new List<int> { 11, 12, 1, 2, 3 } },
            new() { CropCode = "SA1", CropName = "菠菜", SeasonMonths = new List<int> { 10, 11, 12, 1, 2 } },
            new() { CropCode = "WM1", CropName = "西瓜", SeasonMonths = new List<int> { 5, 6, 7, 8 } },
            new() { CropCode = "MG1", CropName = "芒果", SeasonMonths = new List<int> { 5, 6, 7, 8 } },
            new() { CropCode = "BB1", CropName = "苦瓜", SeasonMonths = new List<int> { 5, 6, 7, 8, 9 } },
            new() { CropCode = "BM1", CropName = "竹筍", SeasonMonths = new List<int> { 5, 6, 7, 8 } },
            new() { CropCode = "SW1", CropName = "地瓜", SeasonMonths = new List<int> { 9, 10, 11, 12 } },
            new() { CropCode = "TN1", CropName = "橘子", SeasonMonths = new List<int> { 10, 11, 12, 1, 2 } },
            new() { CropCode = "LY1", CropName = "荔枝", SeasonMonths = new List<int> { 5, 6, 7 } },
            new() { CropCode = "SC1", CropName = "甘蔗", SeasonMonths = new List<int> { 11, 12, 1, 2, 3 } },
            new() { CropCode = "ST1", CropName = "草莓", SeasonMonths = new List<int> { 12, 1, 2, 3, 4 } },
            new() { CropCode = "CR1", CropName = "紅蘿蔔", SeasonMonths = new List<int> { 11, 12, 1, 2 } }
        };

        var inSeason = seasonalData
            .Where(s => s.SeasonMonths.Contains(currentMonth))
            .ToList();

        return Ok(inSeason);
    }

    // ==================== 天氣警報 ====================

    /// <summary>
    /// GET /api/produce/weather-alerts - 中央氣象署颱風/豪大雨警報
    /// </summary>
    [HttpGet("weather-alerts")]
    public async Task<ActionResult<WeatherAlertDto>> GetWeatherAlerts()
    {
        try
        {
            var client = _httpClientFactory.CreateClient();
            var rssUrl = "https://www.cwa.gov.tw/rss/forecast/36_08.xml";
            var response = await client.GetStringAsync(rssUrl);

            var doc = XDocument.Parse(response);
            var items = doc.Descendants("item").ToList();

            if (items.Count == 0)
            {
                return Ok(new WeatherAlertDto { AlertType = "None" });
            }

            var firstItem = items.First();
            var title = firstItem.Element("title")?.Value ?? "";
            var description = firstItem.Element("description")?.Value ?? "";

            var alertType = title.Contains("颱風") ? "Typhoon" :
                           title.Contains("豪雨") || title.Contains("大雨") ? "HeavyRain" : "Other";

            return Ok(new WeatherAlertDto
            {
                AlertType = alertType,
                Severity = "Warning",
                Title = title,
                Message = description,
                AffectedCrops = new List<string> { "葉菜類", "瓜果類", "根莖類" }
            });
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "天氣警報 API 讀取失敗，回傳無警報狀態");
            return Ok(new WeatherAlertDto { AlertType = "None" });
        }
    }

    // ==================== 省錢食譜 ====================

    /// <summary>
    /// GET /api/produce/budget-recipes - 依當季低價食材推薦食譜
    /// </summary>
    [HttpGet("budget-recipes")]
    public async Task<ActionResult<List<BudgetRecipeDto>>> GetBudgetRecipes()
    {
        var today = DateTime.Now.ToString("yyyy-MM-dd");

        var cheapCrops = await _db.PriceHistories
            .Where(p => p.TransDate == today)
            .OrderBy(p => p.AvgPrice)
            .Take(5)
            .ToListAsync();

        var recipes = new List<BudgetRecipeDto>
        {
            new()
            {
                RecipeName = "清炒時蔬",
                MainIngredients = cheapCrops.Take(2).Select(c => c.CropName).ToList(),
                Reason = "使用今日最低價蔬菜",
                ImageUrl = "🥬",
                Steps = new List<string> { "蔬菜洗淨切段", "熱油爆香蒜末", "大火快炒2分鐘", "加鹽調味起鍋" }
            },
            new()
            {
                RecipeName = "蔬菜味噌湯",
                MainIngredients = cheapCrops.Take(3).Select(c => c.CropName).ToList(),
                Reason = "低價食材煮湯營養滿分",
                ImageUrl = "🍲",
                Steps = new List<string> { "蔬菜切丁", "水滾後放入蔬菜", "煮軟後加味噌攪拌", "撒蔥花即完成" }
            },
            new()
            {
                RecipeName = "涼拌沙拉",
                MainIngredients = cheapCrops.Take(2).Select(c => c.CropName).ToList(),
                Reason = "免開火省瓦斯",
                ImageUrl = "🥗",
                Steps = new List<string> { "蔬菜洗淨瀝乾", "切成適口大小", "淋上和風醬汁", "拌勻即可食用" }
            }
        };

        return Ok(recipes);
    }

    // ==================== 市場比價 ====================

    /// <summary>
    /// GET /api/produce/compare/{cropName} - 全台各市場今日價格比較
    /// </summary>
    [HttpGet("compare/{cropName}")]
    public async Task<ActionResult<List<ProduceDto>>> ComparePrices(string cropName)
    {
        var today = DateTime.Now.ToString("yyyy-MM-dd");
        var items = await _db.PriceHistories
            .Where(p => p.TransDate == today && p.CropName.Contains(cropName))
            .OrderBy(p => p.AvgPrice)
            .Select(p => new ProduceDto
            {
                CropCode = p.CropCode,
                CropName = p.CropName,
                MarketName = p.MarketName,
                AvgPrice = p.AvgPrice,
                TransVolume = p.TransVolume,
                TransDate = p.TransDate
            })
            .ToListAsync();

        return Ok(items);
    }

    // ==================== 收藏管理 ====================

    /// <summary>
    /// GET /api/produce/favorites - 取得使用者收藏（含價格提醒狀態）
    /// </summary>
    [Authorize]
    [HttpGet("favorites")]
    public async Task<ActionResult<List<FavoriteAlertDto>>> GetFavorites()
    {
        var userId = User.FindFirst("deviceId")?.Value ?? "";
        var today = DateTime.Now.ToString("yyyy-MM-dd");

        var favorites = await _db.UserFavorites
            .Where(f => f.UserId == userId)
            .ToListAsync();

        var todayPrices = await _db.PriceHistories
            .Where(p => p.TransDate == today)
            .ToDictionaryAsync(p => p.CropCode, p => p.AvgPrice);

        var result = favorites.Select(f =>
        {
            var currentPrice = todayPrices.GetValueOrDefault(f.ProduceId);
            return new FavoriteAlertDto
            {
                Id = f.Id,
                UserId = f.UserId,
                ProduceId = f.ProduceId,
                CropName = f.CropName,
                TargetPrice = f.TargetPrice,
                CurrentPrice = currentPrice > 0 ? currentPrice : null,
                IsAlertTriggered = currentPrice > 0 && currentPrice <= f.TargetPrice
            };
        }).ToList();

        return Ok(result);
    }

    /// <summary>
    /// DELETE /api/produce/favorites/{produceId}
    /// </summary>
    [Authorize]
    [HttpDelete("favorites/{produceId}")]
    public async Task<IActionResult> RemoveFavorite(string produceId)
    {
        var userId = User.FindFirst("deviceId")?.Value ?? "";
        var favorite = await _db.UserFavorites
            .FirstOrDefaultAsync(f => f.UserId == userId && f.ProduceId == produceId);

        if (favorite == null) return NotFound();

        _db.UserFavorites.Remove(favorite);
        await _db.SaveChangesAsync();
        return NoContent();
    }

    // ==================== 社群回報 ====================

    /// <summary>
    /// POST /api/produce/community-price - 回報零售價
    /// </summary>
    [Authorize]
    [HttpPost("community-price")]
    public async Task<IActionResult> SubmitCommunityPrice([FromBody] CommunityPriceSubmitDto dto)
    {
        var userId = User.FindFirst("deviceId")?.Value ?? "";

        var entry = new CommunityPrice
        {
            CropCode = dto.CropCode,
            RetailPrice = dto.RetailPrice,
            Location = dto.Location,
            ReportedBy = userId,
            ReportedAt = DateTime.UtcNow
        };

        _db.CommunityPrices.Add(entry);
        await _db.SaveChangesAsync();

        return Ok(new { message = "回報成功" });
    }

    /// <summary>
    /// GET /api/produce/community-price/{cropCode}
    /// </summary>
    [HttpGet("community-price/{cropCode}")]
    public async Task<ActionResult<List<CommunityPriceDto>>> GetCommunityPrices(string cropCode)
    {
        var prices = await _db.CommunityPrices
            .Where(c => c.CropCode == cropCode)
            .OrderByDescending(c => c.ReportedAt)
            .Take(20)
            .Select(c => new CommunityPriceDto
            {
                Id = c.Id,
                CropCode = c.CropCode,
                RetailPrice = c.RetailPrice,
                Location = c.Location,
                ReportedBy = c.ReportedBy,
                ReportedAt = c.ReportedAt
            })
            .ToListAsync();

        return Ok(prices);
    }

    // ==================== FCM Token 註冊 ====================

    /// <summary>
    /// POST /api/produce/fcm/token
    /// </summary>
    [Authorize]
    [HttpPost("fcm/token")]
    public async Task<IActionResult> RegisterFcmToken([FromBody] FcmTokenRequest request)
    {
        var userId = User.FindFirst("deviceId")?.Value ?? "";

        var existing = await _db.FcmTokens.FirstOrDefaultAsync(f => f.UserId == userId);
        if (existing != null)
        {
            existing.Token = request.FcmToken;
            existing.UpdatedAt = DateTime.UtcNow;
        }
        else
        {
            _db.FcmTokens.Add(new FcmToken
            {
                UserId = userId,
                Token = request.FcmToken
            });
        }

        await _db.SaveChangesAsync();
        return Ok(new { message = "FCM Token 已註冊" });
    }
}
