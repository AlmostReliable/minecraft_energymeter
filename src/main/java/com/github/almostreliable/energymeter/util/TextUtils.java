package com.github.almostreliable.energymeter.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;

import com.almostreliable.energymeter.ModConstants;

import com.github.almostreliable.energymeter.util.TypeEnums.TRANSLATE_TYPE;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public final class TextUtils {

    private static final Locale LOCALE = Locale.getDefault();
    private static final DecimalFormat DF = (DecimalFormat) NumberFormat.getInstance(LOCALE).clone();
    private static final String[] UNITS = {"", "k", "M", "G", "T", "P"};

    private TextUtils() {}

    /**
     * Gets a {@link ResourceLocation} with the given key
     * and the namespace of the mod.
     *
     * @param key the key to generate the {@link ResourceLocation} with
     * @return the generated {@link ResourceLocation}
     */
    public static ResourceLocation getRL(String key) {
        return ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, key);
    }

    /**
     * Gets a translated phrase within the mod's namespace.
     *
     * @param type the translation type to resolve the translation from
     * @param key  the translation key
     * @return the translated phrase
     */
    public static String translateAsString(TRANSLATE_TYPE type, String key) {
        return translate(type, key).getString();
    }

    /**
     * Generates a {@link MutableComponent} within the mod's namespace
     * with a custom type, key and optional color.
     *
     * @param type  the type of the translation
     * @param key   the unique key of the translation
     * @param color an optional color
     * @return the translated phrase
     */
    public static MutableComponent translate(TRANSLATE_TYPE type, String key, ChatFormatting... color) {
        var output = Component.translatable(getTranslationKey(type, key));
        return color.length == 0 ? output : output.withStyle(color[0]);
    }

    /**
     * Colors a given String with the given color.
     *
     * @param input the string to color
     * @param color an optional color
     * @return the colorized string
     */
    public static MutableComponent colorize(String input, ChatFormatting color) {
        return Component.literal(input).withStyle(color);
    }

    /**
     * Formats Forge Energy into a readable String with an easy-to-read suffix.
     * Can also give back the full formatted energy amount if a true boolean is passed in.
     *
     * @param number   the energy amount to format
     * @param extended the boolean to define if full energy amount is used
     * @return the readable string representation of the energy
     */
    public static Tuple<String, String> formatEnergy(Number number, boolean extended) {
        if (!extended) {
            // convert numbers to compact form
            var numberOfDigits = number.intValue() == 0 ? 0 :
                (int) (1 + Math.floor(Math.log10(Math.abs(number.doubleValue()))));
            var base10Exponent = numberOfDigits < 4 ? 0 : 3 * ((numberOfDigits - 1) / 3);
            var normalized = number.doubleValue() / Math.pow(10, base10Exponent);
            return new Tuple<>(formatNumber(normalized, 2), UNITS[base10Exponent / 3] + "FE");
        }
        // normal energy format
        return new Tuple<>(formatNumber(number, 3), "FE");
    }

    /**
     * Gets the translation key from the provided type and key.
     *
     * @param type the type of the translation
     * @param key  the unique key of the translation
     * @return the translation key
     */
    private static String getTranslationKey(TRANSLATE_TYPE type, String key) {
        return String.format("%s.%s.%s", type.toString().toLowerCase(), ModConstants.MOD_ID, key);
    }

    /**
     * Formats numbers with fraction digits into readable strings by
     * adjusting the fraction digit amount.
     *
     * @param input       the number to format
     * @param maxFraction the maximum amount of fraction digits
     * @return the readable string representation of the number
     */
    private static String formatNumber(Number input, int maxFraction) {
        DF.setRoundingMode(RoundingMode.DOWN);
        DF.setMinimumFractionDigits(1);
        DF.setMaximumFractionDigits(maxFraction);
        return DF.format(input);
    }
}
