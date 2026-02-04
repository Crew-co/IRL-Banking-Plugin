package net.crewco.Banking.data.cdata

enum class CardType(
    val displayName: String,
    val defaultDailyLimit: Double,
    val annualFee: Double
) {
    DEBIT("Debit Card", 5000.0, 0.0),
    CREDIT("Credit Card", 10000.0, 50.0),
    BUSINESS_DEBIT("Business Debit Card", 25000.0, 0.0),
    BUSINESS_CREDIT("Business Credit Card", 50000.0, 100.0),
    PREMIUM("Premium Card", 100000.0, 250.0)
}