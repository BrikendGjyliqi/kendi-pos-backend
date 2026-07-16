package com.kendi.pos.restotable;

public enum ReservationStatus {
    PENDING_REQUEST,   // Kamarieri dergoi kerkesen, pret admin
    CONFIRMED,         // Admini konfirmoi, tavolina eshte RESERVED
    ARRIVED,           // Klienti erdhi, tavolina eshte ON_DINE
    NO_SHOW,           // Klienti nuk erdhi
    CANCELLED,         // U anulua para orarit
    DECLINED           // Admini refuzoi kerkesen
}