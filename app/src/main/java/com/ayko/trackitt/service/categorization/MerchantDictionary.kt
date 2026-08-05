package com.ayko.trackitt.service.categorization

import com.ayko.trackitt.domain.model.TransactionCategory

val MERCHANT_DICTIONARY: Map<String, TransactionCategory> = mapOf(
    // Shopping
    "melcom" to TransactionCategory.SHOPPING,
    "game" to TransactionCategory.SHOPPING,
    "palace" to TransactionCategory.SHOPPING,
    "koala" to TransactionCategory.SHOPPING,

    // Groceries
    "shoprite" to TransactionCategory.GROCERIES,
    "maxmart" to TransactionCategory.GROCERIES,
    "marina mall" to TransactionCategory.GROCERIES,
    "spring supermarket" to TransactionCategory.GROCERIES,
    "hs home supermarket" to TransactionCategory.GROCERIES,
    "supermarket" to TransactionCategory.GROCERIES,

    // Food
    "papaye" to TransactionCategory.FOOD,
    "kfc" to TransactionCategory.FOOD,
    "chicken inn" to TransactionCategory.FOOD,
    "pizza inn" to TransactionCategory.FOOD,
    "marwako" to TransactionCategory.FOOD,
    "burger king" to TransactionCategory.FOOD,
    "frankies" to TransactionCategory.FOOD,
    "slice and topping" to TransactionCategory.FOOD,
    "slice  and topping" to TransactionCategory.FOOD,

    // Transport
    "bolt" to TransactionCategory.TRANSPORT,
    "uber" to TransactionCategory.TRANSPORT,
    "yango" to TransactionCategory.TRANSPORT,
    "trotro" to TransactionCategory.TRANSPORT,
    "taxi" to TransactionCategory.TRANSPORT,
    "top oil" to TransactionCategory.TRANSPORT,

    // Bills
    "ecg" to TransactionCategory.BILLS,
    "gwcl" to TransactionCategory.BILLS,
    "dstv" to TransactionCategory.BILLS,
    "gotv" to TransactionCategory.BILLS,
    "airtel" to TransactionCategory.BILLS,
    "surfline" to TransactionCategory.BILLS,
    "etranzact" to TransactionCategory.BILLS,

    // Airtime / Top-up
    "mtn airtime" to TransactionCategory.AIRTIME,


    // Data bundles → Bills
    "mtnbundle" to TransactionCategory.BILLS,
    "mtn bundle" to TransactionCategory.BILLS,
    "vodafone" to TransactionCategory.BILLS,

    // Ecobank POS merchants
    "spotify" to TransactionCategory.BILLS,
    "netflix" to TransactionCategory.BILLS,
    "youtube" to TransactionCategory.BILLS,
    "google one" to TransactionCategory.BILLS,
    "amazon" to TransactionCategory.SHOPPING,
    "ebay" to TransactionCategory.SHOPPING,
    "wallet2bank" to TransactionCategory.TRANSFER,
    "bank2wallet" to TransactionCategory.TRANSFER,
    "momo → bank" to TransactionCategory.TRANSFER,
    "bank → momo" to TransactionCategory.TRANSFER,
    "zenith momo pos" to TransactionCategory.TRANSFER,
    "zen online" to TransactionCategory.EDUCATION,
    "atm" to TransactionCategory.OTHER,

    // Transfer / Remittance
    "bigpay" to TransactionCategory.TRANSFER,
    "interoperability push" to TransactionCategory.TRANSFER,
    "interoperability pull" to TransactionCategory.TRANSFER,
    "itc" to TransactionCategory.TRANSFER,
    "taptapsend" to TransactionCategory.TRANSFER,
    "send wave" to TransactionCategory.TRANSFER,

    // Health
    "greenville hospital" to TransactionCategory.HEALTH,
    "hospital" to TransactionCategory.HEALTH,
    "pharmacy" to TransactionCategory.HEALTH,
    "clinic" to TransactionCategory.HEALTH,

    // Church
    "icgc" to TransactionCategory.CHURCH,
    "life centre" to TransactionCategory.CHURCH,
    "cornerstone chapel" to TransactionCategory.CHURCH,
    "dayspring" to TransactionCategory.CHURCH,

    // Pocket money / family support
    "adwoa frimpong" to TransactionCategory.POCKET_MONEY,
    "esi quaye" to TransactionCategory.POCKET_MONEY,

    // Repeat food vendors identified from labeled notes
    "nana yeboah" to TransactionCategory.FOOD,
    "akua addo" to TransactionCategory.FOOD,
)

val PRIORITY_NOTE_DICTIONARY: Map<String, TransactionCategory> = mapOf(
    "ac payment" to TransactionCategory.HOME_PROJECT,
    "ac workmanship" to TransactionCategory.HOME_PROJECT,
    "ac materials" to TransactionCategory.HOME_PROJECT,
    "pocket money" to TransactionCategory.POCKET_MONEY,
    "birthday present" to TransactionCategory.GIFTS,
    "gift" to TransactionCategory.GIFTS,
    "transcript payment" to TransactionCategory.EDUCATION,
    "transcript" to TransactionCategory.EDUCATION,
    "lesson" to TransactionCategory.EDUCATION,
    "snacks" to TransactionCategory.SNACKS,
    "biscuits" to TransactionCategory.SNACKS,
    "pie" to TransactionCategory.SNACKS,
    "screen" to TransactionCategory.ELECTRONICS,
    "laptop" to TransactionCategory.ELECTRONICS,
    "charger" to TransactionCategory.ELECTRONICS,
    "phone" to TransactionCategory.ELECTRONICS,
    "ups" to TransactionCategory.ELECTRONICS,
)

