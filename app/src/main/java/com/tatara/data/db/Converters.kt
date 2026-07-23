package com.tatara.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter fun fromLocalDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter fun fromLocalTime(value: LocalTime?): String? = value?.toString()
    @TypeConverter fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter fun fromInstant(value: Instant?): String? = value?.toString()
    @TypeConverter fun toInstant(value: String?): Instant? = value?.let(Instant::parse)
}
