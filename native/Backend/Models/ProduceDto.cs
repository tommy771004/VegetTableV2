using System.Text.Json.Serialization;

namespace ProduceApi.Models;

/// <summary>
/// 標準農產品 DTO - 前端使用英文欄位
/// </summary>
public class ProduceDto
{
    public string CropCode { get; set; } = string.Empty;
    public string CropName { get; set; } = string.Empty;
    public string MarketName { get; set; } = string.Empty;
    public double UpperPrice { get; set; }
    public double MiddlePrice { get; set; }
    public double LowerPrice { get; set; }
    public double AvgPrice { get; set; }
    public double TransVolume { get; set; }
    public string TransDate { get; set; } = string.Empty;
}

/// <summary>
/// 政府 MOA API 原始格式 DTO - 中文欄位映射
/// </summary>
public class MoaProduceDto
{
    [JsonPropertyName("作物代號")]
    public string CropCode { get; set; } = string.Empty;

    [JsonPropertyName("作物名稱")]
    public string CropName { get; set; } = string.Empty;

    [JsonPropertyName("市場名稱")]
    public string MarketName { get; set; } = string.Empty;

    [JsonPropertyName("上價")]
    public double UpperPrice { get; set; }

    [JsonPropertyName("中價")]
    public double MiddlePrice { get; set; }

    [JsonPropertyName("下價")]
    public double LowerPrice { get; set; }

    [JsonPropertyName("平均價")]
    public double AvgPrice { get; set; }

    [JsonPropertyName("交易量")]
    public double TransVolume { get; set; }

    [JsonPropertyName("交易日期")]
    public string TransDate { get; set; } = string.Empty;
}

public class PaginatedResult<T>
{
    public List<T> Items { get; set; } = new();
    public int TotalCount { get; set; }
    public int Page { get; set; }
    public int PageSize { get; set; }
}

public class HistoricalPriceDto
{
    public string Date { get; set; } = string.Empty;
    public double AvgPrice { get; set; }
    public double TransVolume { get; set; }
}

public class PricePredictionDto
{
    public double PredictedPrice { get; set; }
    public string Trend { get; set; } = string.Empty; // "上漲", "下跌", "持平"
    public double MovingAverage7Day { get; set; }
}

public class PriceAnomalyDto
{
    public string CropCode { get; set; } = string.Empty;
    public string CropName { get; set; } = string.Empty;
    public double PreviousPrice { get; set; }
    public double CurrentPrice { get; set; }
    public double ChangePercent { get; set; }
    public string TransDate { get; set; } = string.Empty;
}

public class WeatherAlertDto
{
    public string AlertType { get; set; } = "None";
    public string Severity { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
    public List<string> AffectedCrops { get; set; } = new();
}

public class BudgetRecipeDto
{
    public string RecipeName { get; set; } = string.Empty;
    public List<string> MainIngredients { get; set; } = new();
    public string Reason { get; set; } = string.Empty;
    public string ImageUrl { get; set; } = string.Empty; // Emoji character
    public List<string> Steps { get; set; } = new();
}

public class FavoriteAlertDto
{
    public int Id { get; set; }
    public string UserId { get; set; } = string.Empty;
    public string ProduceId { get; set; } = string.Empty;
    public string CropName { get; set; } = string.Empty;
    public double TargetPrice { get; set; }
    public double? CurrentPrice { get; set; }
    public bool IsAlertTriggered { get; set; }
}

public class CommunityPriceDto
{
    public int Id { get; set; }
    public string CropCode { get; set; } = string.Empty;
    public double RetailPrice { get; set; }
    public string Location { get; set; } = string.Empty;
    public string ReportedBy { get; set; } = string.Empty;
    public DateTime ReportedAt { get; set; }
}

public class CommunityPriceSubmitDto
{
    public string CropCode { get; set; } = string.Empty;
    public double RetailPrice { get; set; }
    public string Location { get; set; } = string.Empty;
}

public class SeasonalCropDto
{
    public string CropCode { get; set; } = string.Empty;
    public string CropName { get; set; } = string.Empty;
    public List<int> SeasonMonths { get; set; } = new();
}

public class AuthTokenRequest
{
    public string DeviceId { get; set; } = string.Empty;
}

public class AuthTokenResponse
{
    public string Token { get; set; } = string.Empty;
}

public class FcmTokenRequest
{
    public string FcmToken { get; set; } = string.Empty;
}
