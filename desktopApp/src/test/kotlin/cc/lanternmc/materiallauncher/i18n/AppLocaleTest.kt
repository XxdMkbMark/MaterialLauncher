/*
 * Copyright (C) 2026 Mark <github@xxdmkbmark> & Pidan <github@bretren>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cc.lanternmc.materiallauncher.i18n

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLocaleTest {
    @Test
    fun chineseTagsStayChinese() {
        assertEquals(Locale.forLanguageTag("zh-CN"), AppLocale.toJvmLocale("zh-CN"))
        assertEquals(Locale.forLanguageTag("zh-CN"), AppLocale.toJvmLocale("zh"))
        assertEquals(Locale.forLanguageTag("zh-CN"), AppLocale.toJvmLocale("zh_TW"))
    }

    @Test
    fun englishTagsUseEnglish() {
        assertEquals(Locale.forLanguageTag("en-US"), AppLocale.toJvmLocale("en-US"))
        assertEquals(Locale.forLanguageTag("en-US"), AppLocale.toJvmLocale("en"))
        assertEquals(Locale.forLanguageTag("en-US"), AppLocale.toJvmLocale("EN_GB"))
    }

    @Test
    fun unknownTagsFallBackToChinese() {
        assertEquals(Locale.forLanguageTag("zh-CN"), AppLocale.toJvmLocale("fr-FR"))
        assertEquals(Locale.forLanguageTag("zh-CN"), AppLocale.toJvmLocale(""))
        assertEquals(Locale.forLanguageTag("zh-CN"), AppLocale.toJvmLocale("  "))
    }

    @Test
    fun missingConfigFollowsSystem() {
        assertEquals(
            Locale.forLanguageTag("en-US"),
            AppLocale.chooseLocale(null, Locale.forLanguageTag("en-US")),
        )
        assertEquals(
            Locale.forLanguageTag("zh-CN"),
            AppLocale.chooseLocale(null, Locale.forLanguageTag("zh-CN")),
        )
        assertEquals(
            Locale.forLanguageTag("zh-CN"),
            AppLocale.chooseLocale(null, Locale.forLanguageTag("fr-FR")),
        )
    }

    @Test
    fun explicitConfigOverridesSystem() {
        assertEquals(
            Locale.forLanguageTag("en-US"),
            AppLocale.chooseLocale("en-US", Locale.forLanguageTag("zh-CN")),
        )
        assertEquals(
            Locale.forLanguageTag("zh-CN"),
            AppLocale.chooseLocale("zh-CN", Locale.forLanguageTag("en-US")),
        )
    }
}
