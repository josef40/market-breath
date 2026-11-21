# 📊 Market Breath

A Spring Boot app that tracks major market indices (SPY, QQQ, IWM) in relation to key moving averages (10, 20, 50).

## 🧬 Architecture
* **Frontend:** HTML, CSS, JavaScript
* **Backend:** Spring Boot 4.1.2

## 🔧 Prerequisites
* `JDK 25+`
* `Maven 3.x`

## 📐 Project Structure
```
market-data-service/
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── josef/
        │           └── marketbreath/
        │               ├── controller/
        │               │   └── MarketController.java
        │               ├── model/
        │               │   ├── DailyClose.java
        │               │   └── PriceRow.java
        │               ├── repo/
        │               │   └── DailyCloseRepository.java
        │               ├── service/
        │               │   └── MarketService.java
        │               └── MarketApplication.java
        └── resources/
            └── static/
                ├── index.html
                ├── script.js
                └── style.css
```

## 🚀 Getting Started
1. Run `MarketBreathApplication.java`
2. Open **http://localhost:8080**