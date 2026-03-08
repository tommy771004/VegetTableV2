using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using ProduceApi.Data;
using ProduceApi.Services;

var builder = WebApplication.CreateBuilder(args);

// --- JWT 安全驗證 ---
var jwtSecret = builder.Configuration["Jwt:SecretKey"];
if (string.IsNullOrWhiteSpace(jwtSecret))
{
    throw new InvalidOperationException(
        "JWT SecretKey 未設定。請透過環境變數 Jwt__SecretKey 注入金鑰（至少 32 字元）。\n" +
        "本地開發：請在 appsettings.Development.json 中設定 Jwt:SecretKey。\n" +
        "正式環境：export Jwt__SecretKey=\"YourSuperSecretKey\"");
}

var keyBytes = Encoding.UTF8.GetBytes(jwtSecret);

// --- 服務註冊 ---
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Entity Framework Core (SQLite for dev, can swap to PostgreSQL/SQL Server)
builder.Services.AddDbContext<ProduceDbContext>(options =>
    options.UseSqlite(builder.Configuration.GetConnectionString("DefaultConnection")
        ?? "Data Source=produce.db"));

// HttpClient for MOA API
builder.Services.AddHttpClient("MoaApi", client =>
{
    client.BaseAddress = new Uri("https://data.moa.gov.tw/");
    client.Timeout = TimeSpan.FromSeconds(30);
});

// 背景同步服務
builder.Services.AddHostedService<ProduceSyncWorker>();
builder.Services.AddHostedService<PriceAlertWorker>();

// JWT 認證
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = false,
            ValidateAudience = false,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(keyBytes)
        };
    });

builder.Services.AddAuthorization();

// CORS
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

var app = builder.Build();

// --- 資料庫自動遷移 ---
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<ProduceDbContext>();
    db.Database.EnsureCreated();
}

// --- 中介軟體 ---
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseCors();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.Run();
