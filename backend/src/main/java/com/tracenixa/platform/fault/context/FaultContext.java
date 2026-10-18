package com.tracenixa.platform.fault.context;

import com.tracenixa.platform.fault.model.FaultConfig;

public final class FaultContext {

    private static final ThreadLocal<FaultConfig> ACTIVE_FAULT = new ThreadLocal<>();

    private FaultContext() {}

    public static void setActiveFault(FaultConfig faultConfig) {
        ACTIVE_FAULT.set(faultConfig);
    }

    public static FaultConfig getActiveFault() {
        return ACTIVE_FAULT.get();
    }

    public static void clear() {
        ACTIVE_FAULT.remove();
    }
}
