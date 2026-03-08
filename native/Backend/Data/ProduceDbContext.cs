using Microsoft.EntityFrameworkCore;

namespace ProduceApi.Data;

public class ProduceDbContext : DbContext
{
    public ProduceDbContext(DbContextOptions<ProduceDbContext> options) : base(options) { }

    public DbSet<UserFavorite> UserFavorites => Set<UserFavorite>();
    public DbSet<PriceHistory> PriceHistories => Set<PriceHistory>();
    public DbSet<CommunityPrice> CommunityPrices => Set<CommunityPrice>();
    public DbSet<FcmToken> FcmTokens => Set<FcmToken>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PriceHistory>()
            .HasIndex(p => new { p.CropCode, p.TransDate });

        modelBuilder.Entity<UserFavorite>()
            .HasIndex(f => new { f.UserId, f.ProduceId })
            .IsUnique();

        modelBuilder.Entity<CommunityPrice>()
            .HasIndex(c => c.CropCode);

        modelBuilder.Entity<FcmToken>()
            .HasIndex(f => f.UserId)
            .IsUnique();
    }
}

public class UserFavorite
{
    public int Id { get; set; }
    public string UserId { get; set; } = string.Empty;
    public string ProduceId { get; set; } = string.Empty;
    public string CropName { get; set; } = string.Empty;
    public double TargetPrice { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

public class PriceHistory
{
    public int Id { get; set; }
    public string CropCode { get; set; } = string.Empty;
    public string CropName { get; set; } = string.Empty;
    public string MarketName { get; set; } = string.Empty;
    public double AvgPrice { get; set; }
    public double TransVolume { get; set; }
    public string TransDate { get; set; } = string.Empty;
    public DateTime SyncedAt { get; set; } = DateTime.UtcNow;
}

public class CommunityPrice
{
    public int Id { get; set; }
    public string CropCode { get; set; } = string.Empty;
    public double RetailPrice { get; set; }
    public string Location { get; set; } = string.Empty;
    public string ReportedBy { get; set; } = string.Empty;
    public DateTime ReportedAt { get; set; } = DateTime.UtcNow;
}

public class FcmToken
{
    public int Id { get; set; }
    public string UserId { get; set; } = string.Empty;
    public string Token { get; set; } = string.Empty;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
