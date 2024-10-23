package com.github.almostreliable.energymeter.core;

import com.github.almostreliable.energymeter.network.ClientSyncPacket;

@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public enum Constants {
    ;

    // mod info
    public static final String PIPEZ_ID = "pipez";
    public static final String CCT_ID = "computercraft";
    // ids
    public static final String METER_ID = "meter";
    // utility keys
    public static final String IO_STATE_ID = "io";
    public static final String NETWORK_ID = "network";
    // translation & nbt keys
    public static final String SIDE_CONFIG_ID = "side_config";
    public static final String TRANSFER_RATE_ID = "transfer_rate";
    public static final String STATUS_ID = "status";
    public static final String NUMBER_MODE_ID = "number_mode";
    public static final String MODE_ID = "mode";
    public static final String ACCURACY_ID = "accuracy";
    public static final String IO_SIDE_ID = "io_side";
    public static final String IO_MODE_ID = "io_mode";
    public static final String IO_SCREEN_ID = "screen";
    public static final String INTERVAL_ID = "interval";
    public static final String THRESHOLD_ID = "threshold";

    /**
     * Decimal color values to ensure consistent color values.
     */
    public enum UI_COLORS {
        ;

        public static final int WHITE = 15_790_320;
        public static final int MINT = 65_442;
        public static final int GRAY = 11_447_982;
        public static final int GREEN = 65_328;
        public static final int BLUE = 40_929;
        public static final int YELLOW = 16_768_512;
        public static final int ORANGE = 16_737_792;
        public static final int PURPLE = 12_976_383;
        public static final int PINK = 16_711_782;
        public static final int ROSE = 16_711_920;
        public static final int RED = 14_286_889;
    }

    /**
     * Flags to ensure only required data is synced in a {@link ClientSyncPacket}.
     */
    public enum SYNC_FLAGS {
        ;

        public static final int SIDE_CONFIG = 1;
        public static final int TRANSFER_RATE = 1 << 1;
        public static final int NUMBER_MODE = 1 << 2;
        public static final int STATUS = 1 << 3;
        public static final int MODE = 1 << 4;
        public static final int ACCURACY = 1 << 5;
        public static final int INTERVAL = 1 << 6;
        public static final int THRESHOLD = 1 << 7;
    }
}
