package com.ayko.trackitt.service.sms

data class SmsProvider(
    val key: String,
    val name: String,
    val senders: Set<String>,
)

object SmsProviders {

    val MOMO = SmsProvider("MobileMoney", "MTN MoMo", setOf("mobilemoney", "momo"))
    val TELECEL = SmsProvider(
        "T-CASH",
        "Telecel Cash",
        setOf("t-cash", "t cash", "tcash", "telecelcash", "vf-cash", "vf cash", "vfcash", "vodafonecash"),
    )
    val ECOBANK = SmsProvider("ECOBANK", "Ecobank", setOf("ecobank"))
    val GHANAPAY = SmsProvider("GhanaPay", "GhanaPay", setOf("ghanapay"))
    val AT_MONEY = SmsProvider("ATMoney", "AT Money", setOf("atmoney", "at money", "airteltigo"))
    val GMONEY = SmsProvider("G-Money", "G-Money", setOf("g-money", "gmoney"))
    val GCB = SmsProvider("GCB", "GCB Bank", setOf("gcb", "gcb bank", "gcbbank"))
    val STANBIC = SmsProvider("Stanbic", "Stanbic Bank", setOf("stanbic", "stanbicbank"))
    val FIDELITY = SmsProvider("Fidelity", "Fidelity Bank", setOf("fidelity", "fidelitybank"))
    val ABSA = SmsProvider("ABSA", "Absa Bank", setOf("absa", "absa bank", "absaghana"))
    val ACCESS = SmsProvider("ACCESS", "Access Bank", setOf("accessalert", "access bank", "accessbank"))
    val GTBANK = SmsProvider("GTBank", "GTBank", setOf("gtbank", "gtbghana"))
    val CALBANK = SmsProvider("CalBank", "CalBank", setOf("calbank"))
    val ZENITH = SmsProvider("Zenith", "Zenith Bank", setOf("zenith", "zenithbank"))
    val UBA = SmsProvider("UBA", "UBA", setOf("uba", "ubaghana"))
    val REPUBLIC = SmsProvider("Republic", "Republic Bank", setOf("republic", "republicbank"))
    val CBG = SmsProvider("CBG", "CBG", setOf("cbg", "cbgbank"))
    val ZEEPAY = SmsProvider("Zeepay", "Zeepay", setOf("zeepay"))

    /** Providers shown in SMS sources settings, in display order. */
    val known: List<SmsProvider> = listOf(
        MOMO, TELECEL, GHANAPAY, AT_MONEY, GMONEY, ZEEPAY,
        ECOBANK, GCB, STANBIC, FIDELITY, ABSA, ACCESS, GTBANK, CALBANK, ZENITH, UBA, REPUBLIC, CBG,
    )

    private val bySender: Map<String, SmsProvider> =
        known.flatMap { provider -> provider.senders.map { it to provider } }.toMap()

    fun forSender(sender: String?): SmsProvider? {
        val normalized = sender?.trim()?.lowercase() ?: return null
        return bySender[normalized] ?: bySender[normalized.replace(" ", "")]
    }

    /** Provider for a sender that isn't in the registry, named after its sender ID. */
    fun unknown(sender: String?): SmsProvider {
        val cleaned = sender?.trim().orEmpty()
        val name = when {
            cleaned.isEmpty() -> "SMS"
            cleaned.trimStart('+').all { it.isDigit() } -> "SMS"
            else -> cleaned
        }
        return SmsProvider(name, name, emptySet())
    }

    fun prefKey(provider: SmsProvider) = "source_${provider.key}"

    const val OTHER_SENDERS_PREF = "source_other"
}