val PRIORITY_REFERENCE_DICTIONARY: Map<String, TransactionCategory> = mapOf(
    "transcript" to TransactionCategory.EDUCATION,
    "lesson" to TransactionCategory.EDUCATION,
)

val REFERENCE_DICTIONARY: Map<String, TransactionCategory> = mapOf(
    // Transport
    "motor" to TransactionCategory.TRANSPORT,
    "fuel" to TransactionCategory.TRANSPORT,
    "petrol" to TransactionCategory.TRANSPORT,
    "diesel" to TransactionCategory.TRANSPORT,
    "trotro" to TransactionCategory.TRANSPORT,
    "taxi" to TransactionCategory.TRANSPORT,
    "bolt" to TransactionCategory.TRANSPORT,
    "uber" to TransactionCategory.TRANSPORT,
    "car" to TransactionCategory.TRANSPORT,
    "transport" to TransactionCategory.TRANSPORT,

    // House
    "soap" to TransactionCategory.HOUSE,
    "soqp" to TransactionCategory.HOUSE,
    "rent" to TransactionCategory.HOUSE,
    "water" to TransactionCategory.HOUSE,
    "light" to TransactionCategory.HOUSE,
    "electric" to TransactionCategory.HOUSE,
    "cleaning" to TransactionCategory.HOUSE,
    "detergent" to TransactionCategory.HOUSE,
    "dustbin" to TransactionCategory.HOUSE,
    "electricity" to TransactionCategory.HOUSE,

    // Food
    "food" to TransactionCategory.FOOD,
    "lunch" to TransactionCategory.FOOD,
    "dinner" to TransactionCategory.FOOD,
    "breakfast" to TransactionCategory.FOOD,
    "chop" to TransactionCategory.FOOD,
    "waakye" to TransactionCategory.FOOD,
    "pizza" to TransactionCategory.FOOD,
    "burger" to TransactionCategory.FOOD,
    "chicken" to TransactionCategory.FOOD,
    "stew" to TransactionCategory.FOOD,
    "bread" to TransactionCategory.FOOD,
    "slice" to TransactionCategory.FOOD,
    "indomie" to TransactionCategory.FOOD,
    "malt" to TransactionCategory.FOOD,
    "coke" to TransactionCategory.FOOD,
    "delivery" to TransactionCategory.FOOD,
    "meat" to TransactionCategory.FOOD,
    "rice" to TransactionCategory.FOOD,
    "egg" to TransactionCategory.FOOD,
    "drink" to TransactionCategory.FOOD,

    // Beauty
    "hair" to TransactionCategory.BEAUTY,
    "salon" to TransactionCategory.BEAUTY,
    "cream" to TransactionCategory.BEAUTY,
    "makeup" to TransactionCategory.BEAUTY,
    "nails" to TransactionCategory.BEAUTY,
    "barber" to TransactionCategory.BEAUTY,
    "baber" to TransactionCategory.BEAUTY,
    "barbering" to TransactionCategory.BEAUTY,

    // Bills
    "dstv" to TransactionCategory.BILLS,
    "internet" to TransactionCategory.BILLS,
    "surfline" to TransactionCategory.BILLS,

    // Bills (internet/data)
    "data" to TransactionCategory.BILLS,

    // Airtime
    "airtime" to TransactionCategory.AIRTIME,

    // Groceries
    "market" to TransactionCategory.GROCERIES,
    "supermarket" to TransactionCategory.GROCERIES,

    // Shopping
    "shirt" to TransactionCategory.SHOPPING,
    "clothe" to TransactionCategory.SHOPPING,
    "clothes" to TransactionCategory.SHOPPING,
    "card" to TransactionCategory.SHOPPING,

    // Electronics
    "screen" to TransactionCategory.ELECTRONICS,
    "laptop" to TransactionCategory.ELECTRONICS,
    "charger" to TransactionCategory.ELECTRONICS,
    "phone" to TransactionCategory.ELECTRONICS,
    "tablet" to TransactionCategory.ELECTRONICS,
    "electricals" to TransactionCategory.ELECTRONICS,
    "tv" to TransactionCategory.ELECTRONICS,
    "ups" to TransactionCategory.ELECTRONICS,
    "earbuds" to TransactionCategory.ELECTRONICS,
    "airpods" to TransactionCategory.ELECTRONICS,
    "electronics" to TransactionCategory.ELECTRONICS,

    // Church
    "offering" to TransactionCategory.CHURCH,
    "tithe" to TransactionCategory.CHURCH,
    "seed offering" to TransactionCategory.CHURCH,
    "church" to TransactionCategory.CHURCH,

    // Health
    "hospital" to TransactionCategory.HEALTH,
    "pharmacy" to TransactionCategory.HEALTH,
    "clinic" to TransactionCategory.HEALTH,
    "lab" to TransactionCategory.HEALTH,
)
